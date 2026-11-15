# B4: Two status lines for one MCP server

Checked 2026-11-15 by gpt-6.1-sol.

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
7. No chat was sent, no credit was consumed, and no reply or web tool execution was observed. The live server roster was not read. No per-server labels were observed on either surface, immediately or after reopening. Historical server names from November 13 are not evidence of today's roster.

## Verdict
**cannot tell** for the live B4 reproduction: device ownership blocked both permitted attempts. Static evidence shows shared status semantics, different label visibility and independent snapshots. It does not justify calling the live discrepancy fixed or a confirmed remaining bug.

## Recommended action
**S: rerun the scoped emulator check once the device is free**, with one new-chat deepseek-v4.1-flash prompt to fetch https://example.com using web and name its title. Record each actual server's labels on both surfaces immediately and after reopening. No code change recommended from this incomplete run. If stale status reproduces, prefer reporting the delegate refresh issue upstream; a fork-only label change belongs in `chat/fork/ComposerToolsSheetContent.kt` under fork/README rule 2.

## Only the operator can answer
Nothing about product behavior yet. Device ownership needs coordination before another run; I did not disturb the holder.

## Not checked
Live server roster/labels, model selection, web execution, reply title, reopening behavior, installed-build/source identity, OAuth/reconnect, failure/offline states, iOS, release app or backend implementation. The app could not be opened, so I could not leave it on a new chat; no app UI was changed by this run.
