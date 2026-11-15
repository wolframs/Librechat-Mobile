# G3: PDFs as text

Checked 2026-11-15 by GPT-6.1 Sol.

## Question
Do mobile PDF uploads match the backend's native Anthropic, Surplus (Claude), Surplus and OpenRouter rows? Is old-fork feature 4 still needed?

## What I looked at
- Read `BRIEF.md`, root `CLAUDE.md` (This fork), `fork/README.md`, ledger state/re-landing sections, `APP_CHECK_2026-11-13.md`, catch-up steps 7/7a and relevant chat/model/network module guidance.
- Local source at `0058fb60`. Mobile paths below are relative to `/home/wolfram/Projects/Librechat-Mobile`. Backend paths are relative to the Air's `~/projects/librechat`.
- Air, read only through `ssh air-agents`: `~/LibreChatDocs/{README,fork-customizations,follow-ups,deployments-2026-10,cost-and-pricing,ops}.md`; targeted `git grep`, `sed`, `git log -1`, `git status --short`. Source HEAD `6f42475e6`, clean. `ops.md:5-19` names that same deployed revision on 15 November. This is source plus deployment-document evidence, not fresh container inspection.
- Backend policy: `librechat.yaml:350-457`; `packages/api/src/endpoints/custom/config.ts:65-103`; `packages/data-provider/src/{schemas.ts:54-118,resolve-llm-delivery-path.ts:253-360,file-config.ts:665,1057-1058,1406-1407}`.
- Backend processing: `api/server/services/Files/process.js:769-810,920-1080`; `packages/api/src/files/{documents/crud.ts:30-41,encode/document.ts:38-149,189-252,281-405}`; `api/app/clients/BaseClient.js:1792-1803`; `api/server/controllers/agents/client.js:2610-2616`; `packages/api/src/agents/files/delivery.ts:42-92`.
- Web: `client/src/components/Chat/Input/Files/{AttachFileMenu.tsx:184-290,DragDropModal.tsx:30-65}`; `client/src/utils/files.ts:550-660`; `client/src/hooks/{Agents/useAgentToolPermissions.ts:38-52,Files/useUploadOptions.ts:58-97,Files/useFileHandling.ts:360-415}`; `packages/data-provider/src/data-service.ts:610-620`.
- Mobile: `core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/response/UploadRouting.kt:38-43,63-83,217-224,284-318,356-433`; `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/ChatUiState.kt:568-573,1011-1021,1062-1133`; its `delegate/UploadIntakeDelegate.kt:115-155,180-204,238-252,275-281,394-398`.
- Mobile wire: `feature/chat/src/androidMain/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/FileAttachmentDelegate.kt:210-258`; corresponding `iosMain/.../delegate/IosFileHandler.kt:118-127`; `core/network/src/commonMain/kotlin/com/garfiec/librechat/core/network/api/FilesApi.kt:55-110`.
- Compatibility: `git log --oneline 08bca78f..upstream/develop -- <routing/model paths>` returned only `d9afdcc9` (MCP availability, unrelated). GitHub all-state searches for PDF, upload provider and Surplus found no matching Surplus fix. [#334](https://github.com/garfiec/Librechat-Mobile/issues/334), Upload as text, was already closed on 11 August.

## What I found
### Server today
The override is endpoint policy, not a requirement that clients send `context`. `librechat.yaml:448-457` keeps `legacyFileUploadUX: true` and lists only `application/pdf` under `providerUnsupportedMimeTypes` for BOTH Surplus names. Neither native Anthropic nor OpenRouter has that override.

| Selected row | PDF uploaded with no `tool_resource` | Model-facing result |
|---|---|---|
| `anthropic` | Provider delivery | Base64 Anthropic `document` source, filename context; no citations flag. |
| `Surplus (Claude)` | Server changes provider delivery to text | Extracts at upload; stores original bytes, `text`, `llmDeliveryPath: text`; injects text through the attachment message's `fileContext`. |
| `Surplus` | Same override despite OpenAI-compatible transport | Same extracted-text delivery. |
| `OpenRouter` | Provider delivery | OpenAI-compatible `file` part with filename and base64 data URL, or `input_file` if Responses is used. Actual model support remains provider/model dependent. |

Evidence: resolver `:281-356`, upload processor `:797-810,920-1080`, encoder `:38-149,281-405`, BaseClient/context prepend references above. Explicit `tool_resource=context` gives text on all four rows. Code/search resources are separate tool destinations.

The override runs AFTER the legacy provider choice, including explicit “Upload to Provider.” Surplus had returned 200 while dropping PDFs on both transports. Documented probes also found plain-text document sources dropped on Surplus (Claude), while images arrived (`cost-and-pricing.md:703-721`). Ordinary extracted text avoids those document blocks.

Old provider-route PDFs are covered by `encode/document.ts:189-252,313-350`: extract at send time on either Surplus row, emit `File: "name"` and token-limited text in the historical message. Extraction failure, over 15 MB, or no text retains native document delivery, which may still be dropped. New image-only PDFs fail upload with “No text found in document” (`documents/crud.ts:41`). This supplies text, not PDF page images or OCR.

The detailed 11 November deployment record `:445-459` places this in the THIRD deployment (`b517e238f`) and records a successful Surplus (Claude) PDF upload with `llmDeliveryPath: text`. The cost guide calls it the second deployment; prefer the detailed record for chronology. `follow-ups.md:30` still lists old-chat PDFs, scanned PDFs and child sharing as untested then. I did not repeat them.

### Web side
The deployed YAML keeps the destination chooser. “Upload to Provider” sends no `tool_resource`; “Upload as Text” sends `context`. Both use `/api/files` with the selected endpoint name (`useFileHandling:369-370,413-415`, data-service upload function). Drag/drop uses the same destinations and MIME checks.

The document check accepts EITHER endpoint type OR `provider || endpoint` in the document-capable set; OpenRouter spelling is normalized. The `provider` in these hooks comes from the selected agent, not directly from custom endpoint config. All three custom rows expose `type: custom` (`custom/config.ts:71`), which itself qualifies. Native Anthropic qualifies by name. Thus all four offer PDFs through provider upload. There is no web-side Surplus/PDF special case: the server applies the override.

### Mobile side
`UploadIntakeDelegate` resolves picks, waits for an agent provider, then auto-routes or stages the manual sheet. Auto is the preference default; shares auto-route. Manual offers provider and text for these PDFs, plus search when allowed. Confirm-time routing context currently stores `selectedAgentProvider` directly.

`ChatUiState.routingAgentProvider:1099-1100` is agents-only. Custom `EndpointConfig.provider` is NOT consulted by upload routing. The router accepts either resolved provider/name OR endpoint `type`. Its mirrored document set includes `anthropic`, `openAI`, `bedrock`, `custom`, `google`, `vertexai`, `mistralai`, `mistral`, `deepseek`, `moonshot`, `openrouter`, `xai`. Bedrock has a broader MIME list; otherwise PDFs qualify natively. PDF is also text-extractable.

`scripts/mirrors.json:80-107,122-132,420-432,1019-1032` registers the provider set, Bedrock MIME types, parser MIME types, Anthropic document restrictions and provider predicate. These watch official LibreChat, not this backend fork's extra policy. Mobile `EndpointFileConfig` does not decode `providerUnsupportedMimeTypes` (`FileUploadConfig.kt:47-55`); server enforcement makes that harmless for delivery here.

| Mobile selection | Default PDF request | Matches server? |
|---|---|---|
| `anthropic` | PROVIDER via native set; `/api/files`, endpoint `anthropic`, no `tool_resource` | Yes: native document. |
| `Surplus (Claude)` | PROVIDER via `type: custom`; original row name, no `tool_resource` | Yes: backend forces text. |
| `Surplus` | PROVIDER via `type: custom`; original row name, no `tool_resource` | Yes: backend forces text. |
| `OpenRouter` | PROVIDER via normalized name and/or `type: custom`; original row name, no `tool_resource` | Yes at protocol level; model acceptance untested. |

Android and iOS both pass the selected endpoint unchanged and map TEXT to `context`, PROVIDER to null. Android sends `message_file=true`. This table is inferred execution from source, not an emulator observation.

### Ledger feature 4
PARTLY present: `EndpointConfig.kt:11-12` already has `provider` and `extendedCacheTTL`, restored as `FORK-HOOK(cache-ttl)`. `fork/src/commonMain/kotlin/com/garfiec/librechat/fork/cachettl/CacheTtlRules.kt:15-23` uses both to show/arm cache controls. They remain needed there; parameter-panel identity is not a substitute for transport identity.

The two routing expressions proposed in `FORK_FEATURE_LEDGER.md:456-464` are NOT restored. They are unnecessary for PDFs on these four rows: `custom` already sends PDFs to provider, and the backend owns Surplus text conversion. Their wider effects on other provider-specific MIME choices are outside this four-row PDF finding.

Do not blindly restore that recipe: `ChatUiState.fileConfigEndpoint:573` also reads `routingAgentProvider`. Returning custom `provider: anthropic` there would select mobile policy under `anthropic` instead of `Surplus (Claude)` on supported servers. Keep capability identity separate from name-keyed file policy. The actual upload must retain the custom row name for the backend override.

## Verdict
**already fine** for PDF delivery on the four rows, by source and documented deployment evidence. No mobile PDF patch is needed. Feature 4's fields are present and needed for cache TTL; the missing upload expressions do not fix a demonstrated PDF gap here.

## Recommended action
**S:** Update the ledger: fields restored by cache-ttl; old provider-routing expressions not required for this PDF case. No code change now. If broader provider routing is later justified, put its helper in `feature/chat/.../fork/` with thin marked hooks, preserving name-keyed policy and upload endpoint; core additions follow core-owned fork files/hooks under `fork/README.md`.

## Only the operator can answer
Nothing blocks this finding. Future live acceptance needs a usable OpenRouter credential; the 13 November app check records its key as expired. I did not inspect credentials.

## Not checked
No emulator, builds, tests, live uploads, HTTP calls, provider probes, container inspection or database access. No fresh end-to-end proof for any row. No full inventory of other document types, agent/child sharing, endpoint switching, resend limits or iOS UI behavior. Deployment records describe historical runs, not a newly observed runtime result.
