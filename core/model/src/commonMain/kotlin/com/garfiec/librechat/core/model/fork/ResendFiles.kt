package com.garfiec.librechat.core.model.fork

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.floor

/**
 * How many earlier files a turn sends to the model again, as the operator's server stores it on a
 * conversation and a preset: `true` (or absent) every one, `false` or `0` none, a whole number the
 * most recent N. [limit] is null for every one. Files on the new message are always sent.
 *
 * Mirrors the server's `getResendFilesLimit` (packages/data-provider/src/schemas.ts on the backend
 * fork): anything that is neither `false` nor a number reads as every file.
 */
@Serializable(with = ResendFilesSerializer::class)
data class ResendFiles(val limit: Int?) {
    /** Whether any earlier file is resent. */
    val any: Boolean get() = limit != 0

    /** The N of "the most recent N", null for every file and for none. */
    val count: Int? get() = limit?.takeIf { it > 0 }

    /** `true`, `false` or the count: blank stores `true` and 0 stores `false`, as the web panel does. */
    fun wire(): JsonPrimitive = when (val n = limit) {
        null -> JsonPrimitive(true)
        0 -> JsonPrimitive(false)
        else -> JsonPrimitive(n)
    }

    companion object {
        val ALL = ResendFiles(null)
        val NONE = ResendFiles(0)

        /** The web panel's upper bound. */
        const val MAX = 1000

        fun of(any: Boolean, count: Int?): ResendFiles = when {
            !any -> NONE
            count == null || count <= 0 -> ALL
            else -> ResendFiles(count)
        }

        /**
         * Reads the count field's text: blank is every file, 0 none, N the most recent N (at most
         * [MAX]). Characters that are not digits are dropped, as the web field drops them. Also takes
         * `true` and `false`, which an agent's stored parameters carry.
         */
        fun parse(text: String): ResendFiles = when (text.trim()) {
            "true" -> ALL
            "false" -> NONE
            else -> when (val n = text.filter { it in '0'..'9' }.take(MAX.toString().length + 1).toIntOrNull()) {
                null -> ALL
                0 -> NONE
                else -> ResendFiles(minOf(n, MAX))
            }
        }
    }
}

/** What a request or an agent carries for the count field's [text], or null when [key] is another parameter. */
fun resendFilesWire(key: String, text: String): JsonElement? =
    if (key == "resendFiles") ResendFiles.parse(text).wire() else null

internal object ResendFilesSerializer : KSerializer<ResendFiles> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ResendFiles", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ResendFiles) =
        encoder.encodeSerializableValue(JsonElement.serializer(), value.wire())

    override fun deserialize(decoder: Decoder): ResendFiles {
        val value = decoder.decodeSerializableValue(JsonElement.serializer()) as? JsonPrimitive
        // A quoted "false" or "3" is not `false` or a number to the server either.
        if (value == null || value.isString) return ResendFiles.ALL
        value.booleanOrNull?.let { return if (it) ResendFiles.ALL else ResendFiles.NONE }
        val number = value.doubleOrNull?.takeIf { it.isFinite() } ?: return ResendFiles.ALL
        return ResendFiles(floor(number).coerceIn(0.0, Int.MAX_VALUE.toDouble()).toInt())
    }
}
