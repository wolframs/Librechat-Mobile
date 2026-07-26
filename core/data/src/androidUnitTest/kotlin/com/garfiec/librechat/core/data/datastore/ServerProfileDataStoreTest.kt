package com.garfiec.librechat.core.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ServerProfileDataStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    private fun createStore(name: String): ServerDataStore {
        val dataStore = PreferenceDataStoreFactory.create {
            File(tmpFolder.root, "$name.preferences_pb")
        }
        return ServerDataStore(
            dataStore = dataStore,
            appScope = CoroutineScope(dispatcher),
            ioDispatcher = dispatcher,
        )
    }

    @Test
    fun `remembered servers and warning decisions are scoped by normalized URL`() =
        runTest(dispatcher) {
            val store = createStore("profiles")

            store.rememberServer("http://one.example.com/")
            store.rememberServer("https://two.example.com")
            store.setHttpWarningSuppressed("http://one.example.com/", suppressed = true)

            assertThat(store.rememberedServers.first()).containsExactly(
                "http://one.example.com",
                "https://two.example.com",
            )
            assertThat(store.isHttpWarningSuppressed("http://one.example.com")).isTrue()
            assertThat(store.isHttpWarningSuppressed("https://two.example.com")).isFalse()
        }

    @Test
    fun `credential registry persists pointers but never password material`() =
        runTest(dispatcher) {
            val store = createStore("credentials")
            val ref = SavedLoginCredentialRef(
                serverUrl = "https://chat.example.com/",
                credentialId = "user@example.com · chat.example.com",
                username = "user@example.com",
            )

            store.rememberLoginCredential(ref)

            assertThat(store.savedLoginCredentials("https://chat.example.com").first())
                .containsExactly(ref.copy(serverUrl = "https://chat.example.com"))
        }

    @Test
    fun `new credential identity replaces the legacy pointer for the same server username`() =
        runTest(dispatcher) {
            val store = createStore("credential-id-migration")
            val legacy = SavedLoginCredentialRef(
                serverUrl = "https://chat.example.com",
                credentialId = "user@example.com · chat.example.com",
                username = "user@example.com",
            )
            val canonical = legacy.copy(
                credentialId = "user@example.com · chat.example.com · 0123456789abcdef",
            )

            store.rememberLoginCredential(legacy)
            store.rememberLoginCredential(canonical)

            assertThat(store.savedLoginCredentials(legacy.serverUrl).first())
                .containsExactly(canonical)
        }

    @Test
    fun `forgetting a server removes its warning decision and credential pointers`() =
        runTest(dispatcher) {
            val store = createStore("forget")
            val serverUrl = "http://local.example.com"
            store.rememberServer(serverUrl)
            store.setHttpWarningSuppressed(serverUrl, suppressed = true)
            store.rememberLoginCredential(
                SavedLoginCredentialRef(
                    serverUrl = serverUrl,
                    credentialId = "user@example.com · local.example.com",
                    username = "user@example.com",
                ),
            )

            store.forgetServer(serverUrl)

            assertThat(store.rememberedServers.first()).isEmpty()
            assertThat(store.isHttpWarningSuppressed(serverUrl)).isFalse()
            assertThat(store.savedLoginCredentials(serverUrl).first()).isEmpty()
        }
}
