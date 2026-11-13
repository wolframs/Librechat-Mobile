package com.garfiec.librechat.fork.thread

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.ui.theme.isLiquidGlass

/** Reply text fills the row; a user's text takes its own width, so its bubble hugs it. */
@Composable
fun threadTextWidth(): Modifier = if (LocalThreadUserText.current) Modifier else Modifier.fillMaxWidth()

/**
 * The thread fades out just above the composer and is hidden beneath it. Not under Liquid Glass,
 * where the composer samples the thread behind it.
 */
@Composable
fun threadComposerBackdrop(): Modifier {
    if (isLiquidGlass) return Modifier
    val color = MaterialTheme.colorScheme.background
    return Modifier.drawBehind {
        val fade = ComposerFade.toPx()
        drawRect(
            Brush.verticalGradient(listOf(color.copy(alpha = 0f), color), startY = -fade, endY = 0f),
            topLeft = Offset(0f, -fade), size = Size(size.width, fade),
        )
        drawRect(color)
    }
}

private val ComposerFade = 12.dp
