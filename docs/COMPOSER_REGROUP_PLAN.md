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
- [ ] 3. Round 1, one full set: composer model chip, model sheet, regrouped plus
      sheet with the new icons, trimmed three-dot menu. Build, install,
      screenshots of each surface on the emulator.
- [ ] 4. Judge round: GPT-6 Astra as critic, with the screenshots, the mock and
      the operator's reference screenshots. Score out of 10 and a defect list.
- [ ] 5. Repeat 3 and 4 with the judge's findings until the score is 9. Findings
      about the rest of the app are collected here, not dropped.
- [ ] 6. Tests, static checks, hook registry, ledger entry, merge to `develop`,
      push to both remotes.

## Judge rounds

(score, date, what changed; newest last)

## What the judge said about the rest of the app

(collected across rounds)
