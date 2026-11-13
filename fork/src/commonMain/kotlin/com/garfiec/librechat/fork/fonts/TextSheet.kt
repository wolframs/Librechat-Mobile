package com.garfiec.librechat.fork.fonts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.data.datastore.ChatFontSize
import com.garfiec.librechat.core.data.datastore.SettingsDataStore
import com.garfiec.librechat.core.ui.components.AdaptiveCard
import com.garfiec.librechat.core.ui.components.AdaptiveModalBottomSheet
import com.garfiec.librechat.core.ui.components.AdaptiveRadioButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSheet(onDismiss: () -> Unit) {
    val store = koinInject<ReadingPreferencesStore>()
    val scope = rememberCoroutineScope()
    val prefs = LocalReadingPreferences.current
    var slot by remember { mutableStateOf(FontSlot.INTERFACE) }
    val settings = koinInject<SettingsDataStore>()
    val size by settings.chatFontSize.collectAsState(initial = ChatFontSize.MEDIUM)
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { ComposerDragHandle() }) {
        ComposerSheetHeader(textLabel(), onDismiss)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            AdaptiveSegmentedChoice(
                listOf(stringResource(Res.string.fonts_interface),
                    stringResource(Res.string.fonts_you), stringResource(Res.string.fonts_model)),
                slot.ordinal, { slot = FontSlot.entries[it] }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val options = (if (slot == FontSlot.INTERFACE) emptyList() else listOf("same")) + ReadingFont.entries.map { it.id }
            val checked = prefs.stored(slot).let { if (it in options) it else "system" }
            AdaptiveCard(Modifier.fillMaxWidth()) {
                options.forEach { id ->
                    val family = if (id == "same") prefs.resolved(FontSlot.INTERFACE) else ReadingFont.fromId(id)
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = checked == id, role = Role.RadioButton,
                            onClick = { scope.launch { store.setFont(slot, id) } },
                        ).heightIn(min = 56.dp).padding(horizontal = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        AdaptiveRadioButton(selected = checked == id, onClick = null)
                        Text(
                            when (id) {
                                "same" -> stringResource(Res.string.fonts_same)
                                "system" -> stringResource(Res.string.fonts_system)
                                else -> ReadingFont.fromId(id).displayName
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = LocalReadingFamilies.current[family] ?: FontFamily.Default,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.fonts_spacing), style = MaterialTheme.typography.titleSmall)
            AdaptiveSegmentedChoice(
                listOf(stringResource(Res.string.fonts_compact), stringResource(Res.string.fonts_comfortable),
                    stringResource(Res.string.fonts_spacious), stringResource(Res.string.fonts_wide)),
                prefs.spacing.ordinal, { scope.launch { store.setSpacing(ParagraphSpacing.entries[it]) } }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.fonts_size), style = MaterialTheme.typography.titleSmall)
            AdaptiveSegmentedChoice(
                listOf(stringResource(Res.string.fonts_small),
                    stringResource(Res.string.fonts_medium), stringResource(Res.string.fonts_large)),
                size.ordinal, { scope.launch { settings.setChatFontSize(ChatFontSize.entries[it]) } }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.fonts_you), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(Res.string.fonts_sample_you), style = slotTextStyle(MaterialTheme.typography.bodyLarge, FontSlot.USER))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.fonts_model), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(Res.string.fonts_sample_one), style = slotTextStyle(MaterialTheme.typography.bodyLarge, FontSlot.MODEL))
            Spacer(Modifier.height(prefs.spacing.dp.dp))
            Text(stringResource(Res.string.fonts_sample_two), style = slotTextStyle(MaterialTheme.typography.bodyLarge, FontSlot.MODEL))
            Spacer(Modifier.height(24.dp))
        }
    }
}
