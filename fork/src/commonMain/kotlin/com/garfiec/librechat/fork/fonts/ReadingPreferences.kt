package com.garfiec.librechat.fork.fonts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class FontSlot(val key: String) {
    INTERFACE("fork_font_interface"), USER("fork_font_user"), MODEL("fork_font_model")
}

enum class ReadingFont(val id: String, val displayName: String) {
    SYSTEM("system", "System"), ALEO("aleo", "Aleo"), INTER("inter", "Inter"),
    LITERATA("literata", "Literata"), SOURCE_SERIF("source_serif", "Source Serif"),
    ATKINSON("atkinson", "Atkinson Hyperlegible"), LORA("lora", "Lora");
    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: SYSTEM
    }
}

enum class ParagraphSpacing(val dp: Int) {
    COMPACT(2), COMFORTABLE(8), SPACIOUS(12), WIDE(20);
    val id: String get() = name.lowercase()
    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: COMFORTABLE
    }
}

/** Line height as a multiple of the text size; Regular is the type scale's own. */
enum class LineSpacing(val factor: Float) {
    TIGHT(1.35f), REGULAR(1.5f), RELAXED(1.65f);
    val id: String get() = name.lowercase()
    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: REGULAR
    }
}

data class ReadingPreferences(
    val interfaceFont: String = "system",
    val userFont: String = "same",
    val modelFont: String = "same",
    val spacing: ParagraphSpacing = ParagraphSpacing.COMFORTABLE,
    val lineSpacing: LineSpacing = LineSpacing.REGULAR,
) {
    fun stored(slot: FontSlot): String = when (slot) {
        FontSlot.INTERFACE -> interfaceFont
        FontSlot.USER -> userFont
        FontSlot.MODEL -> modelFont
    }
    fun resolved(slot: FontSlot): ReadingFont = ReadingFont.fromId(
        stored(slot).let { if (slot != FontSlot.INTERFACE && it == "same") interfaceFont else it },
    )
}

class ReadingPreferencesStore(dataStore: DataStore<Preferences>, scope: CoroutineScope, io: CoroutineDispatcher) {
    private val store = dataStore
    private val state = MutableStateFlow<ReadingPreferences?>(null)
    val prefs: StateFlow<ReadingPreferences?> = state.asStateFlow()
    init {
        scope.launch(io) {
            try {
                store.data.collect { state.value = decode(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                state.value = ReadingPreferences()
            }
        }
    }
    suspend fun setFont(slot: FontSlot, id: String) {
        store.edit { it[stringPreferencesKey(slot.key)] = id }
    }
    suspend fun setSpacing(spacing: ParagraphSpacing) {
        store.edit { it[stringPreferencesKey("chat_paragraph_spacing")] = spacing.id }
    }
    suspend fun setLineSpacing(spacing: LineSpacing) {
        store.edit { it[stringPreferencesKey(LINE_SPACING_KEY)] = spacing.id }
    }
    suspend fun reset() {
        store.edit { prefs ->
            FontSlot.entries.forEach { prefs.remove(stringPreferencesKey(it.key)) }
            prefs.remove(stringPreferencesKey("chat_paragraph_spacing"))
            prefs.remove(stringPreferencesKey(LINE_SPACING_KEY))
        }
    }
    companion object {
        private const val LINE_SPACING_KEY = "fork_line_spacing"
        fun decode(prefs: Preferences) = ReadingPreferences(
            interfaceFont = prefs[stringPreferencesKey(FontSlot.INTERFACE.key)] ?: "system",
            userFont = prefs[stringPreferencesKey(FontSlot.USER.key)] ?: "same",
            modelFont = prefs[stringPreferencesKey(FontSlot.MODEL.key)] ?: "same",
            spacing = ParagraphSpacing.fromId(prefs[stringPreferencesKey("chat_paragraph_spacing")]),
            lineSpacing = LineSpacing.fromId(prefs[stringPreferencesKey(LINE_SPACING_KEY)]),
        )
    }
}
