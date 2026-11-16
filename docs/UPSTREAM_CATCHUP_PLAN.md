# Upstream catch-up plan (started 2026-11-13)

Read before continuing the catch-up with the community app (`upstream` =
garfiec/Librechat-Mobile). Tick a step here when it is done and verified.
Delete this file when the last step is ticked.

## Where we stand (measured 2026-11-13)

- Fork `develop`: 43 commits ahead of the merge base `7b2311af` (2026-08-28),
  368 behind `upstream/develop` at `08bca78f`.
- Fork delta: 322 files, about 10,500 added lines; 66 files are fork-only,
  256 are edits inside upstream's files.
- Trial merge: 278 conflict hunks in 158 files (137 content, 20 where upstream
  deleted the file we edited, 1 add/add on the Room schema `10.json`).
- Upstream pace: 29 commits in September, 233 in October, 106 by 12 November.
- Room: the fork's v10 is fork-only; upstream has its own v10 to v15.

## Operator's rule (2026-11-13)

"If the merge from upstream is a pain, that means we start with refactoring the
code so the merges become easier. If the code is not a problem, we merge
upstream in next." Local data loss on the phone is acceptable; the server holds
what matters.

## Working choice (the agent's, open to overrule)

The merge is a pain, and for a structural reason: fork features live inside
upstream's busiest files, so every later sync repeats it. Refactoring on the old
base would mean refactoring against host files upstream has since deleted or
rewritten. So the refactor happens during the catch-up: upstream's version of
every upstream-owned file is the base, and each fork feature is re-landed on it
in an isolated form (fork-only files plus thin hooks). Features upstream has
built itself are dropped.

## Steps

- [x] 1. Measure the merge.
- [x] 2. Fork feature ledger: per feature, where it lives, upstream's
      counterpart, verdict (drop / keep clean / port behind a seam), size.
      Result: `docs/FORK_FEATURE_LEDGER.md`.
- [x] 3. Operator pass (2026-11-13). His words: saving the login data and
      server URL is "an absolute must have, if no one upstream has implemented
      that"; the cache TTL rail too, "but it needs an upgrade to take the fixes
      i made on the host and web ui side into account"; the prices and costs
      panel "is nice if the UI lets us implement it without us going slightly
      insane". He named no other feature. He also asked to modularize the fork's
      code while re-landing it, so later merges need no detail edits.
- [x] 4. Seam design: the `:fork` module, marked hook lines, a hook registry
      and its check script. Rules in `fork/README.md`.
- [x] 5. Merge `upstream/develop` with upstream's files as the base (the tree
      is upstream's plus the fork's documents; fork features are absent until
      step 6). Done 2026-11-13 on `sync/upstream-2026-11`: the foss debug build
      succeeds and the app starts on the emulator at the server-address screen.
      Tests not run here; the code is identical to upstream's.
- [ ] 6. Re-land, each in `:fork` with marked hooks, each checked on the
      emulator signed in as the test account:
  - [x] 6a. Scaffold: `:fork` module, hook check, the guard that drops a cache
        database with the fork's old schema, the old debug app id switch.
  - [x] 6b. Saved login and server address: in, checked on the emulator as far
        as it goes without a password manager. Real save and pick: step 8.
  - [x] 6c. Cache TTL rail (in; native Anthropic run for real, the rest listed
        in the ledger), at parity with the web pill of 2026-11-11
        (Claude on OpenRouter rows, `extendedCacheTTL` from the server, the
        marketplace caveat, arm cycle 1h, 5m, off).
  - [x] 6d. Prices and costs: a sheet opened from the fork's gutter, no header
        or menu hook needed.
  - [x] 6e. Upgrade test on the emulator (2026-11-13): old fork build signed in,
        new build over it, opens signed in, database on upstream's v15. Still
        to do on the Pixel, with the build under the old debug app id.
- [x] 7. Mobile gaps against the deployed backend fork (OptMem switches,
      routing and cost data, PDFs as text, web fetch, Markdown import): compare
      with the app code, then decide per item.
      Compared 2026-11-15: `reports/gap-checks-2026-11-15/SUMMARY.md` and one
      file per point there. The decisions per item are still open.
- [x] 7a. The app check of 2026-11-13 (`docs/APP_CHECK_2026-11-13.md`): decide
      per bug and improvement whether it is fixed here, behind a hook, or
      reported upstream. B5 (default-on servers) belongs to step 7.
      Stock-take 2026-11-15, from the ledger: done in the fork are B1 and B5
      (default-on connectors and first-send selection), improvement 3 (usable
      providers first in the model list) and improvement 4 (opaque header).
      Undecided: B2 (raw `no_user_key` error), B3 (a chat whose first message
      failed is missing from the drawer), B4 (two status lines for one server),
      B6 (a failed share shows internals), improvement 1 (tool calls by plain
      name), 2 (first model should be one that can answer), 5 (empty line on
      MCP server cards). Improvement 6 is the server's title step. Not checked:
      whether upstream's commits since 2026-11-13 fixed any of B2 to B6.
      Checked 2026-11-15 in the same reports folder: B2, improvements 1 and 2
      are real gaps that upstream has not touched; B3, B4's live half, B6 and
      improvement 5 still need the emulator.
      Built and decided 2026-11-16 (steps 7 and 7a together): what was built, seen on the
      emulator, left out and why is in `reports/gap-fixes-2026-11-16/OUTCOME.md`. Still open from
      it: the search caption needs a field served by the backend; the first-model fallback has not run for real.
- [ ] 8. Device acceptance on the Pixel with the real password manager: a
      picked saved login signs in directly (built 2026-11-13, not yet tried
      there). The old debug app id is gone since the rebrand, so the upgrade
      over the old install no longer applies.

Fork features the operator did not name on 2026-11-13 stay out for now; they
are listed in the ledger with sizes.

## Merges since the catch-up

- 2026-11-15: `upstream/develop` at `af0ea66e` (v2026.11.2), 39 commits, 9 files in conflict, all at
  hook lines. One hook moved without a conflict: git put the Text hook of
  `ChatOverflowSectionsTest` onto a helper upstream had added below the old one, and only the test
  compile showed it. `fork-hooks.py` counts hooks per file and cannot see that; after a merge,
  compile the tests too (`./gradlew :feature:chat:testDebugUnitTest`), not only the app.
