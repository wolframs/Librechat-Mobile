package com.garfiec.librechat.fork.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
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
