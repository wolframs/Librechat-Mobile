package com.garfiec.librechat.fork.login

/** Process-memory only: never put this value in navigation, saved state, logs or storage. */
class PendingCredentialSave(val ref: SavedLoginCredentialRef, val password: String)

class PendingCredentialSaveHandoff {
    private var token: String? = null
    private var pending: PendingCredentialSave? = null

    fun stage(token: String, value: PendingCredentialSave?) {
        this.token = token
        pending = value
    }

    fun consume(token: String?): PendingCredentialSave? {
        val result = pending.takeIf { this.token == token }
        clear()
        return result
    }

    fun clear() {
        token = null
        pending = null
    }
}

internal fun pendingSave(server: String, username: String, password: String, picked: PasswordCredential?): PendingCredentialSave? {
    val name = username.trim()
    if (server.isBlank() || name.isBlank() || password.isBlank()) return null
    if (picked?.id == name && picked.password == password) return null
    return PendingCredentialSave(SavedLoginCredentialRef(server, name, name), password)
}
