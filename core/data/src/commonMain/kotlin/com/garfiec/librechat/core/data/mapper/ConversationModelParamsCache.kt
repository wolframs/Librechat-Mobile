package com.garfiec.librechat.core.data.mapper

import com.garfiec.librechat.core.model.Conversation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal fun Conversation.encodeCachedModelParams(json: Json): String? {
    val params = CachedConversationModelParams.from(this)
    return params.takeUnless { it.isEmpty() }?.let {
        json.encodeToString(CachedConversationModelParams.serializer(), it)
    }
}

internal fun Conversation.restoreCachedModelParams(
    encoded: String?,
    json: Json,
): Conversation {
    val params = encoded?.let {
        try {
            json.decodeFromString(CachedConversationModelParams.serializer(), it)
        } catch (_: Exception) {
            null
        }
    } ?: return this
    return copy(
        assistantId = params.assistantId,
        temperature = params.temperature,
        topP = params.topP,
        topK = params.topK,
        frequencyPenalty = params.frequencyPenalty,
        presencePenalty = params.presencePenalty,
        maxOutputTokens = params.maxOutputTokens,
        maxContextTokens = params.maxContextTokens,
        maxTokens = params.maxTokens,
        system = params.system,
        promptPrefix = params.promptPrefix,
        modelLabel = params.modelLabel,
        chatGptLabel = params.chatGptLabel,
        reasoningEffort = params.reasoningEffort,
        reasoningSummary = params.reasoningSummary,
        effort = params.effort,
        verbosity = params.verbosity,
        useResponsesApi = params.useResponsesApi,
        disableStreaming = params.disableStreaming,
        thinking = params.thinking,
        thinkingBudget = params.thinkingBudget,
        thinkingLevel = params.thinkingLevel,
        thinkingDisplay = params.thinkingDisplay,
        stop = params.stop,
        spec = params.spec,
        tools = params.tools,
        webSearch = params.webSearch,
        urlContext = params.urlContext,
        promptCache = params.promptCache,
        promptCacheTtl = params.promptCacheTtl,
        imageDetail = params.imageDetail,
        fileTokenLimit = params.fileTokenLimit,
        region = params.region,
        resendFiles = params.resendFiles,
    )
}

@Serializable
private data class CachedConversationModelParams(
    val assistantId: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val maxOutputTokens: Int? = null,
    val maxContextTokens: Int? = null,
    val maxTokens: Int? = null,
    val system: String? = null,
    val promptPrefix: String? = null,
    val modelLabel: String? = null,
    val chatGptLabel: String? = null,
    val reasoningEffort: String? = null,
    val reasoningSummary: String? = null,
    val effort: String? = null,
    val verbosity: String? = null,
    val useResponsesApi: Boolean? = null,
    val disableStreaming: Boolean? = null,
    val thinking: Boolean? = null,
    val thinkingBudget: Int? = null,
    val thinkingLevel: String? = null,
    val thinkingDisplay: String? = null,
    val stop: List<String>? = null,
    val spec: String? = null,
    val tools: List<String>? = null,
    val webSearch: Boolean? = null,
    val urlContext: Boolean? = null,
    val promptCache: Boolean? = null,
    val promptCacheTtl: String? = null,
    val imageDetail: String? = null,
    val fileTokenLimit: Int? = null,
    val region: String? = null,
    val resendFiles: Boolean? = null,
) {
    fun isEmpty(): Boolean = values().all { it == null }

    private fun values(): List<Any?> = listOf(
        assistantId,
        temperature,
        topP,
        topK,
        frequencyPenalty,
        presencePenalty,
        maxOutputTokens,
        maxContextTokens,
        maxTokens,
        system,
        promptPrefix,
        modelLabel,
        chatGptLabel,
        reasoningEffort,
        reasoningSummary,
        effort,
        verbosity,
        useResponsesApi,
        disableStreaming,
        thinking,
        thinkingBudget,
        thinkingLevel,
        thinkingDisplay,
        stop,
        spec,
        tools,
        webSearch,
        urlContext,
        promptCache,
        promptCacheTtl,
        imageDetail,
        fileTokenLimit,
        region,
        resendFiles,
    )

    companion object {
        fun from(conversation: Conversation) = CachedConversationModelParams(
            assistantId = conversation.assistantId,
            temperature = conversation.temperature,
            topP = conversation.topP,
            topK = conversation.topK,
            frequencyPenalty = conversation.frequencyPenalty,
            presencePenalty = conversation.presencePenalty,
            maxOutputTokens = conversation.maxOutputTokens,
            maxContextTokens = conversation.maxContextTokens,
            maxTokens = conversation.maxTokens,
            system = conversation.system,
            promptPrefix = conversation.promptPrefix,
            modelLabel = conversation.modelLabel,
            chatGptLabel = conversation.chatGptLabel,
            reasoningEffort = conversation.reasoningEffort,
            reasoningSummary = conversation.reasoningSummary,
            effort = conversation.effort,
            verbosity = conversation.verbosity,
            useResponsesApi = conversation.useResponsesApi,
            disableStreaming = conversation.disableStreaming,
            thinking = conversation.thinking,
            thinkingBudget = conversation.thinkingBudget,
            thinkingLevel = conversation.thinkingLevel,
            thinkingDisplay = conversation.thinkingDisplay,
            stop = conversation.stop,
            spec = conversation.spec,
            tools = conversation.tools,
            webSearch = conversation.webSearch,
            urlContext = conversation.urlContext,
            promptCache = conversation.promptCache,
            promptCacheTtl = conversation.promptCacheTtl,
            imageDetail = conversation.imageDetail,
            fileTokenLimit = conversation.fileTokenLimit,
            region = conversation.region,
            resendFiles = conversation.resendFiles,
        )
    }
}
