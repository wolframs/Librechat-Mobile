package com.garfiec.librechat.fork.modelseed

import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.core.model.endpoint.KeyState

/** First usable row in catalogue order; null waits for config or an earlier unresolved key. */
fun firstModelEntry(
    models: Map<String, List<String>>,
    configs: Map<String, EndpointConfig>,
    keys: Map<String, KeyState>,
): Map.Entry<String, List<String>>? {
    val candidates = models.entries.filter { it.value.isNotEmpty() }
    for (entry in candidates) {
        val config = configs[entry.key] ?: return null
        if (config.userProvide != true && config.userProvideURL != true) return entry
        when (keys[entry.key]) {
            is KeyState.Set -> return entry
            KeyState.Loading, null -> return null
            KeyState.Unset, is KeyState.Expired -> Unit
        }
    }
    // No usable row: retain the existing first-provider setup path.
    return candidates.firstOrNull()
}
