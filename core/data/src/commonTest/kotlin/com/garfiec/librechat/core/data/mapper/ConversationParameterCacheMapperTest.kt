package com.garfiec.librechat.core.data.mapper

import com.garfiec.librechat.core.data.db.entity.ConversationEntity
import com.garfiec.librechat.core.model.Conversation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ConversationParameterCacheMapperTest {

    @Test
    fun `parameter snapshot survives the Room entity round trip`() {
        val conversation = Conversation(
            conversationId = "conversation-1",
            title = "Cached chat",
            user = "user-1",
            endpoint = "anthropic",
            model = "claude-opus-5",
            modelLabel = "Claude Opus Five",
            promptPrefix = "Persisted custom instructions",
            maxContextTokens = 828400,
            resendFiles = true,
            thinking = true,
            thinkingBudget = 2000,
            promptCache = true,
            promptCacheTtl = "1h",
            webSearch = true,
            topP = 0.7,
            topK = 5,
            createdAt = Instant.parse("2026-07-25T10:00:00Z"),
            updatedAt = Instant.parse("2026-07-25T11:00:00Z"),
        )

        val entity = conversation.toEntity()
        val restored = entity.toModel()

        assertNotNull(entity.modelParams)
        assertEquals(conversation.modelLabel, restored.modelLabel)
        assertEquals(conversation.promptPrefix, restored.promptPrefix)
        assertEquals(conversation.maxContextTokens, restored.maxContextTokens)
        assertEquals(conversation.resendFiles, restored.resendFiles)
        assertEquals(conversation.thinking, restored.thinking)
        assertEquals(conversation.thinkingBudget, restored.thinkingBudget)
        assertEquals(conversation.promptCache, restored.promptCache)
        assertEquals(conversation.promptCacheTtl, restored.promptCacheTtl)
        assertEquals(conversation.webSearch, restored.webSearch)
        assertEquals(conversation.topP, restored.topP)
        assertEquals(conversation.topK, restored.topK)
    }

    @Test
    fun `sparse conversation does not manufacture a complete parameter snapshot`() {
        val entity = Conversation(
            conversationId = "conversation-1",
            endpoint = "anthropic",
            model = "claude-opus-5",
        ).toEntity()

        assertNull(entity.modelParams)
    }

    @Test
    fun `corrupt parameter JSON degrades to metadata-only cache entry`() {
        val entity = ConversationEntity(
            conversationId = "conversation-1",
            title = "Cached chat",
            user = "user-1",
            endpoint = "anthropic",
            endpointType = null,
            model = "claude-opus-5",
            agentId = null,
            tags = "[]",
            iconURL = null,
            greeting = null,
            modelParams = "{not-json",
            createdAt = 0,
            updatedAt = 0,
        )

        val restored = entity.toModel()

        assertEquals("claude-opus-5", restored.model)
        assertNull(restored.promptPrefix)
        assertNull(restored.promptCacheTtl)
    }
}
