package com.garfiec.librechat.fork.mcp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * What this device last learned of each chat's switch-offs. A chat's detail is read again once per
 * process and whenever its `updatedAt` moved; between those, and offline, this is what the picker shows.
 */
class ConnectorStore(private val dataStore: DataStore<Preferences>) {
    suspend fun read(account: String, chat: String): Set<String>? = decode(dataStore.data.first()[key(account)])[chat]?.toSet()

    suspend fun remember(account: String, chat: String, off: Set<String>) {
        dataStore.edit { prefs ->
            val entries = decode(prefs[key(account)]).toMutableMap()
            entries.remove(chat)
            entries[chat] = off.toList()
            prefs[key(account)] = Json.encodeToString(entries.entries.toList().takeLast(100).associate { it.toPair() })
        }
    }

    private fun key(account: String) = stringPreferencesKey("acct:$account:mcp_off")
    private fun decode(raw: String?): Map<String, List<String>> =
        raw?.let { runCatching { Json.decodeFromString<Map<String, List<String>>>(it) }.getOrNull() }.orEmpty()
}
