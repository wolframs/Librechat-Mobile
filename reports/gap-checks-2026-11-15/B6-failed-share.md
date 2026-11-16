# B6: Failed shares expose internals; metadata denial can crash

Checked 2026-11-16 by gpt-6.1-sol.

## Question
What does a failed incoming file share show, why does it fail, and did upstream fix it after the 13 November app check?

## What I looked at
- Read BRIEF.md, root CLAUDE.md (This fork), fork/README.md, the ledger's re-landing section, APP_CHECK_2026-11-13.md, catch-up steps 7/7a, and the app/chat/files module guidance.
- Committed source: `git show HEAD:<path>`, HEAD `0beea5c114f8f239ce0c556e93dd7759734544b5`, branch `develop`. Concurrent edits belong to the other agent; none were changed.
- Paths below are relative to the repository. Line numbers refer to that HEAD.
- `app/src/main/kotlin/com/garfiec/librechat/MainActivity.kt:196-212,232-274`: ACTION_SEND, SEND_MULTIPLE and content ACTION_VIEW intake.
- `app/src/main/kotlin/com/garfiec/librechat/navigation/LibreChatNavHost.kt:48-66` and `feature/chat/src/androidMain/kotlin/com/garfiec/librechat/feature/chat/ShareIntentConsumer.kt:67-107`: addressed delivery to one composer.
- Under `feature/chat/src/`: `commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/ChatViewModel.kt:1340-1366`, `viewmodel/delegate/UploadIntakeDelegate.kt:122-155,275-282` in the same common package.
- `androidMain/kotlin/com/garfiec/librechat/feature/chat/util/FileUtils.kt:23-32`; `androidMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/FileAttachmentDelegate.kt:62-68,106-129,244-259,294-319`.
- In the common chat package: `viewmodel/ChatNotice.kt:164-165`, `components/StreamErrorMessages.kt:109-111`, `screen/ChatScreenEffects.kt:68-80`, `components/ComposerDraftStrip.kt:199-205,235-256`, `components/CommonChatInputCore.kt:524-535`.
- `feature/chat/src/commonMain/composeResources/values/strings.xml:823-825`: upload error copy.
- Compared `08bca78f..upstream/develop` for intake, metadata, upload, notice and composer paths; upstream tip `dc8d3dfe` (2026-11-16). Read the relevant diff and commit `852c7e5e`.
- `gh issue list --repo garfiec/Librechat-Mobile --state all` searches: share, share in:title, upload/error, permission/file, attachment/failed; read #716 and #855.
- Device: `device_open(hostId=local, deviceId=emulator-5554)`, bound agent-device CLI; debug package only. Read `help open`; its `--launch-args` supports Android `am start` arguments.
- Opened debug MainActivity explicitly. All launch, snapshot, tap and screenshot operations used agent-device/T3 device tools. adb was used only for filtered logcat.
- Two ACTION_SEND launches, MIME `application/pdf`, `--eu android.intent.extra.STREAM <URI>`, no URI permission grant. No picker, real file read, upload to the server, or model turn was needed.

## What I found
1. **B6 remains reproducible.** Sent `content://media/external/file/2147483647`, a nonexistent item. The tray showed title `2147483647`, red outline, “Didn't upload · Tap to retry”, and “1 file didn't upload. Tap it to retry, or remove it.” Screenshot inspection confirmed the tray; Send was disabled. Retrying failed again.
2. The full accessibility snapshot after retry contained the snackbar text: `Failed to upload 2147483647: java.io.FileNotFoundException: No item at content://media/external/file/2147483647`, plus Dismiss. This reproduces the content-address and implementation-detail leak. It does not reproduce the old package-name text verbatim.
3. The filename query returned no usable name; `resolveFileName` falls back to `uri.lastPathSegment` (FileUtils:23-32). `describe` accepts that as the name (FileAttachmentDelegate:62-68). “46” in the old check is therefore consistent with a provider row id, not the server-generated file UUID. This explanation of the historical title is inferred; today's numeric title is observed.
4. `openInputStream` then throws before the repository upload call (FileAttachmentDelegate:125 versus 244). The broad catch stores `e.message` through `ChatNotice.uploadFailed` (:306-309). The notice embeds the reason unchanged, and the localized template is `Failed to upload %1$s: %2$s`. ChatScreenEffects displays it as a long snackbar. Localization does not sanitize it.
5. **An earlier permission failure is worse.** Sent `content://com.android.providers.downloads.documents/document/46` without a grant. Logcat at 14:40:10 device time recorded share receipt, `applyShare`, then `FATAL EXCEPTION: main`, SecurityException: opening DownloadStorageProvider requires access through ACTION_OPEN_DOCUMENT or related APIs. Stack: `resolveFileName:25` → `describe:63` → `attachWithAutoRouting:279` → `intakePickedFiles:150`. The next snapshot showed a restored old chat, with no failed attachment or helpful share error.
6. The metadata query and `getType` run before the upload try/catch. UploadIntakeDelegate has `finally` but no catch at this boundary. Thus permission denial during metadata lookup can terminate the app, while denial/not-found during byte reading becomes the raw snackbar. This distinction is supported by the stack and source; not every failed URI takes the same path.
7. Today's failures were deliberately invalid test delivery: one URI had no access grant; the other named no item. The original app check also explicitly attributes its failure to test permissions. These observations do **not** show that a normal share from a cooperating sender fails. A content URI string alone does not give the recipient permission to read its provider.
8. **Upstream has not fixed B6.** The relevant upload-handler change since `08bca78f` is [#855](https://github.com/garfiec/Librechat-Mobile/pull/855), commit `852c7e5e` on 14 November. It adds `conversationId` and `isTemporary` to uploads so temporary-chat files expire. Filename fallback and exception display remain unchanged. No changes appeared in the intake/metadata/notice paths checked. The composer change `4fbf2d60` concerns iPhone Duo layout, not this failure handling.
9. No matching issue was found in the searches above. Closed [#716](https://github.com/garfiec/Librechat-Mobile/issues/716) concerns shares arriving during sign-in; closed #696 concerns iOS share intake. Neither is this Android metadata/read failure. Search coverage is not proof that no issue exists.

## Verdict
**real gap**. The content-address leak and numeric title still reproduce. A denied metadata lookup can also crash. The invalid test share explains the read failure; it does not justify exposing platform exceptions or crashing.

## Recommended action
Report upstream; **M (a few hours)** for a bounded Android intake fix and verification. Catch expected provider access/not-found failures at metadata lookup and stream opening, show localized “Couldn't open the shared file. Share it again from the original app.”, and use “Shared file” when no display name is available. Do not treat a provider id as a human filename. Keep the existing remove/retry tray behavior for failures that already have an attachment entry.
If a local fix is needed first, place Android intake/error classification in a fork-only package inside `feature/chat` (it needs chat-owned types), with marked hooks at metadata/read boundaries; English/German messages belong in `:fork` resources. Do not add unmarked edits to upstream files. Verify metadata denial, byte-read denial and missing metadata in the existing test classes, plus one valid granted share on device.

## Only the operator can answer
None needed to establish B6. Whether to carry a local patch while awaiting upstream is a product decision.

## Not checked
- A valid share from a real sender with a URI grant; the original file/provider and exact 13 November launch command; package-name wording on the later read-error path today.
- iOS, physical phone, multi-file shares, signed-out intake, backend behavior, or a model turn. No server HTTP/SSH requests, Gradle, installs, code edits or commits.
- The installed debug APK's exact source hash was not established; runtime findings concern the already-installed app. Committed-source comparisons use the HEAD named above, not another agent's edits.
- Removed the synthetic failed attachment; left the app on the empty new-chat landing and closed this thread's device panel without shutting down the emulator.
