package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.ConversationRepository
import com.garfiec.librechat.core.model.Conversation
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConversationSnapshotLoaderTest {

    private val repository = mockk<ConversationRepository>()

    @Test
    fun `detail response wins over a potentially sparse cache row`() = runTest {
        val detail = Conversation(
            conversationId = "conversation-1",
            promptPrefix = "Persisted custom instructions",
        )
        coEvery { repository.refreshConversation("conversation-1", null) } returns Result.Success(detail)

        val result = repository.loadConversationSnapshot("conversation-1")

        assertThat(result).isEqualTo(Result.Success(detail))
        coVerify(exactly = 0) { repository.getConversation(any(), any()) }
    }

    @Test
    fun `cached snapshot is used when detail refresh fails`() = runTest {
        val cached = Conversation(
            conversationId = "conversation-1",
            promptCacheTtl = "1h",
        )
        coEvery { repository.refreshConversation("conversation-1", null) } returns
            Result.Error(message = "offline")
        coEvery { repository.getConversation("conversation-1", null) } returns Result.Success(cached)

        val result = repository.loadConversationSnapshot("conversation-1")

        assertThat(result).isEqualTo(Result.Success(cached))
    }
}
