package com.garfiec.librechat.fork.cachettl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Owned by one chat VM, including its unsaved landing slot. Queue drains never take this arm. */
class CacheTtlSession {
    var arm: String? by mutableStateOf(null)
        private set
    fun reconcile(canExtend: Boolean) { if (!canExtend) arm = null }
    fun cycle(canExtend: Boolean) { arm = if (canExtend) nextArm(arm) else null }
    fun payload(base: JsonObject?, route: CacheTtlRoute, direct: Boolean): JsonObject? {
        reconcile(route.canExtend)
        if (!direct) return base
        val taken = arm
        arm = null
        return if (taken == null) base else JsonObject(base.orEmpty() + ("cacheTTL" to JsonPrimitive(taken)))
    }
}
