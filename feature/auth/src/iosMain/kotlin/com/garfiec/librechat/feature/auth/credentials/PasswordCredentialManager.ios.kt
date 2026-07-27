package com.garfiec.librechat.feature.auth.credentials

import androidx.compose.runtime.Composable

/**
 * iOS Password AutoFill is field-driven, not an app-facing credential CRUD API. The login,
 * registration, and reset fields declare Username/Password/NewPassword content types, allowing the
 * system keyboard and the user's configured password provider to offer and save credentials.
 *
 * Returning null disables only LibreChat's Android-style explicit "Use saved login" button. An
 * AuthenticationServices credential-identity store is not an equivalent: it is for credential
 * provider extensions and contains identities, not passwords. The app also connects to arbitrary
 * runtime server domains, so it cannot ship a fixed Associated Domains entitlement for them.
 */
@Composable
actual fun rememberPasswordCredentialManager(): PasswordCredentialManager? = null
