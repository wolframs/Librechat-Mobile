package com.garfiec.librechat.feature.chat.fork

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.fork.fonts.FontSlot
import com.garfiec.librechat.fork.fonts.LocalReadingFamilies
import com.garfiec.librechat.fork.fonts.LocalReadingPreferences
import com.garfiec.librechat.fork.thread.LocalThreadUserText
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

@Composable
fun readingTypography(base: MarkdownTypography): MarkdownTypography {
    val slot = if (LocalThreadUserText.current) FontSlot.USER else FontSlot.MODEL
    val family = LocalReadingFamilies.current[LocalReadingPreferences.current.resolved(slot)]
    return object : MarkdownTypography by base {
        override val h1 = base.h1.copy(fontFamily = family)
        override val h2 = base.h2.copy(fontFamily = family)
        override val h3 = base.h3.copy(fontFamily = family)
        override val h4 = base.h4.copy(fontFamily = family)
        override val h5 = base.h5.copy(fontFamily = family)
        override val h6 = base.h6.copy(fontFamily = family)
    }
}
