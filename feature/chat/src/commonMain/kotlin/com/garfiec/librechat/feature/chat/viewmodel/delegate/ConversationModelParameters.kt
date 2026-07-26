package com.garfiec.librechat.feature.chat.viewmodel.delegate

import com.garfiec.librechat.core.model.Conversation
import com.garfiec.librechat.core.ui.components.EndpointParameterRegistry
import com.garfiec.librechat.core.ui.components.ModelParameters

/**
 * Reconstructs the editable parameter state from the conversation snapshot stored by LibreChat.
 *
 * A conversation is authoritative: presets are copied into it when the chat is created and may
 * later be edited or deleted independently. Start from the endpoint registry's defaults so
 * parameters omitted by the server retain the provider-specific values shown for a new chat.
 */
internal fun Conversation.toModelParameters(): ModelParameters {
    val endpointDefaults = EndpointParameterRegistry.getDefinitions(
        endpoint = endpoint.orEmpty(),
        extendedEffortSupported = false,
        model = model,
    ).fold(ModelParameters.DEFAULT) { parameters, definition ->
        definition.default?.let { parameters.withUpdatedKey(definition.key, it) } ?: parameters
    }
    val conversationDynamicValues = buildMap {
        stop?.let { put("stop", it.joinToString("\n")) }
        effort?.let { put("effort", it) }
        reasoningEffort?.let { put("reasoning_effort", it) }
        reasoningSummary?.let { put("reasoning_summary", it) }
        verbosity?.let { put("verbosity", it) }
        useResponsesApi?.let { put("useResponsesApi", it.toString()) }
        disableStreaming?.let { put("disableStreaming", it.toString()) }
        thinkingDisplay?.let { put("thinkingDisplay", it) }
        thinkingLevel?.let { put("thinkingLevel", it) }
        promptCache?.let { put("promptCache", it.toString()) }
        promptCacheTtl?.let { put("promptCacheTtl", it) }
        imageDetail?.let { put("imageDetail", it) }
        region?.let { put("region", it) }
    }
    return endpointDefaults.copy(
        temperature = temperature?.toFloat() ?: endpointDefaults.temperature,
        topP = topP?.toFloat() ?: endpointDefaults.topP,
        topK = topK ?: endpointDefaults.topK,
        maxOutputTokens = maxOutputTokens ?: maxTokens ?: endpointDefaults.maxOutputTokens,
        maxContextTokens = maxContextTokens ?: endpointDefaults.maxContextTokens,
        frequencyPenalty = frequencyPenalty?.toFloat() ?: endpointDefaults.frequencyPenalty,
        presencePenalty = presencePenalty?.toFloat() ?: endpointDefaults.presencePenalty,
        customName = modelLabel ?: chatGptLabel ?: endpointDefaults.customName,
        customInstructions = system ?: promptPrefix ?: endpointDefaults.customInstructions,
        thinking = thinking ?: endpointDefaults.thinking,
        thinkingBudget = thinkingBudget?.toString() ?: endpointDefaults.thinkingBudget,
        webSearch = webSearch ?: endpointDefaults.webSearch,
        urlContext = urlContext ?: endpointDefaults.urlContext,
        fileTokenLimit = fileTokenLimit ?: endpointDefaults.fileTokenLimit,
        resendFiles = resendFiles ?: endpointDefaults.resendFiles,
        dynamicValues = endpointDefaults.dynamicValues + conversationDynamicValues,
    )
}
