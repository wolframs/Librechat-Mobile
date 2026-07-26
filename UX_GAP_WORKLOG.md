# UX Gap Plan and Worklog

This is the persistent planning and execution ledger for UX and reliability gaps found
while exercising LibreChat Mobile against real self-hosted servers.

It exists so that work can survive context compaction, agent handoffs, interrupted
sessions, and the inevitable future question: "What exactly did we change, and why?"

This document is intentionally separate from `DISCOVERY.md`:

- `DISCOVERY.md` describes the product and backend feature surface.
- This file tracks concrete mobile-client gaps, implementation decisions, verification,
  and remaining work.

## Operating Rules

1. Revalidate an item's evidence against the current branch before implementing it.
2. Prefer one independently testable gap per commit. Combine items only when they share
   the same state model or migration and splitting them would create an invalid midpoint.
3. Do not modify the LibreChat backend to compensate for a mobile-client defect.
4. Preserve compatibility with self-hosted HTTP servers and older supported backends.
5. Add automated coverage in new test files or clearly separated test sections where
   practical.
6. Automated verification and device verification are separate states. An item is not
   `DONE` until both are complete or the missing verification is explicitly waived.
7. Do not overwrite unrelated working-tree changes. At this document's creation,
   `gradle.properties` and `gradle/gradle-daemon-jvm.properties` contained local,
   user-owned Android Studio/toolchain state.
8. Never force-push. The private Forgejo `origin` is primary and pushes fan out to
   Forgejo and GitHub.
9. Update the status matrix and append a session entry whenever implementation work
   changes an item.

## Status Vocabulary

| Status | Meaning |
|---|---|
| `SCOUTED` | Code evidence exists, but implementation has not started. |
| `READY` | Evidence was revalidated and the implementation/test boundary is understood. |
| `IN_PROGRESS` | A working session is actively changing the item. |
| `IMPLEMENTED` | Code exists locally, but verification is incomplete. |
| `AUTO_VERIFIED` | Relevant automated tests and static checks pass. |
| `DEVICE_VERIFIED` | The user confirmed the acceptance path on a device or emulator. |
| `DONE` | Implementation, automated verification, documentation, and required device verification are complete. |
| `DEFERRED` | Intentionally postponed; the item must include a reason. |
| `REJECTED` | Investigation showed that the suspected gap is not a defect or should not be changed. |

## Verified Baseline

Last refreshed: **2026-07-27**

- Branch: `develop`
- HEAD: `c3b2bc673ba7` (`feat(auth): remember servers and use system password manager`)
- Relative to freshly fetched `upstream/develop`: 0 behind, 5 ahead
- Existing local feature commits:
  - `829f16f0` — Anthropic cache TTL controls
  - `40b0e72d` — vertical cache TTL thread-gutter control
  - `c33eb4dc` — paragraph-spacing preference
  - `c9c5adc8` — conversation model-parameter restoration
  - `c3b2bc67` — remembered servers and system password manager
- Android app lint checkpoint: 0 errors, 26 warnings, 3 hints. Most warnings concern
  dependency versions and launcher resources. The cleartext base configuration warning
  is expected because arbitrary self-hosted HTTP endpoints are supported and guarded by
  an explicit per-server warning.
- Unit coverage is substantial in `core/data` and `feature/chat`, but Android UI journey
  coverage is minimal: one `AuthFlowTest` source file with three server-URL assertions.
- No source change was made during the scouting pass that created this document.

Refresh this section whenever branch position, upstream version, or relevant architecture
changes materially.

## Status Matrix

| ID | Priority | Area | Status | Automated | Device |
|---|---:|---|---|---|---|
| UX-001 | P0 | Signed-in server management | `AUTO_VERIFIED` | Passed | Not started |
| UX-002 | P0 | Server/account-scoped tools and MCP | `AUTO_VERIFIED` | Passed | Not started |
| UX-003 | P0 | Stream teardown after clean EOF | `AUTO_VERIFIED` | Passed | Not started |
| UX-004 | P0 | Pagination retry loop | `AUTO_VERIFIED` | Passed | Not started |
| UX-005 | P1 | Credential Manager lifecycle | `AUTO_VERIFIED` | Passed | Not started |
| UX-006 | P1 | Agent editor draft protection | `AUTO_VERIFIED` | Passed | Not started |
| UX-007 | P1 | Memory-safe, cancellable uploads | `AUTO_VERIFIED` | Passed | Not started |
| UX-008 | P2 | Draft attachment and queue recovery | `AUTO_VERIFIED` | Passed | Not started |
| UX-009 | P2 | Banner dismissal persistence and scope | `AUTO_VERIFIED` | Passed | Not started |
| UX-010 | P2 | Localization completeness | `IMPLEMENTED` | German coverage passed | Not started |
| UX-011 | P2 | Accessibility audit | `IMPLEMENTED` | Static regression passed | Not started |
| UX-012 | P2 | Cold-start token decryption | `AUTO_VERIFIED` | Passed | Agent-tested Pixel 7 |
| UX-013 | P3 | Multi-account sign-out wording | `SCOUTED` | Not started | Not started |
| UX-014 | P3 | Archived favorite reconciliation | `SCOUTED` | Not started | Not started |
| UX-015 | P3 | iOS parity placeholders | `SCOUTED` | Not started | Not started |

Priority meaning:

- **P0:** correctness or runaway behavior; start here.
- **P1:** credible data-loss, authentication, or resource-exhaustion risk.
- **P2:** persistent-state, accessibility, localization, or measured performance quality.
- **P3:** bounded edge case, wording improvement, or platform-parity work outside the
  current Android-first testing lane.

## Recommended Execution Order

### Tranche A: Multi-server integrity

1. UX-002 — scope tools and MCP selections correctly.
2. UX-001 — expose and manage remembered server profiles while signed in.
3. UX-005 — close the Credential Manager lifecycle gaps.
4. UX-009 — align warning and banner persistence with server/account boundaries.

These items share the question "which server/account owns this state?" They may share a
migration design, but should still land as separate commits when possible.

### Tranche B: Failure-path reliability

1. UX-003 — guarantee stream teardown.
2. UX-004 — stop implicit pagination retries after failure.
3. UX-007 — stream uploads without whole-file buffering.

### Tranche C: User-work preservation

1. UX-006 — protect unsaved Agent editor work.
2. UX-008 — recover attachment drafts and queued sends.

### Tranche D: Quality and polish

1. UX-011 — accessibility.
2. UX-010 — localization completeness.
3. UX-012 — profile and, if justified, move token decryption.
4. UX-013 — clarify sign-out semantics.
5. UX-014 — reconcile archived favorites.

### Tranche E: iOS parity

1. UX-015 — split into independently shippable platform items after Android work is
   stable and an iOS verification environment is available.

---

## UX-001 — Manage Server Profiles While Signed In

**Priority:** P0

**Status:** `AUTO_VERIFIED`

### Observed behavior

Remembered server profiles and HTTP-warning decisions are managed on
`ServerUrlScreen`, which is primarily reachable during authentication/add-account
navigation. Signed-in Settings displays the active server as static About text and has
no server-management route.

### Evidence

- `feature/auth/.../screen/ServerUrlScreen.kt`
- `feature/settings/.../screen/GeneralSettingsScreen.kt` — `AboutInfo`
- `feature/settings/.../navigation/SettingsNavigation.kt` — no server-profile route
- `core/data/.../datastore/ServerProfileDataStore.kt`

### Desired behavior

- A signed-in user can list remembered servers.
- The active server/account is visibly identified.
- A server profile can be added, edited where safe, or forgotten.
- Forgetting a server clearly explains what happens to accounts, credentials, cached
  data, and its remembered HTTP-warning decision.
- The HTTP warning can be re-enabled/reset for an individual server.
- Switching server does not silently discard the current authenticated account.

### Acceptance checks

- [x] Settings exposes a Server Profiles entry.
- [x] Current server and associated accounts are distinguishable.
- [x] Add/switch/forget flows work from a signed-in session.
- [x] Forget confirmation names the exact server and affected local data.
- [x] HTTP-warning suppression is inspectable and resettable per server.
- [x] Tests cover profile projection and the destructive transaction.
- [ ] User verifies the flow on an Android device.

### Design questions to resolve

- Is "edit URL" actually a new server identity rather than an in-place mutation?
- Should forgetting the last profile return directly to onboarding?
- Should password-manager deletion be offered but remain a distinct system-mediated
  action?

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Decision: URL edits are represented as adding a new server identity. The screen groups
  signed-in accounts under canonicalized server URLs and switches through the existing
  account switcher.
- Forget semantics: an explicit confirmation removes the server's accounts and their
  scoped local data, the server pointer when applicable, HTTP-warning state, and local
  credential references. System password-manager entries are explicitly outside this
  transaction.
- Tests added: `ServerProfilesViewModelTest`.
- Automated verification: settings/shared Android compilation, focused ViewModel tests,
  and Settings Koin verification pass.
- Device verification: pending.
- Next action: commit independently, then begin UX-005.

---

## UX-002 — Scope Tool and MCP Selections to Server/Account

**Priority:** P0

**Status:** `AUTO_VERIFIED`

### Observed behavior

`SettingsDataStore.selectedMcpServers` and `enabledTools` use global preference keys.
`ChatViewModel` restores them for a new chat regardless of active server/account.
`ChatRequestBuilder` serializes stored MCP names without intersecting them with the
current server's advertised MCP list.

### Evidence

- `core/data/.../datastore/SettingsDataStore.kt`
  - `selectedMcpServers`
  - `enabledTools`
  - `setSelectedMcpServers`
  - `setEnabledTools`
- `feature/chat/.../viewmodel/ChatViewModel.kt` — initial tool/MCP restoration
- `feature/chat/.../viewmodel/ChatRequestBuilder.kt` — `buildEphemeralAgent`

### Risk

A selection made on server A can be restored and sent to server B. At best this causes
confusing errors; at worst the request silently enables an unintended tool with a
matching identifier.

### Desired behavior

- Persist server-defined selections under an account or server identity.
- Validate restored selections against the current server's capabilities before
  displaying or sending them.
- Define whether preferences should follow an account, a server, a model/preset, or a
  conversation. Do not choose this implicitly during implementation.
- Migrate or safely discard legacy global values.

### Acceptance checks

- [x] Select MCP/tool on server A; switching to B does not restore or send it.
- [x] Switching back to A restores the account's selection.
- [x] Removed server capabilities are pruned before request construction.
- [x] Request-builder tests prove stale identifiers cannot be serialized.
- [x] Persistence tests cover safe removal of legacy global keys.
- [ ] User verifies with two server profiles or a controlled capability change.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Decision: scope selections by account identity, because both tool permissions and MCP
  names are server/user-defined. Legacy global selections are discarded rather than
  guessed onto the currently active account.
- Request safety: MCP names are intersected with the current server list; unavailable
  code/file tools are omitted even if stale persisted state contains them.
- Tests added:
  - `AccountScopedToolSelectionTest`
  - `ChatRequestBuilderToolValidationTest`
- Automated verification: focused `core:data` and `feature:chat` unit tests pass.
- Device verification: pending.
- Next action: commit this item independently, then begin UX-001.

---

## UX-003 — Guarantee Stream Teardown After Clean EOF

**Priority:** P0

**Status:** `AUTO_VERIFIED`

### Observed behavior

`StreamingManagerDelegate.launchStream` supports an `onTerminated` safety callback for a
flow that completes without a `Final` or `Error` event. Normal sends provide this
callback. `MessageEditingDelegate` does not, so edit/regenerate/continue paths accept the
default no-op.

### Evidence

- `feature/chat/.../viewmodel/delegate/StreamingManagerDelegate.kt`
  - `launchStream`
  - documented clean-termination gap
- `feature/chat/.../viewmodel/ChatViewModel.kt` — normal send supplies termination cleanup
- `feature/chat/.../viewmodel/delegate/MessageEditingDelegate.kt` — resubmit path omits it

### Failure shape

If the SSE flow completes cleanly without a terminal frame—for example after a proxy
cutoff, empty 404 response, or server edge case—the edit/regenerate/continue UI may stay
in streaming mode indefinitely.

### Desired behavior

All stream entry points share one teardown contract. A collector ending without a
terminal event must transition to a deterministic recoverable state exactly once.

### Acceptance checks

- [x] Unit test covers clean EOF for normal send.
- [x] Unit test covers clean EOF for edit/regenerate/continue.
- [x] Stop/spinner/queued-message state is cleared or advanced consistently.
- [x] No double-finalization when a terminal event is followed by flow completion.
- [x] Existing abort watchdog and resume tests remain green.
- [ ] Fault-injected device test confirms the composer recovers.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- `launchStream` now owns unexpected clean-EOF teardown instead of accepting an optional
  caller callback, so new-send and resubmit paths cannot diverge.
- Clean EOF preserves partial content, clears streaming/tool state, surfaces a recoverable
  connection error, pauses queued sends, and reconciles an existing conversation.
- The existing per-session end latch makes the EOF action a no-op after Final or Error.
- A new focused test file covers normal send, edit/regenerate/continue semantics, and
  terminal-event-plus-completion double-finalization.
- The complete `feature:chat` Android unit suite and module Detekt pass.
- Fault-injected device/emulator verification remains pending.

---

## UX-004 — Stop Pagination Auto-Retry Loops

**Priority:** P0

**Status:** `AUTO_VERIFIED`

### Observed behavior

Agent Marketplace and Skills calculate `shouldLoadMore` from list position, `hasMore`,
and `!isLoadingMore`, then trigger `loadMore()` from `LaunchedEffect`. After a failed
page, the ViewModel resets `isLoadingMore` to false while the list remains at the
threshold, making `shouldLoadMore` true again.

### Evidence

- `feature/agents/.../screen/AgentMarketplaceScreen.kt`
- `feature/agents/.../viewmodel/AgentMarketplaceViewModel.kt`
- `feature/skills/.../screen/SkillsListScreen.kt`
- `feature/skills/.../viewmodel/SkillsListViewModel.kt`

### Failure shape

A persistent second-page failure can cause immediate repeated requests while the user
remains near the bottom. In Skills, an error with existing rows is not clearly surfaced,
so the loop may be silent.

### Desired behavior

One viewport threshold crossing initiates at most one page request. A failed page enters
a visible retry state and requires explicit retry or a meaningful new trigger.

### Acceptance checks

- [x] Persistent page failure results in one request, not an unbounded loop.
- [x] Existing rows remain visible.
- [x] An inline error and Retry action are visible.
- [x] Retry succeeds without a full screen reload.
- [x] Tests cover both Agent Marketplace and Skills.
- [ ] User verifies with a mocked/throttled page failure.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Agent Marketplace and Skills now keep next-page failures separate from first-page errors.
- A failed page latches its error, preserves existing rows and cursor/page state, and blocks
  threshold-driven requests until the user chooses the inline Retry action.
- Retry requests only the failed next page; search, category, refresh, and first-page loads
  intentionally clear the stale page latch.
- Separate tests for both features prove repeated automatic `loadMore` calls remain no-ops
  after failure and that explicit retry appends the recovered page.
- Complete Agent and Skills Android unit suites and module Detekt pass.
- Mocked/throttled device verification remains pending.

---

## UX-005 — Complete Credential Manager Lifecycle

**Priority:** P1

**Status:** `AUTO_VERIFIED`

### Observed behavior

- A successful password login can create `PendingCredentialSave`.
- A password login that proceeds through 2FA navigates to `TwoFactorViewModel`, which has
  no password/save bridge.
- Selecting an already saved credential calls the ordinary login path and can create a
  new pending save, prompting for a redundant update.
- Credential IDs are derived from email plus a display-oriented host label. Different
  LibreChat paths on the same host can collide even though account/server identity
  derivation correctly treats them as distinct.
- The iOS implementation is currently a no-op.

### Evidence

- `feature/auth/.../viewmodel/LoginViewModel.kt`
- `feature/auth/.../viewmodel/TwoFactorViewModel.kt`
- `feature/auth/.../screen/LoginScreen.kt`
- `feature/auth/.../credentials/PasswordCredentialManager.kt`
- `feature/auth/src/androidMain/.../PasswordCredentialManager.android.kt`
- `feature/auth/src/iosMain/.../PasswordCredentialManager.ios.kt`
- `core/common/.../extensions/StringExt.kt` — display host label
- `core/common/.../identity/AccountIdDerivation.kt` — canonical server identity

### Desired behavior

- Successful 2FA completes the original password-save intent without retaining plaintext
  longer than necessary.
- Using an unchanged saved credential does not prompt to save it again.
- Credential identity uses canonical server identity, including meaningful path/port
  distinctions.
- Passwords never enter Room, DataStore, logs, saved state, or durable project docs.

### Acceptance checks

- [x] Fresh password login offers system save.
- [x] Password login plus 2FA offers system save only after success.
- [x] Selecting a saved credential does not create a redundant save prompt.
- [x] Same host with different LibreChat paths produces distinct credential identities.
- [x] Failed login and abandoned 2FA clear transient password handoff state.
- [x] Android Credential Manager tests cover create, retrieve, update, cancellation, and
  provider failure.
- [ ] User verifies with both ordinary and 2FA test accounts.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Password-save intent crosses the 2FA route in a process-memory-only singleton. It is
  consumed after successful verification and cleared on abandonment; it is never placed
  in navigation state, DataStore, Room, logs, or durable docs.
- Saved-credential login bypasses the create/update prompt. New credential labels retain a
  readable host and append the canonical server identity hash so path and port variants do
  not collide.
- Android provider behavior is isolated behind a small gateway and tested for scoped
  retrieval, create/update, dismissal/provider failure, and coroutine cancellation.
- Automated verification: auth and data unit suites, auth/data Detekt, Koin graph
  verification, and shared Android compilation pass.
- Device verification: pending for ordinary and 2FA accounts.

---

## UX-006 — Protect Unsaved Agent Editor Work

**Priority:** P1

**Status:** `AUTO_VERIFIED`

### Observed behavior

`AgentEditorUiState` contains many editable fields but lives only in a ViewModel
`MutableStateFlow`. Back navigation exits directly. There is no dirty-state model,
discard confirmation, local draft, or process-restoration mechanism.

### Evidence

- `feature/agents/.../viewmodel/AgentEditorViewModel.kt`
- `feature/agents/.../screen/AgentEditorScreen.kt`
- Agent editor top-bar back action

### Desired behavior

- Navigating away with unsaved changes asks whether to discard them.
- Routine configuration/process recreation restores a safe draft where feasible.
- Successful save clears the dirty/draft state.
- Existing agents and new-agent drafts have explicit, non-colliding identities.

### Acceptance checks

- [x] Back with no changes exits immediately.
- [x] Back with changes offers Stay/Discard.
- [x] Save then Back does not show a stale warning.
- [x] Rotation/configuration recreation preserves edits.
- [x] Process-death behavior is documented and tested to the chosen level.
- [ ] User verifies a multi-section edit on device.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- A content-only `AgentEditorDraft` defines both dirty comparison and restoration. Server
  reference catalogs, validation messages, loading state, and modal state are excluded so
  asynchronous background loads do not produce false unsaved-change warnings.
- Drafts are serialized into the navigation entry's cross-platform `SavedStateHandle`.
  This preserves edits across Android configuration changes and Android process
  recreation while avoiding indefinite storage in application preferences.
- Draft payloads include an explicit create/edit identity; a draft for one agent cannot
  hydrate another agent or a new-agent destination.
- Top-bar and platform back actions share the same confirmation path. Successful
  save/delete/revert and explicit discard clear the restoration payload.
- Tests added: `AgentEditorDraftStateTest`; the Koin verification fixture now recognizes
  `SavedStateHandle` as a dynamically supplied ViewModel parameter.
- Automated verification: the complete Agents unit suite, Agents Android compilation,
  module verification, metadata Detekt, and whitespace checks pass.
- Device verification remains pending.

---

## UX-007 — Make Uploads Memory-Safe and Responsively Cancellable

**Priority:** P1

**Status:** `AUTO_VERIFIED`

### Observed behavior

Android reads the selected file with `InputStream.readBytes()`. iOS loads `NSData` and
copies it into a `ByteArray`. The upload API then consumes the complete byte array for
multipart construction.

### Evidence

- `feature/files/src/androidMain/.../AndroidFileReader.kt`
- `feature/files/src/iosMain/.../IosFileReader.kt`
- `feature/files/.../viewmodel/FilesViewModel.kt`
- `core/network/.../api/FilesApi.kt`

### Risk

Large files can exist in multiple in-memory copies. Cancellation cannot reliably
interrupt a blocking whole-file read, producing memory pressure, OOM, and misleading
cancel UX.

### Desired behavior

- Stream file content from the platform source into the request body.
- Enforce known server/client size limits before uploading when metadata is available.
- Make cancellation cooperative during both read and network transfer.
- Preserve useful progress and error reporting.

### Acceptance checks

- [x] Upload does not require a full-file `ByteArray`.
- [x] Cancel during local read stops promptly.
- [x] Cancel during network transfer stops promptly.
- [x] Oversized files fail before expensive work when the limit is known.
- [x] Existing image/document uploads still work.
- [ ] Stress test on a memory-constrained emulator with a large synthetic file passes.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- The Files screen now passes a reopenable `StreamingUploadSource` through the repository
  and into Ktor's multipart `ChannelProvider`; the existing byte-array overload remains
  available for callers that already hold generated content in memory.
- Android reopens the selected content URI only when Ktor starts the request and streams
  it through a cancellable channel. iOS now exposes a filesystem-backed channel rather
  than copying `NSData` into a second allocation.
- Known file sizes are checked against the server's effective Agent-file limit before the
  upload channel is opened. Unknown sizes continue safely and are enforced server-side.
- Ktor's multipart copy path cancels the source channel when either local reading or
  network writing fails or is cancelled; the Android adapter closes its `InputStream`
  on EOF or channel cancellation.
- Tests added:
  - `FilesApiStreamingUploadTest`
  - `FilesUploadStreamingTest`
- Automated verification: focused network and Files tests, affected data tests, Android
  compilation, metadata Detekt, and whitespace checks pass.
- iOS compilation is unavailable on this Linux host because the network module's Apple
  cinterop targets are disabled; device/emulator stress verification remains pending.

---

## UX-008 — Recover Attachment Drafts and Queued Messages

**Priority:** P2

**Status:** `AUTO_VERIFIED`

### Observed behavior

Room `DraftEntity` stores only conversation ID, text, timestamp, and account ID.
Composer attachments and queued messages exist only in memory.

### Evidence

- `core/data/.../db/entity/DraftEntity.kt`
- `core/data/.../repository/DraftRepository.kt`
- `feature/chat/.../viewmodel/ComposerState.kt`
- `feature/chat/.../viewmodel/QueueState.kt`
- `feature/chat/.../viewmodel/ChatViewModel.kt`

### Desired behavior

Define and implement a recoverable representation for attachments and queued sends
without persisting credentials, inaccessible platform handles, or misleading upload
states.

### Acceptance checks

- [x] Text plus already-uploaded attachment references survive process death.
- [x] Local-only/unuploaded files are restored honestly or explicitly reported lost.
- [x] Queued sends have documented recovery semantics.
- [x] Draft cleanup on successful send/delete is atomic.
- [x] Migration handles existing text-only drafts.
- [ ] User verifies force-stop/relaunch behavior.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Room v9 adds a nullable, feature-owned recovery payload beside draft text; both fields are cached
  and written atomically, while existing text-only rows migrate with a null payload.
- Uploaded attachment references and complete queued-send configuration now survive process death.
  Local platform handles are never serialized and a relaunch reports how many could not be restored.
- Every recovered queue starts paused, regardless of its pre-restart state, so cold launch never
  sends user content automatically.
- A queued edit persists the stashed new-message composer and the borrowed original queue item,
  abandoning only the in-progress edit if the process dies.
- New focused serialization and repository tests cover full configuration round-trips, exclusion of
  local-only handles, unknown payload versions, atomic text/payload writes, and legacy rows.
- Complete `core:data` and `feature:chat` unit suites plus metadata Detekt and whitespace checks pass.
- Device force-stop/relaunch verification remains pending.

---

## UX-009 — Persist and Correctly Scope Banner Dismissals

**Priority:** P2

**Status:** `AUTO_VERIFIED`

### Observed behavior

- Server banner IDs dismissed through `BannerStateHolder` are held in an in-memory set
  and return after app restart.
- `VersionCheckStateHolder` correctly tracks which server produced the currently visible
  mismatch banner, preventing stale cross-server display.
- Permanent mismatch suppression is nevertheless stored as one global backend-version
  string. Dismissing version X on server A suppresses version X on server B.

### Evidence

- `shared/.../navigation/BannerStateHolder.kt`
- `shared/.../navigation/VersionCheckStateHolder.kt`
- `core/data/.../datastore/SettingsDataStore.kt`

### Desired behavior

Classify each banner type deliberately:

- session-only dismissal,
- persistent global dismissal,
- persistent server-scoped dismissal, or
- persistent account-scoped dismissal.

Do not apply one storage policy to all banners by accident.

### Acceptance checks

- [x] Restart behavior matches the documented policy for server announcements.
- [x] Version suppression on server A does not suppress server B.
- [x] A changed backend version can resurface the warning.
- [x] Server removal clears or safely orphans scoped dismissal state.
- [x] Tests cover two servers advertising the same incompatible version.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Ordinary server-announcement dismissals now persist under the canonical server identity.
- Upstream `persistable=true` announcements remain mandatory: the client renders no dismiss
  action and does not hide them even if a stale matching dismissal exists.
- Backend-version suppression is keyed by canonical server identity and exact reported version.
- Forgetting a remembered server clears both announcement and version-warning decisions.
- New focused tests cover restart recreation, two servers reusing the same banner/version,
  version changes, legacy global-key isolation, mandatory-banner policy, and forget cleanup.
- Device/emulator verification remains pending.

---

## UX-010 — Close Localization Gaps

**Priority:** P2

**Status:** `IMPLEMENTED`

### Observed behavior

Shipped locale resources are incomplete for newer features. During the scouting
checkpoint, German resources were missing approximately:

- 6 authentication/server-password strings,
- 60 chat strings,
- 38 conversation/account/project strings,
- 36 settings strings,
- 3 file strings.

Counts are a scouting signal, not a translation-quality assessment, and must be
recomputed before implementation.

### Desired behavior

- Every shipped locale has intentional coverage for all user-visible strings.
- Missing strings fall back safely during development but are detected in CI.
- Machine translation is not silently presented as reviewed human translation.

### Acceptance checks

- [x] Add a repeatable resource-key coverage check.
- [x] Resolve the German gaps first, subject to user review.
- [ ] Verify long strings and RTL layouts rather than checking key counts alone.
- [ ] No user-facing string remains hardcoded in Kotlin.

### Implementation update — 2026-07-27

- Status: `IMPLEMENTED`
- Added `scripts/check-localization.py`; CI now fails when German loses parity with a module's
  English base resources and reports gaps in every other shipped locale.
- Added German coverage for all 166 keys that were missing at revalidation time across core UI,
  Agents, Auth, Chat, Conversations, Files, and Settings.
- These translations are a coding-pass draft subject to the user's language review; they are not
  represented as reviewed localization-vendor output.
- Other incomplete locales continue to use Compose's English fallback. Their exact gaps remain
  visible in CI instead of being filled with unlabeled machine translation.
- Remaining: semantic hardcoded-string extraction, RTL/long-string inspection, and device review.

---

## UX-011 — Accessibility Audit

**Priority:** P2

**Status:** `IMPLEMENTED`

### Observed behavior

Manual inspection found interactive Compose icons with null or absent accessible labels,
including some overflow and edit/remove actions. Many other null descriptions are
correctly decorative inside already labeled controls; this requires semantic inspection,
not blanket replacement.

The Android app lint task does not meaningfully cover most KMP `commonMain` Compose
semantics, so its clean error count does not settle this item.

### Initial evidence candidates

- Projects overflow action
- Drawer conversation overflow action
- Agent handoff edit/remove actions

### Desired behavior

- Every action has a meaningful spoken label and role.
- Decorative imagery is excluded from the semantics tree.
- Touch targets, focus order, dialog announcements, live streaming updates, contrast,
  and dynamic type are tested.

### Acceptance checks

- [x] Run a semantic code audit without converting decorative icons into noisy labels.
- [ ] Add Compose UI assertions for critical navigation and chat actions.
- [ ] Test TalkBack traversal on the server, login, drawer, chat, settings, and Agent
  editor paths.
- [ ] Verify largest supported font size without clipped essential controls.

### Implementation update — 2026-07-27

- Status: `IMPLEMENTED`
- Audited icon-only Compose actions separately from decorative icons nested beside visible labels.
- Added localized, context-bearing labels to the Projects screen/drawer overflow controls and to
  Agent handoff edit/remove controls. Conversation-row overflow was revalidated as already labeled.
- Added the custom `IconButtonContentDescription` Detekt rule. It rejects a null Icon description
  specifically inside `IconButton`, while allowing decorative null descriptions elsewhere.
- New isolated rule tests cover rejection, labeled actions, and the decorative-icon exception;
  the rule passes across the affected common source sets.
- Remaining: Compose semantics journey assertions, TalkBack traversal, font scaling, contrast, and
  device-level focus/live-update behavior.

---

## UX-012 — Remove or Justify Main-Thread Token Decryption

**Priority:** P2

**Status:** `AUTO_VERIFIED`

### Observed behavior

Android `TokenDataStore` creates `EncryptedSharedPreferences` and initializes its token
cache synchronously in construction. `NavHostViewModel` comments explicitly note that
the decrypt still occurs on Main when Koin constructs the store.

### Evidence

- `core/data/src/androidMain/.../datastore/TokenDataStore.kt`
- `shared/.../navigation/NavHostViewModel.kt`

### Desired behavior

Measure first. If startup impact is material, initialize/decrypt off Main while keeping
navigation deterministic and avoiding a flash of unauthenticated UI.

### Acceptance checks

- [x] Capture a cold-start baseline on a representative slower device.
- [x] Use StrictMode/Perfetto or equivalent evidence.
- [x] If changed, prove no auth-screen flash, race, or duplicate navigation.
- [ ] If measurement is negligible, record the evidence and mark `REJECTED`.

### Implementation update — 2026-07-27

- Status: `AUTO_VERIFIED`
- Direct timing instrumentation on the attached Pixel 7 measured encrypted-preference construction
  at 94–116 ms and active-token decryption at roughly 2.2–2.4 ms on each cold launch. The secure-store
  work was therefore material and the `REJECTED` path does not apply.
- Android now constructs the encrypted store lazily during `TokenManager.warmUp()`, called by
  `AccountRegistry` on its IO dispatcher before `AccountReadyGate` completes.
- Startup auth state is explicitly unresolved until that gate and an IO-dispatched repository check
  finish. The root renders a neutral progress surface; initial auth redirects and Android deep links
  await the authoritative result.
- Seven post-change cold launches measured 955–1,156 ms to first draw, versus 2,421–2,605 ms for the
  five instrumented baseline launches immediately before the change.
- A 4-second, 3-fps launch recording showed only the platform splash, neutral progress surface, and
  authenticated landing page—no login or wrong-account content frame.
- New isolated tests prove Android construction does not touch encrypted storage, repeated warm-up
  initializes it once, and account seeding warms tokens even for an empty roster. Full affected unit
  suites, Android compilation, and static checks pass.
- User device confirmation remains pending; the agent-side measurement preserved app data.

---

## UX-013 — Clarify Multi-Account Sign-Out Semantics

**Priority:** P3

**Status:** `IMPLEMENTED`

### Observed behavior

Settings asks whether the user wants to "sign out." The repository removes the current
account and promotes the most recent surviving account, so the app may remain signed in.
The behavior is internally coherent but the wording does not explain it.

### Evidence

- `feature/settings/.../screen/AccountSettingsScreen.kt`
- `core/data/.../repository/AuthRepositoryImpl.kt`
- `core/data/.../repository/AccountSwitcher.kt`

### Desired behavior

Use "Sign out of this account" when other accounts remain. Consider a separately
confirmed "Sign out of all accounts" action if it serves a real user need.

### Acceptance checks

- [x] Confirmation names the account/server being removed.
- [x] Copy differs appropriately for last-account versus multi-account state.
- [x] Promoted-account transition is visually understandable.

### Implementation update — 2026-07-27

- A dedicated roster-backed presentation model mirrors the repository’s established promotion rule:
  the most-recently-active surviving account.
- The confirmation names the current account and server. With another account present, both the
  action label and title say “Sign out of this account” and the message names the exact account/server
  that will appear after removal. The last-account copy instead explains the return to sign-in.
- The actual revoke, local purge, promotion, account transition, and navigation paths are unchanged.
  Focused tests cover both successor selection and the last-account state.

---

## UX-014 — Reconcile Archived Favorites

**Priority:** P3

**Status:** `SCOUTED`

### Observed behavior

Favorite reconciliation only scans non-archived conversations for stale local saved
tags. A conversation archived while favorited and then unfavorited elsewhere can retain
the local saved tag until it is unarchived.

### Evidence

- `core/data/.../repository/ConversationRepositoryImpl.kt` — explicit reconciliation
  comment near archived handling

### Acceptance checks

- [ ] Archived favorites reconcile without loading an unbounded conversation history.
- [ ] Unarchiving does not briefly show a stale favorite.
- [ ] Repository tests cover archive → remote unfavorite → sync → unarchive.

---

## UX-015 — Resolve Explicit iOS Parity Placeholders

**Priority:** P3

**Status:** `SCOUTED`

### Observed placeholders

- Password Credential Manager implementation returns no result.
- Settings avatar upload is a placeholder.
- Agent avatar picker is a placeholder.
- PDF preview reports that it is unavailable.
- Some chat attachment platform handling remains no-op or partial.

### Evidence

- `feature/auth/src/iosMain/.../PasswordCredentialManager.ios.kt`
- `feature/settings/src/iosMain/.../AvatarUploadDialog.ios.kt`
- `feature/agents/src/iosMain/.../AgentAvatarPicker.ios.kt`
- `feature/files/src/iosMain/.../PdfPreview.ios.kt`
- `feature/chat/.../viewmodel/delegate/PlatformFileHandler.kt`

### Execution note

Split this umbrella item before implementation. Each platform feature needs its own
acceptance path and should not be declared complete from Android-only verification.

---

## Standard Verification Checklist

Run the smallest relevant checks during iteration, then the broader suite before
committing:

- [ ] `git diff --check`
- [ ] Relevant module unit tests
- [ ] Relevant new regression tests
- [ ] `:app:assembleDebug`
- [ ] `:app:lintDebug`
- [ ] KMP/iOS compilation when common code or expect/actual APIs change
- [ ] Confirm `git status --short` contains only intended files
- [ ] Review the diff for accidental generated files, secrets, credentials, and unrelated
  formatting churn
- [ ] Record device/emulator verification separately

Terminal Android builds on this workstation may require explicit environment variables:

```bash
JAVA_HOME=/snap/android-studio/current/jbr \
ANDROID_HOME=/home/wolfram/Android/Sdk \
ANDROID_SDK_ROOT=/home/wolfram/Android/Sdk \
./gradlew <task>
```

## Per-Item Update Template

Copy this under the relevant item or into the session log:

```markdown
### Implementation update — YYYY-MM-DD

- Status:
- Branch / commit:
- Revalidated evidence:
- Decision:
- Files changed:
- Tests added:
- Automated verification:
- Device verification:
- Remaining risks:
- Next action:
```

## Session Log

Append entries; do not rewrite history merely because a later implementation changes the
conclusion. Correct earlier entries with a new dated note.

### 2026-07-27 — Initial persistent gap ledger

- Scouted the current `develop` branch after fetching upstream.
- Confirmed that the branch was not missing upstream commits.
- Converted the static audit into UX-001 through UX-015.
- Corrected the version-warning description: current-banner ownership is server-aware,
  while permanent suppression remains globally version-keyed.
- Ran `:app:lintDebug`: 0 errors, 26 warnings, 3 hints.
- No product source files changed.
- Next recommended action: revalidate and implement UX-002 as the first isolated change.

### 2026-07-27 — UX-007 upload streaming

- Replaced whole-file buffering in the Files screen with reopenable multipart streams.
- Added known-size preflight enforcement and cancellation-focused tests.
- Confirmed Ktor closes the streaming source after EOF, read failure, network failure, or
  request cancellation.
- Android automated verification passes; memory-constrained device stress and Apple-host
  verification remain pending.

### 2026-07-27 — UX-006 Agent editor draft protection

- Added content-aware dirty tracking and a shared top-bar/system-back discard flow.
- Restored multi-section drafts through `SavedStateHandle`, scoped to the exact create or
  edit destination.
- Cleared draft state after successful save, delete, revert, or explicit discard.
- Complete Agents unit tests and static checks pass; device navigation and process-kill
  verification remain pending.

### 2026-07-27 — UX-008 attachment and queue recovery

- Added a versioned, account-scoped draft payload for uploaded attachment references and queued
  message snapshots.
- Restored queues are always paused; inaccessible local attachments are explicitly reported lost.
- Migrated Room from v8 to v9 without invalidating existing text-only drafts.
- Complete affected unit suites and static checks pass; device force-stop/relaunch verification
  remains pending.

### 2026-07-27 — UX-010 German localization coverage

- Added a repeatable Compose resource-key coverage report and made German completeness a CI gate.
- Filled the 166 revalidated German key gaps without claiming external human review.
- Other locale gaps remain explicit English fallbacks and are reported by the same check.
- Hardcoded-string extraction plus RTL, long-string, and device review remain pending.

### 2026-07-27 — UX-011 icon-action accessibility guard

- Labeled the four revalidated icon-only actions that had disappeared from accessibility services.
- Added a narrow custom Detekt rule and isolated tests preventing null descriptions inside
  `IconButton` without making decorative icons noisy.
- Full TalkBack, font-scale, contrast, focus-order, and Compose journey checks remain pending.

### 2026-07-27 — UX-012 off-main secure-token warm-up

- Pixel 7 timing found 94–116 ms of encrypted-store setup on the cold-start path.
- Moved secure-store initialization behind the IO-backed account-readiness gate and made auth routing
  explicitly unresolved until reconciliation completes.
- Post-change first draw measured 955–1,156 ms versus 2,421–2,605 ms in the immediately preceding
  instrumented baseline; recorded frames showed no login/authenticated-content flash.
