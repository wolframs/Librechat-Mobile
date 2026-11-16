package com.garfiec.librechat.feature.chat.fork.keyerrors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.garfiec.librechat.core.ui.datetime.LocalDateTimeFormat
import com.garfiec.librechat.feature.chat.fork.LocalThreadIdentity
import com.garfiec.librechat.feature.chat.screen.endpointLabel
import com.garfiec.librechat.feature.chat.screen.userKeyErrorMessage
import com.garfiec.librechat.fork.keyerrors.ThreadKeyError
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun threadKeyErrorMessage(raw: String): String? {
    val error = remember(raw) { ThreadKeyError.parse(raw) } ?: return null
    // A saved error belongs to its payload's endpoint, never the composer's current selection.
    val label = error.endpointLabel(LocalThreadIdentity.current.configs)
    val message = userKeyErrorMessage(error, label, LocalDateTimeFormat.current)
    return stringResource(message.res, *message.args.toTypedArray())
}
