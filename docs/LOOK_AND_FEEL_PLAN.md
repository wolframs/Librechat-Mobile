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
- [x] 1. Connectors: default-on servers shown and switchable per chat
      (`defaultOn`, `mcpOff`), and the first-message bug. Feature id `mcp-default-on`.
- [x] 2. Restyle: header, thread, landing, order of the full model list.
      Feature id `thread`.
- [x] 3. Reading settings, feature id `fonts`: fonts in three slots as on the
      web UI (interface, you, model), six bundled families plus the system
      font; and paragraph spacing, configurable (operator, 2026-11-13: "in some
      contexts, I wished that I could tighten paragraphs in the past, in
      others, the larger space made more sense"). One sheet for both, reachable
      from the chat in two taps and from Settings. The old spacing key
      `chat_paragraph_spacing` and its values stay. Font files:
      `fork/src/commonMain/composeResources/font`, licenses `fork/fonts-licenses`.
- [ ] 4. Critic rounds on 2 and 3 from device screenshots, until 9.
- [ ] 5. Full checks, ledger, merge to `develop`, push to both remotes.

## Judge rounds

(score, date, what changed; newest last)

### L1, 2026-11-13: 8/10

Thread 8, header 9, landing 8, Text sheet 7.5, All models 7.5, dark 8.5, large font 8.
Before the round: user bubbles hug their text, the Text sheet rebuilt (sample first, faces as a row
of tiles, glyph tiles for spacing and size), the thread fades out above the composer.

Left for 9, ticked as done and seen on the emulator:

- [ ] Reply headings sized against the body (about 1.4x for h1, semibold, line height 1.2), the
      same gap below at every font scale.
- [ ] Text sheet: the sample shows a heading and bold/italic and stays in view; the slot choice
      sits with the typeface row and reads Replies / Yours / App; "Use app font"; spacing glyphs
      are two plain paragraphs with a label under each; thinner selection outline; line spacing in
      three steps; reset.
- [ ] All models: provider names at body weight and colour, counts secondary, rounded search
      field with an icon, "Set up providers" set apart, no "(0)" counts there.
- [ ] Temporary chat: a labelled control with a visible on state, not a bare eye.
- [ ] The plus badge quieter; a reply's time not hidden behind the composer's fade.
- [ ] The critic's capture shows a tapped reply with its actions in view.

Working choice of mine, to argue in round 2: the slot choice stays under a "Typeface" heading, not
at the top of the sheet, because size and spacing are not per slot.
