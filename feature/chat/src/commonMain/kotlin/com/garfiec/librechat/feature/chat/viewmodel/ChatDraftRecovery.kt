package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.data.endpoint.EndpointDispatch
import com.garfiec.librechat.core.model.request.EphemeralAgent
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.feature.chat.components.AttachedFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Versioned recovery payload stored beside the draft text.
 *
 * Only server-uploaded attachment references are durable. Platform handles for local uploads are
 * deliberately excluded and represented by [lostLocalAttachmentCount] so a restart never pretends
 * an inaccessible file is still attached.
 */
@Serializable
internal data class ChatDraftRecovery(
    val version: Int = CURRENT_VERSION,
    val attachments: List<PersistedAttachment> = emptyList(),
    val lostLocalAttachmentCount: Int = 0,
    val queuedMessages: List<PersistedQueuedMessage> = emptyList(),
    val isQueuePaused: Boolean = false,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
internal data class PersistedAttachment(
    val name: String,
    val isImage: Boolean,
    val fileId: String,
    val filepath: String? = null,
    val type: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
internal data class PersistedQueuedMessage(
    val localId: String,
    val text: String,
    val attachments: List<PersistedAttachment>,
    val endpoint: String,
    val model: String?,
    val agentId: String?,
    val enabledTools: Set<String>,
    val mcpServerNames: Set<String>,
    val modelParameters: PersistedModelParameters,
    val cacheTtl: String?,
    val modelParamsPayload: JsonObject?,
    val ephemeralAgent: EphemeralAgent?,
    val dispatchEndpointType: String?,
    val dispatchKey: String?,
    val dispatchModelDisplayLabel: String?,
    val isTemporary: Boolean,
    val accountId: String?,
)

@Serializable
internal data class PersistedModelParameters(
    val temperature: Float,
    val maxOutputTokens: Int?,
    val topP: Float,
    val frequencyPenalty: Float,
    val presencePenalty: Float,
    val customName: String,
    val customInstructions: String,
    val maxContextTokens: Int?,
    val topK: Int?,
    val resendFiles: Boolean,
    val thinking: Boolean,
    val thinkingBudget: String,
    val webSearch: Boolean,
    val urlContext: Boolean,
    val fileTokenLimit: Int?,
    val dynamicValues: Map<String, String>,
)

internal fun encodeChatDraftRecovery(state: ChatDraftRecovery): String =
    chatDraftJson.encodeToString(state)

internal fun decodeChatDraftRecovery(encoded: String?): ChatDraftRecovery? {
    if (encoded.isNullOrBlank()) return null
    return runCatching {
        chatDraftJson.decodeFromString<ChatDraftRecovery>(encoded)
            .takeIf { it.version == ChatDraftRecovery.CURRENT_VERSION }
    }.getOrNull()
}

internal fun AttachedFile.toPersistedAttachment(): PersistedAttachment? {
    val uploadedFileId = fileId ?: return null
    return PersistedAttachment(
        name = name,
        isImage = isImage,
        fileId = uploadedFileId,
        filepath = filepath,
        type = type,
        width = width,
        height = height,
    )
}

internal fun PersistedAttachment.toAttachedFile(): AttachedFile = AttachedFile(
    uri = filepath ?: fileId,
    name = name,
    isImage = isImage,
    uploadProgress = 1f,
    fileId = fileId,
    filepath = filepath,
    type = type,
    width = width,
    height = height,
)

internal fun QueuedMessage.toPersisted(): PersistedQueuedMessage = PersistedQueuedMessage(
    localId = localId,
    text = text,
    attachments = attachments.mapNotNull(AttachedFile::toPersistedAttachment),
    endpoint = endpoint,
    model = model,
    agentId = agentId,
    enabledTools = enabledTools,
    mcpServerNames = mcpServerNames,
    modelParameters = modelParameters.toPersisted(),
    cacheTtl = cacheTtl?.wireValue,
    modelParamsPayload = modelParamsPayload,
    ephemeralAgent = ephemeralAgent,
    dispatchEndpointType = dispatch.endpointType,
    dispatchKey = dispatch.key,
    dispatchModelDisplayLabel = dispatch.modelDisplayLabel,
    isTemporary = isTemporary,
    accountId = accountId,
)

internal fun PersistedQueuedMessage.toQueuedMessage(): QueuedMessage = QueuedMessage(
    localId = localId,
    text = text,
    attachments = attachments.map(PersistedAttachment::toAttachedFile),
    endpoint = endpoint,
    model = model,
    agentId = agentId,
    enabledTools = enabledTools,
    mcpServerNames = mcpServerNames,
    modelParameters = modelParameters.toModelParameters(),
    cacheTtl = cacheTtl?.let(CacheTtl::fromWire),
    modelParamsPayload = modelParamsPayload,
    ephemeralAgent = ephemeralAgent,
    dispatch = EndpointDispatch(
        endpointType = dispatchEndpointType,
        key = dispatchKey,
        modelDisplayLabel = dispatchModelDisplayLabel,
    ),
    isTemporary = isTemporary,
    accountId = accountId,
)

private fun ModelParameters.toPersisted(): PersistedModelParameters = PersistedModelParameters(
    temperature = temperature,
    maxOutputTokens = maxOutputTokens,
    topP = topP,
    frequencyPenalty = frequencyPenalty,
    presencePenalty = presencePenalty,
    customName = customName,
    customInstructions = customInstructions,
    maxContextTokens = maxContextTokens,
    topK = topK,
    resendFiles = resendFiles,
    thinking = thinking,
    thinkingBudget = thinkingBudget,
    webSearch = webSearch,
    urlContext = urlContext,
    fileTokenLimit = fileTokenLimit,
    dynamicValues = dynamicValues,
)

private fun PersistedModelParameters.toModelParameters(): ModelParameters = ModelParameters(
    temperature = temperature,
    maxOutputTokens = maxOutputTokens,
    topP = topP,
    frequencyPenalty = frequencyPenalty,
    presencePenalty = presencePenalty,
    customName = customName,
    customInstructions = customInstructions,
    maxContextTokens = maxContextTokens,
    topK = topK,
    resendFiles = resendFiles,
    thinking = thinking,
    thinkingBudget = thinkingBudget,
    webSearch = webSearch,
    urlContext = urlContext,
    fileTokenLimit = fileTokenLimit,
    dynamicValues = dynamicValues,
)

private val chatDraftJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
