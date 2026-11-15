# I1: Tool calls by their plain name

Checked 2026-11-15 by gpt-6.1-sol (high).

## Question
How do live and settled MCP calls get their titles, and where should one shared naming rule live?

## What I looked at
- Read `BRIEF.md`, root `CLAUDE.md`, `fork/README.md`, the ledger's state-of-re-landing section through Appendix A, `APP_CHECK_2026-11-13.md`, `UPSTREAM_CATCHUP_PLAN.md`, and `feature/chat/CLAUDE.md`.
- Mobile HEAD `0058fb60`; community upstream/develop `af0ea66e` (v2026.11.2).
- Component paths below are relative to `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/components/`.
- `ToolCallContentPart.kt:95-110,189-199,354-361,416-483`; `StreamingToolCallCard.kt:68-94,287-342,433-446`.
- `ToolLabels.kt:23-66`; `PtcToolTrace.kt:94-113`; `ToolCallParsing.kt:33-51,453-485,725-733,785-830`.
- `McpResourceCarousel.kt:35-39,48-65,71-112`; `McpSignInToolCallCard.kt:42-77`.
- Module rules: `feature/chat/CLAUDE.md:912-934` (Activity groups) and `1046-1048` (Tool intent labels).
- The local official `upstream/` submodule directory is empty. Read official commit `8d3da9bbb220a0a96be9330a2e1cbd385fb786d5` (the app's `UPSTREAM_VERSION`) using `ssh air-agents '... git show <commit>:<path>'` from `~/projects/librechat`.
- Official web paths: `client/src/components/Chat/Messages/Content/ToolCall.tsx:89-140,238-255,331-407,414-469`; its `Parts/intent.ts:19-30,50-89`; `client/src/utils/toolLabels.ts`; `client/src/locales/en/translation.json:364,387,1691-1692,3578`.
- Read Air docs `README.md`, `fork-customizations.md`, `follow-ups.md`; compared its current ToolCall source with the official commit rather than treating fork source as official.
- Ran `git log --oneline 08bca78f..upstream/develop -- <component paths> feature/chat/CLAUDE.md` and `git diff 08bca78f upstream/develop -- <title/parser paths>`.
- Searched open/closed community upstream issues using `gh issue list --repo garfiec/Librechat-Mobile --state all`: `MCP tool name`, `intent label`, `"tool" "name"`, `"tool calls"`.

## What I found
1. The dispatcher selects `toolCall.name`, then `function.name`, then a localized fallback. It classifies using the original name. It computes `displayName = intent ?: toolName` and passes that to generic settled cards. It never calls `parseToolName` there (`ToolCallContentPart.kt:106-110,189-199,354-361,394`).
2. Ordinary live MCP calls fall through to `GenericStreamingToolCard`. Its title is a preparation label or **raw `toolCall.name`**, without reading intent (`StreamingToolCallCard.kt:287-342`). This remains true after that call completes while the overall reply still streams.
3. `preparingToolLabel` explicitly excludes names containing `_mcp_` or `_action_` (`:439-446`). An ordinary `fetch_mcp_web` stays exactly that during preparation, execution and live completion. The Activity groups rule says MCP/action tools keep their name; it does not make the name readable (`CLAUDE.md:929-930`).
4. Settled generic card without valid intent: `fetch_mcp_web` or `generate_image_mcp_imager`, with the generic Build icon. With valid intent: that intent is the title, and the lifted field is removed from the function-arguments dump. Live generic card, with or without intent: wire name. Finalization can therefore change a wire name into a sentence (`ToolCallContentPart.kt:189-199,450-483`; streaming `:336-338`). These are source-derived predictions, not new device observations.
5. Valid mobile intent means a nonempty trimmed string in the **first key** of a parsed JSON object. A later `intent` is an ordinary parameter and must not retitle the card. Mobile parses JSON; it lacks web's explicit prefix extractor for incomplete streamed args (`ToolCallParsing.kt:785-830`; module rule `:1046-1048`).
6. MCP has no universal dedicated call-title card. The settled `contains("mcp")` branch substitutes a resource carousel when output parses as resources/contents or a resource array; each card shows the resource title/name, URI and preview. Otherwise it uses the generic call card. Resource titles do not use intent (`ToolCallContentPart.kt:354-361`; parser `:453-485`; carousel `:35-112`).
7. Live OAuth hold is another exception: it shows a sign-in message naming `authServerName` (or an unnamed fallback), a Lock icon and the sign-in action. This is not the normal tool title (`StreamingToolCallCard.kt:68-78`; sign-in card `:42-77`). MCP App rows are separate output surfaces after the call, not its title.
8. The suffix is understood elsewhere. `parseToolName` splits at the **last** `_mcp_`: before it is the tool, after it is the server. Only native names receive the friendly-name resource map; unknown names stay raw. Its only component caller found is `PtcTraceLine`, which already produces `server · tool`: `web · fetch` and `imager · generate_image` (`ToolLabels.kt:23-66`; `PtcToolTrace.kt:94-99`). Ordinary cards bypass it. Do not strip the suffix and then classify an MCP tool as a native tool of the same name.
9. Official web at the pinned reference splits with `splitToolCallName(name, knownServerNames)`. The function supplies the title subject; the server supplies a subtitle and configured MCP icon. In English, no intent gives `Running fetch`, `Calling fetch` when dispatched, then `Ran fetch` on success, with `in web` beneath. Preparation may show preparation text or `Preparing fetch`. The image call's subject is `generate_image`, its server `imager`. This separates tool/server but is not literally the single string `web · fetch` (web ToolCall `:89-140,331-407,445-465`; English strings above).
10. Web's first-key intent extractor accepts partial streamed strings. Ordinarily intent supplies the live label and persists on success; preparation/calling phases may replace or wrap it. Failure uses an error subtitle ahead of server attribution. Thus “web always shows just intent” would be inaccurate (web intent `:19-30,50-89`; ToolCall `:331-407`).
11. No community upstream fix since 13 November in the checked range. The only live-card diff concerns OAuth action ids and search recomposition. Settled title, parser and ToolLabels files have no diff. Resource and PTC trace files also have no commits in the range. Existing parsing/intent support predates the check. Issue searches found no matching naming issue; #854/#856 concern action OAuth. Search results do not prove that no differently worded issue exists.

## Verdict
**real gap.** Normal cards still leak MCP wire names. Intent masks this only on the settled generic path; live and settled naming disagree. The existing PTC trace already expresses the desired fallback.

## Recommended action
**M (a few hours): one shared call-title resolver used wherever a card names a call.**
- Put a fork-only bridge at `feature/chat/src/commonMain/kotlin/com/garfiec/librechat/feature/chat/fork/ToolCallLabels.kt`. It needs this module's internal parser, `ParsedToolName` and friendly-name resources, so `fork/README.md:20-24` rule 2 applies. Reuse these helpers; do not copy maps into cards or rename transport data.
- Rule: valid first-key intent wins; otherwise MCP becomes `server · tool`; native tools use the existing localized friendly name, unknown names stay raw. Preserve the tool half verbatim, including underscores. Keep preparation/outcome wording outside identity parsing and preserve the MCP/action preparation exclusion.
- Thin fully qualified `FORK-HOOK(tool-labels)` calls replace the settled `displayName` expression and live generic title resolution. Route PTC's local concatenation through that resolver too. Every specialized card needing a call-name fallback must use the same function. Resource names, sign-in instructions, activity labels and output titles keep their own semantics; they are not call identities.
- Keep dispatch on original names. This must not alter `toolCall.name`, call a renamed tool or change output classification. No backend change is needed.
- Only marked call/argument hooks in upstream files; update `fork/hooks.txt` and the ledger with implementation (`fork/README.md:25-31`). New strings go in fork English/German resources. Server-mark lookup is a separate card input; this report does not design it.
- Acceptance: both named MCP examples have identical readable fallback titles live/settled; first-key intent behaves identically; later-key intent stays an argument; native/unknown names and resource/OAuth paths retain their meanings. No code or tests were added in this research pass.

## Only the operator can answer
Nothing blocks this seam. Assumption for implementation: preserve existing first-key intent precedence rather than force server/tool text over model-authored intent.

## Not checked
No emulator, builds, paid chats, backend HTTP requests, database reads/writes, deployment or code edits. No rendered acceptance or iOS run. Web findings describe the exact official reference above, not today's moving main or the deployed browser bundle. Configured server marks and stored arguments from the 13 November chats were not inspected.
