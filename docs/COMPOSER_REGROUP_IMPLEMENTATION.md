# Composer regrouping implementation

Fork surfaces: `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/fork/`.
English and German strings: `fork/src/commonMain/composeResources/values*/strings_composer.xml`.

The composer has a model pill after plus, an endpoint icon, trimmed model name,
and a quieter explicit effort value. Its field says Message. Both header
renderers omit the former model chip. The shared composer stays expanded while
the pill is shown. The model sheet lists at most six available starred models,
including starred specs and agents, in existing picker order. Without stars it
uses account-scoped send-count ranking; the hint explains how to pin a model.
All selection gates come from the existing picker helpers.

Effort is included: the active parameter target and EndpointParameterRegistry
supply options, including model/version/drop-parameter filtering. Existing
getValueForKey / withUpdatedKey / updateModelParameters handle reads and writes.
Enforced specs remain read-only. Effort is omitted during comparison because
upstream parameters edit only the primary lane; Parameters keeps that upstream
behavior. Full model selection and comparison setup are unchanged upstream UI.
Presets and prompts reuse the same screen-owned callbacks as the top bar.

The plus sheet preserves attachment gates, tool locks, search routing/key row,
artifact long-press, MCP status/auth/setup/locks, focus highlighting, context
placement and code-environment upload. It has no model or parameter controls,
and no separate memory switch. The three-dot menu filters the four relocated
actions, preserving conversation actions and all other gates.

## Platform and visual limits

The chip hooks the common ComposerRows used by Android ChatInput and iOS
IosChatInput; no actual signature changed and no iOS-only implementation was
invented. Both header renderers are hooked. iOS was not compiled here. No device,
emulator, adb, browser, or backend requests were used. Light/dark and Material /
Liquid Glass rendering, keyboard layouts, picker transitions, MCP auth/setup,
attachment pickers and actual server parameter application remain manual checks.
Theme tokens and Adaptive components supply both styles.

## Hand checks

1. Open a new or existing chat with model selection enabled: the model pill sits
   after plus; the field says Message; the former header model chip is absent.
2. Tap the pill: Model opens. Star models in More models, reopen Model, verify
   six maximum, endpoint subtitles, current check, and pick-to-dismiss. With no
   stars, previously used available models appear in send-count order.
3. On a registry-supported reasoning model, open Effort, choose a level, and
   verify the pill value. Back returns to Model; enforced specs cannot be edited.
4. Model → Parameters opens the existing parameters page. Model → Presets →
   Load / Save opens the existing actions, only when server gates permit them.
5. Model → More models opens the existing searchable grouped/starred selector.
   Model → Compare with a second model runs the existing comparison action.
6. Tap plus (or pull up): Add to chat shows gated attachment tiles, tool rows,
   Connectors and prompts. Expand Connectors to toggle/sign in/set up servers.
   Hold Artifacts for modes; tap an enabled composer setting chip to check focus.
   Check context gauge placement and code-environment upload where enabled.
7. Open three dots: Load/Save preset, Prompts library and Compare are absent.
   Check conversation actions still work. Repeat in both themes/styles.

## Checks

- `:app:assembleFossDebug`: passed, final combined run 52 seconds.
- `:fork:detektAndroidDebug`, `:fork:detektMetadataCommonMain`: passed.
- `:feature:chat:detektMetadataCommonMain` and normal `:feature:chat:detekt`: passed.
- `:feature:chat:detektAndroidDebug`: fails with 21 type-resolved findings in
  untouched upstream Android files (redundant suspend, dispatcher injection and
  unreachable code). `build-logic/convention/src/main/kotlin/DetektConventionPlugin.kt`
  documents why type-resolved tasks are outside the normal gate. No baselines or
  suppressions were added for these findings and no unrelated upstream code changed.
- `:fork:testDebugUnitTest`: 15 tests passed.
- Targeted `:feature:chat:testDebugUnitTest`: ChatOverflowSectionsTest (13),
  ComposerModelSheetTest (3), ModelPickerShownTest (2), all passed (final run 52 seconds).
- Hook write/check and `git diff --check`: passed.
- Initial implementation compile/lint failures were corrected; no test run
  reached the ten-minute budget. No push or PR.

## Every composer hook

Output of `fork/scripts/fork-hooks.py --list | grep composer`:

```text
composer	feature/chat/src/androidMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatToolsSheetPreviews.kt:115	com.garfiec.librechat.feature.chat.fork.ComposerToolsSheetContent( // FORK-HOOK(composer): attachment and tools surface
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatFloatingTopBar.kt:411	// FORK-HOOK(composer): model lives in composer
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatOptionsBottomSheet.kt:282	// FORK-HOOK(composer): attachment and tools surface
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowItem.kt:105	// FORK-HOOK(composer): conversation actions only
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatTopBarSpec.kt:1	@file:Suppress("NoUnusedImports") // FORK-HOOK(composer): keep upstream imports at the swapped surface
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/ChatTopBarSpec.kt:122	text = com.garfiec.librechat.feature.chat.fork.HeaderModelText, // FORK-HOOK(composer): model lives in composer
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/CommonChatInputCore.kt:1	@file:Suppress("NoUnusedImports") // FORK-HOOK(composer): keep upstream placeholder resource import
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/CommonChatInputCore.kt:327	// FORK-HOOK(composer): keep the model chip below the field
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/CommonChatInputCore.kt:564	// FORK-HOOK(composer): shared Android and iOS model chip
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/CommonChatInputCore.kt:1143	com.garfiec.librechat.feature.chat.fork.messagePlaceholder() // FORK-HOOK(composer): model lives in the chip
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatPullUpSheet.kt:1	@file:Suppress("NoUnusedImports") // FORK-HOOK(composer): keep upstream imports at the swapped surface
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatPullUpSheet.kt:496	com.garfiec.librechat.feature.chat.fork.ComposerToolsSheetContent( // FORK-HOOK(composer): attachment and tools surface
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:441	// FORK-HOOK(composer): model and prompts actions
composer	feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/screen/ChatScreen.kt:1063	} // FORK-HOOK(composer): end composer scope
composer	feature/chat/src/commonTest/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowSectionsTest.kt:68	// FORK-HOOK(composer): presets, prompts and compare moved to the composer sheets.
composer	feature/chat/src/commonTest/kotlin/com/garfiec/librechat/feature/chat/components/ChatOverflowSectionsTest.kt:88	// FORK-HOOK(composer): the landing menu follows the same regrouping.
```
