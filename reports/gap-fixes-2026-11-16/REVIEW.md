# Gap-fix review — 2026-11-16

Reviewed `16dae8c2..HEAD` (`92f30833`) on `develop`, excluding reports/docs from
the code diff; read the requested guidance and OUTCOME.md as context.
Source review only: no Gradle, device/emulator, server HTTP, commits or code edits.
Backend contracts checked through read-only SSH source reads on `air-agents`.

## Findings

1. **bug** — `feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/fork/chatimport/ChatImportRow.kt:32`
   Pick a `.txt` file, or import JSON successfully (no returned conversation ID).
   The effect calls `dismissStatus()` before suspending in `showSnackbar()`.
   That sets IDLE, changes `message` to null and restarts `LaunchedEffect(message)`,
   cancelling the pending snackbar. Invalid-file, failed-import and bulk-success
   feedback therefore disappears immediately or never becomes readable.
   Smallest fix: call `dismissStatus()` after `showSnackbar()` returns, so the
   status remains stable for the snackbar's lifetime.

2. **rule** — `feature/chat/src/androidUnitTest/kotlin/com/garfiec/librechat/feature/chat/viewmodel/delegate/ModelSelectionDelegateTest.kt:799`
   The existing upstream test body is replaced with a fork-specific delayed-key
   scenario through line 825. A marker comment above that body is not the single
   call/argument/dependency hook required by fork/README.md rule 3; this also puts
   new fork test logic outside rule 9's `fork/src/androidUnitTest` location.
   A future upstream edit to this test must now reconcile fork implementation
   logic, despite the registry passing (it only counts markers).
   Smallest fix: restore the upstream scenario with only the marked setup hook
   needed for its ungated endpoint config; retain policy coverage in FirstModelTest
   under `:fork`, instead of replacing the upstream test body.

## Checked and fine

- Hook registry: `python3 fork/scripts/fork-hooks.py` passes (312 hooks, 122 files).
- Added/changed English resource keys all have German counterparts.
- Unknown effective quote lanes stay unknown; small USD values print as decimals.
- Tool labels change presentation only; dispatch keeps original names and first-key intent parsing.
- Saved key-error copy reuses localized messages and the payload endpoint.
- First-model fallback waits for endpoint/key metadata and preserves higher selection tiers.
- Import MIME types, extensions, multipart fields, timezone sign and optional ID match the deployed route.
- Spend windows/credit fields and flat trust PUT, including explicit null for Inherit, match sidecar source.
- MCP summaries operate on presentation copies and preserve nonblank descriptions.
- iOS picker actual matches expect and hides the row; new common code has no obvious platform-only references.
  iOS compilation was not run and is not claimed verified.

## Resolution (lead, 2026-11-16)

Both fixed as proposed. 1: the import row dismisses its status after the snackbar. 2: upstream's
test body is restored with one marked setup line; the fallback policy stays covered in `:fork`
(`FirstModelTest`), and the key-wake path was seen working on the emulator.
