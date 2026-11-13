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
- [ ] 3. Operator pass over the ledger: which features stay.
- [ ] 4. Seam design: fork-owned database, strings, DI module, settings
      section, and the hook points in upstream files. Written into the ledger.
- [x] 5. Merge `upstream/develop` with upstream's files as the base (the tree
      is upstream's plus the fork's documents; fork features are absent until
      step 6). Done 2026-11-13 on `sync/upstream-2026-11`: the foss debug build
      succeeds and the app starts on the emulator at the server-address screen.
      Tests not run here; the code is identical to upstream's.
- [ ] 6. Re-land the kept features one at a time, each with its tests; check
      each on the emulator.
- [ ] 7. Mobile gaps against the deployed backend fork (OptMem switches, cache
      TTL setting, routing and cost data, PDFs as text, web fetch, Markdown
      import): compare with the app code, then decide per item.
- [ ] 8. Device acceptance on the Pixel, then push.
