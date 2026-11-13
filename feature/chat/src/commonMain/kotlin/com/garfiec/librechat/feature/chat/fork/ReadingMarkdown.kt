package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.fork.fonts.FontSlot
import com.garfiec.librechat.fork.fonts.LocalReadingFamilies
import com.garfiec.librechat.fork.fonts.LocalReadingPreferences
import com.garfiec.librechat.fork.thread.LocalThreadUserText
import com.garfiec.librechat.fork.thread.headingStyle
import com.garfiec.librechat.fork.thread.withLineSpacing
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.State
import org.intellij.markdown.MarkdownTokenTypes

/** Preserve the renderer's own block padding and all component/search/citation behavior. */
@Composable
fun ReadingMarkdownBlocks(state: State.Success, components: MarkdownComponents, modifier: Modifier = Modifier) {
    val spacing = LocalReadingPreferences.current.spacing.dp.dp
    Column(modifier) {
        state.node.children.filter { it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE }
            .forEachIndexed { index, node ->
                if (index > 0) Spacer(Modifier.height(spacing))
                MarkdownElement(node, components, state.content)
            }
    }
}

/** Body text takes the chosen face and line spacing; headings are built from it, not from the display scale. */
@Composable
fun readingTypography(base: MarkdownTypography): MarkdownTypography {
    val prefs = LocalReadingPreferences.current
    val slot = if (LocalThreadUserText.current) FontSlot.USER else FontSlot.MODEL
    val family = LocalReadingFamilies.current[prefs.resolved(slot)]
    val line = prefs.lineSpacing.factor
    val body = base.paragraph.copy(fontFamily = family).withLineSpacing(line)
    fun heading(level: Int, color: Color) = headingStyle(body, level).copy(color = color)
    return object : MarkdownTypography by base {
        override val h1 = heading(1, base.h1.color)
        override val h2 = heading(2, base.h2.color)
        override val h3 = heading(3, base.h3.color)
        override val h4 = heading(4, base.h4.color)
        override val h5 = heading(5, base.h5.color)
        override val h6 = heading(6, base.h6.color)
        override val text = base.text.withLineSpacing(line)
        override val paragraph = base.paragraph.withLineSpacing(line)
        override val quote = base.quote.withLineSpacing(line)
        override val ordered = base.ordered.withLineSpacing(line)
        override val bullet = base.bullet.withLineSpacing(line)
        override val list = base.list.withLineSpacing(line)
    }
}
