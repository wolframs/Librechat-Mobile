package com.garfiec.librechat.fork.fonts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import com.garfiec.librechat.core.model.config.theme.ServerThemeDefinition
import com.garfiec.librechat.core.model.ui.UiStyle
import com.garfiec.librechat.core.ui.fork.fonts.LocalInterfaceFont
import com.garfiec.librechat.core.ui.theme.LibreChatTheme
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.FontResource
import org.koin.compose.koinInject

@Suppress("CompositionLocalAllowlist") // One snapshot for all text; no per-message flow collectors.
val LocalReadingPreferences = compositionLocalOf { ReadingPreferences() }

@Suppress("CompositionLocalAllowlist")
val LocalReadingFamilies = compositionLocalOf<Map<ReadingFont, FontFamily>> { emptyMap() }

@Composable
expect fun readingFont(resource: FontResource, file: String, weight: FontWeight, style: FontStyle): Font

@Composable
fun fontFamily(font: ReadingFont): FontFamily {
    if (font == ReadingFont.SYSTEM) return FontFamily.Default
    val roman = when (font) {
        ReadingFont.ALEO -> Res.font.aleo_variable
        ReadingFont.INTER -> Res.font.inter_variable
        ReadingFont.LITERATA -> Res.font.literata_variable
        ReadingFont.SOURCE_SERIF -> Res.font.source_serif_variable
        ReadingFont.ATKINSON -> Res.font.atkinson_variable
        else -> Res.font.lora_variable
    }
    val italic = when (font) {
        ReadingFont.ALEO -> Res.font.aleo_italic_variable
        ReadingFont.INTER -> Res.font.inter_italic_variable
        ReadingFont.LITERATA -> Res.font.literata_italic_variable
        ReadingFont.SOURCE_SERIF -> Res.font.source_serif_italic_variable
        ReadingFont.ATKINSON -> Res.font.atkinson_italic_variable
        else -> Res.font.lora_italic_variable
    }
    val fonts = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).flatMap { weight ->
        listOf(readingFont(roman, "${font.id}_variable.ttf", weight, FontStyle.Normal),
            readingFont(italic, "${font.id}_italic_variable.ttf", weight, FontStyle.Italic))
    }
    return remember(fonts) { FontFamily(fonts) }
}

@Composable
fun slotTextStyle(style: TextStyle, slot: FontSlot): TextStyle = style.copy(
    fontFamily = LocalReadingFamilies.current[LocalReadingPreferences.current.resolved(slot)] ?: FontFamily.Default,
    fontSynthesis = FontSynthesis.None,
)

@Composable
fun ReadingTheme(
    darkTheme: Boolean, accentColor: Color, useDynamicColor: Boolean,
    uiStyle: UiStyle?, serverTheme: ServerThemeDefinition?, content: @Composable () -> Unit,
) {
    val prefs by koinInject<ReadingPreferencesStore>().prefs.collectAsState()
    val ready = prefs ?: return
    val families = ReadingFont.entries.associateWith { fontFamily(it) }
    CompositionLocalProvider(
        LocalReadingPreferences provides ready,
        LocalReadingFamilies provides families,
        LocalInterfaceFont provides families.getValue(ready.resolved(FontSlot.INTERFACE)),
    ) {
        LibreChatTheme(darkTheme, accentColor, useDynamicColor, uiStyle, serverTheme, content)
    }
}
