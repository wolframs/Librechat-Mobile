package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.AccountState
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.garfiec.librechat.core.data.datastore.AccountRoster
import com.garfiec.librechat.core.data.db.dao.ConversationDao
import com.garfiec.librechat.core.data.db.dao.MessageDao
import com.garfiec.librechat.core.data.db.entity.ConversationEntity
import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.core.network.api.ConversationsApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationParameterRefreshRepositoryTest {

    private val conversationsApi = mockk<ConversationsApi>()
    private val conversationDao = mockk<ConversationDao>(relaxed = true)
    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val roster = mockk<AccountRoster>(relaxed = true)
    private val account = AccountId("srv:user-1")
    private val repository = ConversationRepositoryImpl(
        conversationsApi = conversationsApi,
        conversationDao = conversationDao,
        messageDao = messageDao,
        activeAccountProvider = InMemoryActiveAccountProvider(AccountState.Resolved(account)),
        roster = roster,
        json = Json { ignoreUnknownKeys = true },
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `detail refresh uses authoritative parameter cache write`() = runTest {
        coEvery { conversationsApi.getConversation("conversation-1") } returns Conversation(
            conversationId = "conversation-1",
            endpoint = "anthropic",
            model = "claude-opus-5",
        )

        repository.refreshConversation("conversation-1", originAccount = null)

        coVerify(exactly = 1) {
            conversationDao.upsertAuthoritativeSnapshot(
                account.value,
                match<ConversationEntity> {
                    it.conversationId == "conversation-1" && it.modelParams == null
                },
            )
        }
        coVerify(exactly = 0) {
            conversationDao.upsertPreservingTags(any(), any<ConversationEntity>())
        }
    }
}
