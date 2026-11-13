# Look and feel plan, part 2 (started 2026-11-13)

Read before continuing this work. Tick a step when it is done and verified.
Delete this file when the last step is ticked.

## What the operator said (2026-11-13)

- The thread and header restyle of `fork/design/thread-header-restyle.html`:
  "yes", and: "the font choices matter, honestly, I enjoy the ability to choose
  fonts, fonts matter a lot when text is the membrane of interactions
  transferring between us".
- The two bugs from the app check (the first message of a new chat ignores the
  connectors that are switched on; memory and web run while shown as off and
  cannot be switched off per chat): "YES :D do it!"
- iOS is out of scope unless it can be done over ssh on the MacBook, which has
  no Xcode.
- Look and feel changes go through the design critic (GPT-6 Astra) per full
  set of changes, until it says 9 out of 10.

## Steps

- [x] 0. Pixel: the build under the old app id installed over his app
      (same signing key), it starts, the database is on upstream's v15.
      His part: sign in, save and fill a login with his password manager.
- [ ] 1. Connectors: default-on servers shown and switchable per chat
      (`defaultOn`, `mcpOff`), and the first-message bug. Feature id `mcp-default-on`.
- [ ] 2. Restyle: header, thread, landing, order of the full model list.
      Feature id `thread`.
- [ ] 3. Fonts: three slots as on the web UI (interface, you, model), six
      bundled families plus the system font. Feature id `fonts`. The font
      files are in `fork/src/commonMain/composeResources/font`, licenses in
      `fork/fonts-licenses`.
- [ ] 4. Critic rounds on 2 and 3 from device screenshots, until 9.
- [ ] 5. Full checks, ledger, merge to `develop`, push to both remotes.

## Judge rounds

(score, date, what changed; newest last)
