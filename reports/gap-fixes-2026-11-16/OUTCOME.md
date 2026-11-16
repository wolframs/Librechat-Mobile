# Gap fixes of 2026-11-16: what was built, checked and left

Read with `docs/UPSTREAM_CATCHUP_PLAN.md` steps 7 and 7a and `../gap-checks-2026-11-15/SUMMARY.md`.
Per-feature detail (files, hooks) is in `docs/FORK_FEATURE_LEDGER.md`; this file holds the decisions
and what was and was not seen running. `BUILD-BRIEF.md` is the brief the builders got; reuse it.

## The operator's words (2026-11-16)

"go ahead in that order, upstream report can be checked for necessity by an opus sub agent and filed
by them if they find that the issue is not reported yet and actually upstream, spend on the phone
questions: -> yes i want them in there, Ollama: i'm indifferent about that and wouldn't spend effort
on it if it's a larger edit and test/review hassle, Emulator: you're free to use it if it's still
running from any earlier thread"

## Built (GPT-6.1-Sol builders, each diff read by the lead)

"Seen" = on the emulator, debug build of this day, test account, by the lead.

| Item | State |
|---|---|
| Prices: unknown price for this key stays unknown (G2) | Built, unit-tested. Not seen: no model with an unknown price was at hand. The lead also fixed small prices printing as `2.0E-4`; seen as `$0.0002`. |
| Tool calls by plain name (I1) | Seen: `web · fetch` running and settled, `imager · generate_image` in an old chat. |
| Readable key error in the thread (B2) | Seen on the saved reply of 13 November: "No key found. Please provide a key and try again." No "Add key" button on it (needs navigation wiring through upstream's files). Reported upstream as garfiec/Librechat-Mobile#895 (`B2-upstream-report.md`); drop the fork's hook when upstream fixes it. |
| Connector wording (G1) | Seen, English. German not looked at. |
| Search caption on `Surplus (Claude)` (G4) | Not built. The server sends nothing that tells the phone it runs its own search on a row. It would have to serve a per-endpoint `providerWebSearchReplacement: "librechat"`, derived from `lacksProviderWebSearch(resolvedBaseURL)`. Backend change, not done. |
| First model with a usable key (I2) | Built, unit-tested. Not seen: needs an install with no earlier choice, and the emulator's sign-in was left alone. No special case for Ollama (the lead's choice on the operator's "indifferent"). |
| Chat import (G5) | Seen: a text-only `.md` transcript picked from Downloads opened as a chat. JSON, transcripts with images and failure messages not tried. Fixed `maxImages` of 5. iOS: row absent. |
| Spend and trusted sellers in Prices (G2) | Seen, both views. Ordinary account: Prices unchanged. Admin (the test account, made admin for the check on the operator's word of 2026-11-16 and set back to USER afterwards): credit left, today and last 30 days, and the control. One real write: Off, then On again; "In effect" and the hidden-seller count followed, and the key ended as it began (On, source Key). A write sends only `allowUntrusted`. Inherit and a failed write were not tried. |
| MCP server cards: tool names when there is no description (I5) | Seen. `optmem` stays empty: the tool catalogue has no entry for it. |

## Emulator checks (reports in `../gap-checks-2026-11-15/`)

- B4 two status lines: agree after a real call. Nothing built.
- I5: real gap, built (above).
- B6 failed share: internals in the pop-up and a crash, but only for shares without a read grant or
  of a missing file. Nothing built ("Build for what users hit"). Upstream's code, unreported there;
  not filed, because the operator's go-ahead named the key error only.
- B3 chat missing after a failed first message: not reproducible without touching keys. Nothing
  built. The chat from 13 November is in the drawer today (seen), still titled "New Chat".

## Housekeeping

- The emulator's agent-device session of 13 November was closed by the lead on the operator's
  sentence above (idle since 14 November).
- The test account holds one imported chat, "Import Acceptance Test", from the acceptance run.
- `fork/scripts/fork-hooks.py` now leaves `reports/` alone.
