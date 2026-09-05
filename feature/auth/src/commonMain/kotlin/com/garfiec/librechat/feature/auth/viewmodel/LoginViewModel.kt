package com.garfiec.librechat.feature.auth.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garfiec.librechat.core.common.result.ApiException
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.SavedLoginCredentialRef
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.repository.AccountSwitcher
import com.garfiec.librechat.core.data.repository.AuthRepository
import com.garfiec.librechat.core.data.repository.ConfigRepository
import com.garfiec.librechat.core.model.LoginOutcome
import com.garfiec.librechat.feature.auth.credentials.PasswordCredential
import com.garfiec.librechat.feature.auth.credentials.PendingCredentialSave
import com.garfiec.librechat.feature.auth.credentials.PendingCredentialSaveHandoff
import com.garfiec.librechat.feature.auth.oauth.OAuthLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val twoFactorTempToken: String? = null,
    val registrationEnabled: Boolean = false,
    val socialLoginEnabled: Boolean = false,
    val socialLogins: List<String> = emptyList(),
    val savedCredentials: List<SavedLoginCredentialRef> = emptyList(),
    val pendingCredentialSave: PendingCredentialSave? = null,
    // ALLOW_EMAIL_LOGIN (upstream #14180): when the server disables email/password login it now
    // enforces it with a 403 on POST /api/auth/login. Fail-open to true so the form shows until
    // config confirms otherwise. Drives hiding the email/password form.
    val emailLoginEnabled: Boolean = true,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val configRepository: ConfigRepository,
    private val oAuthLauncher: OAuthLauncher,
    private val serverDataStore: ServerDataStore,
    private val accountSwitcher: AccountSwitcher,
    private val credentialSaveHandoff: PendingCredentialSaveHandoff,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // In an add-account flow this screen signs into the PENDING server while another account is
        // live, so its feature flags (registration, social logins) must come from the pending
        // session's probed config — the global startupConfig still describes the live server. The
        // repository layer routes the sign-in calls themselves via the same pending session.
        val configSource = accountSwitcher.pendingAdd?.startupConfig ?: configRepository.startupConfig
        viewModelScope.launch {
            configSource.collect { config ->
                if (config != null) {
                    _uiState.value = _uiState.value.copy(
                        registrationEnabled = config.registrationEnabled,
                        socialLoginEnabled = config.socialLoginEnabled,
                        socialLogins = config.socialLogins.orEmpty(),
                        emailLoginEnabled = config.emailLoginEnabled,
                    )
                }
            }
        }
        viewModelScope.launch {
            val serverUrl = accountSwitcher.pendingAdd?.serverUrl ?: serverDataStore.awaitBaseUrl()
            if (serverUrl.isNotBlank()) {
                serverDataStore.savedLoginCredentials(serverUrl).collect { credentials ->
                    _uiState.value = _uiState.value.copy(savedCredentials = credentials)
                }
            }
        }
    }

    /** The server this screen is signing into: the pending add target when set, else the live one. */
    private fun signInServerUrl(): String =
        accountSwitcher.pendingAdd?.serverUrl ?: serverDataStore.getBaseUrl()

    private var selectedPasswordCredential = false

    fun onEmailChanged(email: String) {
        if (email != _uiState.value.email) selectedPasswordCredential = false
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        if (password != _uiState.value.password) selectedPasswordCredential = false
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        login(offerCredentialSave = !selectedPasswordCredential)
    }

    private fun login(offerCredentialSave: Boolean) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please enter email and password")
            return
        }

        // A new attempt supersedes any abandoned 2FA handoff from this process.
        credentialSaveHandoff.clear()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val serverUrl = signInServerUrl()

            when (val result = authRepository.login(state.email, state.password)) {
                is Result.Success -> {
                    when (val outcome = result.data) {
                        is LoginOutcome.Success -> {
                            val pendingSave = if (offerCredentialSave) {
                                PendingCredentialSave(
                                    ref = SavedLoginCredentialRef(
                                        serverUrl = serverUrl,
                                        credentialId = state.email.trim(),
                                        username = state.email.trim(),
                                    ),
                                    password = state.password,
                                )
                            } else {
                                null
                            }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                password = if (pendingSave == null) "" else state.password,
                                pendingCredentialSave = pendingSave,
                                isLoggedIn = pendingSave == null,
                            )
                        }
                        is LoginOutcome.TwoFactorRequired -> {
                            if (offerCredentialSave) {
                                credentialSaveHandoff.stage(
                                    PendingCredentialSave(
                                        ref = SavedLoginCredentialRef(
                                            serverUrl = serverUrl,
                                            credentialId = state.email.trim(),
                                            username = state.email.trim(),
                                        ),
                                        password = state.password,
                                    ),
                                )
                            }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                // The only remaining plaintext copy now lives in the short-lived,
                                // process-memory handoff and is cleared on success or abandonment.
                                password = "",
                                twoFactorTempToken = outcome.tempToken,
                            )
                        }
                    }
                }
                is Result.Error -> {
                    credentialSaveHandoff.clear()
                    // A 403 from /api/auth/login means the server enforces ALLOW_EMAIL_LOGIN=false
                    // (#14180). Surface a clear reason and hide the form so the user reaches for a
                    // provider instead of retrying credentials that will never be accepted.
                    // checkBan runs BEFORE validateEmailLogin on this route and also answers 403,
                    // so a banned account (or the non-browser-UA soft ban) must keep the server's
                    // own message and leave the form visible — isBanned is the discriminator.
                    val apiException = result.exception as? ApiException
                    val isEmailLoginDisabled =
                        apiException?.statusCode == 403 && !apiException.isBanned
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (isEmailLoginDisabled) {
                            "Email and password sign-in is disabled on this server."
                        } else {
                            result.message ?: "Login failed"
                        },
                        emailLoginEnabled = if (isEmailLoginDisabled) false else _uiState.value.emailLoginEnabled,
                    )
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }

    fun onCredentialSelected(credential: PasswordCredential) {
        selectedPasswordCredential = true
        val ref = _uiState.value.savedCredentials.firstOrNull { it.credentialId == credential.id }
        _uiState.value = _uiState.value.copy(
            email = ref?.username ?: credential.id,
            password = credential.password,
            error = null,
        )
        // Review the selected account and server before submitting. Providers may return entries
        // created outside this app, which have no local saved-credential reference.
    }

    fun onCredentialSaveHandled(saved: Boolean) {
        val pending = _uiState.value.pendingCredentialSave ?: return
        viewModelScope.launch {
            if (saved) {
                try {
                    serverDataStore.rememberLoginCredential(pending.ref)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Credential-provider success already completed the login; a non-secret
                    // DataStore pointer failure must not strand the user on this screen.
                }
            }
            _uiState.value = _uiState.value.copy(
                password = "",
                pendingCredentialSave = null,
                isLoggedIn = true,
            )
        }
    }

    fun consumeTwoFactorNavigation() {
        _uiState.value = _uiState.value.copy(twoFactorTempToken = null)
    }

    /** Set once this screen launches its own OAuth round-trip; gates cookie consumption on resume. */
    private var oAuthLaunched = false

    fun launchOAuth(provider: String) {
        oAuthLaunched = true
        val serverUrl = signInServerUrl()
        // Drop any stale refreshToken cookie for this host BEFORE launching. In add mode the cookie
        // jar is process-global and nothing clears it on add-flow entry, so a launch that the user then
        // cancels would otherwise leave a pre-existing cookie for checkOAuthResult() to consume as the
        // wrong user (the oAuthLaunched guard only blocks the never-launched case). Clearing here means
        // only a cookie minted by THIS round-trip can be present on return.
        oAuthLauncher.clearOAuthCookie(serverUrl)
        oAuthLauncher.launchOAuth(provider, serverUrl)
    }

    fun checkOAuthResult() {
        // Returning from Credential Manager also resumes this screen. Only an OAuth flow
        // explicitly started here may consume the browser cookie and establish a session.
        if (!oAuthLaunched) return

        val serverUrl = signInServerUrl()
        if (serverUrl.isBlank()) return

        val refreshToken = oAuthLauncher.extractTokenFromCookies(serverUrl) ?: return

        oAuthLaunched = false
        // Clear the cookie immediately to avoid re-reading on next onResume
        oAuthLauncher.clearOAuthCookie(serverUrl)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = authRepository.loginWithOAuthToken(refreshToken)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "OAuth login failed",
                    )
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }
}
