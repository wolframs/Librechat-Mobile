package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.ConversationRepository
import com.garfiec.librechat.core.model.Conversation

/**
 * Loads the authoritative conversation snapshot needed by the parameter editor.
 *
 * List-cache rows may be sparse, so prefer the detail endpoint. A cached snapshot remains a useful
 * offline fallback once a prior detail response has populated it.
 */
internal suspend fun ConversationRepository.loadConversationSnapshot(
    conversationId: String,
): Result<Conversation> {
    val refreshed = refreshConversation(conversationId, originAccount = null)
    return if (refreshed is Result.Success) {
        refreshed
    } else {
        getConversation(conversationId, originAccount = null)
    }
}
