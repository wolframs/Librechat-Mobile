package com.garfiec.librechat.feature.auth.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.repository.AccountSwitcher
import com.garfiec.librechat.core.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class ServerUrlUiState(
    val url: String = "",
    val rememberedServers: List<String> = emptyList(),
    val httpWarningSuppressedServers: Set<String> = emptySet(),
    val canForgetServers: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isValidated: Boolean = false,
    val showHttpWarning: Boolean = false,
    val suppressHttpWarning: Boolean = false,
)

/**
 * Validates a server URL and selects it for the sign-in flow. Two modes:
 *
 * - **Normal** (`addAccount = false`): the pre-login screen. Sets the process-global server URL and
 *   validates + caches the config through the live pipeline.
 * - **Add-account** (`addAccount = true`): reached from the account switcher while another account
 *   is live. Must never touch the live account's state: the URL goes into a pending add session
 *   ([AccountSwitcher.beginAdd]) instead of the global store, and validation runs under the pending
 *   identity without publishing to the live server's config state/cache
 *   ([ConfigRepository.probeServerUrl]). The validated config rides on the pending session for the
 *   add-mode login screen.
 */
class ServerUrlViewModel(
    private val serverDataStore: ServerDataStore,
    private val configRepository: ConfigRepository,
    private val accountSwitcher: AccountSwitcher,
    private val addAccount: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerUrlUiState(canForgetServers = !addAccount))
    val uiState: StateFlow<ServerUrlUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // awaitBaseUrl (not getBaseUrl) so a cold-start relaunch doesn't read "" before
            // ServerDataStore's async warm-up resolves, which would skip pre-filling the saved URL.
            // In add mode this pre-fills the ACTIVE server so the common same-server-different-user
            // add is a single tap; typing a different URL adds a new server.
            val existingUrl = serverDataStore.awaitBaseUrl()
            if (existingUrl.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    url = existingUrl,
                )
            }
        }
        viewModelScope.launch {
            serverDataStore.rememberedServers.collect { servers ->
                _uiState.value = _uiState.value.copy(rememberedServers = servers)
            }
        }
        viewModelScope.launch {
            serverDataStore.httpWarningSuppressedServers.collect { servers ->
                _uiState.value = _uiState.value.copy(httpWarningSuppressedServers = servers)
            }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun selectServer(url: String) {
        _uiState.value = _uiState.value.copy(url = url, error = null)
    }

    fun forgetServer(url: String) {
        if (addAccount) return
        viewModelScope.launch {
            val currentUrl = serverDataStore.awaitBaseUrl()
            if (normalizeUrl(currentUrl) == normalizeUrl(url)) {
                serverDataStore.clearServerUrl()
                if (normalizeUrl(_uiState.value.url) == normalizeUrl(url)) {
                    _uiState.value = _uiState.value.copy(url = "")
                }
            }
            serverDataStore.forgetServer(url)
        }
    }

    fun restoreHttpWarning(url: String) {
        viewModelScope.launch {
            serverDataStore.setHttpWarningSuppressed(url, suppressed = false)
        }
    }

    fun validateAndConnect() {
        val url = normalizeUrl(_uiState.value.url)
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a server URL")
            return
        }

        // Show warning dialog if user enters an HTTP URL
        // Auto-add https:// if no scheme provided
        val normalizedUrl = if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            "https://$url"
        } else {
            url
        }

        if (normalizedUrl.startsWith("http://", ignoreCase = true)) {
            viewModelScope.launch {
                if (serverDataStore.isHttpWarningSuppressed(normalizedUrl)) {
                    doValidateAndConnect(normalizedUrl)
                } else {
                    _uiState.value = _uiState.value.copy(
                        showHttpWarning = true,
                        suppressHttpWarning = false,
                    )
                }
            }
        } else {
            doValidateAndConnect(normalizedUrl)
        }
    }

    fun confirmHttpConnection() {
        _uiState.value = _uiState.value.copy(showHttpWarning = false)
        val url = normalizeUrl(_uiState.value.url)
        doValidateAndConnect(url, suppressHttpWarningOnSuccess = _uiState.value.suppressHttpWarning)
    }

    fun dismissHttpWarning() {
        _uiState.value = _uiState.value.copy(
            showHttpWarning = false,
            suppressHttpWarning = false,
        )
    }

    fun setSuppressHttpWarning(suppress: Boolean) {
        _uiState.value = _uiState.value.copy(suppressHttpWarning = suppress)
    }

    private fun doValidateAndConnect(
        url: String,
        suppressHttpWarningOnSuccess: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = if (addAccount) validatePendingServer(url) else validateLiveServer(url)

            when (result) {
                is Result.Success -> {
                    serverDataStore.rememberServer(url)
                    if (suppressHttpWarningOnSuccess) {
                        serverDataStore.setHttpWarningSuppressed(url, suppressed = true)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isValidated = true,
                        suppressHttpWarning = false,
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Could not connect to server",
                    )
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }

    private suspend fun validateLiveServer(url: String): Result<*> {
        val previousUrl = serverDataStore.awaitBaseUrl()
        // Set the URL first so API calls use it
        serverDataStore.setServerUrl(url)
        val result = configRepository.validateServerUrl(url)
        if (result is Result.Error) {
            if (previousUrl.isBlank()) {
                serverDataStore.clearServerUrl()
            } else {
                serverDataStore.setServerUrl(previousUrl)
            }
        }
        return result
    }

    private suspend fun validatePendingServer(url: String): Result<*> {
        // beginAdd requires a resolved active account; the switcher only offers "add" while one is
        // live, but a logout/expiry racing the tap must surface as an error, not a crash.
        val begun = runCatching { accountSwitcher.beginAdd(url) }
        if (begun.isFailure) {
            return Result.Error(
                begun.exceptionOrNull(),
                "Could not start adding an account. Try again.",
            )
        }
        val result = accountSwitcher.withPendingIdentity { configRepository.probeServerUrl() }
        when (result) {
            is Result.Success -> accountSwitcher.attachPendingConfig(result.data)
            // Drop the pending session so an abandoned attempt leaves no staged state; a retry
            // begins a fresh one.
            is Result.Error -> accountSwitcher.cancelAdd()
            is Result.Loading -> Unit
        }
        return result
    }

    private fun normalizeUrl(url: String): String = url.trim().trimEnd('/')
}
