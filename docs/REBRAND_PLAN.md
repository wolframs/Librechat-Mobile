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

## Decided (operator, 2026-11-14)

1. The product is decoupled, the code stays coupled: "i'm fine with the code staying coupled, i'm
   with you on the link to upstream being useful". The Kotlin package and the hook architecture stay.
2. App id: he is fine with `io.github.wolframs.mercuric` and offered his own domain `wsiener.dev`
   "in case you might consider it referenceable". My working choice: **`dev.wsiener.mercuric`**, the
   reverse of a domain he owns, independent of where the code is hosted.

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

## Steps

- [ ] 1. `:fork` `brand/Brand.kt`: the name, the fork's and upstream's repository addresses.
- [ ] 2. Build: `applicationId = "dev.wsiener.mercuric"`; the `-PlegacyDebugApplicationId` switch and
      the guard for the old fork's cache database go (both only served the old id).
- [ ] 3. Name: `app_name` (Mercuric / Mercuric Dev), the five other strings in all ten locales, and
      the code that shows the name (theme preview, MCP host name, diagnostics, the pictures folder).
- [ ] 4. Links: update check and release notes off (the fork publishes no releases); "Report a
      problem" hidden (it mails upstream's maintainer); privacy policy link to the fork's copy.
- [ ] 5. Attribution: a row in About ("Based on Switchboard"), the fork's lines in `NOTICE`, a
      note at the top of the README.
- [ ] 6. Icon default: Ember-alchemy (his naming log names it as the brand's icon).
- [ ] 7. Emulator: fresh install under the new id, sign in, the name in launcher, About and
      settings, icon choice, a chat. Then the old test install is removed from the emulator.
- [ ] 8. Full checks, ledger, root CLAUDE.md pointer, push, the Pixel (beside his old app, which
      he removes himself when ready).

Not part of this: iOS names and bundle id (iOS is not built here), fastlane and store metadata,
release and Play workflows (the fork publishes nothing), renaming the repositories, a signed
release build (asked 2026-11-14, see the ledger once answered).
