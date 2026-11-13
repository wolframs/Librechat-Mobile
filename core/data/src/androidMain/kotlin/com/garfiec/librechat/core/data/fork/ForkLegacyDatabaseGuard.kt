package com.garfiec.librechat.core.data.fork

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/** Room identity hash of the fork's own schema v10, which differs from upstream's v10. */
private const val FORK_V10_IDENTITY_HASH = "404c7600b0a0063cc516e0ace5975212"

/**
 * Deletes the cache database when it still has the fork's old schema. Must run before Room opens the
 * file: Room would apply upstream's 10 -> 11 migration to it and fail validation. The database is a
 * read-through cache, so the server refills it.
 */
fun discardForkLegacyDatabase(context: Context, name: String) {
    val file = context.getDatabasePath(name)
    if (!file.exists()) return
    val hash = runCatching {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.rawQuery("SELECT identity_hash FROM room_master_table LIMIT 1", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }.getOrNull()
    if (hash == FORK_V10_IDENTITY_HASH) context.deleteDatabase(name)
}
