package com.garfiec.librechat.fork.fonts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garfiec.librechat.core.data.datastore.ChatFontSize
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.ui.components.AdaptiveModalBottomSheet
import com.garfiec.librechat.core.ui.components.AdaptiveSegmentedChoice
import com.garfiec.librechat.core.ui.components.AdaptiveTextButton
import com.garfiec.librechat.fork.composer.ComposerDragHandle
import com.garfiec.librechat.fork.composer.ComposerSheetHeader
import com.garfiec.librechat.fork.resources.*
import com.garfiec.librechat.fork.thread.headingStyle
import com.garfiec.librechat.fork.thread.withLineSpacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun textLabel(): String = stringResource(Res.string.fonts_text)

/** Owns the sheet without adding navigation state to either upstream host. */
@Composable
fun rememberTextSheet(): () -> Unit {
    var open by remember { mutableStateOf(false) }
    if (open) TextSheet { open = false }
    return { open = true }
}

/** The reply face by name, for a settings row that opens the sheet. */
@Composable
fun textSummary(): String = when (val face = LocalReadingPreferences.current.resolved(FontSlot.MODEL)) {
    ReadingFont.SYSTEM -> stringResource(Res.string.fonts_system)
    else -> face.displayName
}

/**
 * The sample stays in view above the controls, which scroll beneath it, so a change is seen where it
 * is made. The slot choice sits with the faces: it governs the typeface only, size and spacing are
 * the same for all three.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSheet(onDismiss: () -> Unit) {
    val store = koinInject<ReadingPreferencesStore>()
    val settings = koinInject<SettingsDataStore>()
    val scope = rememberCoroutineScope()
    val prefs = LocalReadingPreferences.current
    val size by settings.chatFontSize.collectAsState(initial = ChatFontSize.MEDIUM)
    var slot by remember { mutableStateOf(FontSlot.MODEL) }
    AdaptiveModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { ComposerDragHandle() },
    ) {
        ComposerSheetHeader(textLabel(), onDismiss)
        Sample(prefs, size, Modifier.padding(horizontal = Inset))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 16.dp)) {
            SectionTitle(stringResource(Res.string.fonts_typeface))
            AdaptiveSegmentedChoice(
                listOf(stringResource(Res.string.fonts_model), stringResource(Res.string.fonts_you),
                    stringResource(Res.string.fonts_interface)),
                SlotOrder.indexOf(slot), { slot = SlotOrder[it] }, Modifier.fillMaxWidth().padding(horizontal = Inset),
            )
            Spacer(Modifier.height(8.dp))
            FontRow(prefs, slot) { scope.launch { store.setFont(slot, it) } }
            Spacer(Modifier.height(SectionGap))
            Choices(Res.string.fonts_size, SizeNames, size.ordinal,
                { scope.launch { settings.setChatFontSize(ChatFontSize.entries[it]) } }) {
                Text("A", style = MaterialTheme.typography.titleMedium.copy(fontSize = SizeGlyphSp[it].sp, lineHeight = 24.sp))
            }
            Spacer(Modifier.height(SectionGap))
            Choices(Res.string.fonts_line, LineNames, prefs.lineSpacing.ordinal,
                { scope.launch { store.setLineSpacing(LineSpacing.entries[it]) } }) {
                LinesGlyph(lineGap = 3 + it * 2, paragraphGap = null)
            }
            Spacer(Modifier.height(SectionGap))
            Choices(Res.string.fonts_spacing, SpacingNames, prefs.spacing.ordinal,
                { scope.launch { store.setSpacing(ParagraphSpacing.entries[it]) } }) {
                LinesGlyph(lineGap = 3, paragraphGap = 4 + it * 3)
            }
            Spacer(Modifier.height(8.dp))
            AdaptiveTextButton(
                onClick = {
                    scope.launch {
                        store.reset()
                        settings.setChatFontSize(ChatFontSize.MEDIUM)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
            ) { Text(stringResource(Res.string.fonts_reset)) }
        }
    }
}

/** A user bubble, then a reply with a heading and two paragraphs, set as the thread sets them. */
@Composable
private fun Sample(prefs: ReadingPreferences, size: ChatFontSize, modifier: Modifier = Modifier) {
    val scale = sizeScale(size)
    val line = prefs.lineSpacing.factor
    val base = MaterialTheme.typography.bodyLarge
    val yours = slotTextStyle(base, FontSlot.USER).scaled(scale).withLineSpacing(line)
    val reply = slotTextStyle(base, FontSlot.MODEL).scaled(scale).withLineSpacing(line)
    Column(
        modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Text(
            stringResource(Res.string.fonts_sample_you),
            Modifier.align(Alignment.End)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            style = yours,
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.fonts_sample_heading), style = headingStyle(reply, 2))
        Spacer(Modifier.height(prefs.spacing.dp.dp))
        Text(emphasized(stringResource(Res.string.fonts_sample_one)), style = reply)
        Spacer(Modifier.height(prefs.spacing.dp.dp))
        Text(stringResource(Res.string.fonts_sample_two), style = reply)
    }
}

/** `**bold**` and `*italic*` in a sample string, so the sample shows both cuts of a face. */
private fun emphasized(text: String): AnnotatedString = buildAnnotatedString {
    var bold = false
    var italic = false
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> { bold = !bold; index += 2 }
            text[index] == '*' -> { italic = !italic; index += 1 }
            else -> {
                withStyle(SpanStyle(
                    fontWeight = if (bold) FontWeight.Bold else null,
                    fontStyle = if (italic) FontStyle.Italic else null,
                )) { append(text[index]) }
                index += 1
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.padding(start = Inset, end = Inset, bottom = 8.dp), style = MaterialTheme.typography.titleSmall)
}

/** One row of faces, each named in its own face; it scrolls sideways. */
@Composable
private fun FontRow(prefs: ReadingPreferences, slot: FontSlot, onPick: (String) -> Unit) {
    val options = (if (slot == FontSlot.INTERFACE) emptyList() else listOf("same")) + ReadingFont.entries.map { it.id }
    val checked = prefs.stored(slot).let { if (it in options) it else "system" }
    val families = LocalReadingFamilies.current
    val list = rememberLazyListState()
    // The chosen face is in view when the sheet opens and after a change of slot.
    LaunchedEffect(slot) { list.scrollToItem(options.indexOf(checked).coerceAtLeast(0)) }
    LazyRow(
        Modifier.fillMaxWidth().selectableGroup(), state = list,
        contentPadding = PaddingValues(horizontal = Inset), horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it }) { id ->
            val name = when (id) {
                "same" -> stringResource(Res.string.fonts_same)
                "system" -> stringResource(Res.string.fonts_system)
                else -> ReadingFont.fromId(id).displayName
            }
            val face = families[if (id == "same") prefs.resolved(FontSlot.INTERFACE) else ReadingFont.fromId(id)]
                ?: FontFamily.Default
            ChoiceTile(checked == id, { onPick(id) }, Modifier.widthIn(min = 84.dp)) {
                Text("Ag", style = MaterialTheme.typography.titleLarge.copy(fontFamily = face))
                Text(name, style = MaterialTheme.typography.labelMedium.copy(fontFamily = face), maxLines = 1)
            }
        }
    }
}

/** A titled row of equal tiles, each a glyph over its name. */
@Composable
private fun Choices(
    title: StringResource, names: List<StringResource>, selected: Int, onSelect: (Int) -> Unit, glyph: @Composable (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(title))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Inset).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            names.forEachIndexed { index, name ->
                ChoiceTile(index == selected, { onSelect(index) }, Modifier.weight(1f)) {
                    Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) { glyph(index) }
                    Text(
                        stringResource(name), style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The chosen tile is told by its outline and tint; glyph and name share the tile, so it reads as one answer. */
@Composable
private fun ChoiceTile(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.heightIn(min = 64.dp).clip(TileShape)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh)
            .then(if (selected) Modifier.border(1.5.dp, colors.primary, TileShape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        content = content,
    )
}

/**
 * Lines of text in small: three lines whose gap is the line spacing, or with [paragraphGap] two
 * paragraphs of two lines each, where only the gap between the paragraphs differs.
 */
@Composable
private fun LinesGlyph(lineGap: Int, paragraphGap: Int?) {
    val line = Modifier.height(2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(1.dp))
    Column {
        if (paragraphGap == null) {
            Box(line.width(26.dp))
            Spacer(Modifier.height(lineGap.dp))
            Box(line.width(26.dp))
            Spacer(Modifier.height(lineGap.dp))
            Box(line.width(16.dp))
        } else {
            repeat(2) { paragraph ->
                if (paragraph > 0) Spacer(Modifier.height(paragraphGap.dp))
                Box(line.width(26.dp))
                Spacer(Modifier.height(lineGap.dp))
                Box(line.width(16.dp))
            }
        }
    }
}

private fun TextStyle.scaled(scale: Float): TextStyle = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

private fun sizeScale(size: ChatFontSize): Float = when (size) {
    ChatFontSize.SMALL -> 0.85f
    ChatFontSize.MEDIUM -> 1f
    ChatFontSize.LARGE -> 1.2f
}

private val SlotOrder = listOf(FontSlot.MODEL, FontSlot.USER, FontSlot.INTERFACE)
private val SizeNames = listOf(Res.string.fonts_small, Res.string.fonts_medium, Res.string.fonts_large)
private val LineNames = listOf(Res.string.fonts_compact, Res.string.fonts_comfortable, Res.string.fonts_spacious)
private val SpacingNames = LineNames + Res.string.fonts_wide
private val SizeGlyphSp = listOf(14, 17, 21)
private val Inset = 16.dp
private val SectionGap = 16.dp
private val TileShape = RoundedCornerShape(14.dp)
