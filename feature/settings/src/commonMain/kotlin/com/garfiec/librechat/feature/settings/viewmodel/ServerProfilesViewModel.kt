package com.garfiec.librechat.feature.settings.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garfiec.librechat.core.common.identity.AccountState
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.common.identity.deriveServerId
import com.garfiec.librechat.core.data.datastore.AccountEntry
import com.garfiec.librechat.core.data.datastore.AccountRoster
import com.garfiec.librechat.core.data.datastore.ServerDataStore
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.data.repository.AccountSwitcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class ServerProfileAccountUiModel(
    val accountId: String,
    val displayLabel: String,
    val isActive: Boolean,
)

@Immutable
data class ServerProfileUiModel(
    val url: String,
    val accounts: List<ServerProfileAccountUiModel>,
    val isActiveServer: Boolean,
    val httpWarningSuppressed: Boolean,
)

@Immutable
data class ServerProfilesUiState(
    val profiles: List<ServerProfileUiModel> = emptyList(),
    val pendingForget: ServerProfileUiModel? = null,
    val isBusy: Boolean = false,
    val error: String? = null,
)

class ServerProfilesViewModel(
    private val serverDataStore: ServerDataStore,
    accountRoster: AccountRoster,
    activeAccountProvider: ActiveAccountProvider,
    private val accountSwitcher: AccountSwitcher,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerProfilesUiState())
    val uiState: StateFlow<ServerProfilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                serverDataStore.rememberedServers,
                serverDataStore.httpWarningSuppressedServers,
                accountRoster.entriesFlow(),
                activeAccountProvider.state,
                serverDataStore.currentUrlFlow,
            ) { remembered, suppressed, accounts, accountState, currentUrl ->
                buildServerProfiles(
                    rememberedServers = remembered,
                    suppressedWarnings = suppressed,
                    accounts = accounts,
                    activeAccountId = (accountState as? AccountState.Resolved)?.id?.value,
                    currentUrl = currentUrl,
                )
            }.collect { profiles ->
                _uiState.value = _uiState.value.copy(profiles = profiles)
            }
        }
    }

    fun switchAccount(accountId: String) {
        runBusyAction { accountSwitcher.switch(accountId) }
    }

    fun requestForget(profile: ServerProfileUiModel) {
        _uiState.value = _uiState.value.copy(pendingForget = profile, error = null)
    }

    fun cancelForget() {
        _uiState.value = _uiState.value.copy(pendingForget = null)
    }

    fun confirmForget() {
        val profile = _uiState.value.pendingForget ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            try {
                // Removing an active last account emits navigation away from this screen. Keep the
                // remainder of the explicitly-confirmed transaction alive so the profile and its
                // warning/credential pointers cannot survive as a half-forgotten shell.
                withContext(NonCancellable) {
                    profile.accounts
                        .sortedBy { it.isActive } // inactive first; flip/log out at most once, last
                        .forEach { accountSwitcher.remove(it.accountId) }
                    if (normalize(serverDataStore.awaitBaseUrl()) == normalize(profile.url)) {
                        serverDataStore.clearServerUrl()
                    }
                    serverDataStore.forgetServer(profile.url)
                    settingsDataStore.clearServerBannerDismissals(
                        deriveServerId(profile.url).value,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    pendingForget = null,
                    isBusy = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = e.message ?: "Could not forget server profile",
                )
            }
        }
    }

    fun restoreHttpWarning(url: String) {
        runBusyAction { serverDataStore.setHttpWarningSuppressed(url, suppressed = false) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun runBusyAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, error = null)
            try {
                block()
                _uiState.value = _uiState.value.copy(isBusy = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    error = e.message ?: "Server profile action failed",
                )
            }
        }
    }
}

internal fun buildServerProfiles(
    rememberedServers: List<String>,
    suppressedWarnings: Set<String>,
    accounts: List<AccountEntry>,
    activeAccountId: String?,
    currentUrl: String,
): List<ServerProfileUiModel> {
    val normalizedCurrent = normalize(currentUrl)
    val normalizedSuppressed = suppressedWarnings.mapTo(mutableSetOf(), ::normalize)
    val accountsByServer = accounts.groupBy { normalize(it.serverUrl) }
    val urls = buildSet {
        rememberedServers.mapTo(this, ::normalize)
        accounts.mapTo(this) { normalize(it.serverUrl) }
        normalizedCurrent.takeIf(String::isNotBlank)?.let(::add)
    }.filter(String::isNotBlank)

    return urls
        .map { url ->
            ServerProfileUiModel(
                url = url,
                accounts = accountsByServer[url].orEmpty()
                    .sortedWith(
                        compareByDescending<AccountEntry> { it.accountId == activeAccountId }
                            .thenBy { it.displayLabel.lowercase() },
                    )
                    .map { entry ->
                        ServerProfileAccountUiModel(
                            accountId = entry.accountId,
                            displayLabel = entry.displayLabel,
                            isActive = entry.accountId == activeAccountId,
                        )
                    },
                isActiveServer = url == normalizedCurrent,
                httpWarningSuppressed = url in normalizedSuppressed,
            )
        }
        .sortedWith(
            compareByDescending<ServerProfileUiModel> { it.isActiveServer }
                .thenBy { it.url.lowercase() },
        )
}

private fun normalize(url: String): String = url.trim().trimEnd('/')
