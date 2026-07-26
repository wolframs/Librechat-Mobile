package com.garfiec.librechat.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationParameterSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes the complete parameter snapshot returned for an existing chat`() {
        val conversation = json.decodeFromString<Conversation>(
            """
            {
              "conversationId": "conversation-1",
              "endpoint": "anthropic",
              "model": "claude-opus-5",
              "modelLabel": "Claude Opus Five",
              "promptPrefix": "Persisted custom instructions",
              "maxContextTokens": 828400,
              "topP": 0.7,
              "max_tokens": 8192,
              "resendFiles": true,
              "thinking": true,
              "thinkingBudget": 2000,
              "promptCache": true,
              "promptCacheTtl": "1h",
              "web_search": true,
              "reasoning_summary": "detailed",
              "useResponsesApi": false
            }
            """.trimIndent(),
        )

        assertEquals("Claude Opus Five", conversation.modelLabel)
        assertEquals("Persisted custom instructions", conversation.promptPrefix)
        assertEquals(828400, conversation.maxContextTokens)
        assertEquals(0.7, conversation.topP)
        assertEquals(8192, conversation.maxTokens)
        assertEquals(true, conversation.resendFiles)
        assertEquals(true, conversation.thinking)
        assertEquals(2000, conversation.thinkingBudget)
        assertEquals(true, conversation.promptCache)
        assertEquals("1h", conversation.promptCacheTtl)
        assertEquals(true, conversation.webSearch)
        assertEquals("detailed", conversation.reasoningSummary)
        assertEquals(false, conversation.useResponsesApi)
    }
}
