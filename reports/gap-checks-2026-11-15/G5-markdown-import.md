# G5: Markdown import

Checked 2026-11-15 by gpt-6.1-sol.

## Question
Can a Claude/ChatGPT Exporter Markdown transcript picked on the phone use the backend fork's conversation import?

## What I looked at
- Mobile `develop` HEAD `0058fb60db77b42d9122cfa1f870770bb41bb29e`; upstream/develop `af0ea66ebd7657a4ef96be8877229f82288605af`.
- Read BRIEF.md, CLAUDE.md (This fork), fork/README.md, docs/FORK_FEATURE_LEDGER.md:101–355, docs/APP_CHECK_2026-11-13.md, docs/UPSTREAM_CATCHUP_PLAN.md (including 7/7a), and module guidance for conversations/settings/data/network.
- Backend read only through `ssh air-agents`: `~/projects/librechat`, branch `local-features`, HEAD `6f42475e62f3175d61edac6a31f318730d2d5580`. This is source evidence, not a deployed-image check.
- Read Air docs `~/LibreChatDocs/README.md`, `export.md`, `fork-customizations.md` (especially 329–364), `follow-ups.md`; inspected commits `9265ed160` and `a3f010ea8` and current source.
- Paths below starting `client/`, `packages/`, or `api/` are relative to that Air backend tree. Mobile Kotlin paths use their usual `src/commonMain/kotlin/com/garfiec/librechat/` prefix, then the qualified module package named below.
- Searched all mobile Kotlin call sites for `ConversationImporter`, `importConversation`, `importConversations`, and import UI/picker strings; inspected settings, export, API, repository and Android share entry points.
- Ran `git log --oneline 08bca78f..upstream/develop -- feature/conversations core/data/src/commonMain/kotlin/com/garfiec/librechat/core/data/repository/ConversationRepositoryImpl.kt core/network/src/commonMain/kotlin/com/garfiec/librechat/core/network/api/ConversationsApi.kt`.
- Read-only upstream issue searches: `gh issue list --repo garfiec/Librechat-Mobile --state all --search ...` for `import`, `"conversation import"`, and `"Markdown" "import"`; also PR search for `"import" "conversation"`. No matching import feature/fix found. Searches are not an exhaustive issue inventory.

## What I found
### Web: a complete entry point on the fork
- Settings → Data controls → Your data → Import. `client/src/components/Nav/Settings/registry.tsx:622–629` registers it without a per-row show gate. Component: `client/src/components/Nav/SettingsTabs/Data/ImportConversations.tsx` (called WebImport below).
- Picker `accept` is exactly `.json,.md,.markdown,application/json,text/markdown` (WebImport:214–220). Markdown recognition in the UI uses case-insensitive `.md`/`.markdown` (21).
- It reads Markdown only to count inline `data:image/` links. With images, it asks how many to send from the end; initial count is min(5, image count); accepts integers from 0 to image count (115–162, 222–265). It does not locally parse the transcript into messages.
- Before upload it checks `conversationImportMaxFileSize` from startup config. Multipart fields are `file` (original filename URI-encoded), optional `maxImages`, and `timezoneOffset` in JavaScript getTimezoneOffset minutes (80–102). The button becomes disabled with spinner/Importing (189–210).
- `client/src/data-provider/mutations.ts:981–1010` → `packages/data-provider/src/data-service.ts:861–863` → `api-endpoints.ts:119,163` posts multipart to **POST /api/convos/import**. Success invalidates normal, archived and pinned conversation lists.
- Server multer accepts MIME `application/json`, `text/markdown`, `text/x-markdown`, OR extension `.json`, `.md`, `.markdown`, so a `.md` labeled `text/plain` or octet-stream is allowed (`api/server/routes/files/multer.js:52–65`). Upload has size and rate limits (`api/server/routes/convos.js:752–807`).
- Route passes original filename, app config, `maxImages` and `timezoneOffset` to import. Markdown returns HTTP 201 `{message: "Conversation(s) imported successfully", conversationId: "..."}`; JSON success need not contain an ID (convos.js:791–807).
- Detection is extension OR a non-JSON-looking body with exact `## User:`, `## Assistant:`, `## Prompt:` or `## Response:` role headers (`packages/api/src/conversations/markdown/transcript.ts:151–157`). `api/server/utils/import/importConversations.js:32–49` branches to Markdown before JSON.parse.
- Markdown image storage uses `saveBuffer` + `createFile`, including withheld flags (`api/server/utils/import/markdown.js:31–74`). Every supported user image is kept; all but last N are withheld from model delivery. Assistant images become omission notes, per `transcript.ts:159–161` and Air `export.md:64–81`.
- Afterwards web shows “Chat imported” and navigates to `/c/<conversationId>`; other import success shows the generic toast. Failures show file-type/import/upload errors (WebImport:36–109; English translation.json:1777). This is source behavior, not a browser observation.

### Mobile: import plumbing exists, but no import flow
- **There is no conversation-import screen or file picker wired up. Thus there is no import MIME filter to widen.** `feature/conversations/CLAUDE.md:28–32` explicitly says the importer/repository exist but no screen calls them. Whole-tree Kotlin search confirms only DI registration/tests reference `ConversationImporter`; only repository/tests call the API.
- Settings → Privacy & data has archive/shared chats, offline/cache controls, site icons and delete-all, but no Import (`feature/settings/.../feature/settings/screen/PrivacySettingsPage.kt:63–163`). `DataSettingsViewModel.kt:110–119` has data management and diagnostic export actions, no chat import.
- `feature/conversations/.../feature/conversations/export/ConversationExporter.kt:32–65` builds native JSON from fetched conversation/messages. Markdown export is separate client-side output; export support does not provide an import entry point.
- The nearby `ProjectFilePicker.android.kt:16–24` uses OpenDocument `*/*`, but it picks project attachments, not chat imports. Android open-with/share stages SharedContent for the current chat (`app/src/main/kotlin/com/garfiec/librechat/MainActivity.kt:209–212,232–259`); it does not call the import repository.
- If a future screen calls `ConversationImporter.parseJson`, Markdown fails JSON deserialization. It additionally requires a conversation ID and nonempty messages and rejects shortened tool-call previews (`feature/conversations/.../feature/conversations/export/ConversationImporter.kt:14–38`). This parser is currently dormant, not a live phone rejection.
- The repository is different: `core/data/.../core/data/repository/ConversationRepositoryImpl.kt:323–341` only tries JSON parsing to detect shortened tool-call previews; parse failure falls back to false. **It does not reject Markdown solely for being non-JSON.**
- That method would encode the original text as UTF-8 and upload it as filename `conversation.json`, MIME `application/json`. `core/network/.../core/network/api/ConversationsApi.kt:185–202` submits a multipart `file` to the correct **POST /api/convos/import**. It sends neither `maxImages` nor `timezoneOffset`.
- Inferred from both sides: manually invoking that repository with a normal exporter transcript could already import it, because the backend recognizes role headers despite the JSON filename. Missing options mean default last 5 images (`packages/data-provider/src/config.ts:2566`) and server-process timezone for local export dates (`packages/api/src/conversations/markdown/parse.ts:174–198`). This is not an available user workflow.
- Response handling wrongly models the import acknowledgement as a full `Conversation` (`ConversationsApi.kt:189–202`). `Conversation` has defaults, so an ID-only response can decode (`core/model/.../core/model/Conversation.kt:16–27`). The repository then caches that partial/default conversation (`ConversationRepositoryImpl.kt:339,548–551`). It does not fetch the imported title/settings/messages. There is no importer success UI or navigation today.
- Upstream changes since the app check address pin/list sync, duplicate navigation, search, account switching and layout. None wires conversation import. The current source still has the gap.

## Verdict
**real gap.** A user cannot pick a transcript for conversation import in the mobile app today. This is a missing import entry point and result flow, not just a JSON-only picker. The server route/parser already exists; the lower-level client targets it but loses file options and treats its acknowledgement as a conversation.

## Recommended action
**M (a few hours):** add a fork-owned Import row/sheet under Settings → Privacy & data, a file picker, and a small upload controller. Keep Markdown bytes intact and bypass `ConversationImporter.parseJson` for Markdown; do not port the backend parser. Preserve the filename and send `text/markdown`; allow `.md`/`.markdown` even when the provider reports text/plain or octet-stream. A broad picker with extension checks avoids provider MIME inconsistencies; include JSON if offering the shared Import action.
Use multipart `file`, `maxImages` and JS-sign `timezoneOffset`; initially a documented default of 5 is sufficient to import, while a small last-N field gives web parity. Read a small `{message, conversationId?}` response, then fetch the real conversation, refresh the list and navigate by ID. Show progress and a plain failure message. Do not cache the acknowledgement as a conversation. JSON may import several chats without an ID, so refresh/show success in that case.
Under fork/README.md: shared UI/controller/platform picker and strings belong in `:fork` (`fork/markdownimport`, English/German); the network helper belongs in a fork-only package inside `:core:network` (core cannot depend on `:fork`). Add only marked hook calls in settings/navigation, register hooks and update the ledger. Use module-local fork bridges if settings/navigation types require them. No backend change is needed.
Proportionate later acceptance: synthetic text-only and inline-image exporter fixtures, raw multipart/response checks, then an authorized phone import that opens the new chat. Inspect image withholding separately from model continuation.

## Only the operator can answer
Nothing blocks the finding. Product choice: keep default last 5 images for the smallest UI, or include the web's last-N prompt immediately.

## Not checked
No emulator, browser, Gradle/build/test execution, HTTP import, database access/write, deployment verification, real transcript read, or model continuation. No iOS picker/runtime acceptance. No claim that every exporter version parses. Only this report was written; no app/backend code changed.
