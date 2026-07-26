package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.AccountState
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.garfiec.librechat.core.data.db.dao.DraftDao
import com.garfiec.librechat.core.data.db.entity.DraftEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DraftRecoveryStateRepositoryTest {

    private val account = AccountId("server:user")

    @Test
    fun `text and recovery payload are cached and written atomically`() = runTest {
        val dao = mockk<DraftDao>(relaxed = true)
        val provider = InMemoryActiveAccountProvider(AccountState.Resolved(account))
        val repository = DraftRepositoryImpl(dao, provider, StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        repository.saveDraftState(
            conversationId = "conversation",
            text = "",
            stateJson = """{"queuedMessages":[{"localId":"queued"}]}""",
        )

        assertThat(repository.getDraftState("conversation")).isEqualTo(
            DraftSnapshot(
                text = "",
                stateJson = """{"queuedMessages":[{"localId":"queued"}]}""",
            ),
        )
        testScheduler.advanceUntilIdle()

        val entity = slot<DraftEntity>()
        coVerify(exactly = 1) { dao.upsertDraft(capture(entity)) }
        assertThat(entity.captured.text).isEmpty()
        assertThat(entity.captured.stateJson).contains("queuedMessages")
        assertThat(entity.captured.accountId).isEqualTo(account.value)
    }

    @Test
    fun `existing text-only row remains readable as a recovery snapshot`() = runTest {
        val dao = mockk<DraftDao>(relaxed = true)
        coEvery { dao.getDraftForAccount("conversation", account.value) } returns DraftEntity(
            conversationId = "conversation",
            text = "legacy text",
            stateJson = null,
            accountId = account.value,
        )
        val provider = InMemoryActiveAccountProvider(AccountState.Resolved(account))
        val repository = DraftRepositoryImpl(dao, provider, StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()

        assertThat(repository.getDraftState("conversation")).isEqualTo(
            DraftSnapshot(text = "legacy text", stateJson = null),
        )
    }
}
