package com.garfiec.librechat.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
class ServerScopedBannerDismissalTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()

    private fun createStore(name: String): Pair<DataStore<Preferences>, SettingsDataStore> {
        val dataStore = PreferenceDataStoreFactory.create {
            File(tmpFolder.root, "$name.preferences_pb")
        }
        return dataStore to SettingsDataStore(
            dataStore = dataStore,
            activeAccountProvider = InMemoryActiveAccountProvider(),
            appScope = CoroutineScope(dispatcher),
            ioDispatcher = dispatcher,
        )
    }

    @Test
    fun `same banner id is dismissed only on its source server and survives recreation`() =
        runTest(dispatcher) {
            val (dataStore, firstStore) = createStore("announcements")

            firstStore.dismissBanner("server-a", "maintenance")

            val recreatedStore = SettingsDataStore(
                dataStore = dataStore,
                activeAccountProvider = InMemoryActiveAccountProvider(),
                appScope = CoroutineScope(dispatcher),
                ioDispatcher = dispatcher,
            )
            assertThat(recreatedStore.dismissedBannerIds("server-a").first())
                .containsExactly("maintenance")
            assertThat(recreatedStore.dismissedBannerIds("server-b").first()).isEmpty()
        }

    @Test
    fun `same incompatible version remains visible on another server and changes resurface`() =
        runTest(dispatcher) {
            val (_, store) = createStore("versions")

            store.setDismissedVersionWarning("server-a", "v0.8.0")

            assertThat(store.dismissedVersionWarning("server-a").first()).isEqualTo("v0.8.0")
            assertThat(store.dismissedVersionWarning("server-b").first()).isNull()
            assertThat(store.dismissedVersionWarning("server-a").first()).isNotEqualTo("v0.9.0")
        }

    @Test
    fun `legacy global dismissals are ignored and removed by scoped writes`() =
        runTest(dispatcher) {
            val (dataStore, store) = createStore("legacy-dismissals")
            val legacyVersion = stringPreferencesKey("dismissed_version_warning")
            val legacyBanners = stringSetPreferencesKey("dismissed_banner_ids")
            dataStore.edit {
                it[legacyVersion] = "v0.8.0"
                it[legacyBanners] = setOf("maintenance")
            }

            assertThat(store.dismissedVersionWarning("server-a").first()).isNull()
            assertThat(store.dismissedBannerIds("server-a").first()).isEmpty()

            store.setDismissedVersionWarning("server-a", "v0.8.0")
            store.dismissBanner("server-a", "maintenance")

            val persisted = dataStore.data.first()
            assertThat(persisted[legacyVersion]).isNull()
            assertThat(persisted[legacyBanners]).isNull()
        }

    @Test
    fun `forget cleanup removes both dismissal types for only the target server`() =
        runTest(dispatcher) {
            val (_, store) = createStore("forget-dismissals")
            store.dismissBanner("server-a", "maintenance")
            store.setDismissedVersionWarning("server-a", "v0.8.0")
            store.dismissBanner("server-b", "maintenance")
            store.setDismissedVersionWarning("server-b", "v0.8.0")

            store.clearServerBannerDismissals("server-a")

            assertThat(store.dismissedBannerIds("server-a").first()).isEmpty()
            assertThat(store.dismissedVersionWarning("server-a").first()).isNull()
            assertThat(store.dismissedBannerIds("server-b").first()).containsExactly("maintenance")
            assertThat(store.dismissedVersionWarning("server-b").first()).isEqualTo("v0.8.0")
        }
}
