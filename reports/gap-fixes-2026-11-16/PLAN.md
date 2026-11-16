# Plan: build the gap fixes, finish the emulator checks (started 2026-11-16)

Read before continuing. Tick a line when it is done and verified. Delete this file when every line
is ticked; what outlives it goes to `docs/UPSTREAM_CATCHUP_PLAN.md` and the ledger first.

## What the operator said (2026-11-16, verbatim, in reply to the summary of 15 November)

"go ahead in that order, upstream report can be checked for necessity by an opus sub agent and filed
by them if they find that the issue is not reported yet and actually upstream, spend on the phone
questions: -> yes i want them in there, Ollama: i'm indifferent about that and wouldn't spend effort
on it if it's a larger edit and test/review hassle, Emulator: you're free to use it if it's still
running from any earlier thread"

## The lead's working choices (open to overrule)

- Builders: GPT-6.1-Sol, reasoning high, one per item, one after another in the shared checkout
  (they share `fork/hooks.txt`, the strings files and the ledger). Brief: `BUILD-BRIEF.md`. Each
  commits on `develop` locally; the lead reads the diff, then starts the next. No emulator for
  builders; the lead installs and checks the lot on the emulator at the end.
- Emulator checks run alongside, one after another, with `../gap-checks-2026-11-15/BRIEF.md`.
- The emulator was held by agent-device session `t3-c38edfd6a471b7a7b145a156` (13 November, idle
  since 14 November 22:47). Closed by the lead on 2026-11-16 on the operator's sentence above.
- Ollama: no special case. The first-model rule is "needs no user key, or has one stored"; whatever
  that gives for the Ollama row stands.
- Spend on the phone: the admin-only summary and the trusted-sellers switch from G2's "M" option.
  Spend for ordinary accounts needs new server routes and is not part of this.
- Markdown import: default of five images, no "last N" prompt.
- The search caption (G4): fixed only if the phone can tell from what the server already sends. No
  backend change in this plan.
- The raw key error is fixed in the fork whether or not upstream takes the report.

## Build, in the operator's order

- [x] 1. Prices sheet: unknown price for this key is shown as unknown (G2, S)
- [x] 2. Tool-call names: one naming function for every card (I1, M; settles half of G4)
- [ ] 3. Raw key error in the thread (B2, M)
- [x] 3u. Upstream report for B2: Opus agent checks whether it is upstream's and unreported, files it if so Filed as garfiec/Librechat-Mobile#895; see `B2-upstream-report.md`.
- [ ] 4. Connector wording (G1, S) and the search caption (G4, S)
- [ ] 5. First model with a usable key (I2, M)
- [ ] 6. Markdown import (G5, M)
- [ ] 7. Spend card and trusted-sellers switch (G2, M)
- [ ] 8. MCP server cards: tool names fill the empty line when the server has no description (I5, S;
      added 2026-11-16 from the emulator check, the lead's choice)

## Emulator checks (result files in `../gap-checks-2026-11-15/`)

- [x] B4 live half → `B4-mcp-status-lines.md` (add to the file)
- [x] I5 → `I5-mcp-server-cards.md`
- [ ] B6 → `B6-failed-share.md`
- [ ] B3 → `B3-failed-first-message.md`

## Afterwards (lead)

- [ ] Build, install the debug app, check items 1 to 7 on the emulator as the test account
- [ ] Update `docs/UPSTREAM_CATCHUP_PLAN.md` steps 7 and 7a, `../gap-checks-2026-11-15/SUMMARY.md`
      and `PLAN.md` there (delete that plan when its four lines are ticked); push
- [ ] Decide B4, I5, B6, B3 from their results; build what is small

## State

- 2026-11-16 14:35: plan written, emulator free.
- 2026-11-16 14:40: started through T3 `delegate_task` (a completion wakes the lead's thread; no
  polling): build 1 (Sol), emulator check B4 (Sol), the upstream report (Opus 5.5, high; writes
  `B2-upstream-report.md` here). Next builder starts when the one before is read and accepted;
  next emulator check (I5, then B6, then B3; their questions are in
  `../gap-checks-2026-11-15/PLAN.md`) when the one before reports.
