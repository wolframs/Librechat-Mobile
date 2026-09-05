package com.garfiec.librechat.core.data.db

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exercise Room's generated migration from the installed fork's v9, not upstream's unrelated v9. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForkMigration9To10Test {
    @Test fun addsUpstreamTablesWithoutLosingDraftPayloadsOrMessageTtl() {
        val db = AndroidSQLiteDriver().open(":memory:")
        try {
            db.execSQL("CREATE TABLE drafts (conversation_id TEXT PRIMARY KEY, text TEXT, state_json TEXT)")
            db.execSQL("CREATE TABLE messages (messageId TEXT PRIMARY KEY, cacheTTL TEXT)")
            db.execSQL("INSERT INTO drafts VALUES ('c1', 'unsent', '{\"queuedMessages\":[\"keep\"]}')")
            db.execSQL("INSERT INTO messages VALUES ('m1', '1h')")
            LibreChatDatabase_AutoMigration_9_10_Impl().migrate(db)
            db.prepare("SELECT text, state_json FROM drafts").use {
                assertTrue(it.step())
                assertEquals("unsent", it.getText(0))
                assertEquals("{\"queuedMessages\":[\"keep\"]}", it.getText(1))
            }
            db.prepare("SELECT cacheTTL FROM messages").use {
                assertTrue(it.step())
                assertEquals("1h", it.getText(0))
            }
            db.execSQL("INSERT INTO servers VALUES ('server-a', '{}')")
            db.execSQL("INSERT INTO prefetch_watermarks VALUES ('account-a', 'c1', 1, 2)")
            db.execSQL("INSERT INTO prefetch_watermarks VALUES ('account-b', 'c1', 3, 4)")
            db.prepare("SELECT COUNT(*) FROM prefetch_watermarks").use {
                assertTrue(it.step())
                assertEquals(2L, it.getLong(0))
            }
        } finally {
            db.close()
        }
    }
}
