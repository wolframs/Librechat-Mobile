# I5: Empty description space on MCP server cards

Checked 2026-11-16 by gpt-6.1-sol.

## Question
What fills the empty space under MCP server names, what data is available, and has upstream changed it?

## What I looked at
- Read BRIEF.md and its required project/fork/app-check/catch-up documentation.
- Mobile committed `HEAD` = `987a8c2f5a6b199a766a6ff6747576244b45922e`, branch `develop`; used `git show HEAD:<path>` for code. Another agent's dirty code was left alone.
- `feature/settings/CLAUDE.md`, `core/{model,network,data}/CLAUDE.md`.
- Settings paths below are relative to `feature/settings/src/commonMain/kotlin/com/garfiec/librechat/feature/settings/`.
- `screen/McpServerCard.kt:122-148`; `viewmodel/McpServerRows.kt:13-26,39-58`; `viewmodel/McpViewModel.kt:143-152,172-177,229-235`; `screen/McpToolsSheet.kt:40-42,65-80,90-109`.
- `core/network/src/commonMain/kotlin/com/garfiec/librechat/core/network/api/McpApi.kt:46-84,129-159`.
- Air read-only via `ssh air-agents`: LibreChatDocs README, fork-customizations, follow-ups; backend branch `local-features`, HEAD `51d9b239d573ebcec6198abc96a5ebba07bdc547`.
- Backend committed `api/server/controllers/mcp.js:130-267,338-349`; `packages/api/src/mcp/utils.ts:718-742,784-793`; web `client/src/components/SidePanel/MCPBuilder/MCPServerCard.tsx:49-51,143-147`.
- `git log --oneline 08bca78f..upstream/develop -- <card, row mapper, API paths>`; current upstream card via `git show upstream/develop:<path>`.
- GitHub API confirmed mobile upstream `develop` = `dc8d3dfec2fc4657f90ca9e89253074c8e488ad4`; official web `main` = `9363db541ecd4e23ab6a41dca47ca48ee2bd418e`. Read official web card with `gh api .../contents/...`.
- `gh issue list --repo garfiec/Librechat-Mobile --state all --search ...` with `MCP description`, `MCP card`, `MCP blank`, `MCP "description" in:title`, and `"server cards"`; read #773.
- Pixel_7, Android 17, local/emulator-5554: opened installed debug app with the explicit MainActivity; drawer → Workspace → MCP servers → View all tools; scrolled tools once. Inspected device screenshots, not just accessibility text (the tools sheet's tree exposed only its drag handle).
- Closed the tools sheet, returned to the empty new-chat composer, and called `device_close(shutdown=false)`.

## What I found
- **The blank space is real today.** All five cards (audio-ears, code-agent, imager, optmem, web) have blank space between title and status. The screenshot matches the card's deliberate `Text(row.description.orEmpty(), minLines = 2, maxLines = 2)` reservation. This is two description lines, not an unrendered tools row (`McpServerCard.kt:129-138`).
- **Server description is supported already.** `/api/mcp/servers` returns sanitized configs; the backend allowlists `description` even for view-only users (`mcp.js:345-349`, `utils.ts:722-742`). Mobile parses it, drops blank descriptions, and renders it directly (`McpApi.kt:145-146`, `McpServerRows.kt:46`). Inference: this account's effective descriptions are absent/blank, because none rendered. I did not capture raw authenticated responses or read live secret-bearing YAML.
- **Tool names and tool descriptions are available separately.** `/api/mcp/tools` returns `{servers: {name: {tools: [{name, pluginKey, description, ...}]}}}` (`mcp.js:209-218,235-267`). Mobile retains each tool's name, description, and server association (`McpApi.kt:61-84`). These tool descriptions are not server descriptions.
- **Tool count is derived, not a required wire field.** The VM groups that catalogue by server and counts it (`McpViewModel.kt:148`). The card already displays a positive count, but only when status is CONNECTED (`McpServerCard.kt:143-145`). On screen, web says “Connected”, “1 tool”, “View only”. Others show no count despite catalogue entries for three of them.
- **Actual catalogue seen without connecting or chatting:** web: `fetch` (1); imager: `get_user_images`, `generate_image` (2); code-agent: `request_fix`, `check_fix`, `resume_fix`, `add_note`, `list_fixes`, `pause_fix`, `archive_fix` (7); audio-ears: `get_user_audio`, `listen_to_audio` (2). No optmem group appeared. Treat missing catalogue entries as unavailable/unknown, not proof that the server has zero tools.
- The all-tools sheet visibly includes descriptions for fetch and code-agent tools; imager and audio-ears entries show names only. The card row model carries `toolCount`, but no tool-name list (`McpServerRows.kt:13-26`). A name fallback needs that extra presentation input; no new endpoint is necessary.
- **Mobile upstream has not fixed this since 13 November.** Current upstream card has the same two-line blank reservation and connected-only tool count. The scoped history contains only `d9afdcc9` (chat-menu filtering/API), no description/card fallback change. Issue searches found no matching empty-card-space report; #773 concerns custom user variables, not this gap. Search absence is not proof no issue exists.
- **Official LibreChat web today:** the card reads `config.description` and conditionally renders one truncated paragraph under the name (`MCPServerCard.tsx:50-51,145-147` at official main above). It does not reserve an empty description paragraph or substitute tool names/count. The Air fork's committed web card follows the same rule. This is source inspection, not a live web render.

## Verdict
**real gap** — a small presentation gap, still present upstream. Description support exists; missing descriptions leave intentionally reserved space unused. Tool names can fill it with data already loaded. Tool count alone mostly repeats the existing status row.

## Recommended action
**S (under an hour):** prefer a nonblank server description; otherwise show a capped, two-line list of available tool names. If neither exists, omit the empty slot in a single-column list (retain uniform reserved height where a grid needs it). Put the fallback presenter/card in `:fork` under `com.garfiec.librechat.fork.mcpcards`, taking plain values and callbacks. Use a marked `FORK-HOOK(mcp-card-summary)` at the settings call site; any adapter requiring `McpServerRow` belongs in that module's `fork` package. Follow `fork/README.md` for hooks/registry and English/German strings. Do not hardcode descriptions for these five server names. No implementation made here.

## Only the operator can answer
Nothing blocks the check. The operator may prefer authored server descriptions or a compact card over tool-name fallback.

## Not checked
- Raw live response bodies, live YAML descriptions, or every server's complete tool descriptions; no reconnect, chat turn, tool execution, or server mutation.
- Installed APK-to-source identity was not independently established; this is the existing debug build, with committed source inspected separately.
- iOS, tablet/grid rendering, Liquid Glass, or live web UI. No Gradle, build, install/reinstall, code edits, or commits.
