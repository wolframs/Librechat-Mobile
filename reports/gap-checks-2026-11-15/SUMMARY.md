# Gap checks of 2026-11-15: summary

Read with `docs/UPSTREAM_CATCHUP_PLAN.md` steps 7 and 7a. One file per point is in this folder; each
was written by a GPT-6.1-Sol agent from the code of both sides (app `develop` at `0058fb60`, backend
fork `local-features` at `6f42475e`), without builds, device runs or server requests unless its row
says otherwise. The lead (Claude) read every file; the order of work below is the lead's
recommendation, not a decision of the operator's.

State: 8 of 12 points are complete. The four that need the emulator (B4's live half, I5, B6, B3) are
not run: the emulator was held by another thread's device session (see `PLAN.md`).

## Verdicts

| Point | Verdict | What it would take |
|---|---|---|
| G1 memory and web switches | Works. Wording gap only: the Connectors page does not say the switches apply to this chat, nor what `optmem` and `web` are. A switch-off saved from the web reaches the phone at the next load of that chat from the server, not live. | S: two sentences, English and German. |
| G2 routing and cost data | Prices and per-turn cost exist. **Bug in the fork's Prices sheet:** when the sidecar says this key's price is unknown, the sheet shows the whole market's best price under the heading for this key (`fork/market/MarketPrice.kt:82`). Missing: credit and spend at a glance, the trusted-sellers switch. Full accounting stays a desk tool. | S: the price fallback. M, optional: an admin spend card and the trust switch. |
| G3 PDFs as text | Fine. The app uploads a PDF the plain way on every row and the server turns it into text on both Surplus rows. The old fork's provider-routing expressions are not needed for this; restoring them as the ledger's appendix describes would even pick the wrong file policy on `Surplus (Claude)`. | Nothing in code. |
| G4 web fetch and search | Fetch and the choice of search are fine on both Surplus rows. Two labels are wrong: the search control on `Surplus (Claude)` can read "Anthropic's built-in search" while the server runs its own, and tool calls show wire names (same as I1). | S: the caption. I1 covers the rest. |
| G5 Markdown import | Real gap: the app has no chat import at all (no screen, no picker); the plumbing below it exists and the server route is there. | M: an Import row under Privacy and data, a picker, upload with `maxImages` and the time zone, then open the imported chat. |
| B2 raw key error | Still there, in upstream too. The pop-up is readable, the saved reply in the thread prints the raw object, for all five key and base-URL error types. | M, and worth reporting upstream. |
| I1 tool calls by plain name | Real gap. A live connector call shows `fetch_mcp_web`; a settled one shows the model's own description when it gave one. The app already writes "web · fetch" in one place (code-run traces). | M: one naming function used by every card. |
| I2 first model that can answer | Real gap, and the web has it too. With no earlier choice the app takes the first listed model, OpenAI on this server, without looking at the key state it already holds. | M: a key-aware last tier in the fork; upstream's spec seeding stays untouched. |
| B4 two status lines | Code half only: the fork's Connectors page can no longer print "Not connected"; the two surfaces still read status separately and neither re-reads after a tool ran. Live comparison not run. | Emulator run first. |
| I5, B6, B3 | Not started. | Emulator. |

## Order the lead recommends

1. The Prices fallback (S): it shows a wrong number today.
2. Tool-call names (M): seen in every chat with connectors; also settles half of G4.
3. The raw key error in the thread (M), with an upstream report.
4. Connector wording and the search caption (S each).
5. First model with a usable key (M): matters on a fresh install only.
6. Markdown import (M): new feature; default of five images first, the web's "last N" prompt later.
7. Spend card and trust switch (M): only if the operator wants them on the phone.

## Only the operator can decide

- Whether the raw key error is reported upstream (an issue on garfiec's repository).
- Whether credit and spend, and the trusted-sellers switch, belong on the phone at all.
- Whether a fresh install may open on the local Ollama row when no provider has a key.
