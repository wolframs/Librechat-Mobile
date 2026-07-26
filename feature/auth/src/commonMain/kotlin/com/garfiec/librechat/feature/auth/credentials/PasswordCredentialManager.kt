package com.garfiec.librechat.feature.auth.credentials

import androidx.compose.runtime.Composable

data class PasswordCredential(
    val id: String,
    val password: String,
)

data class PasswordSaveRequest(
    val id: String,
    val password: String,
)

/**
 * Platform password-manager bridge. Passwords only cross this boundary in memory and are never
 * written to the app's DataStore or Room database.
 */
interface PasswordCredentialManager {
    suspend fun getCredential(allowedIds: Set<String>): PasswordCredential?

    /** Returns true only when the platform provider confirms that it stored the credential. */
    suspend fun saveCredential(request: PasswordSaveRequest): Boolean
}

@Composable
expect fun rememberPasswordCredentialManager(): PasswordCredentialManager?
