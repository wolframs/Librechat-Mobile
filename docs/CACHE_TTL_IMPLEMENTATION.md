# Cache TTL implementation handoff

Implementation is in `fork/.../cachettl/` and `fork/.../market/`, with English and German resources. The only core implementation is `core/data/.../fork/cachettl/CacheTtlMarkerStore.kt`, since core cannot depend on `:fork`. Wire fields and call sites in upstream files are marked hooks. The conversation-level `promptCacheTtl` control and Room schema are unchanged.

## Design decisions

- Messages paint from Room first and refresh from the network. The sidecar captures valid assistant markers before message upserts on ordinary fetch, authoritative refresh and stream-final cache writes. It stores at most 2,000 markers per account under `acct:<accountId>:cache_ttl_markers`; the existing account purge sweeps that key. Message IDs retain all cached branches, and the rail chooses the last eligible reply on the displayed path. The first stored timestamp is retained; missing, malformed or future `createdAt` falls back to first receipt. Older markers outside the bound need a successful refresh to recover. No Room entity, migration, schema or database version changes.
- The common chat screen overlays the rail at the leading thread gutter below `topContentPadding`, which uses the measured header height and status-bar floor. This reaches phone/tablet and Material/Glass, including the native iOS header. It collects the hot message path locally instead of reading the neutralized chrome path. The countdown ticks only while composed and lifecycle STARTED. Info expands the explanation and marketplace caveat.
- The chat ViewModel owns its own arm, including the unsaved landing. Queue submission and queue drain leave it staged. Only a direct normal composer send consumes it, at the startChat call, through the existing top-level model-parameter payload overlay. Regenerate, continue and edit use their existing paths. Eligibility is checked again before consuming; a route that cannot arm clears stale state. The arm is in memory, not a restart-persisted preference.

## Manual acceptance (not performed by the implementation agent)

1. Open a new chat on built-in Anthropic with prompt caching on. Confirm “No cache yet”; tap the main chip through “Next send: 1h”, “Next send: 5m”, then off. Tap Info to read the explanation.
2. Arm 1h and send normally. Confirm the arm clears immediately. When the marked assistant reply arrives, confirm a countdown near 60m (or the server's actual clamped TTL). Send unarmed with a saved conversation-level TTL and confirm the rail follows the reply marker rather than the arm.
3. Regenerate, continue or edit while armed. Confirm the arm remains staged and those submissions carry no one-shot TTL. While a reply is running, queue another message; confirm both enqueue and drain leave the arm for the next direct send.
4. Switch to a custom Anthropic-format row with extendedCacheTTL false/missing. Confirm the rail is a readout, Info says the route sends five minutes, and an old arm is cleared. Switch to a row with extendedCacheTTL true and confirm cycling is enabled.
5. On an OpenRouter-configured row, select a Claude model (including uppercase CLAUDE) and then a non-Claude model. Confirm the rail appears only for Claude. Turn promptCache off and confirm it disappears. Repeat with saved agents whose providers/models and promptCache flags differ from the composer's raw selection.
6. Create two assistant branches with different markers; switch siblings and confirm each countdown follows only its displayed path. An error reply or an unmarked reply must not start a new countdown. Restart the app and reopen the branch to check persisted markers, including offline cache-first display. Switch accounts and confirm no marker crosses accounts.
7. On a marketplace endpoint, open Info and confirm the seller-per-request caveat. On a server without the sidecar route, confirm ordinary chat works with no caveat.
8. Check 10:00 versus rounded-up minutes above ten minutes, expired state, and one-second ticking. Check both UI styles, phone/tablet widths, keyboard open/closed, and the native iOS header for overlap and touch access.

## Verification

- Final standalone `ANDROID_HOME=/home/wolfram/Android/Sdk ./gradlew :app:assembleFossDebug`: passed in 38s. APK: `app/build/outputs/apk/foss/debug/app-foss-debug.apk`.

- `ANDROID_HOME=/home/wolfram/Android/Sdk ./gradlew :fork:testDebugUnitTest :core:data:testDebugUnitTest --tests 'com.garfiec.librechat.fork.cachettl.CacheTtlRulesTest' --tests 'com.garfiec.librechat.core.data.repository.MessageRepositoryRefreshProvenanceTest' :app:assembleFossDebug`: passed in 1m 46s. Five fork tests and six tests in the one upstream class passed; no whole-suite run.
- `:fork:detekt :core:data:detekt :core:model:detekt :feature:chat:detekt`: passed under the locally installed JDK 21 in 25s. The default Java 25 runtime fails inside detekt before analysis. The scaffold's empty expect/actual platform modules now carry a narrow EmptyModule suppression, and the extended send method has a marked LongMethod suppression for the added argument lines.
- `fork/scripts/fork-hooks.py --write`, then `fork/scripts/fork-hooks.py`: passed (39 total hooks, 33 for cache-ttl). `git diff --check`: passed.
- Requested `:fork:tasks --all` was attempted but cannot enumerate all tasks: upstream Compose Preview requires Android test resources and release unit tests that this build disables. The named detekt and test tasks above run successfully without changing those build policies.
- Untested: device rendering/gestures, iOS compilation/runtime, real server wire traffic, marketplace authentication/failure handling against a live server, and DataStore process-death round trips. No emulator, adb, device tool or LibreChat server request was used.

## Hook inventory

Exact output of `fork/scripts/fork-hooks.py --list`, filtered to this feature:

```text
cache-ttl	core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/di/DataModule.kt:564	cacheTtlStore = get(), // FORK-HOOK(cache-ttl): fork-owned marker store
cache-ttl	core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/MessageRepositoryImpl.kt:49	private val cacheTtlStore: // FORK-HOOK(cache-ttl): marker sidecar
cache-ttl	core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/MessageRepositoryImpl.kt:50	com.garfiec.librechat.core.data.fork.cachettl.CacheTtlMarkerStore? = null, // FORK-HOOK(cache-ttl): optional in tests
cache-ttl	core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/MessageRepositoryImpl.kt:66	cacheTtlStore?.remember(accountId, messages) // FORK-HOOK(cache-ttl): network refresh markers
cache-ttl	core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/MessageRepositoryImpl.kt:185	cacheTtlStore?.remember(accountId, messages) // FORK-HOOK(cache-ttl): stream final markers
cache-ttl	core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/MessageRepositoryImpl.kt:212	cacheTtlStore?.remember(accountId, messages) // FORK-HOOK(cache-ttl): authoritative refresh markers
cache-ttl	core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/EndpointConfig.kt:11	val provider: String? = null, // FORK-HOOK(cache-ttl): Anthropic wire format
cache-ttl	core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/EndpointConfig.kt:12	val extendedCacheTTL: Boolean? = null, // FORK-HOOK(cache-ttl): measured 1h support
cache-ttl	core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/Message.kt:63	val cacheTTL: String? = null, // FORK-HOOK(cache-ttl): decode the server-sent TTL
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:561	com.garfiec.librechat.fork.cachettl.CacheTtlRail( // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:562	viewModel.cacheTtl, uiState.selectedEndpoint, uiState.selectedModel, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:563	uiState.endpointConfigs, uiState.modelParameters.dynamicValues, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:564	uiState.selectedAgentProvider, uiState.selection.selectedAgentModel, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:565	uiState.selection.selectedAgentReasoning, viewModel.uiState, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:566	{ it.displayMessages.map { node -> node.message } }, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:567	Modifier.align(Alignment.TopStart).padding(top = topContentPadding, start = 8.dp), // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:568	) // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/ChatViewModel.kt:813	val cacheTtl = com.garfiec.librechat.fork.cachettl.CacheTtlSession() // FORK-HOOK(cache-ttl): conversation-local arm
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/ChatViewModel.kt:815	cacheTtl = cacheTtl, // FORK-HOOK(cache-ttl): share rail arm with direct sends
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/ModelSelectionDelegate.kt:1280	reasoning = reasoningParameters(agent.modelParameters) + // FORK-HOOK(cache-ttl): saved agent caching flag
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/ModelSelectionDelegate.kt:1281	com.garfiec.librechat.fork.cachettl.agentCacheParameters(agent.modelParameters), // FORK-HOOK(cache-ttl): agent flag
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:100	private val cacheTtl: com.garfiec.librechat.fork.cachettl.CacheTtlSession = // FORK-HOOK(cache-ttl): staged one-shot
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:101	com.garfiec.librechat.fork.cachettl.CacheTtlSession(), // FORK-HOOK(cache-ttl): default for isolated callers
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:449	@Suppress("LongMethod") // FORK-HOOK(cache-ttl): the marked request argument adds lines to the upstream method
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:561	modelParams = cacheTtl.payload( // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:562	spec.modelParamsPayload, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:563	com.garfiec.librechat.fork.cachettl.cacheTtlRoute( // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:564	spec.endpoint, spec.model, handle.state.endpointConfigs, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:565	handle.state.modelParameters.dynamicValues, handle.state.selectedAgentProvider, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:566	handle.state.selection.selectedAgentModel, handle.state.selection.selectedAgentReasoning, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:567	), // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:568	fromComposer, // FORK-HOOK(cache-ttl)
cache-ttl	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/SendDispatchDelegate.kt:569	), // FORK-HOOK(cache-ttl)
```
