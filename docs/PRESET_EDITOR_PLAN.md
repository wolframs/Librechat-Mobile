# Preset editor on the model sheet (started 2026-11-14)

Read before touching the Presets page; delete when every step is ticked.

The operator (2026-11-14): the Edit dialog (name and "use this chat's settings") is lackluster next to
the web UI's preset editor (name, endpoint, model, every parameter).

- [ ] 1. A view-model hook that reloads the preset list.
- [ ] 2. `fork/PresetEditing.kt`: a stored preset with a new name, pair and parameters, mirroring
      upstream's `savePreset` mapping; fields it does not map are kept. Tests in a new test file.
- [ ] 3. Editor page in the model sheet: name, endpoint, model (filterable), the parameter controls of
      that parameter set, "Take this chat's model and settings", Save. Save = create the edited copy,
      then delete the old one, then reload; the page returns to the list.
- [ ] 4. Swipe Edit opens the editor; the Edit dialog goes. Strings EN/DE.
- [ ] 5. Emulator: edit name, model and a parameter of a test preset, check the stored result, delete it.
- [ ] 6. Checks, ledger, commit, push, the Pixel.
