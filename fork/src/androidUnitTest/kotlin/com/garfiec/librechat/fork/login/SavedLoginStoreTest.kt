package com.garfiec.librechat.fork.login

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SavedLoginStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `remembered list normalizes deduplicates excludes roster but preserves paths and ports`() {
        assertEquals(listOf("https://one/app", "https://one:8443"), rememberedServerList(
            setOf("", " https://one/ ", "https://one/app/", "https://one/app", "https://one:8443"),
            listOf("https://one"),
        ))
    }

    @Test
    fun `legacy ids map only for this server and unknown provider ids remain usernames`() = runTest {
        val data = PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("refs.preferences_pb") }
        data.edit {
            it[stringPreferencesKey("saved_login_credential_refs")] =
                """[{"serverUrl":"https://one/","credentialId":"alice · old suffix","username":"alice"},
                {"serverUrl":"https://two","credentialId":"bob · old suffix","username":"bob"}]"""
        }
        val store = SavedLoginStore(data)
        val refs = store.refs("https://one")
        assertEquals("alice", credentialUsername("alice · old suffix", refs))
        assertEquals("external", credentialUsername("external", refs))
        assertEquals("bob · old suffix", credentialUsername("bob · old suffix", refs))
        store.rememberCredential(SavedLoginCredentialRef("https://one", "alice", "alice"))
        assertEquals(listOf("alice"), store.refs("https://one").map { it.credentialId })
        assertEquals(1, store.refs("https://two").size)
    }

    @Test
    fun `remember and forget preserve unrelated servers and remove only local refs`() = runTest {
        val data = PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("servers.preferences_pb") }
        val store = SavedLoginStore(data)
        store.rememberServer(" https://one/ ")
        store.rememberServer("https://one")
        store.rememberServer("https://two")
        store.rememberCredential(SavedLoginCredentialRef("https://one", "alice", "alice"))
        store.forgetServer("https://one/")
        assertEquals(setOf("https://two"), store.rememberedServers.first())
        assertTrue(store.refs("https://one").isEmpty())
        assertEquals(setOf("https://two"), data.data.first()[stringSetPreferencesKey("remembered_server_urls")])
    }
}
