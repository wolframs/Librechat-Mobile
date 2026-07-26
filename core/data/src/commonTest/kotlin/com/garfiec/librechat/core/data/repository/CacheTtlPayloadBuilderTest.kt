package com.garfiec.librechat.core.data.repository

import com.garfiec.librechat.core.model.request.ChatRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CacheTtlPayloadBuilderTest {

    private val json = Json { encodeDefaults = false }

    @Test
    fun `one-shot cache TTL serializes at the top level`() {
        val request = ChatPayloadBuilder.build(
            text = "keep this expensive context warm",
            conversationId = "conversation-1",
            endpoint = "anthropic",
            model = "claude-opus-5",
            cacheTtl = "1h",
        )

        assertEquals("1h", request.cacheTTL)
        val encoded = json.encodeToString(ChatRequest.serializer(), request)
        assertTrue("\"cacheTTL\":\"1h\"" in encoded)
    }

    @Test
    fun `cache TTL is omitted when not armed`() {
        val request = ChatPayloadBuilder.build(
            text = "hello",
            conversationId = "conversation-1",
            endpoint = "anthropic",
            model = "claude-opus-5",
        )

        assertNull(request.cacheTTL)
        val encoded = json.encodeToString(ChatRequest.serializer(), request)
        assertFalse("\"cacheTTL\"" in encoded)
    }
}
