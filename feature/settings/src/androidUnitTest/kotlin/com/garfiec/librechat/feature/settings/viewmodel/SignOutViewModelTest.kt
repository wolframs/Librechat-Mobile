package com.garfiec.librechat.feature.settings.viewmodel

import com.garfiec.librechat.core.data.datastore.AccountEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignOutViewModelTest {

    @Test
    fun `current account and most recent survivor explain promoted transition`() {
        val state = buildSignOutUiState(
            entries = listOf(
                account("active", "Wolfram", "https://chat.example.com", 30),
                account("older", "Old account", "https://old.example.com", 10),
                account("next", "Work", "https://work.example.com", 20),
            ),
            activeAccountId = "active",
        )

        assertThat(state.current).isEqualTo(
            SignOutAccountUiModel("Wolfram", "chat.example.com"),
        )
        assertThat(state.successor).isEqualTo(
            SignOutAccountUiModel("Work", "work.example.com"),
        )
    }

    @Test
    fun `last account has no successor`() {
        val state = buildSignOutUiState(
            entries = listOf(account("active", "Wolfram", "http://chat.local:3080", 10)),
            activeAccountId = "active",
        )

        assertThat(state.current).isEqualTo(
            SignOutAccountUiModel("Wolfram", "chat.local:3080"),
        )
        assertThat(state.successor).isNull()
    }

    private fun account(
        id: String,
        label: String,
        url: String,
        lastActiveAt: Long,
    ) = AccountEntry(
        accountId = id,
        serverUrl = url,
        displayLabel = label,
        lastActiveAt = lastActiveAt,
    )
}
