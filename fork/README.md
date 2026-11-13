# The fork's module

Read before adding or changing a fork feature, and before merging from upstream. Update it in the
same commit as a change to these rules. Which features exist and where each hooks in is in
`docs/FORK_FEATURE_LEDGER.md`; the hook lines themselves are listed by `fork/scripts/fork-hooks.py --list`.

## Why it is built this way

Upstream (garfiec/Librechat-Mobile) lands about 230 commits a month and rewrites its own files
freely. Until November 2026 the fork's features were edits inside those files, and one catch-up
cost 278 merge conflicts. Now the fork's code sits in its own module, and upstream's files carry
only marked hook lines. A merge can then conflict only at a hook, and a hook the merge dropped is
reported by a script instead of found on a phone.

## Rules

1. **Fork code lives in `:fork`**, package `com.garfiec.librechat.fork.<feature>`. Every feature
   module gets it through the feature convention plugin. `:fork` depends on `:core:*` only, never on
   a feature module, so fork UI takes plain values and lambdas, not a feature's state classes.
2. **Code a core module must call** sits in a `fork` package inside that core module, as fork-only
   files (a core module cannot depend on `:fork`).
3. **A hook is the only edit in an upstream file**: a call, an argument or a dependency line, with
   `FORK-HOOK(<feature>)` and a few words on what it is for. Use fully-qualified names so no import
   line changes. No reformatting, renaming or moving of upstream code.
4. **The hook registry is `fork/hooks.txt`.** Run `fork/scripts/fork-hooks.py --write` in the commit
   that adds or removes a hook, and `fork/scripts/fork-hooks.py` after every merge from upstream.
5. **Strings** go into `fork/src/commonMain/composeResources/values*/strings.xml`, English and German.
6. **Preferences** go into fork-only classes over the shared DataStore file. A feature that existed
   before November 2026 keeps its old key names, so values on the phone survive.
7. **No changes to upstream's Room entities, schema files or database version.** State that needs a
   table goes into a row of an existing table under a fork key prefix, or into DataStore.
8. **Tests** for fork code live in `fork/src/androidUnitTest`.
9. **Library versions**: fork entries are appended to `gradle/libs.versions.toml` in one marked
   block; upstream's pins are never changed.

## Merging from upstream

```bash
git fetch upstream
git merge upstream/develop
fork/scripts/fork-hooks.py        # every hook still there?
./gradlew :app:assembleFossDebug
```

A conflict at a hook line: take upstream's side, then put the hook back where the code it calls
into now lives. If upstream has built the feature itself, drop the fork's version and its hooks, and
note that in the ledger.
