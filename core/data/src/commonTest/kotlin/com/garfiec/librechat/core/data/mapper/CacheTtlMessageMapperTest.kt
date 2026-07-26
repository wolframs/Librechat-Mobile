package com.garfiec.librechat.core.data.mapper

import com.garfiec.librechat.core.model.Message
import kotlin.test.Test
import kotlin.test.assertEquals

class CacheTtlMessageMapperTest {

    @Test
    fun `cache TTL round trips through the Room entity`() {
        val restored = Message(
            messageId = "message-1",
            conversationId = "conversation-1",
            cacheTTL = "1h",
        ).toEntity().toModel()

        assertEquals("1h", restored.cacheTTL)
    }
}
