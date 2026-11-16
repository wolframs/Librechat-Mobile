# B2: upstream report for the raw key-error reply

Checked 2026-11-16 by an Opus sub agent. Read-only on code; issue filed from the operator's GitHub account.

## Verified (mobile upstream/develop dc8d3dfe = 2026.11.2 + 2 commits; server = LibreChat 8d3da9bb, the submodule pin, read from a shallow clone in scratch)
- Server, stock: `openai/initialize.ts` and `getUserKey` (`data-schemas/methods/key.ts`) throw `Error(JSON.stringify({type:"no_user_key"}))`. `ResumableAgentController` has already sent `{streamId}`, so the init-error branch persists via `saveErrorTurn`: `error:true`, no content, `text` = that JSON (`request.js` ~3597-3616; `shouldPersistCodeWorkspaceInitializationError` is true once headers are sent; `getInitializationFailure` returns null for a plain Error). Nothing fork-specific in this chain.
- Client: `StreamingManagerDelegate.kt:813` parses the key error for the snackbar; the thread goes `MessageContentAndActions.kt:664` → `StreamErrorPart` → `StreamErrorType.markerOrText` (returns raw for unknown types, `StreamErrorType.kt:416`) → `localizedStreamError` (returns unmarked text as is). Key types are excluded from `StreamErrorType` on purpose (KDoc :22-24). No change to these files in af0ea66e..dc8d3dfe.
- So it holds for a plain upstream build on a stock server. Not reproduced on a device; other four types inferred from the same path.

## Existing reports
- #60 (closed Apr 2026): showed `{"type","no_user_key"}` but its cause was keys not being sent (fixed by PR #67); not about rendering a genuine key error.
- #70 / PR #81, PR #669: key UI and live snackbar mapping; the thread rendering was not covered.
- #707 / PR #837: raw errors on sign-in only. Searched issues and PRs, all states, for no_user_key, invalid_user_key, expired_user_key, user key, API key error, raw JSON, raw error, key error, persisted error, error turn, error message in chat, StreamErrorPart, UserKeyError. Nothing covers it.

## Filed
https://github.com/garfiec/Librechat-Mobile/issues/895 — no label set (template's `bug` label not applied; title carries `[Bug]:`).
Body verbatim below (it is longer than this file's 40-line budget on its own).

---
### What happened?

When a send fails because the user's own provider key can't be used, the snackbar is readable ("No key found for OpenAI. Please provide a key and try again." with "Add key"), but the failed reply saved in the thread shows the raw error payload, e.g.:

```
{"type":"no_user_key"}
```

The same applies, by the code, to `invalid_user_key`, `expired_user_key` (`{"type":"expired_user_key","expiredAt":…}`), `no_base_url` and `invalid_base_url`. After the stream ends, the app reloads the conversation, so the raw JSON is what stays in the thread once the snackbar is gone. The web client renders this saved turn as a key error with an "Add your key" / "Update your key" action.

### What did you expect to happen?

The failed reply shows the same readable, localized message the snackbar already shows for that key error (and ideally the same "Add key" action), not JSON.

### Steps to reproduce

1. Stock LibreChat with an endpoint configured `apiKey: "user_provided"`, and no key saved for it for this user.
2. In the app, pick that endpoint (or an agent on it) and send a message.
3. The snackbar says no key was found. The reply in the thread shows `{"type":"no_user_key"}`.

### Where the two paths split (`develop` at dc8d3dfe)

- Server: `initializeClient` throws `new Error(JSON.stringify({ type: "no_user_key" }))` (`packages/api/src/endpoints/openai/initialize.ts`, `getUserKey` in `packages/data-schemas/src/methods/key.ts`). The resumable controller has already sent `{ streamId }`, so it persists the turn via `saveErrorTurn` with `error: true`, no content, and `text` = that JSON (`api/server/controllers/agents/request.js`, initialization-error branch).
- Live path: `StreamingManagerDelegate` (`StreamEndReason.StreamError`) calls `parseUserKeyError` and emits the snackbar.
- Thread path: `MessageContentAndActions` → `isPersistedTurnError` → `StreamErrorPart` → `StreamErrorType.markerOrText` → `localizedStreamError`. `StreamErrorType` deliberately leaves out the user-key types (see its KDoc), and `markerOrText` returns the original string for an unknown type, so the JSON is rendered as is. `parseUserKeyError` / `userKeyErrorMessage` are never consulted here.

### How often does it happen?

Every time (by the code path).

### Platform

Both Android and iOS (the rendering is in `commonMain`). Observed on Android.

### App version

Seen once in a fork based on 2026.11.2; the same code path is unchanged on `develop` at dc8d3dfe.

### LibreChat server version

Server path read at the LibreChat commit `develop` pins (8d3da9bb, v0.8.9-rc1).

### Notes

Found by reading the code, plus that one observed occurrence. Not reproduced on a clean upstream build, and the other four key types were not triggered; they are inferred from the same rendering path.
