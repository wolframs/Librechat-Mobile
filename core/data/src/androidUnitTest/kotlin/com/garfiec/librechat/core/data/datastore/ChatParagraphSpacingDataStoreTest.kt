package com.garfiec.librechat.core.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChatParagraphSpacingDataStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `defaults to comfortable spacing`() = runTest(dispatcher) {
        assertThat(createStore().chatParagraphSpacing.first())
            .isEqualTo(ChatParagraphSpacing.COMFORTABLE)
    }

    @Test
    fun `all spacing choices round trip through DataStore`() = runTest(dispatcher) {
        val store = createStore()

        for (spacing in ChatParagraphSpacing.entries) {
            store.setChatParagraphSpacing(spacing)
            assertThat(store.chatParagraphSpacing.first()).isEqualTo(spacing)
        }
    }

    @Test
    fun `unknown stored value falls back to comfortable`() {
        assertThat(ChatParagraphSpacing.fromString("future-value"))
            .isEqualTo(ChatParagraphSpacing.COMFORTABLE)
    }

    private fun createStore(): SettingsDataStore {
        val dataStore = PreferenceDataStoreFactory.create {
            File(tmpFolder.root, "paragraph-spacing.preferences_pb")
        }
        val accountProvider = InMemoryActiveAccountProvider().apply {
            set(AccountId("srv:test-account"))
        }
        return SettingsDataStore(
            dataStore = dataStore,
            activeAccountProvider = accountProvider,
            appScope = CoroutineScope(dispatcher),
            ioDispatcher = dispatcher,
        )
    }
}
