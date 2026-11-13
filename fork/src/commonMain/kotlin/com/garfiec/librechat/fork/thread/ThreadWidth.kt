package com.garfiec.librechat.fork.thread

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Reply text fills the row; a user's text takes its own width, so its bubble hugs it. */
@Composable
fun threadTextWidth(): Modifier = if (LocalThreadUserText.current) Modifier else Modifier.fillMaxWidth()
