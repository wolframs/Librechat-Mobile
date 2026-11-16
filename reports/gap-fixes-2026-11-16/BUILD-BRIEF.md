# Brief for one build item

You build one item. The item and its report are named in your task.
Other agents work in this checkout at the same time, but only on the emulator and on files under
`reports/`; you are the only one changing code.

## Read first

`CLAUDE.md` ("This fork" and "Writing code here"), `fork/README.md` (the rules for where fork code
lives and what a hook is: they are binding), `docs/FORK_FEATURE_LEDGER.md` from "## State of the
re-landing" to the first appendix, the report for your item in `reports/gap-checks-2026-11-15/`
(its findings carry file and line numbers; check them against today's code before relying on
them), and the `CLAUDE.md` of every module you edit.

## The backend

Source of Wolfram's LibreChat fork: `~/projects/librechat` on his MacBook Air, branch
`local-features`. Reach it only as `ssh air-agents '<command>'`, read only (`cat`, `sed -n`, `grep`,
`git log`, `git show`). No writes, no docker, no deploy. `upstream/` inside this repo is the official
LibreChat, not his fork. No backend change is part of your item: if the item cannot be done without
one, stop and say so.

## Limits

- Stay inside your item. Smallest change that does it; reuse before adding. No drive-by refactors.
- No emulator, no `adb`, no device tools: another agent is on the emulator.
- No HTTP requests to the LibreChat server from curl or a script (a request without a browser
  User-Agent can ban the test account and this machine's address).
- No sub agents. No pushes. No branch switches: work on `develop`.
- Tests: only for logic a plausible edit would break, in the class's existing test file (a new test
  file only for a new class; fork tests in `fork/src/androidUnitTest`). A handful, not dozens. Run
  only the test tasks of the modules you touched, never the whole suite.
- Strings in English and German. Comments state present-tense rules, no history.

## Before you finish

1. `fork/scripts/fork-hooks.py --write` if you added or removed a hook, then `fork/scripts/fork-hooks.py`.
2. `./gradlew :app:assembleFossDebug` succeeds, and the touched modules' unit tests pass
   (`./gradlew :feature:chat:testDebugUnitTest` whenever you touched a hook in `feature/chat`).
   If detekt runs in those tasks and complains about your code, fix it.
3. Update `docs/FORK_FEATURE_LEDGER.md` (the re-landing section) for your feature, in the same commit.
4. One commit on `develop` (two if a second is clearly its own thing), message in the style of
   `git log --oneline -15`, ending with the line `Co-Authored-By: GPT-6.1-Sol <noreply@openai.com>`.
   Commit only your own files; leave `reports/` alone.
5. Final reply to the lead, at most twelve lines: what a user now sees, the files and hooks you
   added, what you ran and its result, what is untested or left out and why, and what the lead
   should look at on the emulator to accept it (screen, steps, expected text).
