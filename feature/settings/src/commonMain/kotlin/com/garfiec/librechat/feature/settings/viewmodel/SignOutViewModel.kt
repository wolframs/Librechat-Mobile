package com.garfiec.librechat.feature.settings.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garfiec.librechat.core.common.extensions.serverHostLabel
import com.garfiec.librechat.core.common.identity.AccountState
import com.garfiec.librechat.core.common.identity.ActiveAccountProvider
import com.garfiec.librechat.core.data.datastore.AccountEntry
import com.garfiec.librechat.core.data.datastore.AccountRoster
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
internal data class SignOutAccountUiModel(
    val displayLabel: String,
    val serverHost: String,
)

@Immutable
internal data class SignOutUiState(
    val current: SignOutAccountUiModel? = null,
    val successor: SignOutAccountUiModel? = null,
)

/**
 * Supplies only the device-global account context needed by the sign-out confirmation.
 *
 * Keeping this separate from [SettingsViewModel] avoids making the already broad settings state
 * responsible for roster lifecycle. The repository remains the authority for removal/promotion;
 * this model mirrors its "most recently active survivor" rule solely to explain the pending action.
 */
class SignOutViewModel(
    accountRoster: AccountRoster,
    activeAccountProvider: ActiveAccountProvider,
) : ViewModel() {

    internal val uiState: StateFlow<SignOutUiState> =
        combine(accountRoster.entriesFlow(), activeAccountProvider.state) { entries, accountState ->
            buildSignOutUiState(
                entries = entries,
                activeAccountId = (accountState as? AccountState.Resolved)?.id?.value,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SignOutUiState(),
        )
}

internal fun buildSignOutUiState(
    entries: List<AccountEntry>,
    activeAccountId: String?,
): SignOutUiState {
    val current = entries.firstOrNull { it.accountId == activeAccountId }
    val successor = entries
        .asSequence()
        .filterNot { it.accountId == activeAccountId }
        .maxByOrNull(AccountEntry::lastActiveAt)

    return SignOutUiState(
        current = current?.toSignOutUiModel(),
        successor = successor?.toSignOutUiModel(),
    )
}

private fun AccountEntry.toSignOutUiModel() =
    SignOutAccountUiModel(
        displayLabel = displayLabel,
        serverHost = serverUrl.serverHostLabel(),
    )
