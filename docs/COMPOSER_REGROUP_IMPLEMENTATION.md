# Composer regrouping implementation

Round 2 builds on round 1 (`ab218233`) on `feat/composer-regroup`.
Fork surfaces live in `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/fork/`;
shared sheet chrome lives in `fork/.../composer/SheetChrome.kt`. All added/changed copy is in
English and German `fork/src/commonMain/composeResources/values*/strings_composer.xml`.

## Chrome

Model, Effort, Model presets, All models, Advanced settings, Add to chat, Connectors and Prices
use the shared 56 dp header, centred title, 24 dp close/back icon in a 48 dp target and 16 dp
side margins. The shared handle is 32 x 4 dp, neutral grey in both themes. Groups use 12 dp
spacing, hairline dividers, 56 dp single-line / 64 dp two-line rows and 24 dp leading icons.
Titles and supporting copy use separate weight/size/tone. System back and header back from a
model sub-page return to Model; Connectors returns to Add to chat. The full selector and
parameter controls are upstream bodies embedded in the model sheet, so no second modal
closes the parent. Provider-key navigation retains its existing screen callback.

## A. Composer

The action row contains plus, a quiet filled 34 dp model pill in a 48 dp target, cache, mic and
send. The pill has no provider avatar, shares short-model naming with the model list, and
shows explicit effort quietly after the name. Spec labels win; vendor prefixes are dropped;
Claude family/version ids become readable names. No other names are invented.

The settings line sits between the field and action row, only when populated. Two or more
plain on-state tool/server chips fold into Tools · N, opening Add to chat. Locked, pinned-off,
sign-in, setup, failed, connecting and thinking chips retain their existing individual behavior.
Workspace and context controls retain their upstream slots. The blue plus dot is disabled;
upstream's separate compact-suggestion indicator remains.

The existing cache hook now provides a composer slot: the gutter overlay and dollar button
are gone. Cache route visibility, one-shot arm cycling, marker countdown and hold explanation
are preserved. Idle/expired cache shows only its dot, with cache named for accessibility.
Marketplace discovery still supplies the explanation caveat, and saved agents resolve their
provider/model. The stream path is still collected inside the cache composable.

## B. Model

The list is labelled Favourites or Most used, capped at six and always includes the current
model with a check. An unstarred current model leads; a starred current model beyond the
first six displaces the last row. Rows show the short name, exact id and endpoint label.
All models follows the list. Settings contains registry-gated Effort with a trailing value,
Advanced settings, Model presets and marketplace Prices, followed by Compare models.
Gauge, sliders, bookmark-plus, dollar-sign and columns-2 distinguish the actions.
Existing selection, key, comparison, parameter, preset and spec-lock gates remain in use.

## C. Effort

Header back replaces the old Back row. Model default has its server-setting explanation;
the remaining registry-supported nonempty options follow it once each. A quiet line explains
the quality/time/cost trade-off. Existing parameter reads/writes and enforced-spec locks apply.

## D. Add to chat

Attach tiles retain their gates, with Server files and folder-open; the first section follows
a 24 dp gap. Built-in tools have descriptions and switches, without repeated Off labels.
Search routing/key actions, artifacts hold/mode, code upload and context placement are retained.
Connectors shows N on / None on and opens a separate page. Its intro explains permission to
use a server. Idle selected servers say Connects when you send; connected servers say
Connected; off servers have no status unless attention is required. Only failed/sign-in states
have dots. Server marks are 32 dp; locks, auth/cancel/setup and focused-row highlighting remain.
Prompt library retains the existing navigation callback.

## E. Conversation menu

Both menu renderers say Search conversation and Media through fork label helpers. Other
conversation actions and the round-1 relocated-action filtering are unchanged.

## Verification and limits

- `./gradlew :app:assembleFossDebug`: passed.
- `./gradlew detekt` (aggregate CI gate): passed; no baseline changes.
- `:fork:testDebugUnitTest`: passed (15 tests).
- Targeted `:feature:chat:testDebugUnitTest --tests '*ComposerModelSheetTest' --tests '*ComposerSettingsTest'`:
  passed (6 tests), including naming, current-model inclusion and fold eligibility/attention controls.
- `fork/scripts/fork-hooks.py --write`, then `fork/scripts/fork-hooks.py`: passed.
- Final combined build, aggregate detekt and unit-test run: passed in 53 seconds.
- `git diff --check`: passed. No test run exceeded ten minutes.

The supplied round-1 and reference screenshots were inspected. No emulator, adb, device,
browser, app runtime or live backend requests were used for verification. iOS was not built.
Light/dark screenshots, Material/Liquid Glass rendering, small-screen touch/layout acceptance,
platform back behavior and live auth/parameter application await the operator's installation.
No push or PR.

## Hook changes from round 1

The following is the complete upstream-file delta. Only fork hooks change; original imports
remain. Round 1's three import suppressions remain; two new suppression hooks retain imports
made unused by shared handles/menu labels. Cache keeps one call, moved from the thread overlay
to the composer slot, dropping its overlay modifier. No platform composer signatures change.

### ChatOptionsBottomSheet.kt

```diff
+@file:Suppress("NoUnusedImports") // FORK-HOOK(composer): retain upstream surface imports
+
-        dragHandle = { LowProfileDragHandle() },
+        dragHandle = { com.garfiec.librechat.fork.composer.ComposerDragHandle() } // FORK-HOOK(composer): shared sheet handle,
```

### ChatOverflowMenu.kt

```diff
+@file:Suppress("NoUnusedImports") // FORK-HOOK(composer): retain upstream surface imports
+
-                        OverflowItem(stringResource(Res.string.action_search), Icons.Default.Search, select, dragTarget)
+                        // FORK-HOOK(composer): conversation search label
+                        OverflowItem(com.garfiec.librechat.feature.chat.fork.conversationSearchLabel(),
+                            Icons.Default.Search, select, dragTarget)
-                        OverflowItem(stringResource(Res.string.action_show_all_media), Icons.Outlined.PhotoLibrary, select, dragTarget)
+                        // FORK-HOOK(composer): media label
+                        OverflowItem(com.garfiec.librechat.feature.chat.fork.conversationMediaLabel(),
+                            Icons.Outlined.PhotoLibrary, select, dragTarget)
```

### ChatTopBarSpec.kt

```diff
-        ChatOverflowItem.SEARCH -> entry(stringResource(Res.string.action_search), BarIcons.Search)
-        ChatOverflowItem.SHOW_ALL_MEDIA -> entry(stringResource(Res.string.action_show_all_media), BarIcons.Media)
+        // FORK-HOOK(composer): conversation search label
+        ChatOverflowItem.SEARCH -> entry(com.garfiec.librechat.feature.chat.fork.conversationSearchLabel(), BarIcons.Search)
+        // FORK-HOOK(composer): media label
+        ChatOverflowItem.SHOW_ALL_MEDIA -> entry(com.garfiec.librechat.feature.chat.fork.conversationMediaLabel(), BarIcons.Media)
```

### CommonChatInputCore.kt

```diff
-    FoldingComposerRows(
+    com.garfiec.librechat.feature.chat.fork.ComposerRowsLayout( // FORK-HOOK(composer): settings above actions
```

### ComposerSettingsLine.kt

```diff
-            chips.chips.forEach { chip ->
+            com.garfiec.librechat.feature.chat.fork.ComposerToolsSummary(chips) // FORK-HOOK(composer): folded tool summary
+            // FORK-HOOK(composer): keep attention chips separate
+            com.garfiec.librechat.feature.chat.fork.visibleComposerChips(chips).forEach { chip ->
```

### ToolsButtonDots.kt

```diff
-    if (showsActiveToolsDot(showEphemeralTools, enabledTools, selectedMcpServerNames)) {
+    // FORK-HOOK(composer): no blue plus dot
+    if (com.garfiec.librechat.feature.chat.fork.ShowToolsDot &&
+        showsActiveToolsDot(showEphemeralTools, enabledTools, selectedMcpServerNames)) {
```

### ChatPullUpSheet.kt

```diff
-            LowProfileDragHandle()
+            com.garfiec.librechat.fork.composer.ComposerDragHandle() // FORK-HOOK(composer): shared sheet handle
```

### ChatScreen.kt

```diff
-        { showSavePresetDialog = true }, onNavigateToPromptsLibrary) {
+        { showSavePresetDialog = true }, onNavigateToPromptsLibrary,
+        cache = { // FORK-HOOK(cache-ttl): composer slot
+                    com.garfiec.librechat.fork.market.ChatGutter( // FORK-HOOK(cache-ttl): composer cache chip
+                        viewModel.cacheTtl, uiState.selectedEndpoint, uiState.selectedModel, uiState.serverUrl, // FORK-HOOK(cache-ttl)
+                        uiState.endpointConfigs, uiState.modelParameters.dynamicValues, // FORK-HOOK(cache-ttl)
+                        uiState.selectedAgentProvider, uiState.selection.selectedAgentModel, // FORK-HOOK(cache-ttl)
+                        uiState.selection.selectedAgentReasoning, viewModel.uiState, // FORK-HOOK(cache-ttl)
+                        { it.displayMessages.map { node -> node.message } }, // FORK-HOOK(cache-ttl)
+                    ) // FORK-HOOK(cache-ttl)
+        }, providerKeys = { onNavigateToProviderKeys(it) }) { // FORK-HOOK(composer): model sub-pages
-                    com.garfiec.librechat.fork.market.ChatGutter( // FORK-HOOK(cache-ttl): cache and prices host
-                        viewModel.cacheTtl, uiState.selectedEndpoint, uiState.selectedModel, uiState.serverUrl, // FORK-HOOK(cache-ttl)
-                        uiState.endpointConfigs, uiState.modelParameters.dynamicValues, // FORK-HOOK(cache-ttl)
-                        uiState.selectedAgentProvider, uiState.selection.selectedAgentModel, // FORK-HOOK(cache-ttl)
-                        uiState.selection.selectedAgentReasoning, viewModel.uiState, // FORK-HOOK(cache-ttl)
-                        { it.displayMessages.map { node -> node.message } }, // FORK-HOOK(cache-ttl)
-                        Modifier.align(Alignment.TopStart).padding(top = topContentPadding, start = 8.dp), // FORK-HOOK(cache-ttl)
-                    ) // FORK-HOOK(cache-ttl)
-
```
