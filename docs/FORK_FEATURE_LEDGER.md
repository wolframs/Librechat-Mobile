# Fork feature ledger (2026-11-13)

Read before re-landing a fork feature on upstream's code, and before the next
upstream catch-up. Update it in the same commit that re-lands, drops or changes
a fork feature. The plan this belongs to is `docs/UPSTREAM_CATCHUP_PLAN.md`.

What it compares: the fork's `develop` at `e8acf946` against `upstream/develop`
at `08bca78f`, merge base `7b2311af`. The three appendices are the area reports
(chat; auth, settings and shell; data and the rest) with file paths, symbols and
the hook points per feature. They were written by three sub agents reading both
trees; the database section was re-checked by comparing the schema files.

## Verdict on the merge

A plain merge is 278 conflict hunks in 158 files, and 208 more files merge
silently around fork hooks whose host code upstream rewrote. The cause is
structural: 256 of the fork's 322 changed files are edits inside upstream's own
files, and upstream lands about 230 commits a month in the same places. So the
catch-up takes upstream's version of every upstream-owned file and re-lands the
fork's features on it one at a time, each as fork-only files plus a few hook
lines. About half of the fork's work is no longer needed, because upstream has
built it since.

## Database

The fork's schema v10 is upstream's v9 plus two nullable text columns, nothing
else: `drafts.state_json` (draft attachments and the paused queue) and
`messages.cacheTTL` (the per-message cache-TTL marker). No fork-only tables,
indices or foreign keys. Upstream's v15 adds `message_search`, `messages_fts`,
`project_order` and `tool_call_parts`.

So upstream's schema chain is taken unchanged, and the fork keeps no schema of
its own:

- Draft state goes into upstream's `drafts` table as an extra row under its own
  key prefix, the way upstream stores queued text (`queued:<id>`).
- The TTL marker goes into an account-scoped DataStore entry; the request side
  needs no data-layer change.
- A phone that holds the fork's v10 would crash on the first upstream-based
  build, because Room would run upstream's 10 to 11 migration on the wrong v10
  (inferred from how Room validates; not run). The fix is a fork-only hook that
  deletes the database file when its identity hash is the fork's
  `404c7600b0a0063cc516e0ace5975212`. Local data loss is accepted (operator,
  2026-11-13).

## Features to re-land (the operator decides which stay)

Size: S under an hour of focused work, M a few hours, L a day or more.

| # | Feature | Size | Note |
|---|---|---|---|
| 1 | Cache TTL rail: one-shot 1h/5m arm, countdown, per-message marker | M | Upstream has only the per-conversation TTL dropdown. Touches about 10 upstream files; upstream's server-owned queue rows would lose an armed 1h. |
| 2 | "Prices and costs" panel and the `/cost` link | M | Not in upstream. On iOS the top bar is native now; the hook is an overflow-menu entry. |
| 3 | Server parameter overlay: defaults, ranges, read-only, locked prompt cache | M | Upstream picks the parameter set from the server but applies no overlay. Check against the deployed backend that skipped keys are not erased from the stored conversation. |
| 4 | Endpoint `provider` / `extendedCacheTTL`, and upload routing by provider | S | Two fields and two expressions. |
| 5 | Draft attachments survive process death | M | Upstream drafts are text only. Paused-queue recovery after process death would be L on top; upstream restores the queue's text only. |
| 6 | Tool and MCP selections per account, and validated at send time | S | Upstream's keys are still global and stale names are still sent. |
| 7 | Paragraph spacing preference | S | One padding hook, one settings row. |
| 8 | System password manager (pick, fill, save, 2FA handoff) | M | Not in upstream; login screens were rewritten. Check first on a device whether upstream's autofill already offers fill and save. |
| 9 | Server profiles screen in settings | M | Fork-only screen; removing an account now also signs it out on the server. |
| 10 | Multi-account sign-out explanation | S | |
| 11 | "Don't warn again" for plain HTTP, per server | S | |
| 12 | Banner dismissals kept per server across restarts | S | Upstream keeps them in memory. |
| 13 | Streaming uploads instead of whole-file buffering | M | Upstream's resend interceptor assumes byte-array parts and its PDF decryption needs the bytes; riskiest of the list. |
| 14 | iOS: file picker and PDF preview in Files | S | Upstream still stubs both. |
| 15 | iOS: agent avatar picker | S | Can use upstream's photo picker. |
| 16 | Archived favourites reconciled | S | Upstream still carries a "known gap" comment there. |
| 17 | Accessibility: labels on icon-only buttons, and the lint rule | S | About 4 upstream sites would fail the rule (heuristic scan). |
| 18 | Opt-in build under the old debug app id | S | Upstream added foss/play flavors. |
| 19 | German coverage check, for fork strings | S | Upstream's German is complete; fork strings go into their own files. |
| 20 | Follow Android language changes without restart | S | Two lines; upstream unchanged there. |
| 21 | Startup: read login state off the main thread | ? | Upstream has a token warmer; whether a gate is still needed takes a cold-start trace of the upstream build. |

## Dropped, because upstream covers it

| Feature | What is lost |
|---|---|
| Restoring a conversation's model parameters | The fork's network-first fetch on every open. |
| Recovery when a stream ends without a final event | Nothing; upstream reconciles and retries. |
| Remembered servers on the address screen | Servers without an account, and the Forget button. |
| OAuth cookie gating | Nothing; upstream's SSO runs in an in-app browser. |
| Native autofill field semantics | Nothing. |
| Restoring the previous URL after a failed check | Nothing. |
| Agent editor unsaved-draft protection | Restore after process death. |
| Paging with explicit retry | Nothing. |
| German string backfill | Nothing. |
| iOS settings avatar picker | Nothing. |
| Token warm-up, model-parameter cache (data side) | A small edge case on all-default detail fetches. |
| Fork database numbering and its migration | The phone's local cache, once. |
| Deprecation and DSL cleanups, import ordering | Compiler warnings only. |

## Seam rules

The rules for where fork code goes and how it hooks into upstream's files are in
`fork/README.md`. The appendices below were written before those rules existed;
where they suggest `strings_fork.xml` or fork-only files inside a feature
module, `fork/README.md` wins.

## State of the re-landing

| Feature | State | Hooks (`fork/scripts/fork-hooks.py --list`) |
|---|---|---|
| Saved login: Android Credential Manager and remembered server addresses | implemented; device acceptance pending; required 2FA setup discards save intent | `saved-login` |
| `:fork` module | in | `module` |
| Guard for a cache database with the fork's old schema (Android) | in, not yet run against an old install | `legacy-db` |
| Build under the old debug app id | in, not yet built that way | `legacy-debug-id` |

---

# Appendix A: chat, core/ui, core/model

## Fork feature ledger: `feature/chat/**`, `core/ui/**`, `core/model/**`

Merge base `7b2311af`, fork `develop`, upstream `upstream/develop` @ `08bca78f`
(read in the upstream worktree). Paths below are abbreviated:

- `chat/` = `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/`
- `ui/` = `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/`
- `model/` = `core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/`

Fork delta in this area: 91 files, +2431/-166. Of the 91: 20 are fork-only (new) files, 6 were deleted
upstream, the rest still exist upstream (most heavily rewritten).

Upstream host-file replacements that matter for every port below:

| fork host | upstream state |
|---|---|
| `feature/chat/src/androidMain/.../screen/ChatScreen.kt` and `feature/chat/src/iosMain/.../screen/PlatformScreens.ios.kt` | deleted; one shared `chat/screen/ChatScreen.kt` (1096 lines, commit `27e9c8db`). The top bar sits in it at line ~881 (`ChatFloatingTopBar(... .onSizeChanged { topBarHeightPx = it.height })`), `ChatRoot(` at ~441, `statusBarTop` at ~490. One hook site instead of two. |
| `chat/viewmodel/ChatViewModel.kt` | +1333/-1349. Send path moved to `chat/viewmodel/delegate/SendDispatchDelegate.kt` (`startChat` call ~line 541, `QueuedMessage(` ~131, `clearComposer()` ~386). Queue edit/capture moved to `delegate/QueueOrchestrationDelegate.kt` (`captureComposer` ~562, `applyComposer` ~587, `finishQueuedEdit` ~428). Draft restore and conversation load moved to `delegate/ConversationLoadDelegate.kt` (`restoreDraft` ~425, `loadConversationModel` ~438). Draft save is `ChatViewModel.persistComposerDraft` (~1301). |
| `ui/components/DynamicParameterPanel.kt` | deleted in `efde70e4` (options-sheet redesign). It was already unused at the merge base; `ui/components/ModelParameterContent.kt` dispatches definitions itself and is what replaced it. The fork only changed its Preview import. |
| `ui/components/EndpointParameterRegistry.kt` | +396/-51. `getDefinitions` now = `baseDefinitions` (dropParams) → `applyModelAwareDefaults` → effort/thinkingDisplay gating. New `modelRemovedOptions`, `wireOnlyDefinitions`, `wireKeyFor`. No `EndpointConfig` input. |
| `chat/components/ChatFloatingTopBar.kt` | +421/-52. Two variants: `GlassChatTopBar` (Liquid Glass, renders through `AdaptiveTopBar(spec)`, native on iOS) and `MaterialChatTopBar` (Compose `Row`). Overflow items are `chat/components/ChatOverflowItem.kt` + `chatOverflowSections(...)`. |
| `chat/components/CitationChip.kt` | deleted (`296ee8e0`, citations as source pills). |
| `feature/chat/src/androidInstrumentedTest/.../MessageListPauseScrollInstrumentedTest.kt` | deleted upstream. |

### Ledger

| # | Feature | Fork files (F = fork-only) | Upstream counterpart | Host-file state | Verdict | Size |
|---|---|---|---|---|---|---|
| 1 | Anthropic prompt-cache TTL: one-shot 1h/5m arm, gutter rail with countdown, per-message `cacheTTL` marker, gateway clamp to 5m | F `chat/viewmodel/CacheTtl.kt`, F `chat/components/CacheTtlRail.kt`, F tests `CacheTtlTest`, `CacheTtlUiStateTest`, part of `GatewayCompatibilityTest`; edits in `ChatUiState.kt`, `ComposerState.kt`, `QueueState.kt`, `ChatViewModel.kt`, both ChatScreens, `model/Message.kt`, `model/request/ChatRequest.kt`, `values/strings.xml`, `values-de/strings.xml` | Only the conversation-level `promptCacheTtl` dropdown in the parameter sheet (`EndpointParameterRegistry` `PROMPT_CACHE_KEYS`, `bedrockPromptCacheTtl`, `ConversationParameters.kt`). No one-shot arm, no `ChatRequest.cacheTTL`, no `Message.cacheTTL`, no rail, no countdown. | All hosts exist but rewritten; send path moved to `SendDispatchDelegate`, queue capture to `QueueOrchestrationDelegate`, screen to common `ChatScreen.kt` | PORT | M |
| 2 | Paragraph spacing preference (Markdown block padding) | edits `CachedMarkdown.kt`, `LocalParsedMarkdownCache.kt`, `ChatRoot.kt`, `ChatPrefsState.kt`, `ChatUiState.kt`, `ChatViewModel.kt`, both ChatScreens (enum `ChatParagraphSpacing` and DataStore key live in core/data) | none (no `markdownPadding`, no spacing pref anywhere) | `CachedMarkdown.kt` rewritten but simpler: every render goes through one private `MarkdownWithImages(...)` → `Markdown(...)` | PORT | S |
| 3 | Restore a conversation's stored model parameters on open | F `chat/viewmodel/ConversationSnapshotLoader.kt`, F `chat/viewmodel/delegate/ConversationModelParameters.kt`, F tests `ConversationSnapshotLoaderTest`, `ConversationModelParametersTest`, `ConversationSelectionHydrationTest`, `core/model/.../ConversationParameterSerializationTest`; edits `model/Conversation.kt`, `ModelSelectionDelegate.kt` | Full: `model/ConversationParameters.kt` (`Conversation.parameterPreset()`, `hasParameters`, `parametersStale`), `chat/util/SpecParameters.kt` `Conversation.storedParameters()`, `ConversationLoadDelegate.loadConversationModel` (cache-first apply + one detail backfill per `account:conversation:updatedAt`, `restoreParameters` callback wired in `ChatViewModel` ~674). `Conversation.kt` now carries every field the fork added (snake/camel as separate fields). | rewritten | DROP | – |
| 4 | Custom-endpoint provider identity (`EndpointConfig.provider`, `extendedCacheTTL`) and upload routing by the endpoint's `provider` | edits `model/EndpointConfig.kt`, `ChatUiState.routingAgentProvider`, `ChatViewModel.uploadRoutingContext` | none. Upstream `EndpointConfig` has `providerId` (icon brand) but no `provider`/`extendedCacheTTL`; `routingAgentProvider` still agents-only (`ChatUiState.kt` ~1098) | `EndpointConfig.kt` +103/-1 (append-friendly); routing context moved to `delegate/UploadIntakeDelegate.kt` ~394 | PORT | S |
| 5 | Server-declared parameter overlay: `customParams.paramDefinitions` defaults/ranges/options/type/description/`readonly`, locked `promptCache`, gateway `promptCacheTtl` locked to 5m | F `model/EndpointParameterOverrides.kt` (`applyEndpointOverrides`, `promptCacheEnabled`), edits `model/ParameterDefinition.kt` (`readOnly`), `ui/components/EndpointParameterRegistry.kt`, `ui/components/ModelParameterContent.kt`, `chat/viewmodel/ModelParamPayload.kt`, `ChatRequestBuilder.buildModelParams`, `ChatOptionsBottomSheet.kt`, `ChatOptionsSheetHost.kt`; tests in `GatewayCompatibilityTest` | Partial. Parameter-set selection by `defaultParamsEndpoint` is upstream (`model/config/ParamSettingsKey.kt` `paramSettingsKey`, `chat/util/ParameterTarget.kt`). `paramDefinitions` are read only for reasoning keys' `options`/`type` (`ui/components/ReasoningSettingResolver.kt` `declaredReasoningSettings`) and agent `declaredKeys`. No default/range/readonly overlay. Whole-sheet `readOnlyNote` exists (spec-enforced), not per-definition. | rewritten | PORT (selection-key part DROP) | M |
| 6 | "Prices and costs" header panel (`/cost/markets/endpoints`, `/cost/markets?model=`), link to `/cost` | F `chat/components/MarketPriceButton.kt`, F `chat/viewmodel/MarketViewModel.kt`, F `model/MarketPrice.kt`, F test `MarketViewModelTest`; edits `ChatFloatingTopBar.kt` (1 line), `di/ChatModule.kt` (1 line), strings, `ChatModuleVerificationTest` (MarketRepository is core/data) | none | `ChatFloatingTopBar.kt` rewritten into Glass + Material variants | PORT | M |
| 7 | Recovery when a stream ends without Final/Error | edits `StreamingManagerDelegate.launchStream`/`endStream`, `ChatViewModel` (drop `onTerminated`), `MessageEditingDelegate` (comment), `CLAUDE.md`; F test `StreamingManagerUnexpectedEofTest` | Full: `StreamingManagerDelegate.launchStream` (~357) has no callback and calls `endUnterminatedStream(session, whenGone = StreamEndReason.Reconcile)`; retry-ceiling adjudication via `checkStreamStatus`; queue held | rewritten (+580/-426) | DROP | – |
| 8 | Draft attachment recovery (uploaded `fileId` chips survive process death; local-only handles reported lost) | F `chat/viewmodel/ChatDraftRecovery.kt` (payload v1), F test `ChatDraftRecoveryTest`; edits `ChatViewModel` (`restoreDraft`, `persistDraftState`, `clearComposer`, attachedFiles observer), `SendCompletionDelegate` (draft move), `CLAUDE.md`; Room payload + `DraftRepository.saveDraftState/awaitDraftState/getDraftState` are core/data | none. `DraftRepository` upstream is text-only (`getDraft/awaitDraft/saveDraft/deleteDraft`). Upstream has an in-session draft *undo* (`DraftUndoSnackbarEffect`, `pendingDraftUndo`), not persistence. | hosts moved: `ConversationLoadDelegate.restoreDraft`, `ChatViewModel.persistComposerDraft`, `SendDispatchDelegate.clearComposer`, `SendCompletionDelegate` ~85 | PORT | M |
| 9 | Queued-message recovery across process death (full send config, restored paused) | same files as #8 plus `QueueState.kt`, `MessageQueueDelegate.onQueueChanged` callbacks, `QueuedMessage.cacheTtl` | Overlapping: `delegate/LocalQueueContinuityDelegate.kt` + `viewmodel/LocalQueueStash.kt`. Leaving and re-entering a chat restores full rows, paused until the run re-attaches; process death puts the queued *text* back in the composer (`queued:<conversationId>` draft key) with notice `QUEUE_RESTORED_TO_COMPOSER`. Server-owned queue rows (`QueuedMessage.server`) survive server-side. | `QueuedMessage` far richer upstream (`choice: ModelChoice`, `specTools`, `reasoningOverride`, `codeWorkspace`, `server`, `clientRequestId`, …) | DROP (or PORT L if full restore is required) | – / L |
| 10 | Tool/MCP selection safety: MCP names intersected with server's advertised list at send, stale names pruned and persisted, file-search/code only when the gate is open | edits `ChatRequestBuilder.buildEphemeralAgent`, `ModelSelectionDelegate` MCP load; F test `ChatRequestBuilderToolValidationTest` (account-scoped storage is core/data) | Partial. `ChatUiState.toolGateOpen` (~700) exists and gates the UI, but `buildEphemeralAgent` still serializes `effectiveMcpServerNames` and `FILE_SEARCH`/`CODE_INTERPRETER` from `enabledTools` unfiltered (only memory and artifacts re-check). No pruning in `McpServersDelegate.paint`. Upstream `SettingsDataStore.KEY_SELECTED_MCP_SERVERS` is still global. | MCP load moved to `delegate/McpServersDelegate.kt` (`paint` ~140) | PORT | S |
| 11 | German string coverage | `values-de/strings.xml` in chat (+184) and core/ui (+2) | Full: upstream ships 9 locales; 0 of 1068 chat EN keys and 0 of 385 core/ui EN keys missing in upstream DE. All 41 fork-added DE keys absent from upstream DE are also absent from upstream EN (fork-only feature strings, or keys upstream renamed/removed). Upstream has `scripts/i18n-coverage.py`. | rewritten (+947 chat DE) | DROP | – |
| 12 | Explicit Android API-level guards | `072359cf` touched `SpeechRecognizerController.kt` and androidMain `ChatScreen.kt` (`@SuppressLint`) | Already at the merge base: `SpeechRecognizerController.kt` line 81 had the same `usingOnDevice && SDK_INT >= S` guard; upstream keeps it (~line 88-91) | androidMain `ChatScreen.kt` deleted | DROP (no delta left in this area) | – |
| 13 | Android locale changes observed (`LocalResources.current` instead of `LocalContext.current.resources`) | `core/ui/src/androidMain/.../theme/LocalAppLocale.android.kt` (2 lines) | none; upstream file unchanged since base (still `LocalContext.current.resources`, line 32) | unchanged | KEEP-CLEAN | S |
| 14 | iOS avatar image picker | F `core/ui/src/iosMain/.../platform/IosImagePicker.kt` (used by settings `AvatarUploadDialog.ios.kt`, agents `AgentAvatarPicker.ios.kt`) | Partial: `core/ui/src/iosMain/.../media/IosPhotoPicker.kt` `openPhotoPicker` (PHPicker) and settings `rememberAvatarPicker` (`AvatarUploadDialog.ios.kt`) cover the user avatar. Agent avatar on iOS is still a placeholder upstream (`AgentAvatarPicker.ios.kt`: "no image picker yet"). | n/a (fork-only file) | DROP the core/ui file; agent-avatar part belongs to the feature/agents ledger, re-land on `openPhotoPicker` | – |
| 15 | Accessibility labels on icon-only actions | not in this area (`58831ef8` touched agents, conversations, detekt rule). Here only the rail's own `semantics` (travels with #1) and the market button's `contentDescription` (travels with #6) | – | – | n/a here | – |

Count: PORT 7 (#1, #2, #4, #5, #6, #8, #10); DROP 6 (#3, #7, #9, #11, #12, #14); KEEP-CLEAN 1 (#13);
UNSURE 0 (one sub-question under #5); not in area 1 (#15).

### Notes per feature

### 1. Prompt-cache TTL rail (PORT, M)

Keep as fork-only files: `chat/viewmodel/CacheTtl.kt` (enum `CacheTtl`, `nextCacheTtlArm`, `CacheTtlAnchor`,
`newestCacheTtlAnchor`, `cacheTtlRemainingMillis`, `formatCacheTtlRemaining`) and `chat/components/CacheTtlRail.kt`
unchanged. Move the derived state out of `ChatUiState` into extension properties in `CacheTtl.kt`; they only read
public upstream properties (`selectedEndpoint`, `endpointConfigs`, `modelParameters.dynamicValues`, `messages`,
`composer`): `ChatUiState.cacheTtlEnabled`, `extendedCacheTtlEnabled`, `outgoingCacheTtl`, `cacheTtlAnchor`.
Put the strings (`cache_ttl_*`) in new fork-only resource files `values/strings_fork.xml` and
`values-de/strings_fork.xml` so upstream's `strings.xml` stays untouched.

Minimal upstream-owned edits (pattern to follow: upstream's one-shot `pendingReasoningOverride`, which already
rides composer → queued item → `startChat(reasoningOverride=)`):
- `model/Message.kt`: `val cacheTTL: String? = null`.
- `model/request/ChatRequest.kt`: `val cacheTTL: String? = null`.
- `model/EndpointConfig.kt`: `provider`, `extendedCacheTTL` (shared with #4/#5).
- `chat/viewmodel/ComposerState.kt`: `armedCacheTtl` on `ComposerState` and `ComposerSnapshot`.
- `chat/viewmodel/QueueState.kt`: `QueuedMessage.cacheTtl`; `toComposerSnapshot()` maps it.
- `delegate/QueueOrchestrationDelegate.kt`: `captureComposer` / `applyComposer` carry `armedCacheTtl`.
- `delegate/SendDispatchDelegate.kt`: `QueuedMessage(... cacheTtl = state.outgoingCacheTtl)`; `startChat(... cacheTtl = spec.cacheTtl?.wireValue)`; `clearComposer` resets `armedCacheTtl`.
- `ChatViewModel.kt`: `fun toggleCacheTtlArm()` (5 lines; or a fork-only delegate taking the handle).
- `chat/screen/ChatScreen.kt`: the `if (uiState.cacheTtlEnabled) CacheTtlRail(...)` block next to `ChatFloatingTopBar`, offset by `topBarHeightPx`/`statusBarTop` instead of the fork's fixed `statusBarTop + 64.dp` (the Glass bar has a different height).
- Outside this area: `ChatRepository.startChat` `cacheTtl` parameter, and the Room message column carrying `cacheTTL` (fork's v10 migration).

Gotcha: upstream server-owned queue rows (`QueuedMessage.server != null`) are sent by the server; an armed TTL on such
a row is lost. Decide whether arming forces the local drain or is dropped for server rows.

### 2. Paragraph spacing (PORT, S)

Fork-only: `LocalChatParagraphSpacing` (move from `LocalParsedMarkdownCache.kt` into a new fork file).
Upstream edits: `chat/components/CachedMarkdown.kt` private `MarkdownWithImages` passes
`padding = markdownPadding(block = LocalChatParagraphSpacing.current.blockSpacingDp.dp)` (one call site now, the fork
had two); `chat/screen/ChatScreen.kt` provides the local. Smallest variant: in `ChatScreen`, collect
`settingsDataStore.chatParagraphSpacing` via `koinInject<SettingsDataStore>()` and wrap in
`CompositionLocalProvider`, so `ChatViewModel`, `ChatPrefsState`, `ChatUiState`, `ChatRoot` stay untouched (the fork
threaded it through all four plus `ChatTypographyPrefs`). `SharedChatScreen.kt` gets the default (COMFORTABLE = 8dp)
unless also wrapped. The enum `ChatParagraphSpacing`, DataStore key and the settings UI are outside this area.

### 3. Conversation parameter restore (DROP)

Upstream equivalent: `ConversationLoadDelegate.loadConversationModel` → `applyConversationModel` then
`restoreParameters(loaded, remembered)` (wired in `ChatViewModel` ~674, `wantsStoredParameters = { !isHandedOffNewChat }`),
`Conversation.parameterPreset()` / `storedParameters()`, backfill via `refreshConversation` when
`!hasParameters || parametersStale`. Lost vs fork: the fork fetched the detail endpoint network-first on every open
(`loadConversationSnapshot`); upstream applies the cached row first and fetches once per `updatedAt`. The fork's
`@JsonNames` aliases are replaced by upstream's `topPSnake/topPCamel`, `maxTokensSnake/maxTokensCamel`.
Drop the fork tests too: `ConversationParameterSerializationTest` reads `conversation.topP`/`maxTokens`, which no
longer exist; `ConversationModelParametersTest`/`ConversationSelectionHydrationTest`/`ConversationSnapshotLoaderTest`
test deleted fork code.

### 4. Provider identity and provider-based upload routing (PORT, S)

Upstream edits: `model/EndpointConfig.kt` add `val provider: String? = null`, `val extendedCacheTTL: Boolean? = null`
(append at the end of the data class to keep the hunk isolated). `chat/viewmodel/ChatUiState.kt`
`routingAgentProvider` (~1098): `if (selectedEndpoint == AGENTS) selectedAgentProvider else endpointConfigs[selectedEndpoint]?.provider`.
`chat/viewmodel/delegate/UploadIntakeDelegate.kt` private `uploadRoutingContext()` (~394): same expression for
`agentProvider`. Note `ChatUiState.fileConfigEndpoint` (~572) also uses `routingAgentProvider` behind
`gates.providerScopedUploadsSupported`; the change now also affects which file config is used for a custom endpoint
with `provider`. Check that is wanted. Test: the audio case of `GatewayCompatibilityTest`.

### 5. Server parameter overlay (PORT, M)

Drop: the fork's registry key `customParams?.defaultParamsEndpoint ?: provider ?: endpoint`; upstream
`paramSettingsKey` covers `defaultParamsEndpoint` (only when it is an exact `ENDPOINT_SCHEMA_KEYS` member, and
otherwise falls back to `type`, not to `provider`).
Keep as fork-only: `model/EndpointParameterOverrides.kt` (`applyEndpointOverrides`, `promptCacheEnabled`); rename its
input from the fork's `CustomEndpointParams` to upstream's `CustomParams` (upstream `paramDefinitions` is nullable).
Delete the fork's `CustomEndpointParams` class (upstream `EndpointConfig.customParams: CustomParams?` replaces it).
Upstream edits:
- `model/ParameterDefinition.kt`: `val readOnly: Boolean = false` (file unchanged upstream; trivial).
- `ui/components/EndpointParameterRegistry.getDefinitions`: optional `endpointConfig: EndpointConfig? = null`, apply `applyEndpointOverrides` after `applyModelAwareDefaults` and before the gating map (2-3 lines).
- `ui/components/ModelParameterContent.kt`: pass `endpointConfig` into the `remember` + `getDefinitions` call; render `definition.readOnly` as label: value text (the fork's 5 lines in the `definitions.forEach`).
- `chat/components/ChatOptionsBottomSheet.kt` `ModelParametersPageParams.endpointConfig` and its pass-through; `chat/screen/ChatOptionsSheetHost.kt` sets it from `uiState.endpointConfigs[uiState.selectedEndpoint]`.
- `chat/viewmodel/ModelParamPayload.build`: pass `endpointConfig` to `getDefinitions`; skip `readOnly`; strip `promptCacheTtl` for `provider == anthropic && extendedCacheTTL != true`.
- `ChatRequestBuilder.buildModelParams`: pass `state.endpointConfigs[state.selectedEndpoint]`.
Risk: upstream's `ModelParamPayload` kdoc states that the server unsets every stored field a request omits
(`getUnsetFields`). The fork's `if (definition.readOnly) continue` and the TTL strip now erase that field from the
stored conversation on each send. Probably harmless for a server-locked default (server re-applies it) but verify.
Not touched by the fork and possibly needed: `PresetPromptDelegate` and `feature/agents` also call `getDefinitions`.
UNSURE sub-point: if the deployed backend (fork of v0.8.8-rc1) does not fill `customParams.defaultParamsEndpoint` from
`provider` for `Surplus (Claude)`, upstream picks the `custom` (OpenAI) parameter set, and the fork's `?: provider`
fallback is still needed as a 1-line hook in `model/config/ParamSettingsKey.kt`. Settle: read `/api/endpoints` on the
deployed server and look at that entry's `customParams`.

### 6. Prices and costs (PORT, M)

Keep as fork-only: `chat/components/MarketPriceButton.kt` (split the `AlertDialog` into a `MarketPriceDialog`),
`chat/viewmodel/MarketViewModel.kt`, `model/MarketPrice.kt`, test `MarketViewModelTest`, `MarketRepository` (core/data),
strings `market_*`, `cost_*` in the fork-only resource file.
Upstream edits: `chat/di/ChatModule.kt` `viewModelOf(::MarketViewModel)` (1 line; plus the `ChatModuleVerificationTest`
extra-type entry). Top bar: the fork's single `MarketPriceButton(...)` line in the action row works only for
`MaterialChatTopBar`; the Glass variant renders through `AdaptiveTopBar(spec)` (native on iOS) and takes no Compose
button. Smallest seam that covers both: add `ChatOverflowItem.PRICES` (enum entry in `ChatOverflowItem.kt`, gate in
`chatOverflowSections`, one `when` branch in `ChatFloatingTopBar` that opens the fork dialog). Gate on
`MarketViewModel.state.available` for the current server URL. Cost: a few upstream lines in 2-3 files.

### 7. Stream ended without final (DROP)

Upstream `StreamingManagerDelegate.launchStream` (~357) ends via `endUnterminatedStream(..., Reconcile)`, skips while
a Stop is pending or the account changed, and adjudicates the retry ceiling via `checkStreamStatus`. Lost vs fork: the
fork surfaced the error "Connection closed before the response completed"; upstream reconciles to the server reply
silently (and only shows a network error when the status read also fails). Upstream is the better behaviour. Drop
`StreamingManagerUnexpectedEofTest` and the fork's CLAUDE.md chokepoint paragraph.

### 8. Draft attachment recovery (PORT, M)

Keep as fork-only: `chat/viewmodel/ChatDraftRecovery.kt`, reduced to `attachments` + `lostLocalAttachmentCount`
(drop `queuedMessages`/`isQueuePaused`/`PersistedQueuedMessage`/`PersistedModelParameters` if #9 is dropped); test
`ChatDraftRecoveryTest` trimmed to match. Best as a fork-only delegate (e.g. `DraftAttachmentRecoveryDelegate`)
that observes `fileHandler.attachedFiles` and writes the payload, so upstream files get only call hooks:
- `ConversationLoadDelegate.restoreDraft`: switch `awaitDraft` → `awaitDraftState`, restore chips when the tray is empty (or call the delegate).
- `ChatViewModel.persistComposerDraft` and `SendDispatchDelegate.clearComposer`: write text+payload atomically (`saveDraftState`) instead of `saveDraft`/`deleteDraft`.
- `SendCompletionDelegate` (~85, new-chat draft moved to the real id): move the payload with the text.
- Outside this area: `DraftRepository.saveDraftState/awaitDraftState/getDraftState`, the Room payload column and the fork's v8/v9/v10 schema history.
Gotcha: upstream's `LocalQueueContinuityDelegate` writes queued text into the same draft store under
`queued:<conversationId>`; the fork payload must not be written to those keys.

### 9. Queued-message recovery (DROP; PORT L if required)

Upstream `LocalQueueContinuityDelegate` + `LocalQueueStash` keep full rows across leaving/re-entering a chat (paused
until the run attaches) and, after process death, put the queued text back in the composer with
`ChatNotice.QUEUE_RESTORED_TO_COMPOSER`. Lost vs fork after process death: attachments, per-item model/tools/params and
the separate queue items (upstream joins texts with blank lines into the composer). A full port would need a new
serializer for upstream's much larger `QueuedMessage` (`ModelChoice`, `SpecTools`, `ReasoningOverride`,
`CodeWorkspaceSubmission`, server state) and must skip server-owned rows. The fork's `onQueueChanged` callbacks in
`MessageQueueDelegate` are not needed in any variant: upstream already exposes `queueStates` as a flow.

### 10. Tool/MCP selection validation (PORT, S)

Fork-only: a pure function, e.g. `fun EphemeralAgent.validatedFor(state: ChatUiState): EphemeralAgent?` that
intersects `mcp` with `state.mcpServers.map { it.name }` and drops `fileSearch`/`executeCode` unless
`state.toolGateOpen(...)`; test `ChatRequestBuilderToolValidationTest` adapted. Upstream edits:
`ChatRequestBuilder.buildEphemeralAgent` returns `...validatedFor(state)` (1 line; mind spec-locked tools:
`effectiveMcpServerNames` under a spec are server-defined and should pass); `McpServersDelegate.paint` (~140) prunes
`selectedMcpServerNames` to listed names and calls `settingsDataStore.setSelectedMcpServers` when changed (~5 lines).
The account scoping of the stored selection is core/data (upstream still global `KEY_SELECTED_MCP_SERVERS`), not here.

### 11. German strings (DROP)

Take upstream's `values-de/strings.xml` in both modules. Keep only DE for fork-only strings (`cache_ttl_*`, `market_*`,
`cost_*`, `marketplace_list`) in the fork-only resource files. The other 30 fork-added DE keys (e.g.
`context_usage_*`, `cd_open_pdf`, `ask_user_question_title`) are gone from upstream EN as well, so they are stale.
The fork's `scripts/check-localization.py` CI step (outside this area) checks DE only; upstream's own
`scripts/i18n-coverage.py` checks all 9 locales, where fork-only strings will show as missing in 7 locales.

### 12. Android API guards (DROP)

Nothing left in this area: the merge base already had the guard; the `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`
went with the deleted androidMain `ChatScreen.kt`. Other modules may still carry guards (other ledgers).

### 13. Locale observation (KEEP-CLEAN, S)

`core/ui/src/androidMain/kotlin/com/garfiec/librechat/core/ui/theme/LocalAppLocale.android.kt`: import
`androidx.compose.ui.platform.LocalResources`, `val resources = LocalResources.current`. Upstream file unchanged since
the merge base, so the fork version applies as is.

### 14. iOS avatar picker (DROP here)

Upstream `core/ui/src/iosMain/kotlin/com/garfiec/librechat/core/ui/media/IosPhotoPicker.kt` (`openPhotoPicker`) is
the shared PHPicker bridge, used by upstream's settings `rememberAvatarPicker`. Drop the fork's
`core/ui/src/iosMain/.../platform/IosImagePicker.kt`; the agent avatar (still a placeholder upstream) belongs to the
feature/agents ledger and should be built on `openPhotoPicker`.

### Non-feature delta in this area

| Change | Files | Upstream state | Action |
|---|---|---|---|
| Gradle `android {}` → `extensions.configure<LibraryExtension> {}` | `core/model`, `core/ui`, `feature/chat` `build.gradle.kts` | upstream still `android {}` and builds on the same AGP | drop; redo as one repo-wide sweep later if the deprecation warning matters |
| Preview import `org.jetbrains.compose.ui.tooling.preview.Preview` → `androidx.compose.ui.tooling.preview.Preview` | `ui/components/Dynamic{Checkbox,Dropdown,Input,Slider,TagsInput,Textarea}.kt`, `DynamicParameterPanel.kt` | upstream still uses the org.jetbrains import; `DynamicParameterPanel.kt` deleted | drop (optional sweep) |
| `MenuAnchorType` → `ExposedDropdownMenuAnchorType` | `DynamicDropdown.kt` | upstream did the same | moot |
| `centerAlignedTopAppBarColors` → `topAppBarColors` | `ui/components/LibreChatTopBar.kt` | file rewritten (+26/-37), call gone | moot |
| `TabRow` → `PrimaryTabRow` | `chat/components/ComparisonTabBar.kt` | upstream uses `AdaptiveTabRow` | moot |
| `Icons.Default.Notes` → `Icons.AutoMirrored.Filled.Notes` | `chat/components/CollapsibleContentParts.kt` | upstream still `Icons.Default.Notes` | drop (optional) |
| Exhaustive `when` (`ACTIVITY_LABEL, STEER -> Unit`, removed `else` fallback) | `chat/components/SharedContentParts.kt` | upstream keeps `else ->` that renders `part.text` | drop (the fork change also removed a text fallback) |
| Redundant safe calls | `chat/components/CitationChip.kt` | file deleted | moot |
| Detekt import reordering | `ChatInput.kt`, `LatexBlock.kt`, `MarkdownContent.kt`, `MessageBubble.kt`, `ArtifactPanel.kt`, `InlineArtifactView.android.kt`, iOS `PlatformChatComponents`, `PlatformMediaComponents`, `InlineArtifactView.ios`, `PlatformArtifactPanel.ios`, `WebAssets.ios`, both ChatScreens | all rewritten or deleted | moot |
| Comment-only / kdoc cleanups | `MessageEditingDelegate.kt`, `ChatViewModel.kt` (duplicate kdoc), `QueueState.kt`, `MessageQueueDelegate.kt` kdoc | rewritten | moot |
| `feature/chat/CLAUDE.md` (stream chokepoint wording, "Draft and queue recovery" section) | `feature/chat/CLAUDE.md` | rewritten (+1031/-488) | drop; re-add a short draft-attachment note only with #8 |
| Test adaptations to fork signatures | `ChatViewModelTestFixture.kt` (mocks `awaitDraftState`, `attachedFiles`, `chatParagraphSpacing`), `ChatViewModelDuringRunSendTest.kt` (extra `any()` for `cacheTtl`), `MessageQueueDelegateTest.kt`, `MessageQueueHoldRenewalTest.kt` (`onQueueChanged`), `StreamingManagerEndStreamTest.kt`, `StreamingManagerLifecycleTest.kt` (no `onTerminated`), `ModelSelectionDelegateTest.kt` | upstream rewrote all of them | moot; re-add only the mocks a ported feature needs |
| `@OptIn` declarations in tests | `ChatModuleVerificationTest.kt`, `ServerFileSelectionHandoffTest.kt`; cast removal in `InterfaceConfigV086FlagsTest.kt` | upstream unchanged for the last; others rewritten | drop (warning-only) |
| Instrumented test import order | `MessageListPauseScrollInstrumentedTest.kt` | deleted upstream | moot |
| Merged-in DE strings `banner_dismiss`, `pdf_page_failed` | `core/ui/.../values-de/strings.xml` | present in upstream DE | moot (banner-dismissal persistence itself lives outside this area now) |

---

# Appendix B: auth, settings, shared, app, core/common, core/network

## Fork feature ledger: auth, settings, shared, app, core/common, core/network

Scope: `git diff 7b2311af develop -- feature/auth feature/settings shared app core/common core/network`
(99 files, +2577/-140). Upstream state: `upstream/develop` 08bca78f, read in the scratchpad worktree.
Host-file census for the 99 files: 15 fork-only, 2 unchanged upstream, 68 changed upstream, 14 deleted
upstream (DateExt.kt + test, AccountSettingsScreen.kt, ChatSettingsScreen.kt, ChatSettingsSection.kt,
GeneralSettingsScreen.kt, McpServerDialog.kt, SpeechDetailDialogs.kt, SpeechSettingsSection.kt,
TabbedSettingsScreen.kt, SettingsPreferencesController.kt, SettingsUiState.kt, SettingsViewModel.kt,
SettingsViewModelTest.kt).

Paths below abbreviate `src/commonMain/kotlin/com/garfiec/librechat/` as `…/`.
Several features also depend on fork code in `core/data` (ServerDataStore, SettingsDataStore,
AccountRegistry, TokenDataStore, FileRepository). Those files belong to the core/data ledger; they
are named here only where they are the other half of a seam.

### Ledger

| # | Feature | Fork files (this area) | Upstream counterpart | Host-file state | Verdict | Size |
|---|---|---|---|---|---|---|
| 1 | System password manager (Android Credential Manager: pick, fill, save, 2FA handoff, unchanged-selection no re-save, plain-username IDs) | `feature/auth/…/credentials/PasswordCredentialManager.kt`, `PendingCredentialSaveHandoff.kt`, androidMain `PasswordCredentialManager.android.kt`, iosMain `PasswordCredentialManager.ios.kt`; edits in `LoginViewModel.kt`, `LoginScreen.kt`, `TwoFactorViewModel.kt`, `TwoFactorScreen.kt`, `AuthModule.kt`, `build.gradle.kts`, strings | None. No `CredentialManager`, no picker, no save. Upstream relies on field `contentType` semantics only (platform autofill) | LoginViewModel rewritten (SSO, refusal, 2FA setup, cert trust; ctor changed); LoginScreen rewritten; TwoFactorViewModel/Screen recognisable; AuthModule recognisable (no `authPlatformModule` any more) | PORT | M |
| 2 | OAuth cookie consumption gated on an explicit OAuth launch | `LoginViewModel.checkOAuthResult` / `oAuthLaunched` | Custom-Tab OAuth is gone. SSO runs in an in-app WebView (`SsoLoginScreen`, `SsoLoginViewModel`, `oauth/SsoCookieCapture.kt`); no ON_RESUME cookie read in LoginViewModel/LoginScreen | `OAuthLauncher`, `checkOAuthResult` deleted | DROP | – |
| 3 | Native password autofill semantics (Username/Email/Password/NewPassword) | `LoginScreen.kt`, `RegisterScreen.kt`, `ResetPasswordScreen.kt` | Same, and more: `semantics { contentType = … }` on all three screens plus `withIosTextContentType(...)` (`core/ui/…/components/IosTextContentType.kt`), `LocalAutofillManager.commit()` in RegisterScreen (upstream #529, #530) | Rewritten | DROP | – |
| 4 | Remembered servers on the server-address screen (list, select-with-headers, forget, "warn again") | `ServerUrlViewModel.kt` (`rememberedServers`, `selectServer`, `forgetServer`, `restoreHttpWarning`), `ServerUrlScreen.kt`, strings; data in core/data `ServerDataStore.rememberedServers/rememberServer/forgetServer` | `ServerUrlUiState.knownServers` / `KnownServer` / `knownServersOf(AccountRoster.entriesFlow())`, `connectTo(url)`, `KnownServersSection` in `ServerUrlScreen.kt` (upstream #770) | Both files rewritten (resolveServer probe, cleartext policy, prefill race guard) | DROP | – |
| 5 | Per-server "don't warn again" for HTTP | `ServerUrlViewModel.kt` (`suppressHttpWarning`, `setSuppressHttpWarning`, http branch of `validateAndConnect`, `confirmHttpConnection`, `doValidateAndConnect(suppressHttpWarningOnSuccess)`), `ServerUrlScreen.HttpWarningDialog` checkbox; data in core/data `ServerDataStore.isHttpWarningSuppressed/setHttpWarningSuppressed/httpWarningSuppressedServers` | None. Upstream always shows the dialog (and on iOS refuses http to qualified domain names) | Rewritten but the http branch is recognisable | PORT | S |
| 6 | Restore previous server URL when validation fails | `ServerUrlViewModel.validateLiveServer` (`previousUrl`, `clearServerUrl`) | Upstream probes with `ConfigRepository.resolveServer` before storing anything (`resolveAndCommit`, `commitPendingServer`) | Rewritten | DROP | – |
| 7 | Server profiles screen in settings (list servers + accounts, switch, forget server transaction, reset HTTP warning, add account) | fork-only `feature/settings/…/screen/ServerProfilesScreen.kt`, `viewmodel/ServerProfilesViewModel.kt`, test; hooks in `SettingsNavigation.kt`, `SettingsModule.kt`, `GeneralSettingsScreen.kt`, `TabbedSettingsScreen.kt`, shared `LibreChatNavHost.kt` (`onAddAccount`), strings | Partial: per-account remove/add in the drawer `AccountSwitcherSheet` (`feature/conversations/…/drawer/AccountSwitcherSheet.kt`, `onRemoveAccount`), account removal now revokes the server session (upstream #631). No server-level view, no forget-server transaction, no HTTP-warning reset | GeneralSettingsScreen/TabbedSettingsScreen deleted (replaced by `SettingsHomeScreen.kt` + `SettingsCategoryNavigation.kt` + `*SettingsPage.kt`); SettingsNavigation recognisable | PORT | M |
| 8 | Multi-account sign-out explanation | fork-only `viewmodel/SignOutViewModel.kt` + test; edits in `AccountSettingsScreen.kt`; strings | None. `AccountSettingsPage.kt` shows a fixed `ConfirmDialog(dialog_title_sign_out, dialog_sign_out_message)` | AccountSettingsScreen.kt deleted → `screen/AccountSettingsPage.kt` | PORT | S |
| 9 | Banner dismissals persisted per server; version warning keyed per server | `shared/…/navigation/BannerStateHolder.kt`, `VersionCheckStateHolder.kt`, `NavHostViewModel.kt` (one ctor arg), test `BannerDismissalStateTest.kt`; data in core/data `SettingsDataStore` | Upstream holder is process-memory only (`dismissedKeys`), already keyed by (server, banner) and already refuses `persistable` banners in `isDismissed`. Version warning still one global `SettingsDataStore.dismissedVersionWarning` | Both holders recognisable, nearly unchanged in shape | PORT | S |
| 10 | Warm secure tokens off Main; startup auth unresolved state + neutral surface; deep links await auth | `shared/…/NavHostViewModel.kt` (`isLoggedIn: StateFlow<Boolean?>`, `awaitAuthResolution`, IO `isLoggedIn()`), `shared/…/LibreChatNavHost.kt` (loading Box, `!= false`/`== true`), `app/…/navigation/LibreChatNavHost.kt`, `core/network/…/client/TokenManager.kt` (`warmUp()`), `shared/CLAUDE.md`; data in core/data `AccountRegistry`, `CommonTokenDataStore.warmUp`, `TokenDataStore` factory | Partial: lazy `TokenDataStore` + `TokenCacheWarmer` (createdAtStart, IO). Upstream still seeds `_isLoggedIn = MutableStateFlow(tokenManager.isAuthenticated)` on Main and documents that it falls back to a synchronous keystore build + decrypt when the warmer has not landed. Deep links: `navigator.placeDeepLink` already waits for `accountState !is Warming` | NavHostViewModel heavily rewritten (14 upstream commits, `SessionRoute`, ~30 `_isLoggedIn` sites); shared LibreChatNavHost rewritten (34 commits); app LibreChatNavHost reduced to share-intent wrapper | UNSURE (PORT M if still needed) | M |
| 11 | Logged-out routing lands on Login when a server URL is saved | `Navigator.navigateToAuth(hasServerUrl)`, `navigateToDeepLinkLoggedOut(…, hasServerUrl)`, `NavHostViewModel.hasSavedServerUrl`, `AuthNavigation.kt` (`onBack` for Login) | `Navigator.navigateToSignIn()` + `SessionRoute.SignInActiveAccount` (Login over ServerUrl for an expired/bound account); ServerUrl prefill from `awaitBaseUrl()` otherwise | Navigator rewritten | DROP | – |
| 12 | Paragraph-spacing preference: settings UI | `ChatSettingsDialogs.kt`, `ChatSettingsScreen.kt`, `ChatSettingsSection.kt`, `SettingsViewModel.kt`, `SettingsUiState.kt`, `SettingsPreferencesController.kt`, strings; data in core/data `ChatParagraphSpacing.kt`, `SettingsDataStore` | None | All six hosts deleted. Font size now lives in `screen/ChatLayoutSettingsScreen.kt` (`ChatAppearanceContent`, `PillGroup`) + `viewmodel/ChatLayoutSettingsViewModel.kt` | PORT | S |
| 13 | Opt-in `legacyDebugApplicationId` debug build | `app/build.gradle.kts` | None | Same `buildTypes.debug { applicationIdSuffix = ".debug" }`; upstream added foss/play flavors | KEEP-CLEAN | – |
| 14 | iOS settings avatar picker | `feature/settings/src/iosMain/…/AvatarUploadDialog.ios.kt`, `SettingsPlatformModule.ios.kt` (`IosPickedImage`); core/ui `IosImagePicker.kt` | `AvatarUploadDialog.ios.kt` `rememberAvatarPicker` → `core/ui/…/media/IosPhotoPicker.kt` `openPhotoPicker` (upstream #796); dialog moved to common | Rewritten | DROP | – |
| 15 | Streaming multipart upload | fork-only `core/network/…/upload/StreamingUploadSource.kt`, test `FilesApiStreamingUploadTest.kt`; edit in `api/FilesApi.kt`, `core/network/CLAUDE.md`; callers in core/data `FileRepository*` and features | None. `FilesApi.uploadFile(bytes: ByteArray, …)` only | FilesApi recognisable (adds `LogRedactor.redactFileName`) | PORT | M |
| 16 | `/cost/markets` client | fork-only `core/network/…/api/MarketApi.kt`, test; one line in `di/NetworkModule.kt`; KoinGraph/NetworkModule test lists | None | NetworkModule recognisable | KEEP-CLEAN | – |
| 17 | `EndpointConstants.ANTHROPIC` (used by cache-TTL rail) | `core/common/…/Constants.kt` | None | Recognisable | KEEP-CLEAN | – |
| 18 | Observe Android locale changes (`LocalResources.current`) | core/ui `theme/LocalAppLocale.android.kt` (outside the six modules, listed in the brief) | None: upstream still reads `LocalContext.current.resources` | Unchanged shape | KEEP-CLEAN | – |
| 19 | German resource coverage gate | strings `values-de` in auth/settings/shared; `feature/auth/CLAUDE.md`; (outside area: `scripts/check-localization.py`, `.github/workflows/ci.yml`) | Upstream German is complete for auth (113/113), settings (988/988), shared (18/18); `scripts/i18n-coverage.py` exists but is not run in CI | – | DROP for upstream keys; KEEP-CLEAN gate | – |

Counts: DROP 6 (rows 2, 3, 4, 6, 11, 14), KEEP-CLEAN 5 (13, 16, 17, 18, 19), PORT 7
(1, 5, 7, 8, 9, 12, 15), UNSURE 1 (10). Row 19 splits: the de strings for upstream keys DROP, the CI
gate and de strings for fork-only keys KEEP-CLEAN; it is counted once, as KEEP-CLEAN.

### Notes per feature

### 1. System password manager — PORT, M

What it does now: `LoginScreen` shows "Use saved login" (`rememberPasswordCredentialManager()`
non-null on Android only) → `getCredential(emptySet())` → `LoginViewModel.onCredentialSelected`
fills both fields for review. On `LoginOutcome.Success` with `offerCredentialSave`, the VM holds
`pendingCredentialSave` and withholds `isLoggedIn` until the screen's `LaunchedEffect` has called
`saveCredential` and `onCredentialSaveHandled(saved)`, which stores a `SavedLoginCredentialRef` via
`ServerDataStore.rememberLoginCredential`. On `TwoFactorRequired` the intent is staged in the
`PendingCredentialSaveHandoff` singleton and consumed by `TwoFactorViewModel` on
`VerifyTwoFactorOutcome.Success`; `abandon()`/`onCleared()` clear it. Editing either field clears
`selectedPasswordCredential`, so an unchanged picked credential is not re-offered. IDs are the plain
username (`credentialId = state.email.trim()`); legacy refs map ID→username through
`ServerDataStore.savedLoginCredentials(serverUrl)`.

Upstream: nothing equivalent. Hosts changed: `LoginViewModel` ctor is now
`(authRepository, configRepository, accountSwitcher, ssoRiskDataStore, serverDataStore, certificateTrust)`,
`login()` has a third outcome `LoginOutcome.TwoFactorSetupRequired`, and errors carry `refusal`.
`LoginScreen` keeps `uiState.password` after success (it never clears it), uses
`AdaptiveOutlinedTextField`, and calls `currentOnLoginSuccess()` from `LaunchedEffect(uiState.isLoggedIn)`.
`AuthModule.kt` binds `viewModelOf(::LoginViewModel)` and `TwoFactorViewModel(authRepository, initialTempToken)`.
`gradle/libs.versions.toml` upstream has no `credentials` entries.

Smallest seam (no LoginViewModel edits):
- Fork-only: keep `PasswordCredentialManager.kt`, `.android.kt`, `.ios.kt`, `PendingCredentialSaveHandoff.kt`.
  Add a fork-only `PasswordManagerLoginBridge` (feature/auth/credentials) that remembers the picked
  credential, decides "offer save" (`email/password` differ from the picked pair), runs
  `saveCredential` and writes the ref, and exposes a `SavedLoginButton` composable that calls the
  VM's public `onEmailChanged`/`onPasswordChanged` to fill.
- Upstream-owned lines:
  - `LoginScreen.kt`: in `LaunchedEffect(uiState.isLoggedIn)` call `bridge.saveThen(email, password) { currentOnLoginSuccess() }`
    instead of `currentOnLoginSuccess()` (~2 lines); in `LaunchedEffect(uiState.twoFactorTempToken)` (and
    the setup-token effect if wanted) call `bridge.stageForTwoFactor(email, password)` (~1 line each);
    one `SavedLoginButton(...)` line under the password field.
  - `TwoFactorScreen.kt`: in `LaunchedEffect(uiState.isVerified)` call `bridge.completeTwoFactor { currentOnVerify() }`;
    wrap the back action to `handoff.clear()` (~3 lines).
  - `AuthModule.kt`: `singleOf(::PendingCredentialSaveHandoff)` (1 line).
  - `feature/auth/build.gradle.kts`: two `androidMain` deps; `gradle/libs.versions.toml`: `credentials` version + 2 libs.
  - core/data `ServerDataStore` ref helpers: move to a fork-only store (core/data ledger).
- Rewrite `LoginViewModelTest`/`TwoFactorViewModelTest` fork cases as bridge tests; keep
  `AndroidPasswordCredentialManagerTest` as is.
- Decide whether the 2FA-setup path (`TwoFactorSetupRequired` → `TwoFactorSetupScreen`) should also
  save. The fork never had that path.
- Device check before porting: with upstream's semantics alone, does the Android autofill service
  (Google Password Manager) already offer fill and save on the login form? If it does, the explicit
  button may only duplicate prompts; the fork already had both on Login.

### 2. OAuth cookie gating — DROP
The guard protected a Custom-Tab cookie read on `ON_RESUME`. Upstream deleted that path
(`OAuthLauncher`, `checkOAuthResult`); SSO cookies are read inside the WebView flow
(`SsoLoginViewModel`, `oauth/SsoCookieCapture.kt`). Returning from the Credential Manager sheet cannot
consume a cookie any more. Nothing lost.

### 3. Native autofill semantics — DROP
Upstream `LoginScreen.kt:168`, `RegisterScreen.kt:93-155`, `ResetPasswordScreen.kt:128-150` set
contentType semantics, and `IosTextContentType` covers iOS. Upstream is a superset. Nothing lost.

### 4. Remembered servers on ServerUrlScreen — DROP
Upstream `KnownServersSection` lists the servers of roster accounts, most recent first, with account
count; `connectTo(url)` connects in one tap with the stored headers. Lost: servers validated but with
no account (for example after signing out of the last one), the per-row Forget and "Warn again"
buttons, and select-then-review instead of one-tap connect. If row 7 is ported and the user wants
no-account servers kept, the fork store (`rememberedServers`) survives as core/data fork code and
feeds only the settings screen; do not re-add it to ServerUrlScreen.

### 5. Per-server HTTP-warning suppression — PORT, S
Hooks in upstream `ServerUrlViewModel.kt`:
- `validateAndConnect()` http branch (upstream ~L255-263, after the iOS `cleartextBlockedForDomainNames`
  check): `viewModelScope.launch { if (store.isSuppressed(url)) doValidateAndConnect(url) else show dialog }`.
- `confirmHttpConnection()`: remember the checkbox value; after a successful connect write the
  suppression (inside `resolveAndCommit` success, or keyed on `isValidated`).
- Dialog: upstream's `HttpWarningDialog` in `ServerUrlScreen.kt` takes `onConfirm/onDismiss`; add a
  `suppressForServer` checkbox (two params, ~10 lines) or replace with a fork-only dialog composable.
Fork-only: a small `HttpWarningPolicy` store (core/data) and the UI state flag. Key it by
`cleanServerUrl(url)` as upstream does now, not the fork's `trim().trimEnd('/')`. Its only other
consumer is row 7 ("reset warning"); if row 7 is dropped, this still stands alone.

### 6. Restore previous URL on failed validation — DROP
Upstream never writes the URL before the probe succeeds (`resolveServer` → `commitPendingServer`).

### 7. Server profiles in settings — PORT, M
Fork-only files port almost unchanged: `ServerProfilesScreen.kt`, `ServerProfilesViewModel.kt`
(`buildServerProfiles`, `confirmForget`), `ServerProfilesViewModelTest.kt`.
Hook points now:
- `navigation/SettingsNavigation.kt`: `@Serializable data object ServerProfiles : SettingsRoute`,
  `entry<ServerProfiles> { ServerProfilesScreen(onNavigateBack = onBack, onAddAccount = onAddAccount) }`,
  `subclass(ServerProfiles::class, …)` in `settingsSerializersModule`, and an `onAddAccount: () -> Unit = {}`
  param on `settingsEntries` (≈6 lines).
- Entry row: upstream deleted `GeneralSettingsScreen`/`TabbedSettingsScreen`. Put one `row(key = "server_profiles")`
  in `screen/AccountSettingsPage.kt` next to the `connections_header` section (it currently holds only
  `pull_request_account`), with an `onNavigateToServerProfiles` param threaded from
  `SettingsCategoryNavigation.kt` (`SettingsCategoryKey.ACCOUNT -> AccountSettingsPage(...)`). ≈6 lines.
- `di/SettingsModule.kt`: `viewModelOf(::ServerProfilesViewModel)` (1 line).
- `shared/…/navigation/LibreChatNavHost.kt` `settingsEntries(...)` call (~L939): `onAddAccount = { navigator.navigate(AddAccountServerUrl) }` (1 line).
Behaviour to re-check against upstream: `AccountSwitcher.remove` now revokes the server session
(#631) and promotes `liveSuccessor` (filters `hasSession`). The forget confirmation text should say
the server sessions are signed out. The forget transaction should also drop the server's stored
custom headers (upstream `ServerRepository` header store) — the fork did not; verify.
Dependencies: rows 5 and 9 (HTTP-warning reset, `clearServerBannerDismissals`) and the core/data fork
fields `rememberedServers`, `clearServerUrl`, `forgetServer`.
Scope question for the owner: without no-account servers and HTTP-warning reset, the screen shrinks
to "accounts grouped by server + forget server", most of which the drawer switcher already does.

### 8. Multi-account sign-out explanation — PORT, S
Fork-only `SignOutViewModel.kt` (`buildSignOutUiState`) + test port as is, with one fix: upstream's
successor is `AccountSwitcher.liveSuccessor` = `filter { hasSession(it) }.maxByOrNull { lastActiveAt }`;
the fork mirror ignores `hasSession`, so it would name an expired account as the one that "will
appear". Either call `accountSwitcher.hasSession(id)` (public, `AccountSwitcher.kt:437`) per entry, or
expose a public `previewSuccessor()` (core/data ledger).
Hooks in `screen/AccountSettingsPage.kt`: the `if (showLogoutDialog) ConfirmDialog(...)` block (~L329-337)
→ a fork-only `SignOutConfirmDialog(signOutState, onConfirm = viewModel::logout, onDismiss)`; the
`row(key = "sign_out")` title (~L209) picks `action_sign_out_this_account` when a successor exists; one
`signOutViewModel: SignOutViewModel = koinViewModel()` param. `SettingsModule.kt`: 1 line. Strings:
`dialog_title_sign_out_account`, `dialog_sign_out_last_account_message`,
`dialog_sign_out_switch_account_message`, `action_sign_out_this_account`, `sign_out_this_account`,
`sign_out_this_server` (+ de).

### 9. Persistent per-server banner dismissals — PORT, S
Upstream `BannerStateHolder.kt` (101 lines) is the fork's base minus persistence. Hooks:
- ctor: `private val dismissalStore: BannerDismissalStore? = null` (fork-only interface, impl in core/data).
- `fetchBanner()`: before `bannerRepository.getBanner()`, preload `dismissedKeys` from the store for
  `serverId`; after the fetch, drop the result if the server changed meanwhile (the fork's
  `currentServerId != serverId` guard). ~5 lines.
- `dismissBanner()`: `if (_banner.value?.persistable == true) return` and persist. ~3 lines.
- `VersionCheckStateHolder.kt`: read/write `dismissedVersionWarning(serverId)` /
  `setDismissedVersionWarning(serverId, version)` instead of the global key (2 call sites, L48 and L83-).
- `NavHostViewModel.kt` L122: pass the store (1 line).
Keep `BannerDismissalStateTest.kt`. Forget-server cleanup belongs to row 7.

### 10. Off-Main token warm-up and startup auth gate — UNSURE (PORT M if needed)
Upstream already has the lazy store and `TokenCacheWarmer`, and the fork kept its gate on top of
that at the September merge. What remains open: does upstream's first composition still hit the
synchronous fallback on Main? Upstream says it can (`NavHostViewModel.kt` L126-135 comment).
Settle it: on the Pixel 7, cold-start the upstream build 5-10 times with a log or trace marker in
`TokenDataStore`'s store construction and `loadCacheFromStorage`, noting the thread. If Main never
shows up, DROP. If it does, port with a thinner seam than the fork's `Boolean?` (which touches ~30
`_isLoggedIn` sites in a file upstream changed 14 times):
- Fork-only `StartupAuthGate { content() }` composable: `koinInject<AccountReadyGate>().awaitReady()`
  (or a `TokenCacheWarmer` completion signal), shows the neutral progress surface until ready, then
  composes `content`. Because `NavHostViewModel` is created inside `LibreChatNavHost` as a default
  param, gating outside it means `tokenManager.isAuthenticated` is read only after warm-up, and the
  upstream Boolean seed becomes correct with no Main IO.
- Upstream-owned lines: wrap the call in `app/…/navigation/LibreChatNavHost.kt` (`SharedLibreChatNavHost(...)`)
  and in `shared/…/app/LibreChatApp.kt` (iOS path) — 2 lines each; core/data `AccountRegistry` init:
  `tokenManager.warmUp()` before seeding (1 line), `TokenManager.warmUp()` default method (3 lines) or
  depend on `TokenCacheWarmer` directly.
- The upstream warning "don't gate the first frame on the warm: that hangs a logged-out cold start"
  applies to a warm that waits on someone else; `AccountRegistry` and `TokenCacheWarmer` both drive
  their own load, so the gate must await one of those, not a lazy read.
The fork's deep-link part (`awaitAuthResolution` in app LibreChatNavHost) is moot: upstream places
deep links in shared via `navigator.placeDeepLink` after `accountState.first { it !is Warming }`.

### 11. Login-first routing when a URL is saved — DROP
Upstream `Navigator.navigateToSignIn()` covers the bound/expired account. Lost: a logged-out start
with a saved URL and no bound account opens ServerUrl (prefilled) instead of Login. The fork's
`onBack` on the Login entry goes with it; upstream deliberately routes Login's escape through
`signInEscape.actionOrNull()`.

### 12. Paragraph-spacing settings UI — PORT, S
Hosts deleted; the matching place is `ChatLayoutSettingsScreen.kt` `ChatAppearanceContent`, where
`item(key = "text_size") { PillGroup(...) }` sets font size. Seam: one `item(key = "paragraph_spacing")`
with a `PillGroup` over `ChatParagraphSpacing.entries` and the fork's `paragraphSpacingLabel`;
`ChatLayoutSettingsUiState.paragraphSpacing` + `setParagraphSpacing` in `ChatLayoutSettingsViewModel`
(its `combine` is at 5 flows; nest a pair as it already does for header content/alignment) and one
field in `ChatAppearanceActions`. ~15 lines in upstream files, or keep the state in a fork-only VM and
pass only a value + callback (`ChatAppearanceContent` is stateless and has androidMain previews, so do
not call `koinViewModel()` inside it). Optional: apply spacing in `ChatAppearancePreview`. Strings
`paragraph_spacing*` (+ de). Data and chat rendering are other ledgers.

### 13. `legacyDebugApplicationId` — KEEP-CLEAN
Re-apply the 4-line `if (!providers.gradleProperty("legacyDebugApplicationId")…)` around
`applicationIdSuffix = ".debug"` in upstream `app/build.gradle.kts` `buildTypes.debug`. Upstream now has
`foss`/`play` flavors: the legacy id applies to `fossDebug` (`com.garfiec.librechat`); `playDebug`
would still be `.play`. The documented command becomes `:app:assembleFossDebug`; update
`docs/SURPLUS_MCP_UPSTREAM_SYNC.md`. Keep upstream's `android { }` block; do not re-apply the
`extensions.configure<…>` swap.

### 14. iOS settings avatar picker — DROP
Upstream `AvatarUploadDialog.ios.kt` uses `openPhotoPicker(selectionLimit = 1, decode = …)` from
`core/ui/src/iosMain/…/media/IosPhotoPicker.kt` and a re-encoded preview (HEIC-safe). The fork's
`IosPickedImage` branch in `SettingsPlatformModule.ios.kt` is moot. Note for the agents ledger:
upstream `feature/agents/…/AgentAvatarPicker.ios.kt` is still a placeholder; port the fork's agent
picker onto upstream `openPhotoPicker` and drop the fork's `core/ui/…/platform/IosImagePicker.kt`.

### 15. Streaming multipart upload — PORT, M
Fork-only: `core/network/…/upload/StreamingUploadSource.kt`, `FilesApiStreamingUploadTest.kt`.
Upstream-owned: `api/FilesApi.kt` — add `uploadFile(source: StreamingUploadSource, …)` and make the
ByteArray overload delegate with `StreamingUploadSource(bytes.size.toLong()) { ByteReadChannel(bytes) }`;
the multipart part becomes `ChannelProvider(source.contentLength, source.openChannel)`. Keep upstream's
`LogRedactor.redactFileName(filename)` and log `source.contentLength`. ~40 changed lines in one
function. A separate fork-only API class would duplicate `uploadMimeType`, filename encoding and
timeout logic and drift, so the overload is the smaller risk. Callers (FileRepository, chat/files
pickers) are other ledgers and carry most of the M.

### 16. MarketApi — KEEP-CLEAN
Copy `MarketApi.kt` and `MarketApiTest.kt`; add `singleOf(::MarketApi)` near `singleOf(::AuthApi)` in
upstream `di/NetworkModule.kt` (L227); add `MarketApi`/`MarketRepository` to
`KoinGraphVerificationTest` and `NetworkModuleVerificationTest` lists.

### 17. `EndpointConstants.ANTHROPIC` — KEEP-CLEAN
One line in `core/common/…/Constants.kt`, or move it into a fork-only constants file next to the
cache-TTL code to keep upstream's file untouched.

### 18. Android locale observation — KEEP-CLEAN
`core/ui/src/androidMain/…/theme/LocalAppLocale.android.kt`: replace `LocalContext.current.resources`
with `LocalResources.current` (2 lines). Consider offering upstream.

### 19. German coverage
Upstream German now covers every upstream key in auth/settings/shared, so drop the fork's de entries
for upstream keys (including shared `session_expired_*`). Carry de strings only for fork-only keys,
with each feature. The CI gate (`scripts/check-localization.py --require-complete de`, one CI line) is
fork-only and applies cleanly; the `feature/auth/CLAUDE.md` localization paragraph goes with it.

### Not features (fork delta that upstream made moot)

| Item | Files | Status upstream | Action |
|---|---|---|---|
| Kermit named-tag calls `Logger.d(tag = …)` (c0bef6a7) | AgentsApi, FilesApi, AuthInterceptorPlugin, LibreChatHttpClient, SseClient, SseLineParser | Upstream on Kermit 2.2.0 still uses positional tags (28 sites in core/network) and builds | Drop |
| Ktor `readUTF8Line` → `readLine(LineEnding.Lenient)` (fe11cb38) | SseLineParser | Upstream on Ktor 3.6.1 still uses `readUTF8Line` | Drop |
| `android {}` → `extensions.configure<…Extension>` (438198a8) | build.gradle.kts of app, core/common, core/network, feature/auth, feature/settings, shared | Upstream keeps `android {}` on AGP 9.5.1 | Drop |
| `MenuAnchorType` → `ExposedDropdownMenuAnchorType` (0629e39e, 4f3414db) | McpServerDialog, RoleSkillsAdminScreen, SpeechDetailDialogs, SpeechSettingsSection | Files deleted; upstream already uses `ExposedDropdownMenuAnchorType` | Drop |
| kotlinx-datetime `month.number`/`day` (c4a4e802) | DateExt.kt + test | DateExt.kt deleted (date code now `DayBoundary.kt`, `RelativeTimeReference.kt`); no fork code calls DateExt | Drop |
| `@OptIn` additions (5fb61c73, 6f0f79ea) | CommonModuleVerificationTest, ActiveAccountProviderTest, module verification tests, LibreChatApplication, LibreChatApp | Upstream has its own OptIns where needed | Drop |
| Import reorder / comment removal (4f3414db) | SseHttpTransport.ios.kt, IosPowerStateObserver.kt, MemoriesApiTest, IosKoinAccessor.kt | Cosmetic | Drop |
| `SavedStateHandle` in KoinGraphVerificationTest (6965c438) | app test | Upstream already lists it (L172) | Drop |
| Test ctor updates for fork params | ServerHeaderEditorContractTest, AuthModuleVerificationTest, SettingsModuleVerificationTest, SettingsViewModelTest (deleted upstream), IosKoinGraphTest | Follow the ported features | Re-add only what the ports need |
| Gateway headers | — | Not fork work: no fork delta in core/network for it; it came with the upstream merge | n/a |

---

# Appendix C: core/data, agents, files, conversations, skills, build

## Fork-feature ledger: core/data, feature/{agents,files,conversations,skills}, build, CI, root

Merge base `7b2311af`, fork = `develop` (`e8acf946`), upstream = `upstream/develop` `08bca78f`
(read-only worktree `.../scratchpad/upstream-wt`). Paths below are repo-relative; `D=` is
`core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data`. "Guess" marks anything not
confirmed by reading code.

---

### 1. Database

### 1.1 Fork v10 vs upstream v15, entity by entity

Compared field-by-field (column name, affinity, notNull, default), primary keys, indices and
foreign keys with a script over the two exported schemas.

**The fork's v10 is upstream's v9 plus two nullable columns.** The fork's own 8→9→10 path differs,
but the v10 result matches upstream v9 in every table, index and FK, except:

| Fork-only piece | Table | Type | Feature | Fork commit |
|---|---|---|---|---|
| `state_json` | `drafts` | TEXT, nullable, no default | Draft attachment + paused-queue recovery (UX-008) | `51d69a55` |
| `cacheTTL` | `messages` | TEXT, nullable, no default | Per-message Anthropic cache-TTL marker (cache rail countdown) | `829f16f0` |

There are no fork-only tables or indices, and no fork-only foreign keys. **Banner dismissals,
server profiles, remembered servers, password-manager refs, paragraph spacing and scoped tool
selections are all in DataStore, not Room** (see 1.4).

Upstream v15 has these that fork v10 lacks (no fork content needed; listed for the migration
hazard in 1.3):
- v10: `message_search` (FK → `messages.messageId` ON DELETE CASCADE) and FTS4 `messages_fts`
- v11: `conversations.spec`
- v12: `messages.manualSkills`, `messages.alwaysAppliedSkills`
- v13: `project_order`
- v14: `messages.isUserSubmitted`, `reasoningOverride`, `privacyRevision`;
  `conversations.lastResponseAt`, `lastResponseMessageId`, `lastResponseIsManual`,
  `isMarkedUnread`, `lastSeenAt`, `isCodeConversation`
- v15: `tool_call_parts` (FK → messages CASCADE)

Identity hashes: fork v10 `404c7600b0a0063cc516e0ace5975212`, upstream v9 `bede2f5d…`,
upstream v10 `1fe192d7…`, upstream v15 `a49a6794…`.

### 1.2 Does upstream v15 already cover a fork column under another name?

- `messages.cacheTTL`: **no.** Upstream has a conversation-level `promptCacheTtl` *model
  parameter* (`core/ui/.../EndpointParameterRegistry.kt` `PROMPT_CACHE_KEYS`,
  `core/model/.../ConversationParameters.kt`). It lives in `conversations.modelParams` JSON. It is
  a different wire field: the fork sends a one-shot top-level `cacheTTL` that the deployed backend
  persists on the message. Nothing in v15 records the TTL a turn was sent with.
  `Message.cacheTTL` is only ever set by JSON decoding (server) and by the Room mapper. The
  fork's code never stamps it locally (`grep "cacheTTL ="`), so the column caches a server field
  and losing it is regenerable.
- `drafts.state_json`: **partly.** Upstream keeps local queue rows across ViewModels in memory
  (`feature/chat/.../viewmodel/LocalQueueStash.kt`). It covers process death with a **text-only**
  copy in the same `drafts` table, under a synthetic key `"queued:$conversationId"`
  (`LocalQueueContinuityDelegate.queuedTextKey`). Upstream does not persist uploaded-attachment
  references, the paused flag, or per-row queue state (cache TTL, tools). The fork's payload
  covers all of these.
- `conversations.modelParams`: both sides have the column. Upstream now writes it itself (see
  feature F8), so the fork's use of it is redundant.

### 1.3 Platform builders, destructive fallback, and the upgrade hazard

- Builders: `core/data/src/androidMain/.../di/DataPlatformModule.android.kt` (upstream line ~66:
  `Room.databaseBuilder(androidContext(), LibreChatDatabase::class.java, "librechat.db")`
  `.setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(IO).addMigrations(MIGRATION_3_4,
  MIGRATION_4_5)`). iOS: `core/data/src/iosMain/.../di/DataPlatformModule.ios.kt` (~line 76, path
  from `iosDatabasePath()` in `datastore/IosFreshInstallReset.kt` =
  `Library/Application Support/librechat.db`).
- **Neither side uses `fallbackToDestructiveMigration*` anywhere** (checked both trees for
  core, app and shared).
- **Hazard (inferred from how Room validates; not executed):** an upstream-based build installed
  over a fork-v10 database (the owner's Pixel, legacy id `com.garfiec.librechat`) sees
  `user_version = 10`. It runs upstream's auto-migrations 10→11…14→15, which assume upstream v10.
  That means `message_search`/`messages_fts` are never created and `drafts.state_json` /
  `messages.cacheTTL` are left in place. Room's post-migration schema validation then fails, so
  the app crashes on first DB open ("Migration didn't properly handle …").
  **This needs a fork-only hook even though losing the cache is fine.** Recommended: a fork-only
  function `discardForkSchemaIfPresent(path)`, called in both platform modules before
  `databaseBuilder`. It reads `room_master_table.identity_hash` and deletes `librechat.db`,
  `-wal` and `-shm` when the hash is `404c7600b0a0063cc516e0ace5975212`. That is 1 line per
  platform module plus a fork file.
  A simpler alternative is `fallbackToDestructiveMigrationFrom(true, 10)`. Room may reject it as
  inconsistent with `AutoMigration(10, 11)` (UNSURE; a unit test with `MigrationTestHelper`
  settles it). It would also wipe a genuine upstream-v10 DB, which no fork install has.
  iOS: `resetKeychainLeftByPreviousInstall` treats the DB file as "prior app data". Deleting
  only the DB while DataStore remains does not trigger a keychain wipe (read
  `hasPriorAppData`: DataStore path OR DB path).

### 1.4 Could the fork pieces live in a second, fork-owned Room database?

Yes, both could, and neither has to. Findings per point asked:

| Concern | `drafts.state_json` | `messages.cacheTTL` |
|---|---|---|
| Foreign keys / joins with upstream tables | None (drafts has no FK; PK is `conversation_id` alone) | None today. A side table would key on `messageId` with no cross-DB FK |
| Upstream DAO queries that read the fork column | None. Only `SELECT *` in `DraftDao.getDraftForAccount` | None filter on it. It rides in `SELECT *` of every `MessageDao` read and is decoded by `MessageMapper.toModel` |
| Transactions spanning both | Fork writes text + payload in **one row** atomically (`DraftRepositoryImpl.saveDraftState`). A second DB loses that atomicity; acceptable for recovery data, or duplicate the text into the fork row | Message upsert from server and marker write would be separate; no consistency need (only the newest message's TTL is read: `feature/chat/.../CacheTtl.kt` `newestCacheTtlAnchor`) |
| Account/server scoping | `drafts.accountId`; fork row must carry it | Needs `accountId` + `conversationId` |
| Deletion cascades | Upstream purges drafts in `AccountDataPurger.purge` (`draftDao.deleteAllForAccount`). A fork DB needs a hook there or its own subscription. Draft deletion on send/clear goes through `DraftRepository.deleteDraft` | Messages are deleted by `AccountDataPurger`, `ConversationRepositoryImpl` (lines ~274, ~354), and `PrefetchEngine` (`messageDao.deleteForConversations`, ~551). No cascade into a fork DB → orphans unless purged per account |

**Cheaper than a second DB, and recommended:**
- **Draft state → synthetic key in upstream's own `drafts` table**, as upstream itself does for
  `"queued:<id>"`. A fork-only `DraftStateStore` in core/data uses `DraftDao.upsertDraft` /
  `getDraftForAccount` / `deleteDraftForAccount` with `conversationId =
  "switchboard-state:<id>"` and `text = <payload JSON>`. No schema change. Account purge comes
  for free (`deleteAllForAccount`). Nothing upstream enumerates drafts:
  `observeDraftsForAccount` has no callers. The fork chat code must delete the state row
  wherever it deletes the text draft.
- **Cache-TTL marker → fork-only store keyed per conversation** holding `(messageId, ttl)` for
  the newest message. Use DataStore with an account-scoped key
  `acct:<accountId>:cache_ttl_anchor:<conversationId>` via `accountScopedKey(...)` in
  `datastore/ScopedPreferenceKeys.kt`. Upstream's `AccountScopedPrefsPurger` removes every
  `acct:<id>:*` key, so account purge is free. Alternatively use a one-table fork Room DB. Either
  way it needs hooks where server messages land: the message refresh in `MessageRepositoryImpl`
  and the stream-final save. The chat side then reads that store instead of `message.cacheTTL`.
  The request side needs **no** core/data change: put `"cacheTTL"` into the existing
  `modelParams` JsonObject. `ChatPayloadBuilder.toBody` (upstream line ~132) merges it at the top
  level of the body.
- A second Room DB only pays off if more fork tables arrive. If chosen, it needs its own
  `@Database` class, a schema folder (Room KSP writes per-class subfolders under the module's
  schema dir), builders in a fork-only Koin module appended to `sharedKoinModules`
  (`shared/.../di/SharedKoinModules.kt`, 1 line), and the iOS path helper
  `iosAppSupportDirectory()` (internal in core/data, so the fork file must live in core/data).

### 1.5 DataStore and token storage

All fork keys sit in the single upstream preferences file (`DataStore<Preferences>` built in the
platform modules with `PreferenceMigrationRunner`). No fork key collides with an upstream key,
and upstream's migrations remove only keys they declare (`PreferenceMigrations.kt`, ledger key
`applied_pref_migrations`). A second DataStore file is not needed. Fork-only keys:

| Key | Defined in (fork) | Feature | Scope |
|---|---|---|---|
| `chat_paragraph_spacing` | `SettingsDataStore` | Paragraph spacing | device |
| `acct:<id>:selected_mcp_servers`, `acct:<id>:enabled_tools` (replace upstream's global `selected_mcp_servers`, `enabled_tools`) | `SettingsDataStore.scopedStringSet` | UX-002 scoped tools/MCP | account (purged by `AccountScopedPrefsPurger`) |
| `srv:<serverId>:dismissed_banner_ids` (string set) | `SettingsDataStore.dismissBanner` | UX-009 banner dismissals | server |
| `srv:<serverId>:dismissed_version_warning` (replaces global `dismissed_version_warning`) | `SettingsDataStore.setDismissedVersionWarning(serverId, …)` | UX-009 version warning per server | server |
| `remembered_server_urls` (string set) | `ServerDataStore.rememberServer` | UX-001 server profiles | device |
| `http_warning_suppressed_server_urls` (string set) | `ServerDataStore.setHttpWarningSuppressed` | HTTP-warning suppression per server | device |
| `saved_login_credential_refs` (JSON list of `SavedLoginCredentialRef`) | `ServerDataStore.rememberLoginCredential` | Password-manager refs (UX-005); password never stored | device |

Fork code writing these keys can move into fork-only classes in core/data over the same
`DataStore<Preferences>`, using the internal `accountScopedKey` / `serverScopedKey` /
`serverScopedName` helpers (upstream `ScopedPreferenceKeys.kt`). Keeping the same key names
preserves existing on-device values. Paragraph spacing fits upstream's declarative registry:
a fork-only object of `stringPreference(...)` specs read via `PreferenceStore`
(`datastore/PreferenceStore.kt`, `PreferenceSpec.kt`).

**EncryptedSharedPreferences / Keychain token storage: no fork-only keys.** The fork's changes
there are a test seam (`TokenDataStore(…, encryptedPrefsFactory)`) and a `warmUp()` call
(feature F7). Credential Manager secrets live in the platform provider, not in the app.

---

### 2. Feature table

| # | Feature | Fork files (main) | Upstream counterpart | Host-file state upstream | Verdict | Size |
|---|---|---|---|---|---|---|
| F1 | Draft attachment + paused-queue recovery (data layer) | `D/db/entity/DraftEntity.kt`, `D/repository/DraftRepository.kt`, `DraftRepositoryImpl.kt`, schema 9/10 | Text-only `LocalQueueContinuityDelegate` + in-memory `LocalQueueStash` | All present, recognisable | PORT | S (data); chat side M, other ledger |
| F2 | One-shot cache TTL: request field + per-message marker (data layer) | `D/db/entity/MessageEntity.kt`, `D/mapper/MessageMapper.kt`, `D/repository/ChatPayloadBuilder.kt`, `ChatRepository(.Impl).kt` | Only conversation-level `promptCacheTtl` param | Present; `ChatRepository.startChat` grew; `toBody(json, request, modelParams)` exists | PORT | M |
| F3 | Room fork v8/v9/v10 numbering + fork 9→10 migration | `LibreChatDatabase.kt`, schemas 8/9/10, `ForkMigration9To10Test.kt` | Upstream's own v8–v15 | Rewritten to v15 | DROP (replace with discard hook, §1.3) | S |
| F4 | Persisted server-scoped banner dismissals + per-server version-warning dismissal | `D/datastore/SettingsDataStore.kt`, `ServerScopedBannerDismissalTest.kt` | `shared/.../BannerStateHolder.kt` keys dismissals by server, but **in memory only**; `VersionCheckStateHolder` still uses global `dismissedVersionWarning` | Present | PORT | S |
| F5 | Account-scoped MCP/tool selections (UX-002) | `SettingsDataStore.kt`, `AccountScopedToolSelectionTest.kt` | None: still global keys (`SettingsDataStore` ~350–355, ~834–850) | Present | PORT | S |
| F6 | Paragraph spacing preference (data part) | `D/datastore/ChatParagraphSpacing.kt`, `SettingsDataStore.kt`, `config/detekt/detekt.yml` | None | `SettingsDataStore` present; upstream adds `AppPreferences` registry | KEEP-CLEAN | S |
| F7 | Warm secure tokens off main before readiness gate (UX-012) | `AccountRegistry.kt`, `CommonTokenDataStore.kt`, `TokenDataStore.kt` | `TokenCacheWarmer` (createdAtStart, `DataModule.kt` ~229) + lazy store | Present | DROP | – |
| F8 | Conversation model-parameter cache/recovery (data part) | `D/mapper/ConversationModelParamsCache.kt`, `ConversationMapper.kt`, `D/db/ConversationCacheMerge.kt`, `ConversationDao.upsertAuthoritativeSnapshot` | `ConversationMapper` `parameterPreset()` → `CachedParameters(asOf, params)`; DAO keeps `modelParams ?: existing` | Rewritten | DROP | – |
| F9 | Favourites reconciled across archived conversations (UX-014) | `ConversationRepositoryImpl.syncFavoritesFromServer`, `ConversationDao.getConversationsWithTagForAccount` | None; upstream still carries the "Known gap" comment | Present, same shape (~451–497) | PORT | S |
| F10 | Marketplace prices repository (Surplus prices panel) | `D/repository/MarketRepository.kt` (new), `DataModule.kt` +1 | None | `DataModule` present | KEEP-CLEAN | S |
| F11 | Streaming multipart uploads, size pre-check (UX-007) | `D/repository/FileRepository(.Impl).kt`, `feature/files/.../platform/FileReader.kt`, `AndroidFileReader.kt`, `IosFileReader.kt`, `viewmodel/FilesViewModel.kt` (+ core/network `StreamingUploadSource`, `FilesApi` overload) | None; upstream is still `ByteArray`, adds PDF decryption that needs bytes (`FileRepositoryImpl.preparePdf`) and `ReplayableUploadBodies` (assumes byte-array parts) | `FilesViewModel` rewritten (+203/−?); `FileReader` unchanged | PORT | M |
| F12 | iOS Files upload picker + PDFKit preview (UX-015) | `feature/files/src/iosMain/.../FilePickerLauncher.ios.kt`, `PdfPreview.ios.kt`, `IosFileReader.kt` | Still stubs/placeholders | Present; `PdfPreview` signature identical (`FilePreviewDisplayData`) | PORT | S |
| F13 | Agent editor unsaved-draft protection (UX-006) | `viewmodel/AgentEditorDraft.kt` (new), `AgentEditorStateHandle.kt`, `AgentEditorViewModel.kt`, `screen/AgentEditorScreen.kt`, `AgentEditorDialogs.kt`, `di/AgentsModule.kt`, `components/model/*` | `AgentEditorRequests.hasUnsavedChanges()`, `AgentEditorViewModel.onBackRequested/discardChanges`, core/ui `DiscardChangesDialog`, `PlatformBackHandler` | Reworked | DROP (process-death restore lost) | – (opt. M) |
| F14 | Paging needs explicit retry after page error (UX-004) | `AgentMarketplaceViewModel.kt`, `AgentMarketplaceScreen.kt`, `SkillsListViewModel.kt`, `SkillsListScreen.kt` | `loadMoreFailed` + `retryLoadMore()` in both VMs | Rewritten | DROP | – |
| F15 | iOS agent avatar picker | `feature/agents/src/iosMain/.../AgentAvatarPicker.ios.kt`, `AgentsPlatformModule.ios.kt` (+ core/ui `IosImagePicker.kt`) | Placeholder; upstream has core/ui `media/IosPhotoPicker.kt` `openPhotoPicker` | Present | PORT | S |
| F16 | Icon-only action labels + detekt rule (UX-011) | `detekt-rules/.../IconButtonContentDescriptionRule.kt` (new) + provider line; labels in `AgentHandoffsSection.kt`, `DrawerContent.kt`, `ProjectsScreen.kt` | No rule; upstream still has ~4 violations | Provider present | KEEP-CLEAN (rule) + PORT S (labels) | S |
| F17 | German resource coverage enforcement (UX-010) | `scripts/check-localization.py` (new), `.github/workflows/ci.yml` +3, `values-de/strings.xml` backfill | `scripts/i18n-coverage.py` (all locales, not in CI); upstream German now complete (0 missing, measured) | ci.yml present | KEEP-CLEAN (script+CI); DROP backfill | S |
| F18 | Legacy debug applicationId switch | `app/build.gradle.kts` | None; upstream added `foss`/`play` flavors | Rewritten (flavors) | PORT | S |
| F19 | Credential Manager dependencies | `gradle/libs.versions.toml` (`credentials`, `credentials-play-services-auth`), `feature/auth/build.gradle.kts` | None | Present | KEEP-CLEAN (but see F-Droid risk) | S |
| F20 | Gradle/Compose DSL modernisation | `build-logic/.../*ConventionPlugin.kt`, all `build.gradle.kts`, `libs.versions.toml` mpp entries | Upstream still uses old accessors (CMP 1.12.2, AGP 9.5.1) | Present | DROP (not a feature; redo later if wanted) | – |
| F21 | Fork docs | `CLAUDE.md`, `UX_GAP_WORKLOG.md`, `docs/SURPLUS_MCP_UPSTREAM_SYNC.md`, `docs/BUILD_WARNING_AUDIT.md`, module CLAUDE.md | – | – | KEEP-CLEAN (rewrite DB sections) | S |

Count in this table: DROP 6 (F3, F7, F8, F13, F14, F20), KEEP-CLEAN 4 (F6, F10, F19, F21),
PORT 10 (F1, F2, F4, F5, F9, F11, F12, F15, F18, plus F16 labels), mixed 1 (F17: KEEP-CLEAN
script, DROP backfill). F16 is counted as PORT. UNSURE: none as a whole; the open sub-questions
are marked inline.

---

### 3. Per-feature notes

### F1 Draft attachment + paused-queue recovery (data part)
- Fork: `DraftSnapshot(text, stateJson)`, `DraftRepository.getDraftState/awaitDraftState/
  saveDraftState` (`D/repository/DraftRepository.kt`), cache of `DraftSnapshot` in
  `DraftRepositoryImpl`, `DraftEntity.stateJson` (`state_json`). Consumer:
  `feature/chat/.../viewmodel/ChatDraftRecovery.kt` (other ledger).
- Upstream: `DraftRepository` unchanged in shape (`getDraft/awaitDraft/saveDraft/deleteDraft`).
  Queue continuity: `feature/chat/.../delegate/LocalQueueContinuityDelegate.kt` (text under
  `queued:<id>`), `LocalQueueStash.kt`.
- Lost if dropped: server-uploaded attachment refs and per-row queue state across process death.
- Seam: fork-only `D/repository/DraftStateStore.kt` (interface + impl over `DraftDao` with key
  `switchboard-state:<conversationId>`, account from `ActiveAccountProvider`). Register in a
  fork Koin module or with 1 line in `DataModule`. **No edit to `DraftEntity`,
  `DraftRepository(.Impl)` or the schema.** Chat-side calls are owned by the chat ledger.

### F2 One-shot cache TTL (data part)
- Fork: `MessageEntity.cacheTTL`, `MessageMapper` (2 lines), `ChatPayloadBuilder.buildRequest(…,
  cacheTtl)` → `ChatRequest.cacheTTL`, `ChatRepository.startChat(…, cacheTtl)`.
  Tests: `CacheTtlMessageMapperTest`, `CacheTtlPayloadBuilderTest`.
- Upstream: `ChatPayloadBuilder.toBody(json, request, modelParams)` overlays `modelParams` at the
  top level. No message marker.
- Seam: request: put `cacheTTL` into `modelParams` in the chat ViewModel. **Zero core/data
  edits**, and `ChatRequest.cacheTTL` becomes unnecessary. Marker: fork-only `CacheTtlAnchorStore`
  (DataStore account-scoped key per conversation, or a 1-table fork Room DB). Hooks: where
  server messages are upserted (`MessageRepositoryImpl` refresh path) and where the stream's final
  message is saved. 1–3 lines each in upstream files. Guess: exact hook lines not located; the
  message-load path was not traced.
- Open question: does the chat screen ever read network messages before Room? If it does, the
  marker could be taken from the network list with no store. Read `MessageRepositoryImpl` and
  the chat message-load delegate to settle it.

### F3 Fork schema numbering
- Drop fork `8.json`, `9.json`, `10.json`, the `AutoMigration(9,10)` line,
  `ForkMigration9To10Test.kt` and the `core/data/CLAUDE.md` "Fork schema numbering" section.
  Take upstream's schemas 1–15.
- Replace with the identity-hash discard hook in §1.3 (fork file plus 1 line in each of
  `DataPlatformModule.android.kt` / `.ios.kt`), and a test that opens a fork-v10 file.

### F4 Banner dismissals persisted per server
- Fork: `SettingsDataStore.dismissedBannerIds(serverId)`, `dismissBanner`,
  `dismissedVersionWarning(serverId)`, `setDismissedVersionWarning(serverId, v)`,
  `clearServerBannerDismissals`. Consumers in `shared/.../BannerStateHolder.kt` and
  `VersionCheckStateHolder.kt` (other ledger).
- Upstream: `BannerStateHolder.dismissBanner` adds `dismissKey(serverId, bannerId)` to an
  in-memory `dismissedKeys`. It survives account switches, not restarts.
  `VersionCheckStateHolder` (~line 48) reads the global `settingsDataStore.dismissedVersionWarning`.
- Seam: fork-only `D/datastore/ServerDismissalStore.kt` with the same keys
  (`srv:<id>:dismissed_banner_ids`, `srv:<id>:dismissed_version_warning`). Hooks in shared: seed
  `dismissedKeys` from the store and write on dismiss (~4 lines), and the version check
  read/write (~2 lines). No `SettingsDataStore` edit.

### F5 Account-scoped MCP/tool selection
- Fork: `SettingsDataStore.selectedMcpServers` / `enabledTools` via `scopedStringSet(::mcpServersKey)`;
  the setters write `acct:<id>:…` and remove the bare legacy key.
- Upstream: global keys; callers `ChatViewModel.kt` ~1220–1221, `ModelSelectionDelegate.kt` ~1180,
  ~1211.
- Seam: fork-only `D/datastore/AccountScopedToolSelection.kt` holding the flows and setters.
  Then either (a) replace the 4 member bodies in `SettingsDataStore` with delegation (~8 lines),
  or (b) repoint the 4 chat call sites. Account purge is automatic. The chat-side
  `ChatRequestBuilder` validation belongs to the chat ledger.

### F6 Paragraph spacing (data part)
- Fork: `ChatParagraphSpacing` enum (fork-only file), `SettingsDataStore.chatParagraphSpacing` /
  `setChatParagraphSpacing`, key `chat_paragraph_spacing`.
- Seam: keep the enum file and add a fork-only `SwitchboardPreferences` object with
  `stringPreference("chat_paragraph_spacing", …)`, read through upstream `PreferenceStore`.
  No `SettingsDataStore` edit. Upstream-file touch: `config/detekt/detekt.yml` line ~225, add
  `LocalChatParagraphSpacing` to `allowedCompositionLocals` (UI side, other ledger).

### F7 Token warm-up before the gate — DROP
- Fork: `TokenManager.warmUp()` (core/network), `CommonTokenDataStore.warmUp()`, called first in
  the `AccountRegistry` init; `TokenDataStore.encryptedPrefsFactory` test seam;
  `TokenDataStoreLazyInitializationTest`.
- Upstream: `D/datastore/TokenCacheWarmer.kt` runs `warmTokenCache()` at startup off main, with
  a synchronous-load fallback. The lazy encrypted store came from upstream (per
  `docs/SURPLUS_MCP_UPSTREAM_SYNC.md`).
- Possible loss: the fork forced the warm-up to finish **before** `AccountReadyGate` completes.
  Upstream runs it concurrently. Guess: harmless because of the fallback. Re-measure only if
  cold-start jank on Main returns.

### F8 Model-parameter cache — DROP
- Fork: `ConversationModelParamsCache.kt` (`encodeCachedModelParams` / `restoreCachedModelParams`,
  `CachedConversationModelParams`), `ConversationCacheMerge.kt`
  (`preserveLocalCacheFieldsFrom`), `ConversationDao.upsertAuthoritativeSnapshot`, and
  `getConversation` using it.
- Upstream: `ConversationMapper.toEntity` stores `parameterPreset()` as `CachedParameters(asOf,
  params)`; `toModel` applies `withParameters` plus `parametersAsOf`.
  `ConversationDao.upsertPreservingTags` keeps `modelParams ?: existing.modelParams`.
- Loss: when a detail fetch resolves to all defaults, upstream keeps the older cached params,
  while the fork cleared them. Unverified whether `parametersAsOf` arbitrates this elsewhere.

### F9 Archived favourites — PORT S
- Fork: `syncFavoritesFromServer` calls `collectServerFavorites(account, isArchived=false/true)`.
  Stale removal queries `ConversationDao.getConversationsWithTagForAccount(accountId,
  tagJsonToken)` (`tags LIKE '%"Saved"%'`). Tests: `ArchivedFavoriteReconciliationTest`,
  `ConversationTagQueryTest`.
- Upstream: same method at `ConversationRepositoryImpl.kt` ~451–497, with the "Known gap"
  comment. `ConversationsApi.getConversations(cursor, limit, isArchived, tags, …)` already takes
  `isArchived`.
- Seam: fork-only `D/repository/ArchivedFavoriteReconciler.kt` taking `ConversationsApi`,
  `ConversationDao`, `ActiveAccountProvider`. The upstream method body becomes a 1-line
  delegation. For the stale pass, reuse the existing
  `observeConversationsForAccount(account, isArchived = true).first()`; that avoids a new DAO
  query and keeps `AccountScopedDaoRule` quiet. The fork's bounded LIKE query is an optimisation
  that needs an upstream DAO edit (+1 query).

### F10 MarketRepository — KEEP-CLEAN
- Fork-only `D/repository/MarketRepository.kt` (`MarketRepository`, `MarketRepositoryImpl(MarketApi)`).
  Hook: `DataModule.kt` `singleOf(::MarketRepositoryImpl) bind MarketRepository::class` (+import),
  plus the `MarketApi::class` entry in `DataModuleVerificationTest`. It depends on core/network
  `MarketApi` and core/model `MarketPrice` / `MarketplaceEndpoints` (other ledger).

### F11 Streaming multipart uploads — PORT M
- Fork: `FileRepository.uploadFile(source: StreamingUploadSource, …)` + impl;
  `FileReader.openUploadSource` **replaces** `readBytes`;
  `AndroidFileReader.openUploadSource` (ContentResolver stream → `toByteReadChannel`, size via
  `OpenableColumns.SIZE`); `IosFileReader.openUploadSource` (kotlinx-io `SystemFileSystem.source`
  + `SecurityScopedSource`); `FilesViewModel.uploadFile` adds an `effectiveFileSizeLimit("agents")`
  pre-check. Tests: `FilesUploadStreamingTest`, core/network `FilesApiStreamingUploadTest`.
- Upstream: `FilesViewModel.uploadFile(fileRef, pdfPassword)` reads bytes.
  `FileRepositoryImpl.withUploadBytes` → `preparePdf(bytes, …)` decrypts protected PDFs
  (`PdfPasswordProtectedException`, `pdfNormalizer`) and needs the whole file in memory. iOS
  `IosFileReader.readBytes` is now NSData-based. core/network
  `client/ReplayableUploadBodies.kt` marks **every** multipart body repeatable and documents that
  a one-shot streamed part would be resent truncated.
- Seam: keep upstream's `FileReader.readBytes`. Add a fork-only `StreamingFileReader` interface
  implemented by fork-only Android/iOS classes (or extend the platform readers in fork files
  bound in the platform Koin modules), and a fork-only `StreamingFileUploader` over `FilesApi`,
  so `FileRepository` is not touched. Hook in upstream `FilesViewModel.uploadFile`: if the file
  is not a PDF (by MIME/extension; PDFs keep the bytes path because of decryption) and a
  streaming reader is bound, run the size check and stream (~15 lines).
- Must verify: OkHttp's replay through `ReplayableUploadBodies` calls `writeTo` again. With
  Ktor's `ChannelProvider` that must reopen the source (the fork claims so for Ktor-level replay;
  the OkHttp-level duplicate write is untested). The iOS `TransportRecoveryPlugin` resends at Ktor
  level, so a fresh provider call is expected (guess).

### F12 iOS Files upload + PDF preview — PORT S
- Fork: `FilePickerLauncher.ios.kt` (UIDocumentPicker), `PdfPreview.ios.kt` (PDFKit `PDFView` in
  `UIKitView`, `produceState` download, `IosPdfLoadState`), `IosFileReader.kt`.
- Upstream: `FilePickerLauncher.ios.kt` is a no-op stub; `PdfPreview.ios.kt` shows
  `pdf_preview_unavailable_ios`. The signature is unchanged. Every string key the fork uses
  exists upstream, and so do `InfoRow(label, value)` / `FileInfoFallback`
  (`screen/FilePreviewDialog.kt` ~272, ~333) and `ByteArray.toNSData()` (core/ui
  `media/MediaActions.ios.kt`).
- Seam: move the bodies to fork-only iOS files (`IosDocumentPicker.kt`, `IosPdfKitPreview.kt`).
  The upstream `actual` functions become 1-line delegations. Upstream's `IosFileSizeFormatter.kt`
  was deleted (`formatFileSize` moved to core/ui `platform/FileSizeFormatter.ios.kt`); fix
  imports if needed. iOS has not been compiled on this host.

### F13 Agent editor draft protection — DROP
- Fork: `AgentEditorDraft.kt` (content-only baseline), `AgentEditorStateHandle`
  (`initializeDraftTracking`, `markSaved`, `replaceWithCleanState`, `requestBack`, `discardDraft`,
  `SavedStateHandle` key `agent_editor_draft`), `AgentsModule` passes `savedStateHandle`,
  `@Serializable` on `components/model/*`, strings `discard_agent_changes_title/message`,
  `discard`, `stay`.
- Upstream: `AgentEditorRequests.kt` `hasUnsavedChanges()` (compares `toUpdateAgentRequest()` to
  the baseline); `AgentEditorViewModel.onBackRequested / discardChanges /
  dismissDiscardConfirmation`; `AgentEditorScreen` has `PlatformBackHandler(enabled =
  hasUnsavedChanges || isCommitting)`; `AgentEditorDialogs` uses core/ui `DiscardChangesDialog`.
- Lost: restoring a dirty form after **process death** (SavedStateHandle). Optional port, M: a
  fork-only serializer of upstream's request shape plus 3–4 hook lines in the upstream VM.

### F14 Paging explicit retry — DROP
- Upstream `AgentMarketplaceViewModel` (`loadMoreFailed`, `retryLoadMore`, ~335/~375) and
  `SkillsListViewModel` (doc ~73, `retryLoadMore` ~299). Fork tests
  `AgentPaginationRetryTest`, `SkillsPaginationRetryTest` could be rerun against upstream as a
  check. The `SkillsListViewModel` conflict hunks are this feature only.

### F15 iOS agent avatar picker — PORT S
- Upstream `AgentAvatarPicker.ios.kt` is a placeholder initial.
- Seam: fork-only iOS file, with the actual function delegating to it. Prefer upstream core/ui
  `media/IosPhotoPicker.kt` `openPhotoPicker(...)` over the fork's core/ui `IosImagePicker.kt`.
  The `AgentsPlatformModule.ios.kt` binding (+4 lines) remains.

### F16 Icon-only labels + detekt rule
- Rule: fork-only `detekt-rules/.../IconButtonContentDescriptionRule.kt` + test. Hook:
  `LibreChatRuleSetProvider.kt` list +1 line.
- With the rule active, upstream code fails it (heuristic scan, ~4 sites):
  `feature/agents/.../components/AgentHandoffsSection.kt` ~217, ~220 (the fork had labelled
  these with `cd_edit_handoff` / `cd_remove_handoff`), `feature/chat/src/androidMain/.../ChatInput.kt`
  ~165, `feature/settings/.../providerkeys/ProviderKeyCard.kt` ~126. Upstream conversations has
  no `MoreVert, contentDescription = null` left, so `cd_project_actions` is probably moot.
  Labels need new strings, which means fork-only string files (F17).

### F17 German coverage
- Fork: `scripts/check-localization.py --require-complete de` (keys only; it globs
  `**/composeResources/values/strings.xml`); CI step in `.github/workflows/ci.yml` after
  "Verify vendored web assets"; ~369 lines of `values-de` backfill across 8 modules (agents 32,
  conversations 39, files 5 in this area).
- Upstream: German complete. The fork script run against the upstream tree reported 0 missing in
  every module; upstream `i18n-coverage.py --detector parity` reports `TOTAL missing=0`. CI does
  not run it.
- Plan: DROP the backfill. Keep the script and the CI line, or switch CI to
  `scripts/i18n-coverage.py --detector parity` (all 9 locales, which would then demand fork keys
  in every locale). Put fork-only keys (this area: `cd_edit_handoff`, `cd_remove_handoff`,
  `discard_agent_changes_*`, `discard`, `stay`, `cd_project_actions`) in fork-only
  `values/switchboard_strings.xml` + `values-de/switchboard_strings.xml`. The script must then
  glob `*.xml`, not just `strings.xml` (S).

### F18 Legacy debug applicationId — PORT S
- Fork: in `app/build.gradle.kts` the `debug { applicationIdSuffix = ".debug" }` is guarded by
  `providers.gradleProperty("legacyDebugApplicationId")`.
- Upstream: flavors `foss` / `play` (`play` has `.play` suffix); the debug block is ~line 40.
  Port the 2-line guard. The build command becomes `:app:assembleFossDebug
  -PlegacyDebugApplicationId=true`; update `docs/SURPLUS_MCP_UPSTREAM_SYNC.md`. **Pair with the
  F3 discard hook**, since this is exactly the install that holds fork v10.

### F19 Credential Manager dependencies — KEEP-CLEAN, with a caveat
- `libs.versions.toml`: `credentials = "1.6.0"`, libraries `credentials` and
  `credentials-play-services-auth`; `feature/auth` androidMain uses both.
- Caveat: `credentials-play-services-auth` pulls Google Play Services into the `foss` flavor.
  Upstream builds F-Droid from it. That matters only if the fork ever ships foss/F-Droid; if so,
  make it `playImplementation` (guess about the F-Droid scanner; the GMS dependency itself is
  known).

### F20 Gradle/Compose DSL modernisation — DROP (non-feature)
- Fork: `android {}` → `extensions.configure<com.android.build.api.dsl.LibraryExtension>` in 13
  module build files; `com.android.build.gradle.LibraryExtension` → `…api.dsl.LibraryExtension`
  in `AndroidLibraryConventionPlugin`, `KmpLibraryConventionPlugin`,
  `AndroidComposeConventionPlugin`; `KmpComposeConventionPlugin` replaces `compose.*` accessors
  with catalog entries `compose-*-mpp`; `-Xexpect-actual-classes` in `KmpLibraryConventionPlugin`.
- Upstream still uses the deprecated forms on AGP 9.5.1 / CMP 1.12.2, so this is not moot. But
  **the fork's catalog pins `compose-material3-mpp = 1.10.0-alpha05` and
  `compose-material-icons-mpp = 1.7.3`, chosen for CMP 1.11.0-beta03.** Re-applying them on CMP
  1.12.2 would mismatch or downgrade Material3. Redo from scratch later if wanted; it is
  warning-only.

### F21 Docs — KEEP-CLEAN
- Root `CLAUDE.md` (two pointer sections), `UX_GAP_WORKLOG.md`, `docs/SURPLUS_MCP_UPSTREAM_SYNC.md`,
  `docs/BUILD_WARNING_AUDIT.md`, `core/data/CLAUDE.md` (draft-state + warm-up + schema sections:
  rewrite for F1/F3/F7), `feature/agents/CLAUDE.md` (F13/F14: drop),
  `feature/files/CLAUDE.md` (F11: keep).

---

### 4. Non-feature delta and whether upstream made it moot

| Item | Where | Moot upstream? |
|---|---|---|
| `MenuAnchorType` → `ExposedDropdownMenuAnchorType` | agents components, skills `SkillAclSharingSection`, `SkillEditorScreen` | **Moot**: upstream uses `ExposedDropdownMenuAnchorType` (10 files, 0 old imports) |
| `Icons.Default.DriveFileMove/OpenInNew` → AutoMirrored | conversations `ConversationActionsMenu.kt`, `ProjectComponents.kt` | Not moot (`ConversationActionsMenu.kt` ~168 still old); warning-only, drop |
| `ScrollableTabRow` → `PrimaryScrollableTabRow` | files `FilesScreen.kt` | FilesScreen rewritten upstream; drop |
| `rememberTransformableState` 4-arg lambda | files `PdfPreview.android.kt` | Upstream still 3-arg on CMP 1.12.2; version-dependent, drop |
| kotlinx-datetime `monthNumber/dayOfMonth` → `month.number/day` | conversations `ConversationExporter.kt` | Not moot (upstream ~92–93 old); trivial, optional |
| Redundancy removal (`e.message ?: …`, `!!`) | `ConfigRepositoryImpl.kt`, `AgentEditorForm.kt`, `AgentEditorMappers.kt` | Not checked per line; drop |
| `@OptIn` / `@file:OptIn` in tests; import re-ordering | core/data tests, module verification tests, `GateProbeDeviceTest.kt` | Drop; upstream tests win |
| Module verification tests listing fork types (`MarketApi`, `SavedStateHandle`) | `DataModuleVerificationTest`, `AgentsModuleVerificationTest`, `Conversations/FilesModuleVerificationTest` | Keep only the `MarketApi` line with F10 |
| Detekt allowlist token `LocalChatParagraphSpacing` | `config/detekt/detekt.yml` | Keep with F6 (1 token on upstream line ~225) |
| `.gitignore` `gradle/gradle-daemon-jvm.properties` | root | Not in upstream; keep (1 line) |
| Fork v8/v9/v10 schema JSON, `ForkMigration9To10Test` | core/data | Drop (F3) |
| `ConversationRepositoryImplTest` edits, `DataStoreRoundTripTest` / `ChatPayloadBuilderTest` / `MessageMapperTest` deletions | core/data tests | Take upstream's |
