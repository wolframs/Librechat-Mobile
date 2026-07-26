package com.garfiec.librechat.feature.settings.viewmodel

import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.garfiec.librechat.core.data.datastore.AccountEntry
import com.garfiec.librechat.core.data.datastore.AccountRoster
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.data.repository.AccountSwitcher
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerProfilesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun profileProjectionCombinesRememberedServersAndAccountsWithoutDuplicates() {
        val profiles = buildServerProfiles(
            rememberedServers = listOf(
                "https://a.example.com/",
                "https://remembered.example.com",
            ),
            suppressedWarnings = setOf("https://remembered.example.com/"),
            accounts = listOf(
                account("a:user-1", "https://a.example.com", "Alice"),
                account("a:user-2", "https://a.example.com/", "Bob"),
                account("b:user-3", "https://account-only.example.com", "Carol"),
            ),
            activeAccountId = "a:user-2",
            currentUrl = "https://a.example.com/",
        )

        assertThat(profiles.map { it.url }).containsExactly(
            "https://a.example.com",
            "https://account-only.example.com",
            "https://remembered.example.com",
        ).inOrder()
        assertThat(profiles.first().isActiveServer).isTrue()
        assertThat(profiles.first().accounts.map { it.displayLabel })
            .containsExactly("Bob", "Alice")
            .inOrder()
        assertThat(profiles.last().httpWarningSuppressed).isTrue()
    }

    @Test
    fun confirmedForgetRemovesAccountsThenProfileAndCurrentPointer() = runTest(dispatcher) {
        val activeProvider = InMemoryActiveAccountProvider().apply {
            set(AccountId("a:active"))
        }
        val remembered = MutableStateFlow(listOf("https://a.example.com"))
        val suppressed = MutableStateFlow(setOf("https://a.example.com"))
        val currentUrl = MutableStateFlow("https://a.example.com")
        val entries = MutableStateFlow(
            listOf(
                account("a:active", "https://a.example.com", "Active"),
                account("a:other", "https://a.example.com", "Other"),
            ),
        )
        val serverDataStore = mockk<ServerDataStore>(relaxed = true).also {
            every { it.rememberedServers } returns remembered
            every { it.httpWarningSuppressedServers } returns suppressed
            every { it.currentUrlFlow } returns currentUrl
            coEvery { it.awaitBaseUrl() } returns "https://a.example.com"
        }
        val roster = mockk<AccountRoster>().also {
            every { it.entriesFlow() } returns entries
        }
        val switcher = mockk<AccountSwitcher>(relaxed = true)
        val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
        val viewModel = ServerProfilesViewModel(
            serverDataStore = serverDataStore,
            accountRoster = roster,
            activeAccountProvider = activeProvider,
            accountSwitcher = switcher,
            settingsDataStore = settingsDataStore,
        )
        advanceUntilIdle()

        viewModel.requestForget(viewModel.uiState.value.profiles.single())
        viewModel.confirmForget()
        advanceUntilIdle()

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            switcher.remove("a:other")
            switcher.remove("a:active")
            serverDataStore.clearServerUrl()
            serverDataStore.forgetServer("https://a.example.com")
            settingsDataStore.clearServerBannerDismissals(any())
        }
        assertThat(viewModel.uiState.value.pendingForget).isNull()
        assertThat(viewModel.uiState.value.isBusy).isFalse()
    }

    private fun account(
        id: String,
        serverUrl: String,
        label: String,
    ) = AccountEntry(
        accountId = id,
        serverUrl = serverUrl,
        displayLabel = label,
        lastActiveAt = 1L,
    )
}
