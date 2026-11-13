package com.garfiec.librechat.core.ui.fork.thread

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import com.garfiec.librechat.core.model.ui.GlassCapability
import com.garfiec.librechat.core.ui.components.LocalInSeparateWindow
import com.garfiec.librechat.core.ui.theme.LocalGlassLevel

/** The native iOS bar keeps its platform controls and rename interaction. */
@Composable
fun useThreadHeader(): Boolean = LocalGlassLevel.current != GlassCapability.NATIVE || LocalInSeparateWindow.current

@Composable
fun opaqueThreadHeader(modifier: androidx.compose.ui.Modifier): androidx.compose.ui.Modifier =
    modifier.then(androidx.compose.ui.Modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background))
