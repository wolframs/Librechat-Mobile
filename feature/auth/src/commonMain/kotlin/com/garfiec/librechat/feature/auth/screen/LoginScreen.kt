package com.garfiec.librechat.feature.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garfiec.librechat.feature.auth.credentials.PasswordSaveRequest
import com.garfiec.librechat.feature.auth.credentials.rememberPasswordCredentialManager
import com.garfiec.librechat.feature.auth.resources.*
import com.garfiec.librechat.feature.auth.resources.Res
import com.garfiec.librechat.feature.auth.viewmodel.LoginViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToTwoFactor: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val currentOnNavigateToTwoFactor by rememberUpdatedState(onNavigateToTwoFactor)
    val credentialManager = rememberPasswordCredentialManager()
    val scope = rememberCoroutineScope()
    var isChoosingCredential by remember { mutableStateOf(false) }

    // Check for OAuth result when returning from Chrome Custom Tab
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.checkOAuthResult()
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            currentOnLoginSuccess()
        }
    }

    LaunchedEffect(uiState.pendingCredentialSave) {
        val pending = uiState.pendingCredentialSave ?: return@LaunchedEffect
        val saved = credentialManager?.saveCredential(
            PasswordSaveRequest(
                id = pending.ref.credentialId,
                password = pending.password,
            ),
        ) ?: false
        viewModel.onCredentialSaveHandled(saved)
    }

    LaunchedEffect(uiState.twoFactorTempToken) {
        val tempToken = uiState.twoFactorTempToken
        if (tempToken != null) {
            currentOnNavigateToTwoFactor(tempToken)
            // Consume the signal so backing out of the 2FA screen returns here instead of
            // re-triggering this effect against the retained ViewModel's stale token.
            viewModel.consumeTwoFactorNavigation()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text(stringResource(Res.string.email_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .contentType(ContentType.EmailAddress + ContentType.Username)
                    .testTag("login_email"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false,
                ),
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text(stringResource(Res.string.password_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .contentType(ContentType.Password)
                    .testTag("login_password"),
                singleLine = true,
                visualTransformation = passwordMaskTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                enabled = !uiState.isLoading,
            )

            if (credentialManager != null && uiState.savedCredentials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isChoosingCredential = true
                            val credential = credentialManager.getCredential(
                                uiState.savedCredentials.mapTo(mutableSetOf()) { it.credentialId },
                            )
                            val ref = uiState.savedCredentials.firstOrNull {
                                it.credentialId == credential?.id
                            }
                            if (credential != null && ref != null) {
                                viewModel.loginWithSavedCredential(ref, credential.password)
                            }
                            isChoosingCredential = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("login_saved_credential"),
                    enabled = !uiState.isLoading && !isChoosingCredential,
                ) {
                    if (isChoosingCredential) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(Res.string.use_saved_login))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.password_manager_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("login_error"),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::login,
                modifier = Modifier.fillMaxWidth().testTag("login_submit"),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.continue_button))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onNavigateToForgotPassword) {
                Text(stringResource(Res.string.forgot_password))
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.registrationEnabled) {
                TextButton(onClick = onNavigateToRegister) {
                    Text(stringResource(Res.string.no_account_sign_up))
                }
            }

            val socialLogins = uiState.socialLogins
            if (uiState.socialLoginEnabled && socialLogins.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.or_continue_with),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                socialLogins.forEach { provider ->
                    OutlinedButton(
                        onClick = { viewModel.launchOAuth(provider) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                    ) {
                        Text(oAuthProviderLabel(provider))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        BackAffordanceOverlay(onBack)
    }
}

private fun oAuthProviderLabel(provider: String): String = when (provider.lowercase()) {
    "google" -> "Continue with Google"
    "github" -> "Continue with GitHub"
    "discord" -> "Continue with Discord"
    "facebook" -> "Continue with Facebook"
    "apple" -> "Continue with Apple"
    "openid" -> "Continue with OpenID"
    else -> "Continue with ${provider.replaceFirstChar { it.uppercase() }}"
}
