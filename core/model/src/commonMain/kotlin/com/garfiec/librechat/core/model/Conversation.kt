package com.garfiec.librechat.core.model

import com.garfiec.librechat.core.model.serializer.LenientInstantSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Conversation(
    val conversationId: String? = null,
    val title: String? = "New Chat",
    val user: String? = null,
    val endpoint: String? = null,
    val endpointType: String? = null,
    val model: String? = null,
    @SerialName("agent_id") val agentId: String? = null,
    @SerialName("assistant_id") val assistantId: String? = null,
    val tags: List<String> = emptyList(),
    val isArchived: Boolean = false,
    /**
     * Whether a shared link currently exists for this conversation.
     *
     * Derived per **list** request from one batched SharedLink lookup and deliberately NOT
     * persisted, so it is absent from single-conversation payloads and from anything read back
     * out of the local cache. The whole pass is also skipped when `ALLOW_SHARED_LINKS` is off,
     * and a lookup failure drops the flag rather than failing the list — so null means "not
     * known here", never "not shared", and nothing may render an absence as a negative.
     */
    val isShared: Boolean? = null,
    val temperature: Double? = null,
    @SerialName("top_p") @JsonNames("topP") val topP: Double? = null,
    val topK: Int? = null,
    @SerialName("frequency_penalty") @JsonNames("frequencyPenalty") val frequencyPenalty: Double? = null,
    @SerialName("presence_penalty") @JsonNames("presencePenalty") val presencePenalty: Double? = null,
    val maxOutputTokens: Int? = null,
    val maxContextTokens: Int? = null,
    @SerialName("max_tokens") @JsonNames("maxTokens") val maxTokens: Int? = null,
    val system: String? = null,
    val promptPrefix: String? = null,
    val modelLabel: String? = null,
    val chatGptLabel: String? = null,
    @SerialName("reasoning_effort") @JsonNames("reasoningEffort") val reasoningEffort: String? = null,
    @SerialName("reasoning_summary") @JsonNames("reasoningSummary") val reasoningSummary: String? = null,
    /** Reasoning mode for Responses-API models (sibling of [reasoningEffort]). */
    @SerialName("reasoning_mode") val reasoningMode: String? = null,
    /** Reasoning context carried alongside [reasoningMode] for Responses-API models. */
    @SerialName("reasoning_context") val reasoningContext: String? = null,
    val effort: String? = null,
    val verbosity: String? = null,
    val useResponsesApi: Boolean? = null,
    val disableStreaming: Boolean? = null,
    val thinking: Boolean? = null,
    val thinkingBudget: Int? = null,
    @SerialName("thinkingLevel") val thinkingLevel: String? = null,
    val thinkingDisplay: String? = null,
    val stop: List<String>? = null,
    val iconURL: String? = null,
    val greeting: String? = null,
    val spec: String? = null,
    val tools: List<String>? = null,
    @SerialName("web_search") @JsonNames("webSearch") val webSearch: Boolean? = null,
    /** Google Gemini "URL Context" grounding (v0.8.7). Google-only, sibling of [webSearch]. */
    @SerialName("url_context") @JsonNames("urlContext") val urlContext: Boolean? = null,
    val promptCache: Boolean? = null,
    /** Anthropic prompt-cache duration: `"5m"` | `"1h"` (v0.8.7). Persisted per-conversation. */
    val promptCacheTtl: String? = null,
    val imageDetail: String? = null,
    val fileTokenLimit: Int? = null,
    val region: String? = null,
    val resendFiles: Boolean? = null,
    /** Whether the conversation is pinned to the top of the list (v0.8.7). */
    val pinned: Boolean? = null,
    /** Chat Project (folder) this conversation is assigned to, or null if unassigned (v0.8.7). */
    val chatProjectId: String? = null,
    /** True for a temporary chat (v0.8.6): not persisted to normal history and
     *  expired/cleaned up server-side after [expiredAt]. Server-derived. */
    val isTemporary: Boolean? = null,
    /** Expiry for a temporary chat, computed server-side from the interface
     *  `temporaryChatRetention` setting. Null for permanent chats. */
    @Serializable(with = LenientInstantSerializer::class)
    val expiredAt: Instant? = null,
    @Serializable(with = LenientInstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = LenientInstantSerializer::class)
    val updatedAt: Instant? = null,
)

/**
 * Resolves the icon URL to display for this conversation, preferring the per-conversation
 * [iconURL] (set by agents/assistants threads) over the per-endpoint URL from
 * [EndpointConfig.iconURL]. Returns null when no URL is available — callers fall through
 * to the bundled brand glyph or Material fallback.
 */
fun Conversation.resolveEndpointIconUrl(
    endpointConfigs: Map<String, EndpointConfig>,
): String? = iconURL ?: endpoint?.let { endpointConfigs[it]?.iconURL }
