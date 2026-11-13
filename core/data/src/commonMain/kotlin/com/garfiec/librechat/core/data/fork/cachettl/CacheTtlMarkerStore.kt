package com.garfiec.librechat.core.data.fork.cachettl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.common.identity.flatMapAccountOrEmpty
import com.garfiec.librechat.core.common.result.suspendRunCatching
import com.garfiec.librechat.core.model.Message
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class CacheTtlMarker(val messageId: String, val ttl: String, val anchorMillis: Long)

/** Sidecar to Room: all branches retain their markers; logout's acct: sweep removes them. */
class CacheTtlMarkerStore(private val dataStore: DataStore<Preferences>) {
    fun observe(accounts: ActiveAccountProvider) = accounts.flatMapAccountOrEmpty(emptyList<CacheTtlMarker>()) { account ->
        dataStore.data.map { decode(it[key(account.value)]) }
    }

    suspend fun remember(accountId: String, messages: List<Message>) {
        val candidates = messages.filter { !it.isCreatedByUser && !it.error && it.cacheTTL in setOf("5m", "1h") }
        if (candidates.isEmpty()) return
        suspendRunCatching {
            dataStore.edit { prefs ->
                val previous = decode(prefs[key(accountId)]).associateBy { it.messageId }
                val now = Clock.System.now().toEpochMilliseconds()
                val incoming = candidates.map { message ->
                    val created = message.createdAt?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
                    CacheTtlMarker(message.messageId, message.cacheTTL!!,
                        previous[message.messageId]?.anchorMillis ?: created?.takeIf { it <= now } ?: now)
                }
                val merged = (previous.values + incoming).associateBy { it.messageId }.values
                    .sortedByDescending { it.anchorMillis }.take(2000)
                prefs[key(accountId)] = Json.encodeToString(merged)
            }
        }
    }

    private fun key(account: String) = stringPreferencesKey("acct:$account:cache_ttl_markers")
    private fun decode(raw: String?): List<CacheTtlMarker> =
        raw?.let { runCatching { Json.decodeFromString<List<CacheTtlMarker>>(it) }.getOrNull() }.orEmpty()
}
