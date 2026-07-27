package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.common.identity.AccountId
import com.garfiec.librechat.core.common.identity.AccountState
import com.garfiec.librechat.core.common.identity.InMemoryActiveAccountProvider
import com.garfiec.librechat.core.data.datastore.AccountRoster
import com.garfiec.librechat.core.data.db.dao.ConversationDao
import com.garfiec.librechat.core.data.db.dao.MessageDao
import com.garfiec.librechat.core.data.db.entity.ConversationEntity
import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.core.model.response.ConversationListResponse
import com.garfiec.librechat.core.network.api.ConversationsApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchivedFavoriteReconciliationTest {

    private val api = mockk<ConversationsApi>()
    private val conversationDao = mockk<ConversationDao>(relaxed = true)
    private val account = AccountId("srv:user-1")
    private val json = Json { ignoreUnknownKeys = true }
    private val repository = ConversationRepositoryImpl(
        conversationsApi = api,
        conversationDao = conversationDao,
        messageDao = mockk<MessageDao>(relaxed = true),
        activeAccountProvider = InMemoryActiveAccountProvider(AccountState.Resolved(account)),
        roster = mockk<AccountRoster>(relaxed = true),
        json = json,
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `archive then remote unfavorite sync clears saved before unarchive`() = runTest {
        val archivedFavorite = entity(isArchived = true, tagsJson = """["work","Saved"]""")
        coEvery {
            api.getConversations(any(), any(), any(), any(), any(), any(), any())
        } returns ConversationListResponse(emptyList(), null)
        coEvery {
            conversationDao.getConversationsWithTagForAccount(account.value, "\"Saved\"")
        } returns listOf(archivedFavorite)
        coEvery { api.archive("archived-favorite", false) } returns
            Conversation(conversationId = "archived-favorite", isArchived = false)

        repository.syncFavoritesFromServer()
        repository.archive("archived-favorite", isArchived = false)

        val tagsJson = slot<String>()
        coVerifyOrder {
            conversationDao.updateTags(
                "archived-favorite",
                capture(tagsJson),
                any(),
                account.value,
            )
            conversationDao.updateArchived(
                "archived-favorite",
                false,
                any(),
                account.value,
            )
        }
        assertThat(json.decodeFromString<List<String>>(tagsJson.captured))
            .containsExactly("work")
    }

    @Test
    fun `sync paginates archived favorites through tag-filtered endpoint`() = runTest {
        coEvery {
            api.getConversations(any(), any(), false, any(), any(), any(), any())
        } returns ConversationListResponse(emptyList(), null)
        coEvery {
            api.getConversations(null, any(), true, any(), any(), any(), any())
        } returns ConversationListResponse(
            conversations = listOf(Conversation(conversationId = "archived-1", isArchived = true)),
            nextCursor = "archived-next",
        )
        coEvery {
            api.getConversations("archived-next", any(), true, any(), any(), any(), any())
        } returns ConversationListResponse(
            conversations = listOf(Conversation(conversationId = "archived-2", isArchived = true)),
            nextCursor = null,
        )
        coEvery { conversationDao.getByIdForAccount(any(), account.value) } returns null
        coEvery { conversationDao.getConversationsWithTagForAccount(account.value, any()) } returns emptyList()

        repository.syncFavoritesFromServer()

        coVerify(exactly = 1) {
            api.getConversations(null, any(), true, listOf("Saved"), any(), any(), any())
        }
        coVerify(exactly = 1) {
            api.getConversations(
                "archived-next",
                any(),
                true,
                listOf("Saved"),
                any(),
                any(),
                any(),
            )
        }
        coVerify(exactly = 2) { conversationDao.upsert(any()) }
        coVerify(exactly = 1) {
            conversationDao.getConversationsWithTagForAccount(account.value, "\"Saved\"")
        }
    }

    private fun entity(
        isArchived: Boolean,
        tagsJson: String,
    ) = ConversationEntity(
        conversationId = "archived-favorite",
        title = "Archived favorite",
        user = "user-1",
        endpoint = null,
        endpointType = null,
        model = null,
        agentId = null,
        isArchived = isArchived,
        tags = tagsJson,
        iconURL = null,
        greeting = null,
        modelParams = null,
        createdAt = 0L,
        updatedAt = 0L,
        lastSyncedAt = 0L,
    )
}
