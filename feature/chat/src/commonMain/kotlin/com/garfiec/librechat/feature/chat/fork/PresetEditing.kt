package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.core.model.Preset
import com.garfiec.librechat.core.ui.components.EndpointParameterRegistry
import com.garfiec.librechat.core.ui.components.ModelParameters
import com.garfiec.librechat.feature.chat.util.ParameterTarget
import com.garfiec.librechat.feature.chat.viewmodel.delegate.mergedFromPreset

/** What the editor shows for a stored preset: upstream's own load mapping, over the defaults. */
internal fun Preset.editorParameters(): ModelParameters = ModelParameters.DEFAULT.mergedFromPreset(this)

/**
 * [this] stored preset with a new name, pair and parameters, as a new preset (no id). The parameter
 * fields follow upstream's `savePreset` (each value under the parameter set's own spelling, the
 * other spelling cleared, defaults left out); a field it does not map stays as stored, so a field a
 * newer server added survives an edit. A spec stays only while the pair is unchanged.
 */
internal fun Preset.edited(name: String, endpoint: String, model: String?, params: ModelParameters, target: ParameterTarget): Preset {
    fun wireKey(spellings: List<String>) = EndpointParameterRegistry.wireKeyFor(
        endpoint = target.settingsEndpoint,
        provider = target.settingsProvider,
        model = target.model,
        spellings = spellings,
    )
    val defaults = ModelParameters.DEFAULT
    val dyn = params.dynamicValues
    val topP = params.topP.decimal().takeIf { it != defaults.topP.decimal() }
    val topPKey = wireKey(EndpointParameterRegistry.TOP_P_KEYS)
    val maxTokensKey = wireKey(EndpointParameterRegistry.MAX_OUTPUT_TOKENS_KEYS)
    val samePair = endpoint == this.endpoint && model == this.model
    return copy(
        presetId = null,
        title = name,
        endpoint = endpoint.takeIf { it.isNotBlank() },
        model = model,
        spec = spec.takeIf { samePair },
        temperature = params.temperature.decimal().takeIf { it != defaults.temperature.decimal() },
        topPSnake = topP.takeIf { topPKey == "top_p" },
        topPCamel = topP.takeIf { topPKey == "topP" },
        topK = params.topK,
        maxTokensSnake = params.maxOutputTokens.takeIf { maxTokensKey == "max_tokens" },
        maxTokensCamel = params.maxOutputTokens.takeIf { maxTokensKey == "maxTokens" },
        maxOutputTokens = params.maxOutputTokens.takeIf { maxTokensKey == "maxOutputTokens" },
        maxContextTokens = params.maxContextTokens,
        frequencyPenalty = params.frequencyPenalty.decimal().takeIf { it != defaults.frequencyPenalty.decimal() },
        presencePenalty = params.presencePenalty.decimal().takeIf { it != defaults.presencePenalty.decimal() },
        system = params.customInstructions.takeIf { it.isNotBlank() },
        promptPrefix = params.customInstructions.takeIf { it.isNotBlank() },
        modelLabel = params.customName.takeIf { it.isNotBlank() },
        chatGptLabel = params.customName.takeIf { it.isNotBlank() },
        stop = dyn["stop"]?.takeIf { it.isNotBlank() }?.split("\n")?.filter { it.isNotBlank() },
        effort = dyn["effort"],
        reasoningEffort = dyn["reasoning_effort"],
        reasoningMode = dyn["reasoning_mode"],
        reasoningContext = dyn["reasoning_context"],
        reasoningSummary = dyn["reasoning_summary"],
        verbosity = dyn["verbosity"],
        useResponsesApi = dyn["useResponsesApi"]?.toBooleanStrictOrNull(),
        disableStreaming = dyn["disableStreaming"]?.toBooleanStrictOrNull(),
        thinking = params.thinking,
        thinkingBudget = params.thinkingBudget.toIntOrNull(),
        thinkingDisplay = dyn["thinkingDisplay"],
        thinkingLevel = dyn["thinkingLevel"],
        promptCache = dyn["promptCache"]?.toBooleanStrictOrNull(),
        promptCacheTtl = dyn["promptCacheTtl"]?.takeIf { it == "5m" || it == "1h" },
        imageDetail = dyn["imageDetail"],
        fileTokenLimit = params.fileTokenLimit,
        tags = dyn["tags"]?.takeIf { it.isNotBlank() }?.split("\n")?.filter { it.isNotBlank() },
        region = dyn["region"],
        resendFiles = params.resendFiles,
    )
}

/** The slider's value as the decimal it shows: a stored 0.9 is kept as 0.9, not as the float's 0.8999999761581421. */
private fun Float.decimal(): Double = toString().toDouble()
