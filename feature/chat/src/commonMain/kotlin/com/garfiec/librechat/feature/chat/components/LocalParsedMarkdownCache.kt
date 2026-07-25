package com.garfiec.librechat.feature.chat.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.garfiec.librechat.core.data.datastore.ChatParagraphSpacing

val LocalParsedMarkdownCache = staticCompositionLocalOf<ParsedMarkdownCache> {
    error("LocalParsedMarkdownCache not provided; wrap chat content in ChatRoot { }")
}

/**
 * When true, [CachedMarkdown] parses its first (cache-miss) render synchronously instead of
 * the library's async `Loading → Success` flow. `MessageList` provides it around the last
 * message — the slot a just-finalized streaming reply lands in, where the async `Loading`
 * frame renders at near-zero height, collapsing the bottom item and forcing the list to
 * re-anchor (the completion "flash"). Synchronous parse keeps it full-height from frame one.
 * Defaults to false so off-screen / historical messages keep the cheap async path.
 */
val LocalImmediateMarkdown = compositionLocalOf { false }

/** Chat-wide vertical rhythm used by every native Markdown surface. */
val LocalChatParagraphSpacing = compositionLocalOf { ChatParagraphSpacing.COMFORTABLE }
