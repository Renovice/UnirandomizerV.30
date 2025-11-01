# Alternate Form Editing TODO

Tracked work for expanding per-forme editing across the toolchain. Each subsection lists the panels that must be audited and updated so that distinct formes (including Mega/Primal and regional variants) can be edited separately wherever the underlying ROM exposes unique data.

---

## Gen 4 – PokEditor (reference project)
- [ ] Inventory existing panels (Personal, Base Stats, Moves, Learnsets, Evolutions, Encounters, TM/Tutor, etc.) to identify current forme coverage.
- [ ] Map ROM data sources for forme-specific records (Rotom, Giratina, Shaymin, etc.).
- [ ] Update table models to materialise distinct rows per editable forme while avoiding duplicates where data is shared.
- [ ] Ensure CSV export/import paths include the expanded row set.
- [ ] Validate save/writeback routines for every updated panel.
- [ ] UX polish: suffix naming, filtering, and documentation updates.

## Gen 5 – FrostsGen5Editor
- [ ] Catalogue the UI panels and determine current handling of alternate formes (seasonal Deerling/Sawsbuck, Therian formes, etc.).
- [ ] Trace data loading to confirm how forme records are surfaced in memory.
- [ ] Extend each relevant sheet (Personal, Moves, Learnsets, Evolutions, TM/Tutor, Encounters) to display/edit forme-specific rows when the ROM differentiates them.
- [ ] Double-check save/reload flows, especially for shared learnset tables.
- [ ] Update icon caches and search/find features to cope with the additional entries.
- [ ] Add documentation/tooltips describing forme editing behaviour.

## Gen 6 – Uni Randomizer & pk3DS (XY/ORAS)
- [ ] Build a complete list of Gen 6 formes (Megas, Primal Reversions, Cosplay Pikachu, Hoopa-U, etc.) and identify which data structures allow independent editing.
- [ ] **Personal Sheet**: surface forme rows where stats/abilities differ; ensure guaranteed/common held items map correctly.
- [ ] **Moves/Learnsets**: only split rows when the ROM provides separate learn tables (e.g., Hoopa-U moves).
- [ ] **Evolutions**: allow per-forme evolution overrides if stored separately.
- [ ] **TM/HM & Tutors**: confirm compatibility arrays include forme-specific bits; adjust table indexing accordingly.
- [ ] Update CSV/export/import logic, copy/paste mode, and randomizer save writes.
- [ ] Regenerate caches (icons, type tables) to support the higher species count.
- [ ] Mirror the same changes in pk3DS UI panels for parity.

## Gen 7 – Uni Randomizer & pk3DS (SM/USUM)
- [ ] Enumerate all Gen 7 formes (Alolan variants, Ultra Necrozma, Zygarde cores, Ash-Greninja, Totem sizes, etc.) and flag which have independent data.
- [ ] **Personal Sheet**: expose individual rows for formes with unique stats/ability data; support call-rate and guaranteed item quirks.
- [ ] **Moves/Learnsets**: differentiate where alternate learnsets exist (e.g., Lycanroc formes, Zygarde).
- [ ] **Evolutions & Form Triggers**: ensure linked entries (e.g., Zygarde assembly) remain consistent when editing.
- [ ] **TM/HM & Tutors**: validate the compatibility bitfields and expand UI accordingly.
- [ ] **Trainer/Encounter Editors**: surface forme options when trainers/encounters can legally use them (Totems, Ultra Megalopolis fights).
- [ ] Align pk3DS feature set with the Randomizer changes.

## Cross-Project Tasks
- [ ] Standardise naming conventions (e.g., suffixes like "-Mega"/"-Alola") across all editors.
- [ ] Add filters/search helpers so users can toggle base species vs. formes.
- [ ] Update documentation/README entries describing forme support per generation.
- [ ] Create regression test plans (manual or automated) to verify each panel saves/restores forme edits correctly.
- [ ] Explore shared utilities for forme enumeration to minimise duplicated logic between projects.

---

**Status Legend:** unchecked items represent open work; convert to `[x]` as tasks are completed.