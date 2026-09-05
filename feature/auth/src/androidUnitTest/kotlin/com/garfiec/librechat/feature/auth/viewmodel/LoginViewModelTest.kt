package com.garfiec.librechat.feature.auth.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.datastore.SavedLoginCredentialRef
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.repository.AccountSwitcher
import com.garfiec.librechat.core.data.repository.AuthRepository
import com.garfiec.librechat.core.data.repository.ConfigRepository
import com.garfiec.librechat.core.model.LoginOutcome
import com.garfiec.librechat.core.model.User
import com.garfiec.librechat.core.model.config.StartupConfig
import com.garfiec.librechat.feature.auth.credentials.PasswordCredential
import com.garfiec.librechat.feature.auth.credentials.PendingCredentialSaveHandoff
import com.garfiec.librechat.feature.auth.oauth.OAuthLauncher
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val configRepository = mockk<ConfigRepository>(relaxed = true)
    private val oAuthLauncher = mockk<OAuthLauncher>(relaxed = true)
    private val serverDataStore = mockk<ServerDataStore>(relaxed = true)
    private val accountSwitcher = mockk<AccountSwitcher>(relaxed = true)
    private lateinit var credentialSaveHandoff: PendingCredentialSaveHandoff

    private val configFlow = MutableStateFlow<StartupConfig?>(null)

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        credentialSaveHandoff = PendingCredentialSaveHandoff()
        every { configRepository.startupConfig } returns configFlow
        // No add-account flow pending: the VM reads the global config + live server URL.
        every { accountSwitcher.pendingAdd } returns null
        every { serverDataStore.savedLoginCredentials(any()) } returns flowOf(emptyList())
        coEvery { serverDataStore.awaitBaseUrl() } returns "https://chat.example.com"
        every { serverDataStore.getBaseUrl() } returns "https://chat.example.com"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LoginViewModel(
        authRepository = authRepository,
        configRepository = configRepository,
        oAuthLauncher = oAuthLauncher,
        serverDataStore = serverDataStore,
        accountSwitcher = accountSwitcher,
        credentialSaveHandoff = credentialSaveHandoff,
    )

    @Test
    fun `initial state has empty fields`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.email).isEmpty()
        assertThat(state.password).isEmpty()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.isLoggedIn).isFalse()
    }

    @Test
    fun `onEmailChanged updates email and clears error`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")

        assertThat(viewModel.uiState.value.email).isEqualTo("user@example.com")
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `onPasswordChanged updates password and clears error`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPasswordChanged("secret123")

        assertThat(viewModel.uiState.value.password).isEqualTo("secret123")
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `login with blank email shows error`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPasswordChanged("secret123")
        viewModel.login()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Please enter email and password")
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `login with blank password shows error`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.login()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Please enter email and password")
    }

    @Test
    fun `successful login requests a system credential save before completing navigation`() = runTest {
        val user = User(email = "user@example.com", name = "Test User")
        coEvery { authRepository.login("user@example.com", "password123") } returns
            Result.Success(LoginOutcome.Success(user))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.pendingCredentialSave?.ref?.username).isEqualTo("user@example.com")
        assertThat(state.pendingCredentialSave?.password).isEqualTo("password123")
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()

        viewModel.onCredentialSaveHandled(saved = false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoggedIn).isTrue()
        assertThat(viewModel.uiState.value.pendingCredentialSave).isNull()
        assertThat(viewModel.uiState.value.password).isEmpty()
    }

    @Test
    fun `login requiring 2FA sets twoFactorTempToken`() = runTest {
        coEvery { authRepository.login("user@example.com", "password123") } returns
            Result.Success(LoginOutcome.TwoFactorRequired("temp-token-123"))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.twoFactorTempToken).isEqualTo("temp-token-123")
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.password).isEmpty()
        assertThat(credentialSaveHandoff.consume()?.password).isEqualTo("password123")
    }

    @Test
    fun `saved credential selection fills fields without submitting`() = runTest {
        val ref = SavedLoginCredentialRef(
            serverUrl = "https://chat.example.com",
            credentialId = "stored-id",
            username = "user@example.com",
        )
        every { serverDataStore.savedLoginCredentials(any()) } returns flowOf(listOf(ref))
        coEvery { authRepository.login("user@example.com", "password123") } returns
            Result.Success(LoginOutcome.Success(User(email = "user@example.com")))

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCredentialSelected(PasswordCredential(ref.credentialId, "password123"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.pendingCredentialSave).isNull()
        assertThat(state.password).isEqualTo("password123")
        assertThat(state.email).isEqualTo("user@example.com")
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `external password manager entry fills fields without a local reference`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCredentialSelected(
            PasswordCredential("external@example.com", "selected-password"),
        )
        assertThat(viewModel.uiState.value.email).isEqualTo("external@example.com")
        assertThat(viewModel.uiState.value.password).isEqualTo("selected-password")
        assertThat(viewModel.uiState.value.isLoggedIn).isFalse()
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun `selected password manager credentials do not offer another save on login`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            Result.Success(LoginOutcome.Success(User(email = "user@example.com")))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCredentialSelected(PasswordCredential("user@example.com", "secret"))
        viewModel.login()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isLoggedIn).isTrue()
        assertThat(viewModel.uiState.value.pendingCredentialSave).isNull()
    }

    @Test
    fun `editing a selected password offers to save the updated plain username`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            Result.Success(LoginOutcome.Success(User(email = "user@example.com")))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCredentialSelected(PasswordCredential("user@example.com", "secret"))
        viewModel.onPasswordChanged("updated")
        viewModel.login()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.pendingCredentialSave?.ref?.credentialId)
            .isEqualTo("user@example.com")
    }

    @Test
    fun `explicit OAuth launch consumes its result only once`() = runTest {
        every { oAuthLauncher.extractTokenFromCookies(any()) } returns "oauth-result"
        coEvery { authRepository.loginWithOAuthToken("oauth-result") } returns Result.Success(User(email = "user@example.com"))
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.launchOAuth("google")
        viewModel.checkOAuthResult()
        advanceUntilIdle()
        viewModel.checkOAuthResult()
        advanceUntilIdle()
        coVerify(exactly = 1) { authRepository.loginWithOAuthToken("oauth-result") }
        assertThat(viewModel.uiState.value.isLoggedIn).isTrue()
    }

    @Test
    fun `resuming without an OAuth launch does not consume stale cookies`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.checkOAuthResult()
        advanceUntilIdle()
        io.mockk.verify(exactly = 0) { oAuthLauncher.extractTokenFromCookies(any()) }
        coVerify(exactly = 0) { authRepository.loginWithOAuthToken(any()) }
        assertThat(viewModel.uiState.value.isLoggedIn).isFalse()
    }

    @Test
    fun `consumeTwoFactorNavigation clears the temp token`() = runTest {
        coEvery { authRepository.login("user@example.com", "password123") } returns
            Result.Success(LoginOutcome.TwoFactorRequired("temp-token-123"))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.login()
        advanceUntilIdle()

        viewModel.consumeTwoFactorNavigation()

        assertThat(viewModel.uiState.value.twoFactorTempToken).isNull()
    }

    @Test
    fun `login failure shows error message`() = runTest {
        coEvery { authRepository.login("user@example.com", "wrong") } returns
            Result.Error(message = "Invalid credentials")

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.error).isEqualTo("Invalid credentials")
        assertThat(state.isLoading).isFalse()
        assertThat(state.isLoggedIn).isFalse()
    }

    @Test
    fun `login failure with null message uses default`() = runTest {
        coEvery { authRepository.login("user@example.com", "wrong") } returns
            Result.Error(message = null)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("wrong")
        viewModel.login()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isEqualTo("Login failed")
    }

    @Test
    fun `startup config updates registration and social login settings`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        configFlow.value = StartupConfig(
            registrationEnabled = true,
            socialLoginEnabled = true,
            socialLogins = listOf("google", "github"),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.registrationEnabled).isTrue()
        assertThat(state.socialLoginEnabled).isTrue()
        assertThat(state.socialLogins).containsExactly("google", "github")
    }

    @Test
    fun `startup config with null socialLogins defaults to empty`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        configFlow.value = StartupConfig(socialLogins = null)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.socialLogins).isEmpty()
    }

    @Test
    fun `login shows loading state during request`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns
            Result.Success(LoginOutcome.Success(User(email = "user@example.com")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")

        // Before login completes, loading should be false initially
        assertThat(viewModel.uiState.value.isLoading).isFalse()

        viewModel.login()
        advanceUntilIdle()

        // After login completes, loading should be false again
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }
}
