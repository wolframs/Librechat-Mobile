package com.garfiec.librechat.fork.fonts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.garfiec.librechat.fork.resources.Res
import org.jetbrains.compose.resources.FontResource

@Composable
actual fun readingFont(resource: FontResource, file: String, weight: FontWeight, style: FontStyle): Font {
    val assets = LocalContext.current.assets
    return remember(file, weight, style, assets) {
        Font(Res.getUri("font/$file").removePrefix("file:///android_asset/"), assets,
            weight = weight, style = style,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }
}
