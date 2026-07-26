package com.garfiec.librechat.core.data.db

import com.garfiec.librechat.core.data.db.entity.ConversationEntity

/**
 * Merges local-only cache fields into an incoming server row.
 *
 * Tags are always preserved because some list/stream responses omit them. Model parameters are
 * preserved only when the incoming response is sparse; a complete conversation refresh replaces
 * the older snapshot.
 */
internal fun ConversationEntity.preserveLocalCacheFieldsFrom(
    existing: ConversationEntity?,
    preserveModelParamsWhenMissing: Boolean = true,
): ConversationEntity {
    if (existing == null) return this
    return copy(
        tags = existing.tags,
        modelParams = if (preserveModelParamsWhenMissing) {
            modelParams ?: existing.modelParams
        } else {
            modelParams
        },
    )
}
