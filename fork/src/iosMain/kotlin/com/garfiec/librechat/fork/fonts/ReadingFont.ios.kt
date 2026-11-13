package com.garfiec.librechat.fork.fonts

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
actual fun readingFont(resource: FontResource, file: String, weight: FontWeight, style: FontStyle): Font =
    Font(resource, weight, style)
