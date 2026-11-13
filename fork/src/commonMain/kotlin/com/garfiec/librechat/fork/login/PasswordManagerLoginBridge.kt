package com.garfiec.librechat.fork.login

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.repository.AccountSwitcher
import kotlinx.coroutines.CancellationException
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class PasswordManagerLoginBridge(
    private val store: SavedLoginStore,
    private val servers: ServerDataStore,
    switcher: AccountSwitcher,
    private val handoff: PendingCredentialSaveHandoff,
) : ViewModel() {
    private val pendingServer = switcher.pendingAdd?.serverUrl
    private var picked: PasswordCredential? = null
    private suspend fun server(): String = pendingServer ?: servers.awaitBaseUrl()

    fun editUsername(value: String, update: (String) -> Unit) {
        if (value != picked?.id) picked = null
        update(value)
    }

    fun editPassword(value: String, update: (String) -> Unit) {
        if (value != picked?.password) picked = null
        update(value)
    }

    suspend fun pick(manager: PasswordCredentialManager): PasswordCredential? {
        val refs = store.refs(server())
        // An unrestricted picker also accepts entries created outside this app and legacy IDs.
        val credential = manager.getCredential(emptySet()) ?: return null
        return credential.copy(id = credentialUsername(credential.id, refs)).also { picked = it }
    }

    suspend fun saveThen(manager: PasswordCredentialManager?, username: String, password: String, next: () -> Unit) {
        val pending = pendingSave(server(), username, password, picked)
        picked = null
        savePending(manager, store, pending)
        next()
    }

    suspend fun stageForTwoFactor(token: String, username: String, password: String) {
        handoff.stage(token, pendingSave(server(), username, password, picked))
        picked = null
    }

    fun discard() {
        picked = null
        handoff.clear()
    }
}

internal suspend fun savePending(manager: PasswordCredentialManager?, store: SavedLoginStore, pending: PendingCredentialSave?) {
    if (manager == null || pending == null) return
    if (manager.saveCredential(PasswordSaveRequest(pending.ref.credentialId, pending.password))) {
        try {
            store.rememberCredential(pending.ref)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A reference write must not prevent entry into an authenticated session.
        }
    }
}

class TwoFactorSaveBridge(handoff: PendingCredentialSaveHandoff, token: String?, private val store: SavedLoginStore) : ViewModel() {
    private var pending = handoff.consume(token)

    suspend fun complete(manager: PasswordCredentialManager?, next: () -> Unit) {
        val value = pending
        pending = null
        savePending(manager, store, value)
        next()
    }

    override fun onCleared() {
        pending = null
    }
}

@Composable
fun rememberLoginBridge(): PasswordManagerLoginBridge = koinViewModel()

@Composable
fun rememberTwoFactorSaveBridge(token: String?): TwoFactorSaveBridge =
    koinViewModel { parametersOf(token) }
