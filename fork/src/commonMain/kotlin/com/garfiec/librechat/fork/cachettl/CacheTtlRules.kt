package com.garfiec.librechat.fork.cachettl

import com.garfiec.librechat.core.data.fork.cachettl.CacheTtlMarker
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.Message
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class CacheTtlRoute(val endpoint: String, val shown: Boolean, val canExtend: Boolean)

fun cacheTtlRoute(endpoint: String, model: String?, configs: Map<String, EndpointConfig>,
    parameters: Map<String, String>, agentProvider: String?, agentModel: String?,
    agentParameters: Map<String, String>): CacheTtlRoute {
    val agent = endpoint == "agents"
    val destination = if (agent) agentProvider.orEmpty() else endpoint
    val targetModel = if (agent) agentModel else model
    val config = configs[destination]
    val caching = (if (agent) agentParameters else parameters)["promptCache"] != "false"
    val supported = destination == "anthropic" || config?.provider == "anthropic" ||
        (config?.customParams?.defaultParamsEndpoint == "openrouter" && targetModel?.contains("claude", true) == true)
    val shown = caching && supported
    return CacheTtlRoute(destination, shown, shown && (destination == "anthropic" || config?.extendedCacheTTL == true))
}

fun agentCacheParameters(parameters: JsonElement?): Map<String, String> =
    ((parameters as? JsonObject)?.get("promptCache") as? JsonPrimitive)?.let { mapOf("promptCache" to it.content) }.orEmpty()

fun nextArm(arm: String?): String? = when (arm) { null -> "1h"; "1h" -> "5m"; else -> null }

/** Path order, not global timestamp order: sibling replies cannot start this branch's clock. */
fun cacheTtlAnchor(path: List<Message>, markers: List<CacheTtlMarker>): CacheTtlMarker? {
    val byId = markers.associateBy { it.messageId }
    return path.asReversed().firstNotNullOfOrNull { message ->
        byId[message.messageId]?.takeIf { !message.isCreatedByUser && !message.error && it.ttl in setOf("5m", "1h") }
    }
}

fun remainingMillis(anchor: CacheTtlMarker, now: Long): Long =
    anchor.anchorMillis + (if (anchor.ttl == "1h") 3_600_000L else 300_000L) - now

fun formatRemaining(millis: Long): String {
    val seconds = (millis.coerceAtLeast(0) + 999) / 1000
    return if (millis > 600_000) {
        "${(millis + 59_999) / 60_000}m"
    } else {
        "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
