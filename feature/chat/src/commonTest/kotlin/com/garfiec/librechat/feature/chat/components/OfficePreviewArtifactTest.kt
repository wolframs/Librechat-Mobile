package com.garfiec.librechat.feature.chat.components

import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.model.Attachment
import com.garfiec.librechat.feature.chat.components.artifact.ArtifactType
import com.garfiec.librechat.feature.chat.components.artifact.INLINE_FIT_MIN_HEIGHT
import com.garfiec.librechat.feature.chat.components.artifact.fitInlineHeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfficePreviewArtifactTest {

    private val sheet = Attachment(
        fileId = "f1",
        filename = "sales.xlsx",
        filepath = "/files/sales.xlsx",
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        text = "<table><tr><td>1</td></tr></table>",
        textFormat = "html",
    )

    @Test
    fun `the panel copy carries the original file so its overflow can open save and share it`() {
        val artifact = assertNotNull(officePreviewArtifact(sheet, isDarkTheme = false, inline = false))
        val source = assertNotNull(artifact.source)
        assertEquals("f1", source.fileId)
        assertEquals("/files/sales.xlsx", source.filepath)
        assertEquals("sales.xlsx", source.filename)
        assertEquals(sheet.type, source.mime)
        assertTrue(ArtifactType.isOfficePreviewMime(artifact.type))
    }

    @Test
    fun `a preview bucket type is not handed on as the file's MIME`() {
        val bucketed = sheet.copy(type = ArtifactType.DEFAULT_OFFICE_PREVIEW_MIME)
        val artifact = assertNotNull(officePreviewArtifact(bucketed, isDarkTheme = false, inline = false))
        assertNull(assertNotNull(artifact.source).mime)
    }

    @Test
    fun `a document with no text has no artifact`() {
        assertNull(officePreviewArtifact(sheet.copy(text = " "), isDarkTheme = false, inline = true))
    }

    @Test
    fun `a fitted slot follows the content between the minimum and the cap`() {
        assertEquals(240.dp, fitInlineHeight(240f, maxHeight = 600.dp))
        assertEquals(INLINE_FIT_MIN_HEIGHT, fitInlineHeight(0f, maxHeight = 600.dp))
        assertEquals(600.dp, fitInlineHeight(4_000f, maxHeight = 600.dp))
    }

    @Test
    fun `a cap below the minimum wins`() {
        assertEquals(80.dp, fitInlineHeight(200f, maxHeight = 80.dp))
    }
}
