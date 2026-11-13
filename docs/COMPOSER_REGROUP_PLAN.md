# Composer regrouping plan (started 2026-11-13)

Read before continuing the look and feel work on the chat screen. Tick a step
when it is done and verified. Delete this file when the last step is ticked.

## What was agreed (operator, 2026-11-13)

The mock in `fork/design/composer-regroup.html` "looks exactly right", after the
Claude app's pattern: the model chip sits in the composer next to the plus; the
plus sheet holds what goes into the chat (files, tool switches, connectors,
skills, memory, prompts) and no model matters; the model chip opens one sheet
for everything about the model (starred models, effort, parameters, presets,
more models, compare); the three-dot menu keeps conversation actions only. He
wanted a different icon set for the attach tiles ("FontAwesome or something
fancier, your choice"): Lucide line icons, vendored.

His process for it, verbatim: "Let GPT 6 Astra in judge/critic \"mode\" rate
your changes each round you make one full set of changes, and climb until they
come back with a 9/10 :'D If they go \"but the rest of the app also looks like
shit\" <- that is signal, not noise".

## Steps

- [x] 1. Mock agreed.
- [x] 2. Icons: Lucide vendored, generator, `ForkIcons`.
- [x] 3. Round 1, one full set: composer model chip, model sheet, regrouped plus
      sheet with the new icons, trimmed three-dot menu. Build, install,
      screenshots of each surface on the emulator.
- [x] 4. Judge round: GPT-6 Astra as critic, with the screenshots, the mock and
      the operator's reference screenshots. Score out of 10 and a defect list.
- [ ] 5. Repeat 3 and 4 with the judge's findings until the score is 9. Findings
      about the rest of the app are collected here, not dropped.
- [ ] 6. Tests, static checks, hook registry, ledger entry, merge to `develop`,
      push to both remotes.

## Judge rounds

(score, date, what changed; newest last)

- **Round 1, 2026-11-13: 5.5 / 10.** Per surface: composer 5, model sheet 6,
  effort page 4, add-to-chat sheet 6, connectors list 4, three-dot menu 7, dark
  theme 6. Main findings: the model sheet does not mark the current model and
  does not say its list is favourites; connector switches on while the row says
  "Not connected", under a header "Tools · 0 on"; the model chip reads as a
  label while the tool chip gets the strong shape; the Effort page has "Back"
  as a row with two forward arrows; the expanded connector list has no
  structure; the model sheet gives selection and advanced settings equal
  weight; "Off" repeated four times; sheet chrome differs between sheets.
  Decisions for round 2:
  - Composer: a real pill with a short name and a small chevron, no provider
    avatar; plus, model pill, mic and send in the action row; other chips on a
    row of their own, plain on-state tools folded into one "Tools · N" chip.
  - Model sheet: "Favourites" label, the current model always listed and
    checked, readable names with the exact id beneath; order favourites, All
    models, settings (Effort with its value, Advanced settings, Model presets,
    Prices), Compare models; a close button; Back from a sub-page returns here.
  - Effort page: back arrow in the header, "Model default", a line on the
    trade-off.
  - Add to chat: no "Off" lines, one-line descriptions, "Built-in tools" and
    "Connectors · N on", "Server files", "Prompt library"; Connectors becomes a
    page of its own with honest status wording.
  - The cache countdown and the prices button leave the thread gutter: the
    countdown becomes a chip in the composer (it sets the next send), prices a
    row in the model sheet. This changes the operator's July placement of the
    rail; the judge called the rail the worst defect outside the regrouping.

## What the judge said about the rest of the app

(collected across rounds)

- Round 1: the left-edge cache pill covers avatars and "expired" stays on
  screen (handled in round 2, see above).
- Round 1: the thread reads like a log viewer: avatar, bold name and timestamp
  on every short message; suggested a right-aligned bubble for the user, a
  full-width column for the reply, timestamps on demand.
- Round 1: the header is a row of outlined pills; the title looks like a button;
  the eye icon on the landing screen is unexplained.
- Round 1: the full model picker opens on providers without a key and "Add key"
  buttons; no model is visible at first sight.
- Round 1: the identity looks assembled from defaults (brown "An" circles,
  purple user avatars, lavender surfaces, multicoloured connector initials); the
  landing screen repeats the model name under a generic greeting.
