package com.garfiec.librechat.core.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** Overlay the server's partial definitions on the selected provider's known controls. */
fun applyEndpointOverrides(
    base: List<ParameterDefinition>,
    config: EndpointConfig?,
): List<ParameterDefinition> {
    val overrides = config?.customParams?.paramDefinitions.orEmpty()
        .mapNotNull { row -> row.text("key")?.let { it to row } }.toMap()
    return base.map { definition ->
        val row = overrides[definition.key]
        val range = row?.get("range") as? JsonObject
        val defaultValue = when (val value = row?.get("default")) {
            is JsonPrimitive -> value.contentOrNull
            is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.joinToString("\n")
            else -> null
        }
        val gatewayTtl = config?.provider == "anthropic" && config.extendedCacheTTL != true &&
            definition.key == "promptCacheTtl"
        definition.copy(
            readOnly = gatewayTtl || row?.bool("readonly") == true,
            default = if (gatewayTtl) "5m" else defaultValue ?: definition.default,
            description = row?.text("description") ?: definition.description,
            min = range?.number("min") ?: definition.min,
            max = range?.number("max") ?: definition.max,
            step = range?.number("step") ?: definition.step,
            options = (row?.get("options") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                ?: definition.options,
            type = when (row?.text("type")) {
                "slider" -> ParameterType.SLIDER
                "dropdown" -> ParameterType.DROPDOWN
                "checkbox" -> ParameterType.CHECKBOX
                "switch" -> ParameterType.SWITCH
                "input", "text" -> ParameterType.TEXT
                "textarea" -> ParameterType.TEXTAREA
                "tags" -> ParameterType.TAGS
                else -> definition.type
            },
        )
    }
}

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
private fun JsonObject.number(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

/** A server-locked cache default takes precedence over stale local preferences. */
fun EndpointConfig?.promptCacheEnabled(localValue: String?): Boolean {
    val override = this?.customParams?.paramDefinitions?.lastOrNull { it.text("key") == "promptCache" }
    val configured = override?.bool("default")
    return if (override?.bool("readonly") == true) {
        configured ?: true
    } else {
        localValue?.toBooleanStrictOrNull() ?: configured ?: true
    }
}
