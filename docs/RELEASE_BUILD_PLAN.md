# A signed release build of Mercuric (started 2026-11-14)

Read before touching the release signing; delete when every step is ticked.

The operator (2026-11-14), asked whether he wants a release build that needs a signing key he
keeps: "that's okay, i can keep keys safe, after all, that's the #2 job of humans these days."

Upstream's build already takes a release key from `keystore.properties` at the repository root
(ignored by git) or from `SIGNING_*` environment variables, and falls back to the debug key.

- [ ] 1. One permanent key, outside the repository, readable by him only; its password in a file
      beside it, never on a command line or in output.
- [ ] 2. `keystore.properties` at the repository root pointing at it.
- [ ] 3. `:app:assembleFossRelease`; the APK's certificate is the new key's.
- [ ] 4. Emulator: install, sign in, a chat, the model sheet, presets, Text, the icon choice
      (the release build is minified; the fork's code has not run minified before).
- [ ] 5. The Pixel: install as `dev.wsiener.mercuric`; the empty "Mercuric Dev" install from
      earlier today goes if it holds nothing.
- [ ] 6. Ledger: the decision, where the key is, its fingerprint, how to build. This file goes.
