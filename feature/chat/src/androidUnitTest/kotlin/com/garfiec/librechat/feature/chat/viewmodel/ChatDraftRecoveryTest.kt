package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.data.endpoint.EndpointDispatch
import com.garfiec.librechat.core.model.request.EphemeralAgent
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.feature.chat.components.AttachedFile
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test

class ChatDraftRecoveryTest {

    @Test
    fun `uploaded attachments and complete queued config survive round trip`() {
        val uploaded = AttachedFile(
            uri = "content://old-local-uri",
            name = "diagram.png",
            isImage = true,
            uploadProgress = 1f,
            fileId = "file-1",
            filepath = "/uploads/file-1",
            type = "image/png",
            width = 640,
            height = 480,
        )
        val queued = QueuedMessage(
            localId = "queue-1",
            text = "Follow up",
            attachments = listOf(uploaded),
            endpoint = "anthropic",
            model = "claude",
            agentId = null,
            enabledTools = setOf("web_search"),
            mcpServerNames = setOf("filesystem"),
            modelParameters = ModelParameters(
                temperature = 0.4f,
                maxOutputTokens = 1234,
                thinking = true,
                dynamicValues = mapOf("reasoning_effort" to "high"),
            ),
            cacheTtl = CacheTtl.ONE_HOUR,
            modelParamsPayload = buildJsonObject { put("temperature", JsonPrimitive(0.4)) },
            ephemeralAgent = EphemeralAgent(mcp = listOf("filesystem"), webSearch = true),
            dispatch = EndpointDispatch(
                endpointType = "anthropic",
                key = "never",
                modelDisplayLabel = "Anthropic",
            ),
            accountId = "server:user",
        )
        val encoded = encodeChatDraftRecovery(
            ChatDraftRecovery(
                attachments = listOfNotNull(uploaded.toPersistedAttachment()),
                queuedMessages = listOf(queued.toPersisted()),
                isQueuePaused = true,
            ),
        )

        val decoded = checkNotNull(decodeChatDraftRecovery(encoded))
        val restoredAttachment = decoded.attachments.single().toAttachedFile()
        val restoredQueue = decoded.queuedMessages.single().toQueuedMessage()

        assertThat(restoredAttachment.fileId).isEqualTo("file-1")
        assertThat(restoredAttachment.uri).isEqualTo("/uploads/file-1")
        assertThat(restoredQueue.text).isEqualTo("Follow up")
        assertThat(restoredQueue.attachments.single().fileId).isEqualTo("file-1")
        assertThat(restoredQueue.modelParameters).isEqualTo(queued.modelParameters)
        assertThat(restoredQueue.cacheTtl).isEqualTo(CacheTtl.ONE_HOUR)
        assertThat(restoredQueue.ephemeralAgent).isEqualTo(queued.ephemeralAgent)
        assertThat(restoredQueue.dispatch).isEqualTo(queued.dispatch)
        assertThat(restoredQueue.accountId).isEqualTo("server:user")
    }

    @Test
    fun `local-only attachment is not serialized as recoverable`() {
        val localOnly = AttachedFile(
            uri = "content://temporary",
            name = "still-uploading.pdf",
            fileId = null,
        )

        assertThat(localOnly.toPersistedAttachment()).isNull()
    }

    @Test
    fun `text-only and unknown-version payloads fail open`() {
        assertThat(decodeChatDraftRecovery(null)).isNull()
        assertThat(decodeChatDraftRecovery("""{"version":999}""")).isNull()
    }
}
