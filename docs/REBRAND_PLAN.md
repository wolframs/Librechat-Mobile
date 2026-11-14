# Rebrand to Mercuric (started 2026-11-14)

Read before changing the app's name, id, icon default or anything that names upstream. Tick steps
as they are done and seen on a device; delete this file when all are ticked.

## What the operator said (2026-11-14)

- He wants the fork "more my own": "Treating the project as *based on garfiec's work* but rebranding
  and decoupling, and, yes, actually creating a new android app id (I'm aware of that meaning that
  db contents on my phone won't transfer ;))".
- Name: **Mercuric** (provisional, his naming log of 2026-11-14). The log's line "keep
  `applicationId` unchanged" is older than his message above, which overrules it.
- The log places the lore in the icon and names the Ember-alchemy variant.

## Open, his to decide (asked 2026-11-14, not yet answered)

1. **How deep the decoupling goes.** Either the product is decoupled and the code stays mergeable
   (name, app id, icon, links, About and NOTICE change; the Kotlin package `com.garfiec.librechat`
   and the hook architecture stay, so merges from upstream keep working), or the code is cut loose
   too (packages renamed, no more merges). 3,134 files carry the package; renaming it ends merging
   in practice.
2. **The app id.** A reverse domain he controls, or `io.github.wolframs.mercuric`.

## What names upstream today (inventory, 2026-11-14)

- Display name: `app_name` "Switchboard" / "Switchboard Dev" (`app/src/{main,debug}/res/values`),
  five more English strings and their copies in 27 translated files (update banner, "Add to
  Switchboard chat", the PDF password note, an accent-colour note, one content description).
- App id: `applicationId = "com.garfiec.librechat"` with `.play` and `.debug` suffixes
  (`app/build.gradle.kts`); the fork's `-PlegacyDebugApplicationId` switch exists only to update
  the operator's old install and goes with the new id.
- **"Report a problem" mails `appsupport@garfiec.com`** (`ReportProblemViewModel.kt`): a fork
  build's reports go to upstream's maintainer. Wrong in every variant.
- Update check and "What's new": `GitHubReleasesApi` reads garfiec's GitHub releases; the update
  banner would offer upstream's builds.
- Privacy policy link: garfiec's `docs/PRIVACY.md`.
- `NOTICE` (Apache 2.0): "Switchboard, Copyright 2026 garfiec", and upstream's own statement that
  modified versions not published by garfiec are independent of the Switchboard project. Stays,
  with the fork's line added; the About page names what the app is based on.
- Not user-visible and independent of the app id: the Kotlin package and `namespace`, the theme
  name, the `librechat://` link scheme, the browser User-Agent the server requires.
- Repositories: GitHub `wolframs/Librechat-Mobile` (a GitHub fork of garfiec's), Forgejo likewise.
  Renaming them is separate from the app.

## Steps (filled in once decision 1 is made)

- [ ] 0. Decisions 1 and 2.
