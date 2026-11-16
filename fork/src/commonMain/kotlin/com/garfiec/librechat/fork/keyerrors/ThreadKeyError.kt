package com.garfiec.librechat.fork.keyerrors

import com.garfiec.librechat.core.model.error.UserKeyError
import com.garfiec.librechat.core.model.error.parseUserKeyError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Key failures in saved replies use the snackbar parser, with invalid-key copy for an undated expiry. */
object ThreadKeyError {
    fun parse(raw: String): UserKeyError? {
        parseUserKeyError(raw)?.let { return it }
        val obj = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return null
        if ((obj["type"] as? JsonPrimitive)?.contentOrNull != "expired_user_key") return null
        return UserKeyError.InvalidUserKey((obj["endpoint"] as? JsonPrimitive)?.contentOrNull)
    }
}
