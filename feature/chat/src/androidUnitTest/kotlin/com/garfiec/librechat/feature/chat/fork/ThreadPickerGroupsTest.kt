package com.garfiec.librechat.feature.chat.fork

import com.garfiec.librechat.feature.chat.util.HeaderIcon
import com.garfiec.librechat.feature.chat.util.HeaderLabel
import com.garfiec.librechat.feature.chat.util.KeyHint
import com.garfiec.librechat.feature.chat.util.PickerRow
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadPickerGroupsTest {
    @Test fun movesWholeBlockedGroupsAfterUsableOnes() {
        fun header(key: String, hint: KeyHint? = null) = PickerRow.Header(
            key, HeaderLabel.Text(key), HeaderIcon.Folder, 1, true, key, hint, key,
        )
        val blocked = header("key", KeyHint.NEEDS_KEY)
        val usable = header("usable")
        val model = PickerRow.Model("model", "usable", "a", false, false, true)
        val rows = listOf(PickerRow.SpecOnlyNote, blocked, usable, model)
        assertEquals(listOf(listOf(PickerRow.SpecOnlyNote), listOf(usable, model), listOf(blocked)), threadPickerGroups(rows))
    }
}
