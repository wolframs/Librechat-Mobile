# B4: Two status lines for one MCP server

Checked 2026-11-15 by gpt-6.1-sol. Live follow-up 2026-11-16 by gpt-6.1-sol.

## Question
Where do Add to chat → Connectors and Workspace → MCP servers get their status today? Can they disagree after a tool runs?

## What I looked at
- Read BRIEF.md, root CLAUDE.md (This fork), fork/README.md, the ledger's State of the re-landing through its first appendix, the dated app check, catch-up steps 7/7a, and chat/settings module instructions.
- Checkout HEAD: `0058fb60`; `git status --short` showed only untracked `reports/`. No builds or code changes.
- Paths below use `chat/` = `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/`; `settings/` = the equivalent `feature/settings/.../feature/settings/`.
- `feature/chat/CLAUDE.md:655–682`; `chat/viewmodel/delegate/McpServersDelegate.kt:79–160,190–204`; `chat/viewmodel/ChatViewModel.kt:1276,1692–1712`: status contract, reads and lifecycle wiring.
- `chat/screen/ChatPullUpSheet.kt:496–500`; `chat/viewmodel/ModelSelectionState.kt:149–153`; `chat/fork/ComposerToolsSheetContent.kt:292–321,661–679`: fork hook, offered servers, labels.
- `settings/viewmodel/McpViewModel.kt:140–248,265–282`; `settings/viewmodel/McpServerRows.kt:31–64`; `settings/screen/McpServerCard.kt:70–84`; `settings/screen/McpServersScreen.kt:124`.
- `core/model/src/commonMain/kotlin/com/garfiec/librechat/core/model/mcp/McpDisplayStatus.kt:37–67`; `core/ui/src/commonMain/kotlin/com/garfiec/librechat/core/ui/mcp/McpStatusPresentation.kt:24–35`.
- `core/network/src/commonMain/kotlin/com/garfiec/librechat/core/network/api/McpApi.kt:59,124–136`; repository getTools/getConnectionStatus implementations: network routes.
- Ran `git log --oneline 08bca78f..upstream/develop -- <delegate and display-status paths>`, compared the delegate at `08bca78f`, and read `d9afdcc9` (#885).
- Queried upstream issues, all states, with `MCP status` and `MCP connected`; read PR #885's title/state. No matching status-disagreement issue found in those results. #891 concerns OpenID session authentication.
- Called device_open(local, emulator-5554). Used its bound agent-device command to open only `dev.wsiener.mercuric.debug` with the brief's explicit MainActivity and `--relaunch`.
- Open failed with DEVICE_IN_USE. Retried once with `--wait 20000`. It failed again after 20001ms: held by session `t3-c38edfd6a471b7a7b145a156`.
- Did not reuse or close the holder. Stopped device interaction and called this thread's device_close with shutdown=false (succeeded).

## What I found
1. Chat's delegate reads `/api/mcp/servers` and `/api/mcp/connection/status`, checks missing per-user variables, folds `/api/mcp/tools` discovery authorization into the status map, and calls `mcpDisplayStatus`. It stores display values in chat state. The fork sheet consumes these; it does not fetch connection status itself.
2. The MCP servers page makes its own reads of the same routes. `McpViewModel.loadAll` starts server/status/tools loads; both arrival orders fold discovery authorization. `mcpServerRows` calls the same `mcpDisplayStatus`, with that page's reconnect set and missing-variable set. The repository methods inspected are network calls, not a shared observable status map.
3. `mcpDisplayStatus` distinguishes connected, connecting, sign-in, setup, on-demand, failed and disconnected. Missing status yields on-demand for request-scoped servers, otherwise unknown. Tool execution is not itself an input.
4. The fork sheet deliberately suppresses some labels. Attention states and pending sign-in are labelled regardless of selection; Connected is labelled only when selected. On-demand, disconnected and unknown have no status text. The management page labels every state except unknown. Therefore “Not connected” versus “Connected” cannot be the literal pair from today's fork row: its branch never renders “Not connected”. This is code evidence, not a device observation.

| Display state | Connectors sheet, from code | MCP servers page, from code |
|---|---|---|
| Connected | Connected if selected; no status line otherwise | Connected |
| Connecting | Connecting… | Connecting… |
| Needs sign-in / setup / failed | Sign-in needed / Needs setup / Can't connect | Same labels |
| On-demand | No status line | Connects when you chat |
| Disconnected | No status line | Not connected |
| Unknown | No status line | No status line |

5. They can still hold different status snapshots (inference from independent reads and refresh triggers). Chat loads initially, after sign-in/cancel/variable operations, and on return to chat. The page loads on creation, explicit refresh and later resumes. No tool-completion status reload occurs in the delegate call sites inspected. Merely reopening the sheet does not reload the connection-status map. `toolbarShown` means tool availability, not sheet visibility; its watcher can request pending discovery but is not a sheet-open status refresh. A changed server state or failed read can therefore leave different snapshots. This does not establish that today's web connector exhibits it.
6. Upstream since `08bca78f` changed this delegate only to propagate `chatSelectable = chatMenu && !consumeOnly` ([#885](https://github.com/garfiec/Librechat-Mobile/pull/885), merged 2026-11-15). Common status folding already exists at `08bca78f`; it is not a post-check fix. #885 also permits different membership: chat filters hidden/consume-only servers unless spec-locked; management lists them.
7. In the 2026-11-15 attempt, no chat was sent, no credit was consumed, and no reply or web tool execution was observed. The live server roster was not read. No per-server labels were observed on either surface, immediately or after reopening. Historical server names from November 13 are not evidence of today's roster.

## Live run 2026-11-16
- Read the binding brief and required project guidance; retained the code investigation above without repeating it. Checkout was `develop`, HEAD `33c5e811` at entry. Another agent owns concurrent code/build work; this run used the installed debug app and did not establish its exact source revision.
- Opened `local` / `emulator-5554` with `device_open`; all UI actions used its bound `agent-device` command, session `t3-0e3da3f4cf6eb24ad291d705`. Launched only `dev.wsiener.mercuric.debug` with the brief's explicit MainActivity and `--relaunch`.
- Evidence: device snapshots and visually inspected `device_screenshot` images before/after. Device clock read 2:30–2:33 (not treated as host wall time). No screenshot files were written by this run.
- On the empty chat, opened composer tools → Connectors, then closed it and opened drawer → Workspace → MCP servers. Chose the same server, `web`, for every comparison. Never tapped Connect or changed connector selection.
- Live roster: `optmem`, `web`, `imager`, `code-agent`, `audio-ears`. Connectors had `optmem` and `web` enabled, the others off. Its helper text said “Enabled connectors connect when you send a message.”
- Selected `Surplus` / `deepseek-v4.1-flash`. Sent exactly one turn: “Use web to fetch https://example.com exactly once and reply only with its page title; use no other tools.”
- Observed one `fetch_mcp_web` card reach Complete, then the reply “Example Domain”; no other tool card appeared. Chat title: “Fetch Example.com Exactly Once”. No second turn, image generation or code-agent job.

| When / server `web` | Connectors | MCP servers |
|---|---|---|
| Before the turn | Enabled; no status line | Not connected; Connect button; View only |
| First visits after the completed reply (Connectors first, management second) | Enabled; Connected | Connected; 1 tool; View only; no Connect button |
| Connectors closed and reopened after the management visit | Enabled; Connected | Most recent management observation above: Connected |

- Before the call, the surfaces differ in label visibility, not opposing status claims: an enabled switch is selection, not a connection claim. After the call, both explicitly agree on Connected. The historical “Not connected” in Connectors versus “Connected” in management did not reproduce at any sampled point.
- The first post-reply observation is on the chat created by that first send. This run does not prove that the old landing's status map refreshed on tool completion; navigation may have created a fresh chat view model (inference). The two surfaces were read sequentially, not simultaneously; no claim about unsampled transient states.
- One stale-ref tap was rejected before acting; retried with the tool's exact emitted ref and succeeded. Closed the sheets, selected drawer New Chat, verified empty composer/new landing, and called `device_close(shutdown=false)` successfully.
- No Gradle, app install/reinstall, backend shell/HTTP requests, direct database access, code edits or commits. Only this report was edited.

## Verdict
**already fine** for the scoped live `web` check on 2026-11-16: both surfaces said Connected after the successful tool call and Connectors still did after reopening. Before the call, management said Not connected while Connectors omitted status. This does not establish an upstream fix or exclude stale snapshots in other lifecycles; the static risk above remains an inference.

## Recommended action
**Nothing** for B4 from this live run. Do not change code or report an upstream bug based on a post-tool disagreement that did not reproduce. If it recurs, capture the exact server and navigation sequence before choosing a refresh fix; fork-only label changes belong in `chat/fork/ComposerToolsSheetContent.kt` under fork/README rule 2.

## Only the operator can answer
Nothing needed for this scoped check.

## Not checked
Exact installed-build/source identity; continuous or simultaneous status sampling; a tool call in an already established chat; OAuth/reconnect, failure/offline states, other servers' tool execution, iOS, release app or backend implementation. The earlier code findings describe the 2026-11-15 checkout, not the other agent's concurrent changes.
