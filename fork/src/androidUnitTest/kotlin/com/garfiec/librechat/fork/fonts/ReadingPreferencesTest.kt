package com.garfiec.librechat.fork.fonts

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingPreferencesTest {
    @Test fun defaultsKeepSystemAndInherit() {
        val prefs = ReadingPreferencesStore.decode(emptyPreferences())
        assertEquals(ReadingPreferences(), prefs)
        FontSlot.entries.forEach { assertEquals(ReadingFont.SYSTEM, prefs.resolved(it)) }
        assertEquals("same", prefs.userFont)
        assertEquals("same", prefs.modelFont)
    }

    @Test fun slotsInheritInterfaceOrOverrideIndependently() {
        val prefs = ReadingPreferences(interfaceFont = "literata", modelFont = "inter")
        assertEquals(ReadingFont.LITERATA, prefs.resolved(FontSlot.USER))
        assertEquals(ReadingFont.INTER, prefs.resolved(FontSlot.MODEL))
        assertEquals(ReadingFont.LITERATA, prefs.resolved(FontSlot.INTERFACE))
    }

    @Test fun unknownIdsFallBackToSystemIncludingInheritedValues() {
        val prefs = ReadingPreferences(interfaceFont = "missing", modelFont = "unknown")
        FontSlot.entries.forEach { assertEquals(ReadingFont.SYSTEM, prefs.resolved(it)) }
        assertEquals(ReadingFont.SYSTEM, ReadingPreferences(interfaceFont = "same").resolved(FontSlot.INTERFACE))
    }

    @Test fun oldSpacingStringsAndNewWideKeepTheirValues() {
        listOf("compact" to 2, "comfortable" to 8, "spacious" to 12, "wide" to 20).forEach { (id, dp) ->
            val decoded = ReadingPreferencesStore.decode(preferencesOf(stringPreferencesKey("chat_paragraph_spacing") to id))
            assertEquals(id, decoded.spacing.id)
            assertEquals(dp, decoded.spacing.dp)
        }
        assertEquals(ParagraphSpacing.COMFORTABLE, ParagraphSpacing.fromId(null))
        assertEquals(ParagraphSpacing.COMFORTABLE, ParagraphSpacing.fromId("unknown"))
    }

    @Test fun persistedSlotKeysAreRead() {
        val prefs = ReadingPreferencesStore.decode(preferencesOf(
            stringPreferencesKey("fork_font_interface") to "aleo",
            stringPreferencesKey("fork_font_user") to "lora",
            stringPreferencesKey("fork_font_model") to "atkinson",
        ))
        assertEquals(ReadingFont.ALEO, prefs.resolved(FontSlot.INTERFACE))
        assertEquals(ReadingFont.LORA, prefs.resolved(FontSlot.USER))
        assertEquals(ReadingFont.ATKINSON, prefs.resolved(FontSlot.MODEL))
    }
}
