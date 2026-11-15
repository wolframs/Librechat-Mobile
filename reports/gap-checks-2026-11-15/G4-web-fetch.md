# G4: Web fetch and web search on Surplus

Checked 2026-11-15 by gpt-6.1-sol.

## Question
Does mobile miss web-client behavior for fetch/search, and does its one-control routing fit the backend's two Surplus rows?

## What I looked at
- Read `BRIEF.md`, `CLAUDE.md` (This fork), `fork/README.md`, `docs/FORK_FEATURE_LEDGER.md:101-357`, `docs/APP_CHECK_2026-11-13.md`, `docs/UPSTREAM_CATCHUP_PLAN.md:70-86`, and `feature/chat/CLAUDE.md:684-721`.
- Mobile checkout HEAD during inspection: `0058fb60`. Below, `chat/` means `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/`.
- `chat/util/WebSearchRoute.kt:51-107`; `chat/viewmodel/ChatUiState.kt:602-678`; `chat/viewmodel/ChatRequestBuilder.kt:42-124`.
- `core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/config/ParamSettingsKey.kt:21-26`; `core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/datastore/AppPreferences.kt:63-70`.
- `chat/viewmodel/ModelParamPayload.kt:55-95`; `core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/ChatPayloadBuilder.kt:99-104`; `chat/viewmodel/delegate/SendDispatchDelegate.kt:403-433`.
- `fork/src/commonMain/kotlin/com/garfiec/librechat/fork/mcp/ConnectorRules.kt:3-17`; ledger's default-on implementation at `docs/FORK_FEATURE_LEDGER.md:275-279`.
- `chat/components/EphemeralToolMeta.kt:54-61`, `StreamingToolCallCard.kt:298-339`, `ToolLabels.kt:23-43`, `PtcToolTrace.kt:97-99`.
- Air reads only through `ssh air-agents`: `~/LibreChatDocs/README.md`, `web-search.md`, `fork-customizations.md:510-532`, `follow-ups.md` (web-search row), `deployments-2026-10.md:484-504`.
- Air backend source HEAD: `6f42475e6`. Below, backend paths are under Air `~/projects/librechat/`.
- `librechat.yaml:19-27,79-93,350-430`; `packages/api/src/endpoints/custom/config.ts:58-85`; `packages/api/src/endpoints/providerWebSearch.ts:35-73` and its spec; `packages/api/src/agents/load.ts:98-176,282-287`.
- Web client: `client/src/Providers/BadgeRowContext.tsx:85-120,223-237`; `client/src/hooks/Plugins/useToolToggle.ts:73-108,135-151`; `client/src/hooks/MCP/defaultOn.ts:8-63`; `client/src/hooks/Conversations/useSetIndexOptions.ts:34-51`.
- Web labels: `client/src/components/Chat/Input/WebSearch.tsx:24-44`, `packages/data-provider/src/parameterSettings.ts:516-527`, `client/src/components/Chat/Messages/Content/ToolCall.tsx:90-107,130-141,311-318`.
- Ran `git log --oneline 08bca78f..upstream/develop -- <routing, request-builder, UI-state and tool-card paths>`. Routing/builder unchanged; UI-state change is MCP filtering #885; tool-card changes #835/#856 concern recomposition/OAuth. Read upstream issue #714; searched all-state issues for web search/tool names/MCP names. #663/#714/#766 cover existing routing/key support, not this fork's remapping/caption. No matching open fix found in those searches.

## What I found
1. **Fetch needs no extra fetch switch or request field.** The server advertises `web` as `defaultOn: true`, `chatMenu: true`; the tool itself is `web_fetch(url, start?, max_chars?)`. Server-side default-on selection equips it unless `mcpOff` excludes `web`. Mobile's fork already represents default-on selection and sends `ephemeralAgent.mcp` / `mcpOff`, including first sends. Web uses the same contract. The old unchecked-default problem is already addressed in this checkout (ledger:275-279; request builder:69-72; backend load:98-114). Search on/off does not govern fetch.

2. **Web has two search controls; mobile deliberately has one.** Web's Tools control writes `ephemeralAgent.web_search`; its parameter switch writes conversation `web_search`. Web also offers a pinned composer badge and remembers Tools choices for new chats in browser storage. Mobile remembers enabled tools globally and search intent per chat, and has a first-search preference; it does not need to copy the second switch or pin UI to gain search capability (`feature/chat/CLAUDE.md:684-721`; `ModelSelectionDelegate.kt:1209-1212`; web toggle:135-151). The web parameter setter can also set `useResponsesApi` for literal endpoint ids openAI/azureOpenAI/custom; the served rows are named `Surplus` / `Surplus (Claude)`, so that setter condition does not match either name. Fetch adds no such field.

3. **Both flags mean the same search on this backend.** `replaceProviderWebSearch` tests the base URL hostname, not the row's label or model. On `api.surplusintelligence.ai` it deletes boolean `model_parameters.web_search: true`; `loadEphemeralAgent` then adds LibreChat's `web_search` tool. Its OR condition pushes that tool once even if both inputs are true. Thus the intended execution is SearXNG → Brave with Tavily page reads on both rows, never Surplus-hosted search (backend provider helper:35-73; load:154-176; yaml:19-27).

4. **Exact app routes, with the search control on, role/capability allowed and LibreChat readiness established:**

| Served row | App's provider test | Default `LIBRECHAT_FIRST` | `BUILT_IN_FIRST`, or retained/preset `web_search: true` |
|---|---|---|---|
| `Surplus` | `type: custom`, no provider/parameter override; built-in provider = null | LIBRECHAT | Falls back to LIBRECHAT; enforced spec preset is the exception below |
| `Surplus (Claude)` | `provider: anthropic`, `customParams.defaultParamsEndpoint: anthropic`; built-in provider = ANTHROPIC | LIBRECHAT | BUILT_IN |

These are code-derived results, not new runtime tests. They follow the actual yaml, served-config builder, `paramSettingsKey`, and route function. Picking a Claude model on plain `Surplus` does not change its provider test.

5. **Both app routes are compatible with the fork.** LIBRECHAT sends `ephemeralAgent.web_search: true` and provider parameter `web_search: false`; BUILT_IN sends provider `web_search: true` (top level and parameter payload), without the ephemeral search flag. The latter's duplicate placement is one search request, not two searches. The backend remaps it before constructing the provider search tool. No ordinary enabled/ready path on these served rows sends the wrong executable search or enables both (`ChatRequestBuilder.kt:51-72,98-124`; route:63-78).

6. **Exceptions and “none” cases:**
- Off → NONE. Saved Agent → no client search flags; its saved tools/parameters govern instead. This is intentional, not proof the Agent cannot search.
- LibreChat readiness pending → CHECKING, with neither search flag; ordinary LibreChat-search sends are held while checking, per `feature/chat/CLAUDE.md:702-705`. Not a completed turn silently verified to lack search.
- After readiness failure, plain `Surplus` has no recognized built-in fallback and can resolve NONE/hide the control; `Surplus (Claude)` can fall back to BUILT_IN and let the backend remap it. This is a conservative client availability difference, potentially visible after a failed auth-check request; no failure was reproduced here. Real missing service credentials would also break the remapped tool.
- Role denial or unavailable capability can yield NONE. A spec's forced `webSearch` takes priority subject to its role/capability gate; a spec preset's `web_search` chooses BUILT_IN where available, else LibreChat. Under enforced parameters the app selects BUILT_IN even on plain `Surplus` because the server sends that preset anyway; the fork remaps it too (`ChatUiState.kt:615-627`; route:63-78).
- Comparison prefers usable LibreChat search so both panes can share tools. A built-in-only primary route does not establish search on lane B; that is the documented comparison limitation (`feature/chat/CLAUDE.md:694-695`; backend fork notes §23 exclude the added side).
- I found no both-enabled app request in the shared builder. Admin/spec additions can independently request search, but on these hosts the backend OR/remap still creates one LibreChat search tool.

7. **Two presentation gaps remain.** BUILT_IN on `Surplus (Claude)` captions the control “Anthropic's built-in search”, although it runs LibreChat search after remapping (`EphemeralToolMeta.kt:60`; route provider enum). The web parameter panel also retains its generic provider-search description; it is not a more accurate capability signal. Separately, web strips the MCP suffix and shows the server, while mobile's generic streaming card still prints `toolCall.name` with a Build icon (`StreamingToolCallCard.kt:330-338`). Thus this new tool can show `web_fetch_mcp_web` instead of “web · web_fetch”. Mobile has a name parser, but the generic card does not use it; its use in PTC traces does not fix this card.

## Verdict
**real gap**, limited to presentation: a misleading search-provider caption on the remapped route, and the generic MCP tool label. Fetch availability and normal search routing are already fine. No additional fetch toggle, default-on implementation, or second search control is needed.

## Recommended action
S: reuse the existing tool-name parser for the generic card, through a fork-owned presentation helper and a marked hook. S: expose the backend's remapping capability in served endpoint config and use a fork-owned caption override so the Claude row says LibreChat search on BUILT_IN too; avoid guessing gateway behavior from a row name. Keep the existing one-control routing. These are two scoped follow-ups, not code changes made by this audit.

## Only the operator can answer
Nothing blocks the routing conclusion. Whether the web pin interaction should also exist on mobile is a product preference, not required for these tools to work.

## Not checked
No emulator, builds, HTTP calls, paid turns, database access, or code changes. The Air yaml and source were read, not live `/api/endpoints` output or container/image state. Deployment and successful calls are the dated 11/13 November records, not independently rerun today. No long tool loop, failure reproduction, iOS run, or saved-Agent/comparison execution test. Only this result file was written.
