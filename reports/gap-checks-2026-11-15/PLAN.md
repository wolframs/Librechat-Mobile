# Plan: gap checks, one agent per point (started 2026-11-15)

Read before continuing. Tick a line when its result file exists and has been read by the lead.
Delete this file when every line is ticked and the lead's summary (`SUMMARY.md`) is written.

## What the operator asked (2026-11-15, verbatim)

"I'm leaning towards letting Sol agents work through each point individually, serially, to inspect
gaps and run app-checks where we're still missing data. Many things might resolve to a clear
direction of sensible action to take once their results are back with you. Actually, serially is
only sensible for any of them that might plausibly need the emulator for tests, the others can
launch read only in parallel, as long as they have permission to write their result file somewhere"

## How

- Agents: GPT-6.1-Sol, reasoning high, one per point, no code changes, each writes one result file
  here. The shared brief is `BRIEF.md`.
- Read-only points start together. Points that may need the emulator run one after another.
- Afterwards the lead reads every file, writes `SUMMARY.md` (per point: verdict and the action it
  recommends, with what only the operator can decide), updates `docs/UPSTREAM_CATCHUP_PLAN.md`
  steps 7 and 7a, and commits this folder. Fixes are a later step, not part of this plan.

## Read-only, in parallel

- [x] G1 memory and web switches → `G1-memory-web-switches.md`
- [x] G2 routing and cost data → `G2-routing-cost-data.md`
- [x] G3 PDFs as text → `G3-pdfs-as-text.md`
- [x] G4 web fetch and web search on Surplus → `G4-web-fetch.md`
- [x] G5 Markdown import → `G5-markdown-import.md`
- [x] B2 raw `no_user_key` error → `B2-raw-key-error.md`
- [x] I1 tool calls by plain name → `I1-tool-call-names.md`
- [x] I2 first model should be one that can answer → `I2-first-model.md`

## With the emulator, one after another (in this order)

- [ ] B4 two status lines for one server → `B4-mcp-status-lines.md`
- [ ] I5 empty line on MCP server cards → `I5-mcp-server-cards.md`
- [ ] B6 a failed share shows internals → `B6-failed-share.md`
- [ ] B3 a chat whose first message failed is missing from the drawer → `B3-failed-first-message.md`

## State

- 2026-11-15 17:50: the eight read-only agents and B4 are started (T3 `delegate_task`, provider
  `codex`, model `gpt-6.1-sol`, `reasoningEffort: high`; each task text is "read BRIEF.md, your
  point is X, result file Y" plus the question). When B4 reports, start I5, then B6, then B3, each
  only after the one before has finished. A completion wakes the lead's thread; no polling.
- Questions for the three not yet started, so they need no re-deriving:
  - I5: the MCP servers page (drawer, Workspace, MCP servers) showed an empty line under each
    server's name. Read the card's code, look at the page on the emulator, say what the server
    sends that could fill it (description, tool names) and what upstream shows there today.
  - B6: a file shared into the app from another app failed and the message named the package and a
    content address, the chip was titled with the file's id. Read the share-intake path; try a share
    into the debug app on the emulator if it can be done without a file picker (an `am start` with
    `ACTION_SEND` is a launch through `agent-device`, not adb); report what a user sees on failure.
  - B3: a chat whose first message failed was missing from the drawer and kept the title "New
    Chat" while the server held it. Read how the list and the title update after a failed first
    turn and what upstream changed since (#872, #887 and neighbours); reproduce only if a failing
    first message can be made cheaply and without breaking the test account's keys.
- 2026-11-15 18:20: G5 is back and read. B4 is back with the code half only: the emulator could
  not be opened (`DEVICE_IN_USE`, held by agent-device session `t3-c38edfd6a471b7a7b145a156`, made
  on 13 November, most likely by the earlier thread "Mobile App Drift Assessment"; the lead's own
  open with `--wait` failed the same way at 18:20, though it had worked at 16:55). Another session
  is not the lead's to close. The four emulator points (B4's live half, I5, B6, B3) wait until the
  operator frees the device or says to close that session. B4's line stays unticked until then;
  its rerun needs only the emulator steps, the code reading is in its file.
