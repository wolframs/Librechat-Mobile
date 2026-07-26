package com.garfiec.librechat.feature.auth.credentials

import androidx.compose.runtime.Composable

/**
 * Compose Multiplatform does not yet bridge these autofill semantics to iOS Password AutoFill.
 * The existing Keychain-backed session tokens remain available; password persistence is therefore
 * intentionally Android-only until an AuthenticationServices-backed implementation is added.
 */
@Composable
actual fun rememberPasswordCredentialManager(): PasswordCredentialManager? = null
