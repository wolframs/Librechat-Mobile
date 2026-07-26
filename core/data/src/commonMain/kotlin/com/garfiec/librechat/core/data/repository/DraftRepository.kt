package com.garfiec.librechat.core.data.repository

data class DraftSnapshot(
    val text: String,
    val stateJson: String?,
)

interface DraftRepository {
    suspend fun getDraft(conversationId: String): String?

    suspend fun getDraftState(conversationId: String): DraftSnapshot?

    /**
     * Like [getDraft] but suspends through the cold-start / post-migration warming window until the
     * active account resolves, then reads. Used to restore a draft into the composer on screen entry:
     * a one-shot [getDraft] there races identity resolution and returns null while the account is
     * still [com.garfiec.librechat.core.common.identity.AccountState.Warming], so the saved draft
     * would only reappear on a later launch.
     */
    suspend fun awaitDraft(conversationId: String): String?

    suspend fun awaitDraftState(conversationId: String): DraftSnapshot?

    suspend fun saveDraft(conversationId: String, text: String)

    /**
     * Atomically stores text and its feature-owned recovery payload. Blank text is valid when the
     * payload contains uploaded attachments or queued sends.
     */
    suspend fun saveDraftState(conversationId: String, text: String, stateJson: String?)

    suspend fun deleteDraft(conversationId: String)
}
