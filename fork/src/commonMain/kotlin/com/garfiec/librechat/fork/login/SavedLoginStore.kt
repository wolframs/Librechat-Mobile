package com.garfiec.librechat.fork.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SavedLoginCredentialRef(val serverUrl: String, val credentialId: String, val username: String)

internal fun normalizedServer(url: String): String = url.trim().trimEnd('/')

internal fun rememberedServerList(remembered: Set<String>, known: List<String>): List<String> {
    val excluded = known.map(::normalizedServer).toSet()
    return remembered.map(::normalizedServer).filter { it.isNotBlank() && it !in excluded }.distinct().sorted()
}

internal fun credentialUsername(id: String, refs: List<SavedLoginCredentialRef>): String =
    refs.firstOrNull { it.credentialId == id }?.username ?: id

/** Only non-secret server addresses and provider references belong in this store. */
class SavedLoginStore(private val dataStore: DataStore<Preferences>) {
    private val serversKey = stringSetPreferencesKey("remembered_server_urls")
    private val refsKey = stringPreferencesKey("saved_login_credential_refs")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val rememberedServers = dataStore.data.map { it[serversKey].orEmpty() }

    private fun decode(value: String?): List<SavedLoginCredentialRef> =
        value?.let { runCatching { json.decodeFromString<List<SavedLoginCredentialRef>>(it) }.getOrDefault(emptyList()) }.orEmpty()

    suspend fun refs(server: String): List<SavedLoginCredentialRef> =
        decode(dataStore.data.first()[refsKey]).filter { normalizedServer(it.serverUrl) == normalizedServer(server) }

    suspend fun rememberServer(server: String) {
        val url = normalizedServer(server)
        if (url.isBlank()) return
        dataStore.edit { it[serversKey] = it[serversKey].orEmpty() + url }
    }

    suspend fun forgetServer(server: String) {
        val url = normalizedServer(server)
        dataStore.edit { prefs ->
            prefs[serversKey] = prefs[serversKey].orEmpty().filterNot { normalizedServer(it) == url }.toSet()
            prefs[refsKey] = json.encodeToString(decode(prefs[refsKey]).filterNot { normalizedServer(it.serverUrl) == url })
        }
    }

    suspend fun rememberCredential(ref: SavedLoginCredentialRef) {
        dataStore.edit { prefs ->
            val rest = decode(prefs[refsKey]).filterNot {
                normalizedServer(it.serverUrl) == normalizedServer(ref.serverUrl) &&
                    (it.username == ref.username || it.credentialId == ref.credentialId)
            }
            prefs[refsKey] = json.encodeToString(rest + ref.copy(serverUrl = normalizedServer(ref.serverUrl)))
        }
    }
}
