package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.datetime.parseIsoInstantOrNull
import com.garfiec.librechat.core.model.FeedbackRating
import com.garfiec.librechat.core.model.Message
import com.garfiec.librechat.core.model.MinimalFeedback
import com.garfiec.librechat.core.model.media.IconSource
import com.garfiec.librechat.core.ui.datetime.LocalDateTimeFormat
import com.garfiec.librechat.feature.chat.components.MessageBubble
import com.garfiec.librechat.feature.chat.components.MessageContentAndActions
import com.garfiec.librechat.feature.chat.components.MessageEditSession
import com.garfiec.librechat.feature.chat.components.MessageTimestamp
import com.garfiec.librechat.feature.chat.components.SearchHighlightOrange
import com.garfiec.librechat.feature.chat.components.SearchHighlightYellow
import com.garfiec.librechat.feature.chat.components.rememberActionRowShown
import com.garfiec.librechat.feature.chat.components.sendflight.sendFlightBubble
import com.garfiec.librechat.feature.chat.components.sendflight.sendFlightHeader
import com.garfiec.librechat.fork.resources.thread_today
import com.garfiec.librechat.fork.resources.thread_yesterday
import com.garfiec.librechat.fork.thread.LocalQuietThread
import com.garfiec.librechat.fork.thread.LocalThreadUserText
import com.garfiec.librechat.fork.thread.showsDayLabel
import com.garfiec.librechat.fork.thread.showsModelLine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import com.garfiec.librechat.fork.resources.Res as ForkRes

@Composable
internal fun ThreadMessage(
    path: List<com.garfiec.librechat.feature.chat.util.MessageNode>,
    index: Int,
    message: Message,
    siblingIndex: Int,
    siblingCount: Int,
    onSiblingNavigation: ((Int) -> Unit)?,
    onEdit: (() -> Unit)?,
    onRegenerate: (() -> Unit)?,
    onCopy: (() -> Unit)?,
    onFeedback: ((MinimalFeedback?) -> Unit)?,
    onReadAloud: (() -> Unit)?,
    onFork: (() -> Unit)?,
    baseUrl: String,
    fontSizeMultiplier: Float,
    isReading: Boolean,
    currentFeedback: FeedbackRating?,
    editSession: MessageEditSession?,
    userAvatarUrl: String?,
    userName: String?,
    showImageDescriptions: Boolean,
    showActionsInitially: Boolean,
    searchQuery: String?,
    isSearchMatch: Boolean,
    isCurrentSearchMatch: Boolean,
    searchFocusedOccurrence: Int,
    onFocusedOccurrencePosition: ((LayoutCoordinates, Rect) -> Unit)?,
    useKatex: Boolean,
    chatLayoutStyle: String,
    showAvatars: Boolean,
    showBubbles: Boolean,
    avatar: IconSource?,
    modifier: Modifier = Modifier,
) {
    if (!LocalQuietThread.current) {
        MessageBubble(
            message = message,
            modifier = modifier,
            siblingIndex = siblingIndex,
            siblingCount = siblingCount,
            onSiblingNavigation = onSiblingNavigation,
            onEdit = onEdit,
            onRegenerate = onRegenerate,
            onCopy = onCopy,
            onFeedback = onFeedback,
            onReadAloud = onReadAloud,
            onFork = onFork,
            baseUrl = baseUrl,
            fontSizeMultiplier = fontSizeMultiplier,
            isReading = isReading,
            currentFeedback = currentFeedback,
            editSession = editSession,
            userAvatarUrl = userAvatarUrl,
            userName = userName,
            showImageDescriptions = showImageDescriptions,
            showActionsInitially = showActionsInitially,
            searchQuery = searchQuery,
            isSearchMatch = isSearchMatch,
            isCurrentSearchMatch = isCurrentSearchMatch,
            searchFocusedOccurrence = searchFocusedOccurrence,
            onFocusedOccurrencePosition = onFocusedOccurrencePosition,
            useKatex = useKatex,
            chatLayoutStyle = chatLayoutStyle,
            showAvatars = showAvatars,
            showBubbles = showBubbles,
            avatar = avatar,
        )
        return
    }
    val isUser = message.isCreatedByUser
    var showActions by rememberActionRowShown(message.messageId, showActionsInitially, isReading)
    val focusManager = LocalFocusManager.current
    val zone = TimeZone.currentSystemDefault()
    val day = message.createdAt?.let(::parseIsoInstantOrNull)?.toLocalDateTime(zone)?.date
    val previousDay = path.getOrNull(index - 1)?.message?.createdAt?.let(::parseIsoInstantOrNull)?.toLocalDateTime(zone)?.date
    Column(modifier.fillMaxWidth()) {
        if (showsDayLabel(day, previousDay)) {
            val today = Clock.System.now().toLocalDateTime(zone).date
            val label = when (day) {
                today -> stringResource(ForkRes.string.thread_today)
                today.minus(1, DateTimeUnit.DAY) -> stringResource(ForkRes.string.thread_yesterday)
                else -> LocalDateTimeFormat.current.let { it.format(day!!.atStartOfDayIn(zone), it.patterns.dateWithYear, zone) }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart) {
            Column(
                modifier = Modifier.fillMaxWidth(if (isUser) .8f else 1f)
                    .clickable(interactionSource = null, indication = null) {
                        focusManager.clearFocus()
                        showActions = !showActions
                    },
            ) {
                // Keep the flight header slot even when a user has no visible sender caption.
                Column(Modifier.then(sendFlightHeader(message.messageId, isUser))) {
                    if (!isUser) ReplyModelLine(message, path.subList(0, index).lastOrNull { !it.message.isCreatedByUser }?.message)
                }
                Column(Modifier.then(sendFlightBubble(message.messageId, isUser))
                    .background(
                        when {
                            isCurrentSearchMatch -> SearchHighlightOrange.copy(alpha = .18f)
                            isSearchMatch -> SearchHighlightYellow.copy(alpha = .12f)
                            isUser -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        },
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = if (isUser) 4.dp else 16.dp),
                    ).then(if (isUser) Modifier.padding(12.dp) else Modifier)) {
                    CompositionLocalProvider(LocalThreadUserText provides isUser) {
                        MessageContentAndActions(
                            message = message, avatar = avatar, isUser = isUser, editSession = editSession,
                            baseUrl = baseUrl, fontSizeMultiplier = fontSizeMultiplier, useKatex = useKatex,
                            showImageDescriptions = showImageDescriptions, searchQuery = searchQuery,
                            isSearchMatch = isSearchMatch, isCurrentSearchMatch = isCurrentSearchMatch,
                            searchFocusedOccurrence = searchFocusedOccurrence, onFocusedOccurrencePosition = onFocusedOccurrencePosition,
                            showActions = showActions, siblingIndex = siblingIndex, siblingCount = siblingCount,
                            onSiblingNavigation = onSiblingNavigation, onEdit = onEdit, onRegenerate = onRegenerate,
                            onCopy = onCopy, onFeedback = onFeedback, onReadAloud = onReadAloud, onFork = onFork,
                            isReading = isReading, currentFeedback = currentFeedback, userName = userName,
                            userAvatarUrl = userAvatarUrl, alignImagesEnd = isUser,
                        )
                    }
                    if (showActions) message.createdAt?.let { MessageTimestamp(it) }
                }
            }
        }
    }
}

@Composable
private fun ReplyModelLine(message: Message, previous: Message?) {
    if (showsModelLine(message.model, message.endpoint, previous?.model, previous?.endpoint, previous != null)) {
        Text(replyLabel(message.model ?: message.sender.orEmpty(), message.endpoint),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
internal fun replyLabel(model: String, endpoint: String?): String {
    val configs = LocalThreadIdentity.current.configs
    return listOfNotNull(shortModelName(model), endpoint?.takeIf { it.isNotBlank() }?.let {
        com.garfiec.librechat.core.model.endpoint.endpointDisplayName(it, configs[it])
    }).joinToString(" · ")
}
