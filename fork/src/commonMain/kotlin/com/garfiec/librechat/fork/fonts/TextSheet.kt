package com.garfiec.librechat.fork.fonts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import kotlinx.coroutines.launch
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

@Composable
fun TextSettingsRow(modifier: Modifier = Modifier) {
    val open = rememberTextSheet()
    AdaptiveTextButton(onClick = open, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(textLabel()) }
}

/**
 * The sample stays first and everything fits one screen, so a change is seen where it is made. The
 * sheet opens at its full height for the same reason.
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
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Sample(prefs.spacing, size, Modifier.padding(horizontal = Inset))
            Spacer(Modifier.height(20.dp))
            AdaptiveSegmentedChoice(
                listOf(stringResource(Res.string.fonts_model), stringResource(Res.string.fonts_you),
                    stringResource(Res.string.fonts_interface)),
                SlotOrder.indexOf(slot), { slot = SlotOrder[it] }, Modifier.fillMaxWidth().padding(horizontal = Inset),
            )
            Spacer(Modifier.height(12.dp))
            FontRow(prefs, slot) { scope.launch { store.setFont(slot, it) } }
            Spacer(Modifier.height(20.dp))
            val spacingNames = listOf(Res.string.fonts_compact, Res.string.fonts_comfortable,
                Res.string.fonts_spacious, Res.string.fonts_wide).map { stringResource(it) }
            Choices(stringResource(Res.string.fonts_spacing), spacingNames, prefs.spacing.ordinal,
                { scope.launch { store.setSpacing(ParagraphSpacing.entries[it]) } }) { SpacingGlyph(ParagraphSpacing.entries[it]) }
            Spacer(Modifier.height(16.dp))
            val sizeNames = listOf(Res.string.fonts_small, Res.string.fonts_medium, Res.string.fonts_large)
                .map { stringResource(it) }
            Choices(stringResource(Res.string.fonts_size), sizeNames, size.ordinal,
                { scope.launch { settings.setChatFontSize(ChatFontSize.entries[it]) } }) {
                Text("A", style = MaterialTheme.typography.titleMedium.copy(fontSize = SizeGlyphSp[it].sp))
            }
        }
    }
}

/** A user bubble and a two-paragraph reply, drawn as the thread draws them. */
@Composable
private fun Sample(spacing: ParagraphSpacing, size: ChatFontSize, modifier: Modifier = Modifier) {
    val scale = sizeScale(size)
    val body = MaterialTheme.typography.bodyLarge
    Column(
        modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Text(
            stringResource(Res.string.fonts_sample_you),
            Modifier.align(Alignment.End)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .padding(12.dp),
            style = slotTextStyle(body, FontSlot.USER).scaled(scale),
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.fonts_sample_one), style = slotTextStyle(body, FontSlot.MODEL).scaled(scale))
        Spacer(Modifier.height(spacing.dp.dp))
        Text(stringResource(Res.string.fonts_sample_two), style = slotTextStyle(body, FontSlot.MODEL).scaled(scale))
    }
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
            ChoiceTile(checked == id, name, { onPick(id) }, Modifier.widthIn(min = 88.dp).heightIn(min = 76.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ag", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = face))
                    Text(name, style = MaterialTheme.typography.labelMedium.copy(fontFamily = face), maxLines = 1)
                }
            }
        }
    }
}

/** A titled row of equal tiles; the chosen one is named beside the title, so a tile needs no label. */
@Composable
private fun Choices(title: String, names: List<String>, selected: Int, onSelect: (Int) -> Unit, glyph: @Composable (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Inset)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(names[selected], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            names.forEachIndexed { index, name -> WeightedTile(index == selected, name, { onSelect(index) }) { glyph(index) } }
        }
    }
}

@Composable
private fun RowScope.WeightedTile(selected: Boolean, name: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    ChoiceTile(selected, name, onClick, Modifier.weight(1f).height(56.dp), content)
}

@Composable
private fun ChoiceTile(
    selected: Boolean, name: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.clip(TileShape)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh)
            .then(if (selected) Modifier.border(2.dp, colors.primary, TileShape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Two short paragraphs of two lines each, set apart by the gap the choice stands for. */
@Composable
private fun SpacingGlyph(spacing: ParagraphSpacing) {
    val line = Modifier.height(2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(1.dp))
    Column {
        repeat(2) { paragraph ->
            if (paragraph > 0) Spacer(Modifier.height((2 + spacing.dp / 2).dp))
            Box(line.width(24.dp))
            Spacer(Modifier.height(3.dp))
            Box(line.width(16.dp))
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
private val SizeGlyphSp = listOf(14, 18, 22)
private val Inset = 16.dp
private val TileShape = RoundedCornerShape(14.dp)
