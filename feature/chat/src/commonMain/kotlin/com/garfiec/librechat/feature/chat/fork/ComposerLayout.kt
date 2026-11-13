package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun ComposerRowsLayout(
    openness: () -> Float, field: @Composable () -> Unit, leading: @Composable () -> Unit,
    settings: @Composable () -> Unit, trailing: @Composable () -> Unit, modifier: Modifier = Modifier,
) {
    if (LocalComposer.current == null) {
        com.garfiec.librechat.feature.chat.components.FoldingComposerRows(openness, field, leading, settings, trailing, modifier)
    } else {
        Column(modifier) {
            field()
            settings()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { leading() }
                trailing()
            }
        }
    }
}
