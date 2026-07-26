package com.garfiec.librechat.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AccountScopedToolSelectionTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val accountProvider = InMemoryActiveAccountProvider()

    private fun createStore(name: String): Pair<DataStore<Preferences>, SettingsDataStore> {
        val dataStore = PreferenceDataStoreFactory.create {
            File(tmpFolder.root, "$name.preferences_pb")
        }
        return dataStore to SettingsDataStore(
            dataStore = dataStore,
            activeAccountProvider = accountProvider,
            appScope = CoroutineScope(dispatcher),
            ioDispatcher = dispatcher,
        )
    }

    @Test
    fun selectionsAreIsolatedAcrossAccountsAndRestoredOnReturn() = runTest(dispatcher) {
        val (_, store) = createStore("scoped-tools")

        accountProvider.set(AccountId("server-a:user-a"))
        store.setSelectedMcpServers(setOf("filesystem-a"))
        store.setEnabledTools(setOf("file_search"))

        accountProvider.set(AccountId("server-b:user-b"))
        assertThat(store.selectedMcpServers.first()).isEmpty()
        assertThat(store.enabledTools.first()).isEmpty()

        store.setSelectedMcpServers(setOf("filesystem-b"))
        store.setEnabledTools(setOf("code_interpreter"))

        accountProvider.set(AccountId("server-a:user-a"))
        assertThat(store.selectedMcpServers.first()).containsExactly("filesystem-a")
        assertThat(store.enabledTools.first()).containsExactly("file_search")

        accountProvider.set(AccountId("server-b:user-b"))
        assertThat(store.selectedMcpServers.first()).containsExactly("filesystem-b")
        assertThat(store.enabledTools.first()).containsExactly("code_interpreter")
    }

    @Test
    fun legacyGlobalSelectionsAreNotAttributedAndAreRemovedOnWrite() = runTest(dispatcher) {
        val (dataStore, store) = createStore("legacy-tools")
        val legacyMcp = stringPreferencesKey("selected_mcp_servers")
        val legacyTools = stringPreferencesKey("enabled_tools")
        dataStore.edit {
            it[legacyMcp] = "server-from-unknown-account"
            it[legacyTools] = "code_interpreter"
        }

        accountProvider.set(AccountId("server-b:user-b"))
        assertThat(store.selectedMcpServers.first()).isEmpty()
        assertThat(store.enabledTools.first()).isEmpty()

        store.setSelectedMcpServers(setOf("server-b"))
        store.setEnabledTools(setOf("file_search"))

        val persisted = dataStore.data.first()
        assertThat(persisted[legacyMcp]).isNull()
        assertThat(persisted[legacyTools]).isNull()
    }
}
