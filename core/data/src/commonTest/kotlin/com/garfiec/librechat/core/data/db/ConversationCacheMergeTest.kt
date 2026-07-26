package com.garfiec.librechat.core.data.db

import com.garfiec.librechat.core.data.db.entity.ConversationEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationCacheMergeTest {

    @Test
    fun `sparse server row preserves cached parameters and local tags`() {
        val existing = entity(
            tags = """["Saved"]""",
            modelParams = """{"promptCacheTtl":"1h"}""",
        )
        val incoming = entity(tags = "[]", modelParams = null)

        val merged = incoming.preserveLocalCacheFieldsFrom(existing)

        assertEquals(existing.tags, merged.tags)
        assertEquals(existing.modelParams, merged.modelParams)
    }

    @Test
    fun `complete refresh replaces cached parameters but retains local tags`() {
        val existing = entity(
            tags = """["Saved"]""",
            modelParams = """{"promptCacheTtl":"5m"}""",
        )
        val incoming = entity(
            tags = "[]",
            modelParams = """{"promptCacheTtl":"1h"}""",
        )

        val merged = incoming.preserveLocalCacheFieldsFrom(existing)

        assertEquals(existing.tags, merged.tags)
        assertEquals(incoming.modelParams, merged.modelParams)
    }

    @Test
    fun `authoritative default-only refresh clears stale cached parameters`() {
        val existing = entity(
            tags = """["Saved"]""",
            modelParams = """{"promptCacheTtl":"1h"}""",
        )
        val incoming = entity(tags = "[]", modelParams = null)

        val merged = incoming.preserveLocalCacheFieldsFrom(
            existing = existing,
            preserveModelParamsWhenMissing = false,
        )

        assertEquals(existing.tags, merged.tags)
        assertEquals(null, merged.modelParams)
    }

    private fun entity(tags: String, modelParams: String?) = ConversationEntity(
        conversationId = "conversation-1",
        title = "Chat",
        user = "user-1",
        endpoint = "anthropic",
        endpointType = null,
        model = "claude-opus-5",
        agentId = null,
        tags = tags,
        iconURL = null,
        greeting = null,
        modelParams = modelParams,
        createdAt = 0,
        updatedAt = 0,
    )
}
