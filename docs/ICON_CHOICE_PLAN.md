# App icon choice (started 2026-11-14)

Read before touching the launcher icon; delete when every step is ticked.

The operator (2026-11-14): he wants to choose between the three sigil palettes inside the app,
"the way Duolingo did it", and remembers its flow as a reminder after the switch that ends in a
"Close app to do it now?" confirm.

- [ ] 1. `fork/scripts/gen-launcher-icon.py` renders all three alloys into the app's resources, and
      a round preview of each into `:fork`'s Compose resources.
- [ ] 2. Manifest: the LAUNCHER category leaves `MainActivity` (one hook line); three
      `activity-alias` entries carry it, Cold-lab enabled. First try them in
      `fork/src/androidMain/AndroidManifest.xml`; if the merge or lint refuses, a marked block in the
      app's manifest.
- [ ] 3. `:fork` `icon/`: the alloys, an Android switcher over `PackageManager` (which alias is
      enabled is the stored choice; a pick is applied when the app goes to the background, or at once
      through "Close now"), no-op on iOS, the sheet and the confirm, English and German strings.
- [ ] 4. Hooks: a row in Settings > Appearance (Android only); the model shortcuts take the current
      alloy's icon.
- [ ] 5. On the emulator: update over the current build, each alloy picked once with "Close now" and
      once with "Later", the drawer shows one icon each time, the app still opens from it.
- [ ] 6. Build, detekt, lint, fork tests, hook registry; ledger; push; the Pixel.
