package com.garfiec.librechat.feature.auth.credentials

import androidx.compose.runtime.Immutable
import com.garfiec.librechat.core.data.datastore.SavedLoginCredentialRef

/**
 * Password-save intent that may need to cross the password-login -> 2FA navigation boundary.
 *
 * This value is deliberately process-memory-only. It must never be placed in saved state,
 * DataStore, Room, navigation arguments, logs, or analytics.
 */
@Immutable
data class PendingCredentialSave(
    val ref: SavedLoginCredentialRef,
    val password: String,
)

/**
 * Short-lived bridge between the login and 2FA ViewModels.
 *
 * Both ViewModels access this singleton from the main thread. Process death intentionally drops
 * the value: preserving the password is less important than keeping it out of durable app state.
 */
class PendingCredentialSaveHandoff {
    private var pending: PendingCredentialSave? = null

    fun stage(value: PendingCredentialSave) {
        pending = value
    }

    fun consume(): PendingCredentialSave? = pending.also { pending = null }

    fun clear() {
        pending = null
    }
}
