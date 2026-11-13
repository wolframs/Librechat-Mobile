# Composer regrouping implementation

Round 3 on `feat/composer-regroup`, following round 2's 7/10 critique. Fork UI lives in
`feature/chat/.../chat/fork/`; shared chrome, cache and prices live in `:fork`.
English and German copy lives in `values*/strings_composer.xml` and `strings_cachettl.xml`.

## A. Composer

The model capsule is 36 dp, secondary-container fill in both themes, 12 dp horizontal insets,
14 sp medium model name, 14 sp regular quiet effort and a 16 dp chevron, in a 48 dp target.
Its name can ellipsize in the remaining action-row width. Plus shows an enabled tool/connector
count only above zero. No Tools summary or plain on-state chip remains. Only attention MCP
chips and a pending thinking override survive the chip filter; upstream workspace/context
slots stay available. The empty composer has its field and 48 dp action row, without an empty
settings row (target 104–112 dp; rendered height not measured).

Cache sits at the end of the leading action row, before mic/send. It appears only for a running
or armed window, as quiet 12 sp tabular text prefixed with Cache. No idle dot or fill.
Tap cycling, hold explanation, route gates, branch markers and send consumption are retained.
More options → Cache window arms the same per-view-model state, with Default (5 minutes),
1 hour for the next message and 5 minutes for the next message.

## B. Model sheet

An unstarred Current model gets its own checked row before Starred (or Most used). Up to six
starred rows remain, including a starred current model even beyond the initial six. Rows show
the short display name at 16 sp medium and only the endpoint label at 13 sp below it.
The first level then has one All models/Effort card and one More options row. More options
contains gated Advanced settings, Model presets, marketplace Prices, Cache window and Compare.
No leading icons on model actions, choices or the All models list. The latter is a fork-owned
body (`ComposerAllModels.kt`) over upstream's shared `PickerInputs`/`pickerRows`, preserving
key gates, spec/agent selection, search, favorites and drag selection without upstream row edits.
Advanced settings retains the shared parameter controls and spec locks.

## C. Effort

The footer reads “Higher effort can improve difficult answers, but may take longer and use
more tokens.”, 14 sp / 20 sp, 24 dp side insets, 12 dp space above. Choices are regular.

## D. Add to chat

Attach tiles have bare 24 dp icons, 14 sp medium labels and 8 dp tile gaps. Tool names alone
use medium weight; descriptions use 13 sp quiet text and wrap. Copy is shortened as specified.
Connectors retains its N on second line; prompt, upload, search-key and artifact-mode actions remain.

## E. Connectors

One explanation precedes the list. Neutral 32 dp monograms replace saturated initials. Names
are regular 16 sp, rows have a 56 dp minimum and grow for wrapped text. Enabled idle rows
have no status; connected, sign-in, setup/failure and spec-lock exceptions retain their text/actions.

## F. Conversation menu

Both Search label hooks are removed, restoring upstream Search. Both Media hooks remain.

## G. Prices

Shared title/close chrome; Prices, Sellers and Performance labels. Four fixed-weight columns
have 8 dp gaps and right-aligned tabular numbers. Text wraps within its weighted cell rather
than painting into its neighbour. Performance is a two-column label/value list.

## Type, verification and limits

Fork-owned rows use 16 sp titles, 13 sp quiet supporting text and 13 sp medium section labels.
Rows use minimum heights rather than fixed heights, allowing wrapped text at 1.3 font scale;
the shared header also grows. Model/attach capsules retain their requested fixed visual sizes.

The thirteen supplied round-2 screenshots and five Claude reference screenshots were inspected
as a contact sheet. No emulator, adb, device, backend requests or subagents were used. No push
or PR. Font-scale 1.3, keyboard-open, Material/Liquid Glass and light/dark rendered acceptance
cannot be claimed without an allowed renderer/device; they remain operator checks. iOS not built.

## Hook delta from round 2

Only these four upstream files change. All other hook lines are unchanged. The registry is
regenerated with `fork/scripts/fork-hooks.py --write` and checked afterward.

### ChatOverflowMenu.kt

```diff
diff --git a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowMenu.kt b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowMenu.kt
index f3a3c059..37a78d52 100644
--- a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowMenu.kt
+++ b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowMenu.kt
@@ -119,8 +119,7 @@ internal fun ChatOverflowMenu(
                     ChatOverflowItem.OPEN_APPS ->
                         OverflowItem(stringResource(Res.string.mcp_app_open_apps, openApps), Icons.Outlined.Apps, select, dragTarget)
                     ChatOverflowItem.SEARCH ->
-                        // FORK-HOOK(composer): conversation search label
-                        OverflowItem(com.garfiec.librechat.feature.chat.fork.conversationSearchLabel(),
+                        OverflowItem(stringResource(Res.string.action_search),
                             Icons.Default.Search, select, dragTarget)
                     ChatOverflowItem.SHOW_ALL_MEDIA ->
                         // FORK-HOOK(composer): media label
```

### ChatTopBarSpec.kt

```diff
diff --git a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatTopBarSpec.kt b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatTopBarSpec.kt
index ee55f55e..2a2d49f1 100644
--- a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatTopBarSpec.kt
+++ b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatTopBarSpec.kt
@@ -196,8 +196,7 @@ private fun overflowMenuItem(item: ChatOverflowItem, uiState: ChatUiState, openA

     return when (item) {
         ChatOverflowItem.OPEN_APPS -> entry(stringResource(Res.string.mcp_app_open_apps, openApps), BarIcons.Apps)
-        // FORK-HOOK(composer): conversation search label
-        ChatOverflowItem.SEARCH -> entry(com.garfiec.librechat.feature.chat.fork.conversationSearchLabel(), BarIcons.Search)
+        ChatOverflowItem.SEARCH -> entry(stringResource(Res.string.action_search), BarIcons.Search)
         // FORK-HOOK(composer): media label
         ChatOverflowItem.SHOW_ALL_MEDIA -> entry(com.garfiec.librechat.feature.chat.fork.conversationMediaLabel(), BarIcons.Media)
         ChatOverflowItem.LOAD_PRESET -> entry(stringResource(Res.string.load_preset), BarIcons.LoadPreset)
```

### ComposerSettingsLine.kt

```diff
diff --git a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ComposerSettingsLine.kt b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ComposerSettingsLine.kt
index aa2a711e..434843f9 100644
--- a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ComposerSettingsLine.kt
+++ b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ComposerSettingsLine.kt
@@ -93,7 +93,8 @@ internal fun ComposerSettingsLine(
     ring: ContextRingState? = null,
     leading: (@Composable () -> Unit)? = null,
 ) {
-    if (chips.isEmpty && ring == null && leading == null) return
+    // FORK-HOOK(composer): attention-only row
+    if (com.garfiec.librechat.feature.chat.fork.visibleComposerChips(chips).isEmpty() && ring == null && leading == null) return
     Row(
         modifier = modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically,
@@ -109,7 +110,6 @@ internal fun ComposerSettingsLine(
             horizontalArrangement = Arrangement.spacedBy(8.dp),
         ) {
             leading?.invoke()
-            com.garfiec.librechat.feature.chat.fork.ComposerToolsSummary(chips) // FORK-HOOK(composer): folded tool summary
             // FORK-HOOK(composer): keep attention chips separate
             com.garfiec.librechat.feature.chat.fork.visibleComposerChips(chips).forEach { chip ->
                 // Keyed by what the chip is, not where it sits: a chip leaving the line ahead of an MCP
```

### ToolsButtonDots.kt

```diff
diff --git a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ToolsButtonDots.kt b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ToolsButtonDots.kt
index b742dd11..76370e13 100644
--- a/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ToolsButtonDots.kt
+++ b/feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ToolsButtonDots.kt
@@ -36,17 +36,9 @@ internal fun BoxScope.ToolsButtonDots(
                 .background(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape),
         )
     }
-    // FORK-HOOK(composer): no blue plus dot
-    if (com.garfiec.librechat.feature.chat.fork.ShowToolsDot &&
-        showsActiveToolsDot(showEphemeralTools, enabledTools, selectedMcpServerNames)) {
-        Box(
-            modifier = Modifier
-                .size(ToolsDotSize)
-                .align(Alignment.TopEnd)
-                .offset(x = (-2).dp, y = 2.dp)
-                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
-        )
-    }
+    // FORK-HOOK(composer): plus count badge
+    com.garfiec.librechat.feature.chat.fork.ComposerToolsBadge(
+        this, showEphemeralTools, enabledTools, selectedMcpServerNames)
 }

 private val ToolsDotSize = 10.dp
```

## Checks run

- `./gradlew :app:assembleFossDebug detekt :fork:testDebugUnitTest :feature:chat:testDebugUnitTest --tests '*ComposerModelSheetTest' --tests '*ComposerSettingsTest'`: passed on the final code, 41 seconds.
- Fork: 15 tests passed; targeted chat: 7 tests passed. Coverage includes badge count, attention-only chips, six starred plus unstarred current, and explicit cache arm/default writes with send consumption.
- `fork/scripts/fork-hooks.py --write`, then `fork/scripts/fork-hooks.py`: passed, 82 hook lines in 30 files.
- `git diff --check`: passed. No individual test/check run exceeded ten minutes.
