package com.garfiec.librechat.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.garfiec.librechat.core.common.identity.CrossAccount
import com.garfiec.librechat.core.data.db.entity.ConversationEntity
import com.garfiec.librechat.core.data.db.preserveLocalCacheFieldsFrom
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    // --- Account-scoped reads (row-tenancy). Every read filters accountId, incl. by-PK:
    // a known/stale/deep-linked id from the shared DB must not let account B read A's row. ---

    @Query(
        "SELECT * FROM conversations WHERE accountId = :accountId AND isArchived = :isArchived " +
            "ORDER BY pinned DESC, updatedAt DESC",
    )
    fun observeConversationsForAccount(accountId: String, isArchived: Boolean): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE conversationId = :id AND accountId = :accountId")
    suspend fun getByIdForAccount(id: String, accountId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE conversationId = :id AND accountId = :accountId")
    fun observeByIdForAccount(id: String, accountId: String): Flow<ConversationEntity?>

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    // Atomic read-merge-write that preserves locally-stored tags and a complete model-parameter
    // snapshot when a sparse server response omits either. Used by paths such as the list endpoint
    // and streaming save. Must run inside a single transaction so it can't be interleaved by a
    // concurrent local write. The body threads `accountId` (reads via `getByIdForAccount`), so it is
    // account-safe despite @CrossAccount suppressing the concrete-body lint.
    @CrossAccount
    @Transaction
    suspend fun upsertPreservingTags(accountId: String, entities: List<ConversationEntity>) {
        for (entity in entities) {
            if (entity.conversationId.isBlank()) {
                upsert(entity)
                continue
            }
            val existing = getByIdForAccount(entity.conversationId, accountId)
            upsert(entity.preserveLocalCacheFieldsFrom(existing))
        }
    }

    @CrossAccount
    suspend fun upsertPreservingTags(accountId: String, entity: ConversationEntity) {
        upsertPreservingTags(accountId, listOf(entity))
    }

    /**
     * Stores an authoritative detail response while retaining local-only tags.
     *
     * Unlike [upsertPreservingTags], a null [ConversationEntity.modelParams] clears an older
     * snapshot: it means the detail response resolved entirely to endpoint defaults, not that a
     * sparse list response omitted the fields.
     */
    @CrossAccount
    @Transaction
    suspend fun upsertAuthoritativeSnapshot(accountId: String, entity: ConversationEntity) {
        if (entity.conversationId.isBlank()) {
            upsert(entity)
            return
        }
        val existing = getByIdForAccount(entity.conversationId, accountId)
        upsert(
            entity.preserveLocalCacheFieldsFrom(
                existing = existing,
                preserveModelParamsWhenMissing = false,
            ),
        )
    }

    @Query("DELETE FROM conversations WHERE conversationId = :id AND accountId = :accountId")
    suspend fun deleteById(id: String, accountId: String)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE conversationId = :id AND accountId = :accountId")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long, accountId: String)

    @Query(
        "UPDATE conversations SET isArchived = :isArchived, updatedAt = :updatedAt " +
            "WHERE conversationId = :id AND accountId = :accountId",
    )
    suspend fun updateArchived(id: String, isArchived: Boolean, updatedAt: Long, accountId: String)

    @Query("UPDATE conversations SET pinned = :pinned WHERE conversationId = :id AND accountId = :accountId")
    suspend fun updatePinned(id: String, pinned: Boolean, accountId: String)

    @Query("UPDATE conversations SET tags = :tagsJson, updatedAt = :updatedAt WHERE conversationId = :id AND accountId = :accountId")
    suspend fun updateTags(id: String, tagsJson: String, updatedAt: Long, accountId: String)

    @Query("UPDATE conversations SET chatProjectId = :chatProjectId WHERE conversationId = :id AND accountId = :accountId")
    suspend fun updateChatProjectId(id: String, chatProjectId: String?, accountId: String)

    // Logout / account-remove scoped purge (the leak fix): delete only this account's rows.
    @Query("DELETE FROM conversations WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)
}
