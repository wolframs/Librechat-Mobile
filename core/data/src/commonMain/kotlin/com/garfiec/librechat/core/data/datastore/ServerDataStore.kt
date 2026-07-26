package com.garfiec.librechat.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.garfiec.librechat.core.network.client.ServerUrlProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * Optional fallback for reading/writing the server URL in a location
 * that survives app reinstall (e.g. iOS Keychain). On platforms without
 * a fallback (Android), pass null.
 */
interface ServerUrlKeychainFallback {
    fun readServerUrl(): String?
    fun writeServerUrl(url: String)
    fun removeServerUrl()
}

/**
 * Non-secret pointer to a password stored by the platform credential provider.
 *
 * The password itself never enters DataStore. The identifier is deliberately server-qualified so
 * two LibreChat servers using the same email address remain distinct in the password manager.
 */
@Serializable
data class SavedLoginCredentialRef(
    val serverUrl: String,
    val credentialId: String,
    val username: String,
)

class ServerDataStore(
    private val dataStore: DataStore<Preferences>,
    appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val keychainFallback: ServerUrlKeychainFallback? = null,
) : ServerUrlProvider {

    private val _currentUrl = MutableStateFlow("")
    val currentUrlFlow: Flow<String> = _currentUrl

    // Set once an explicit setServerUrl/clearServerUrl has run, so the async warm-up below
    // never clobbers a caller's value with the stale persisted read if they raced. Guarded
    // together with [_currentUrl] by [urlMutex] so the warm-up's check-then-set is atomic
    // against a concurrent set/clear (a plain @Volatile only guarantees visibility).
    @Volatile
    private var urlExplicitlySet = false
    private val urlMutex = Mutex()

    // Completes once the async warm-up has resolved (success, failure, or cancellation), so
    // [awaitBaseUrl] callers on the startup path never observe the empty pre-warm-up value.
    private val warmedUp = CompletableDeferred<Unit>()

    init {
        // Warm up the persisted URL asynchronously off the Main thread. Koin instantiates
        // this singleton on Main at startup, so the read must not block. getBaseUrl() reads
        // are lazy (per HTTP request / inside viewModelScope launches) and only happen after
        // the first frame, by which point this warm-up has resolved.
        appScope.launch(ioDispatcher) {
            try {
                var url = dataStore.data
                    .map { prefs -> prefs[KEY_SERVER_URL].orEmpty() }
                    .first()
                // If DataStore is empty (e.g. after reinstall), try the Keychain fallback.
                // Keychain items persist across iOS app reinstalls.
                var restoredFromKeychain = false
                if (url.isBlank() && keychainFallback != null) {
                    val restored = keychainFallback.readServerUrl()
                    if (!restored.isNullOrBlank()) {
                        url = restored
                        restoredFromKeychain = true
                    }
                }
                // Don't overwrite (or re-persist) a URL a caller set/cleared while the warm-up
                // was in flight. The keychain re-persist must also sit under the lock + flag,
                // otherwise a clearServerUrl() that raced the read could be resurrected on disk.
                urlMutex.withLock {
                    if (!urlExplicitlySet) {
                        _currentUrl.value = url
                        // Re-persist the keychain-restored URL so subsequent reads skip the fallback.
                        if (restoredFromKeychain) {
                            dataStore.edit { prefs -> prefs[KEY_SERVER_URL] = url }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Leave the URL empty; onboarding will prompt for a server URL.
            } finally {
                warmedUp.complete(Unit)
            }
        }
    }

    override fun getBaseUrl(): String = _currentUrl.value

    override suspend fun awaitBaseUrl(): String {
        warmedUp.await()
        return _currentUrl.value
    }

    fun hasServerUrl(): Flow<Boolean> =
        dataStore.data.map { prefs ->
            !prefs[KEY_SERVER_URL].isNullOrBlank()
        }

    /** All successfully validated servers, including the current pre-migration server URL. */
    val rememberedServers: Flow<List<String>> =
        dataStore.data.map { prefs ->
            val current = prefs[KEY_SERVER_URL]?.takeIf(String::isNotBlank)
            listOfNotNull(current) +
                prefs[KEY_REMEMBERED_SERVERS].orEmpty()
                    .filterNot { it == current }
                    .sorted()
        }

    val httpWarningSuppressedServers: Flow<Set<String>> =
        dataStore.data.map { prefs -> prefs[KEY_HTTP_WARNING_SUPPRESSED].orEmpty() }

    suspend fun setServerUrl(url: String) {
        val trimmed = normalizeUrl(url)
        urlMutex.withLock {
            urlExplicitlySet = true
            _currentUrl.value = trimmed
        }
        dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = trimmed
        }
        // Mirror to Keychain so the URL survives app reinstall on iOS
        keychainFallback?.writeServerUrl(trimmed)
    }

    /**
     * Clear the server URL from both DataStore and Keychain fallback.
     */
    suspend fun clearServerUrl() {
        urlMutex.withLock {
            urlExplicitlySet = true
            _currentUrl.value = ""
        }
        dataStore.edit { prefs -> prefs.remove(KEY_SERVER_URL) }
        keychainFallback?.removeServerUrl()
    }

    suspend fun rememberServer(url: String) {
        val normalized = normalizeUrl(url)
        if (normalized.isBlank()) return
        dataStore.edit { prefs ->
            prefs[KEY_REMEMBERED_SERVERS] =
                prefs[KEY_REMEMBERED_SERVERS].orEmpty() + normalized
        }
    }

    /**
     * Removes the remembered profile, its HTTP-warning decision, and its non-secret credential
     * pointers. The platform password manager remains the authority for deleting saved passwords.
     */
    suspend fun forgetServer(url: String) {
        val normalized = normalizeUrl(url)
        dataStore.edit { prefs ->
            prefs[KEY_REMEMBERED_SERVERS] =
                prefs[KEY_REMEMBERED_SERVERS].orEmpty() - normalized
            prefs[KEY_HTTP_WARNING_SUPPRESSED] =
                prefs[KEY_HTTP_WARNING_SUPPRESSED].orEmpty() - normalized
            prefs[KEY_SAVED_LOGIN_REFS] = encodeLoginRefs(
                decodeLoginRefs(prefs[KEY_SAVED_LOGIN_REFS])
                    .filterNot { it.serverUrl == normalized },
            )
        }
    }

    suspend fun isHttpWarningSuppressed(url: String): Boolean {
        val normalized = normalizeUrl(url)
        return dataStore.data
            .map { normalized in it[KEY_HTTP_WARNING_SUPPRESSED].orEmpty() }
            .first()
    }

    suspend fun setHttpWarningSuppressed(url: String, suppressed: Boolean) {
        val normalized = normalizeUrl(url)
        dataStore.edit { prefs ->
            val current = prefs[KEY_HTTP_WARNING_SUPPRESSED].orEmpty()
            prefs[KEY_HTTP_WARNING_SUPPRESSED] =
                if (suppressed) current + normalized else current - normalized
        }
    }

    fun savedLoginCredentials(serverUrl: String): Flow<List<SavedLoginCredentialRef>> {
        val normalized = normalizeUrl(serverUrl)
        return dataStore.data.map { prefs ->
            decodeLoginRefs(prefs[KEY_SAVED_LOGIN_REFS])
                .filter { it.serverUrl == normalized }
                .sortedBy { it.username.lowercase() }
        }
    }

    suspend fun rememberLoginCredential(ref: SavedLoginCredentialRef) {
        val normalizedRef = ref.copy(serverUrl = normalizeUrl(ref.serverUrl))
        dataStore.edit { prefs ->
            val withoutSameCredential = decodeLoginRefs(prefs[KEY_SAVED_LOGIN_REFS])
                .filterNot {
                    it.serverUrl == normalizedRef.serverUrl &&
                        (it.credentialId == normalizedRef.credentialId ||
                            it.username == normalizedRef.username)
                }
            prefs[KEY_SAVED_LOGIN_REFS] = encodeLoginRefs(withoutSameCredential + normalizedRef)
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_REMEMBERED_SERVERS = stringSetPreferencesKey("remembered_server_urls")
        private val KEY_HTTP_WARNING_SUPPRESSED =
            stringSetPreferencesKey("http_warning_suppressed_server_urls")
        private val KEY_SAVED_LOGIN_REFS = stringPreferencesKey("saved_login_credential_refs")

        private fun normalizeUrl(url: String): String = url.trim().trimEnd('/')

        private fun decodeLoginRefs(value: String?): List<SavedLoginCredentialRef> =
            value
                ?.takeIf(String::isNotBlank)
                ?.let { encoded ->
                    runCatching {
                        json.decodeFromString<List<SavedLoginCredentialRef>>(encoded)
                    }.getOrDefault(emptyList())
                }
                .orEmpty()

        private fun encodeLoginRefs(refs: List<SavedLoginCredentialRef>): String =
            json.encodeToString(refs)
    }
}
