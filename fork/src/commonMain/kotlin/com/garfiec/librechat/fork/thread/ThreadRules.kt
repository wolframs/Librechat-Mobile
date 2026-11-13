package com.garfiec.librechat.fork.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.garfiec.librechat.fork.fonts.FontSlot
import com.garfiec.librechat.fork.fonts.slotTextStyle
import kotlinx.datetime.LocalDate

/** Compare model and endpoint, never the server's mutable sender caption. */
fun showsModelLine(model: String?, endpoint: String?, previousModel: String?, previousEndpoint: String?, hasPrevious: Boolean): Boolean =
    !hasPrevious || model != previousModel || endpoint != previousEndpoint

fun showsDayLabel(day: LocalDate?, previousDay: LocalDate?): Boolean = day != null && day != previousDay

/** Stable partition preserves upstream order within both sections. */
fun <T> usableGroupsFirst(groups: List<T>, needsKey: (T) -> Boolean): List<T> =
    groups.filterNot(needsKey) + groups.filter(needsKey)

@Suppress("CompositionLocalAllowlist") // Comparison panes opt out of primary-thread chrome.
val LocalQuietThread = compositionLocalOf { true }

@Suppress("CompositionLocalAllowlist") // Message role selects the pending font slot.
val LocalThreadUserText = compositionLocalOf { false }

@Composable
fun userMessageTextStyle(style: TextStyle): TextStyle = slotTextStyle(style, FontSlot.USER)

@Composable
fun modelReplyTextStyle(style: TextStyle): TextStyle = slotTextStyle(style, FontSlot.MODEL)

@Composable
fun threadTextStyle(style: TextStyle, isUser: Boolean): TextStyle =
    if (isUser) userMessageTextStyle(style) else modelReplyTextStyle(style)

/**
 * Headings are sized against the body text they sit in, so a reply's title never shouts over the
 * conversation, and their line height follows their size at every font scale.
 */
fun headingStyle(body: TextStyle, level: Int): TextStyle {
    val size = body.fontSize * when (level) {
        1 -> 1.4f
        2 -> 1.25f
        3 -> 1.125f
        else -> 1f
    }
    return body.copy(fontSize = size, lineHeight = size * 1.2f, fontWeight = FontWeight.SemiBold)
}

fun TextStyle.withLineSpacing(factor: Float): TextStyle = copy(lineHeight = fontSize * factor)
