package com.garfiec.librechat.core.data.datastore

/**
 * Extra vertical rhythm between top-level Markdown blocks in chat messages.
 *
 * [blockSpacingDp] stays as a plain number so :core:data does not depend on Compose UI units.
 */
enum class ChatParagraphSpacing(val blockSpacingDp: Int) {
    COMPACT(2),
    COMFORTABLE(8),
    SPACIOUS(12),
    ;

    companion object {
        fun fromString(value: String?): ChatParagraphSpacing = when (value) {
            "compact" -> COMPACT
            "spacious" -> SPACIOUS
            else -> COMFORTABLE
        }
    }

    fun toStorageString(): String = when (this) {
        COMPACT -> "compact"
        COMFORTABLE -> "comfortable"
        SPACIOUS -> "spacious"
    }
}
