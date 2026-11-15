# Brief for one gap check

You are one of several agents, each checking one point. Your point and your result file are named
in your task. You inspect and report; a lead agent decides what gets built afterwards.

## The project

Mercuric is Wolfram's fork of the community LibreChat mobile app (`upstream` remote =
garfiec/Librechat-Mobile). Repo: `/home/wolfram/Projects/Librechat-Mobile`, branch `develop`.
Read first: `CLAUDE.md` (the "This fork" section), `fork/README.md` (where fork code may live and
what a hook is), `docs/FORK_FEATURE_LEDGER.md` from "## State of the re-landing" to the first
appendix (what the fork has today and how it is built), `docs/APP_CHECK_2026-11-13.md` (the run
the B and I points come from), `docs/UPSTREAM_CATCHUP_PLAN.md` (steps 7 and 7a are this work).
Each module has its own `CLAUDE.md`; read the one for the module you end up in.

## The backend

The app talks to Wolfram's own LibreChat fork, deployed on his MacBook Air.

- Source: `~/projects/librechat` on the Air, branch `local-features`. Docs: `~/LibreChatDocs` on the
  Air (start with `README.md`, then `fork-customizations.md` and `follow-ups.md`).
- Reach it only as `ssh air-agents '<command>'`. Never use another host name for the Air: those
  need a hardware-key touch from Wolfram. Read only: `cat`, `sed -n`, `grep`, `git log`, `git show`,
  `git diff`. No writes, no checkout, no docker, no deploy or restart scripts.
- `upstream/` inside the mobile repo is the official LibreChat, not his fork. `~/Projects/LibreChatDocs`
  on this machine is an August copy: do not use it.

## Limits (all agents)

- No code changes, no commits, no branch switches. The only file you write is your result file.
- No `./gradlew`: another agent may be on the emulator, and builds get in each other's way.
- No HTTP requests to the LibreChat server from curl or a script: a request without a browser
  User-Agent can ban the test account and this machine's address on the first try.
- No database writes anywhere. No sub agents. Search where the thing is likely to be, not across `~`.
- `gh` is fine for reading upstream (`gh issue list/view --repo garfiec/Librechat-Mobile`,
  `gh pr view`). Do not file, comment on or edit anything there.
- Upstream moves fast. Before calling something a gap or a bug, check whether upstream changed it
  since the app check: `git log --oneline 08bca78f..upstream/develop -- <paths>` (all of that is
  already merged into `develop`), and look for an open or closed upstream issue.

## If your task says you may use the emulator

- Device: `hostId` `local`, device id `emulator-5554`. Call the T3 tool `device_open` with both and
  drive the app only with the `agent-device` command it returns (taps, text, screenshots). `adb` is
  for logs only (`adb -s emulator-5554 logcat`). Never address any other device serial.
- App: the debug build `dev.wsiener.mercuric.debug`, current `develop`, signed in as the dedicated
  test account ("Claude Test"). Launch it with
  `open dev.wsiener.mercuric.debug --activity dev.wsiener.mercuric.debug/com.garfiec.librechat.MainActivity --relaunch`
  (without the activity a system chooser appears, because the app has two launcher entries).
  Do not sign out, clear app data, uninstall or reinstall. Leave the release app
  `dev.wsiener.mercuric` on the same emulator alone.
- `agent-device` gotchas: `snapshot` prints only "unchanged" when nothing changed, so add
  `--force-full` whenever you parse it; `open` failing with `DEVICE_IN_USE` is cured by `--wait 20000`,
  never by closing another session; `back` with the keyboard up only hides the keyboard; check which
  field is focused before typing.
- Cost: every chat turn bills real credit. Use `deepseek-v4.1-flash` for plain text and `Haiku 5.5`
  only where a Claude model is needed; one-line prompts; at most six turns in total. No image
  generation, no code-agent jobs.
- Server-side lookups, only if your point needs to know what the server holds for a test chat:
  `ssh air-agents "PATH=\$PATH:/usr/local/bin:/opt/homebrew/bin:\$HOME/.orbstack/bin; docker exec chat-mongodb mongosh --quiet LibreChat --eval '<a find on the test account's own records>'"`.
  Finds only.
- When you are done: close any open sheet, leave the app on a new chat, and call `device_close`
  for the emulator without shutting it down.

## The result file

Write it to the path in your task, at most 100 lines, in this shape:

```
# <point id>: <title>

Checked 2026-11-15 by <your model>.

## Question
## What I looked at        (files with line numbers, commands, device steps)
## What I found            (facts, each with its evidence; mark anything inferred as inferred)
## Verdict                 (one of: already fine / fixed by upstream since / real gap / cannot tell, and why)
## Recommended action      (what to do and where it would live under fork/README.md's rules, with a
                            size: S under an hour, M a few hours, L a day or more; or "report upstream";
                            or "nothing")
## Only the operator can answer
## Not checked
```

Write plainly: short sentences, no filler. State only what you looked at; name what you did not.
Your final reply to the lead is at most five lines: the verdict, the recommended action in one
sentence, and the file's path.
