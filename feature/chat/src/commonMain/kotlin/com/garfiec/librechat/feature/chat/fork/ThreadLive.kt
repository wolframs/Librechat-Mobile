package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.model.EndpointConfig
import com.garfiec.librechat.feature.chat.components.streamingContentColumnPadding
import com.garfiec.librechat.feature.chat.util.MessageNode
import com.garfiec.librechat.feature.chat.viewmodel.ChatUiState
import com.garfiec.librechat.fork.thread.LocalQuietThread
import com.garfiec.librechat.fork.thread.showsModelLine

internal data class ThreadIdentity(val model: String?, val endpoint: String?, val configs: Map<String, EndpointConfig>)

@Suppress("CompositionLocalAllowlist") // Primary thread presentation context, scoped above the list.
internal val LocalThreadIdentity = compositionLocalOf { ThreadIdentity(null, null, emptyMap()) }

@Composable
internal fun ThreadIdentityScope(state: ChatUiState, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalThreadIdentity provides ThreadIdentity(state.selectedModel, state.selectedEndpoint, state.endpointConfigs),
        content = content,
    )
}

@Composable
internal fun liveReplyLabel(model: String, endpoint: String?, path: List<MessageNode>): String {
    if (!LocalQuietThread.current) return model
    val identity = LocalThreadIdentity.current
    val actualModel = identity.model ?: model
    val actualEndpoint = identity.endpoint ?: endpoint
    val previous = path.lastOrNull { !it.message.isCreatedByUser }?.message
    return if (showsModelLine(actualModel, actualEndpoint, previous?.model, previous?.endpoint, previous != null)) {
        replyLabel(actualModel, actualEndpoint)
    } else {
        ""
    }
}

@Composable
internal fun threadContentPadding(layout: String, avatars: Boolean, bubbles: Boolean): Modifier =
    if (LocalQuietThread.current) {
        Modifier.padding(horizontal = 16.dp)
    } else {
        Modifier.streamingContentColumnPadding(layout, avatars, bubbles)
    }

@Composable
internal fun threadLayout(layout: String): String =
    if (LocalQuietThread.current) com.garfiec.librechat.core.common.ChatLayoutConstants.THREAD else layout

internal const val ShowLandingHero = false

internal fun hiddenLandingIdentity(value: String?): String? = value?.takeIf { ShowLandingHero }

@Composable
internal fun CompareThreadScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalQuietThread provides false, content = content)
}
