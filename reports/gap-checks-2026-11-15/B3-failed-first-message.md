# B3: Failed first message missing from the drawer

Checked 2026-11-16 by gpt-6.1-sol (high).

## Question

Does the 13 November failure still leave a server-held chat out of the drawer and titled New Chat?
Do upstream #872, #887 or neighbouring changes fix it?

## What I looked at

- Read BRIEF.md, root CLAUDE.md, fork/README.md, the ledger's current re-landing section, APP_CHECK_2026-11-13.md and catch-up steps 7/7a. Read the chat, conversations and core/data module guidance for these paths.
- Committed code via `git show HEAD:<path>`; initial develop HEAD was `71f3d2de18f349da38e09f16f1791f20d7ce45cc`. Another agent was editing/building. Dirty files were left alone.
- Paths below are under `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/` unless qualified.
- `viewmodel/delegate/SendCompletionDelegate.kt:65-134,185-199,241-264,339-379`; `viewmodel/TitleGenerationGate.kt:8-29`.
- `viewmodel/delegate/StreamingManagerDelegate.kt:423-486,534-562,806-853`; `viewmodel/ChatViewModel.kt:799-802`.
- `viewmodel/delegate/ConversationLoadDelegate.kt:142-175,218-239,462-530`; `viewmodel/delegate/LiveReplyDelegate.kt:282-286`.
- `core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/ConversationRepositoryImpl.kt:82-101,167-195,216-232`; same directory's `ChatRepositoryImpl.kt:147-205`.
- `feature/conversations/src/commonMain/kotlin/com/garfiec/librechat/feature/conversations/drawer/ConversationListStateHolder.kt:112-135,177-198,293-333`; `DrawerViewModel.kt:542`.
- `git log --oneline 08bca78f..upstream/develop -- feature/chat core/data feature/conversations`; compared the eager fetch with `git show 08bca78f:…/SendCompletionDelegate.kt`.
- Read upstream PRs [#872](https://github.com/garfiec/Librechat-Mobile/pull/872) and [#887](https://github.com/garfiec/Librechat-Mobile/pull/887) with `gh pr view`. Both merge commits are ancestors of HEAD.
- Searched all-state upstream issues for `failed conversation title`, `drawer first` and `first failed`. Found related closed #870, #886 and #882; no exact B3 match in those results. This is not an exhaustive issue audit.
- Opened local/emulator-5554 through device_open. Relaunched only `dev.wsiener.mercuric.debug` using its explicit MainActivity. Opened model capsule → All models → scrolled to Set up providers. Inspected snapshot and device_screenshot. Closed both sheet pages with Back, left the empty new chat, and device_close without shutdown.

## What I found

1. No safe selectable no-key row was available. OpenAI, Google, Assistants and xAI (Grok) each showed Needs your key and Add key, without an expand control. `fork/ComposerAllModels.kt:309-330,356-360` explicitly removes row click handling in this state; only Add key is interactive. I did not open it. OpenRouter now appeared among usable groups with 468 models; the old expired-key observation is not evidence that a send there remains free today.
2. Therefore no failing message was sent. Zero billed chat turns were attempted. No key or account setting was read or changed. The requested post-failure observations are: **at once: not tested; after pull to refresh: not tested; after relaunch: not tested**. The launch above preceded any reproduction and is not a post-failure test.
3. Creation already tries to make failed chats visible: on Created, SendCompletionDelegate launches a non-cancellable getConversation for non-temporary chats. A missing cache row triggers a server GET and Room upsert; the drawer observes Room. This exact eager-fetch behaviour already existed at `08bca78f:SendCompletionDelegate.kt:125-130`, so it is not a new upstream fix.
4. The code documents a real timing hazard, but I did not reproduce it: ConversationLoadDelegate:526-530 says Created can precede the server's unawaited save, so its initial GET can 404. The eager fetch has no retry there. **Inference:** if both creation-time reads miss, a later error can leave no conversation row cached despite saved messages.
5. On an ordinary stream error with a known conversation id, StreamingManagerDelegate:846-852 reloads the conversation. Its host implementation calls loadConversation, whose revalidation fetches **messages** (ConversationLoadDelegate:142-175,218-239). It does not rerun loadConversationModel or unconditionally refresh the conversation record/title. Thus that error reload alone does not prove the missing drawer row is repaired.
6. Titles have separate paths. TitleUpdate changes the live title in memory. A non-aborted Final saves the conversation and runs the new-chat/handoff title gate; generateTitle updates Room, with a network-first conversation refresh if its long-poll fails. An Error does not enter that Final path. **Inference:** a first-turn error can retain the placeholder until a later record refresh; a placeholder on the server itself cannot be repaired merely by refreshing the drawer. I did not inspect this server's title-generation behaviour.
7. [#872](https://github.com/garfiec/Librechat-Mobile/pull/872) (`cb2d06d5`, closes #870) reloads page 1 when an unfiltered, unsearched drawer opens with a list at least five minutes old, or with no successful first load. A fresh list is deliberately skipped. Pull to refresh and launch also load page 1. Repository page loads upsert server rows, including titles. **Inference:** if the failed chat is returned on page 1, these reads can recover it; #872 does not guarantee immediate recovery after a local failure.
8. [#887](https://github.com/garfiec/Librechat-Mobile/pull/887) (`b0fb4da3`, closes #886) retries dropped/timed-out starts with the same encoded body/clientRequestId, retries 503 SERVER_NOT_READY, and resumes deduplicated starts. Other API failures still escape. It prevents some uncertain-start failures/duplicates; it does not add a key-error conversation/title refresh.
9. Neighbours are distinct: #883 (`a5a0b7b1`, closes #882) keeps edited/retried/regenerated turns visible over pinned branches; #881 (`d6080bcb`) syncs pinned chats, including on pull to refresh. Neither supplies failed-first-turn conversation/title recovery. The only post-check change in SendCompletionDelegate/TitleGenerationGate history was #835's recomposition work, not a targeted B3 repair.

## Verdict

**cannot tell.** The original symptom was not safely reproduced. Existing code intends to cache chats before a later failure, but already did so on 13 November. Upstream added useful recovery/retry behaviour, not proof that this failed-first-turn case is fixed. The creation-save race and missing error-path record refresh remain code-supported risks, not confirmed device failures.

## Recommended action

Do not mark B3 fixed or change code from this check alone. **M, a few hours:** add a controlled upstream regression covering Created → initial conversation GET 404 → persisted first-turn error, then assert the drawer row and authoritative title recover. Reproduce against a disposable rig rather than altering this account's keys. If it fails, report upstream and refresh the conversation record on the settled error path. Any temporary fork repair should live in a fork-only helper under the chat module's `fork` package, with a marked error-path hook; preserve temporary-chat/account guards and existing Room schema (fork/README.md rules 2, 3 and 8).

## Only the operator can answer

Nothing is needed for this bounded report. A later live test needs a known free failure fixture; no credential change is requested.

## Not checked

No server SSH/DB lookup, backend source inspection, raw server HTTP, Gradle, test run, build, install, reinstall, successful recovery turn, network-failure injection, iOS or physical phone. Installed APK provenance against the inspected HEAD was not independently established. No assertion that the original ten-message server chat is now present or titled correctly.
