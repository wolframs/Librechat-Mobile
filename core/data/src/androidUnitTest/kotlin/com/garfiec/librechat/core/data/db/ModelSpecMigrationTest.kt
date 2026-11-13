package com.garfiec.librechat.core.data.db

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.garfiec.librechat.core.data.mapper.toEntity
import com.garfiec.librechat.core.data.mapper.toModel
import com.garfiec.librechat.core.model.Conversation
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelSpecMigrationTest {
    @Test
    fun migrationPreservesLegacyConversationsAndAddsNullableSpec() {
        val connection = AndroidSQLiteDriver().open(":memory:")
        try {
            connection.execSQL("CREATE TABLE conversations (conversationId TEXT PRIMARY KEY, title TEXT)")
            connection.execSQL("INSERT INTO conversations VALUES ('existing', 'Existing chat')")
            LibreChatDatabase_AutoMigration_10_11_Impl().migrate(connection)
            connection.prepare("SELECT title, spec FROM conversations WHERE conversationId = 'existing'").use {
                assertTrue(it.step())
                assertEquals("Existing chat", it.getText(0))
                assertTrue(it.isNull(1))
            }
            connection.execSQL("UPDATE conversations SET spec = 'curated-spec' WHERE conversationId = 'existing'")
            connection.prepare("SELECT spec FROM conversations").use {
                assertTrue(it.step())
                assertEquals("curated-spec", it.getText(0))
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun conversationCacheRetainsTheSpecName() {
        val cached = Conversation(conversationId = "chat", spec = "curated-spec").toEntity()
        assertEquals("curated-spec", cached.toModel().spec)
    }
}
