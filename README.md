# Uni Randomizer (Working Title)

Uni Randomizer is our all-in-one playground for modernizing the Universal Pokémon Randomizer experience. It keeps the battle-tested randomization logic from Universal Pokémon Randomizer FVX while layering on a suite of rich editors, tooling, and usability upgrades aimed at ROM hackers and challenge-run creators.

## Highlights

- **FVX randomizer core** – every shuffle, encounter tweak, and balance option you expect from FVX remains 100% intact for Generations 1–7 (excluding Let’s Go). If you only want a straight randomize-and-go, you can.
- **Integrated multi-gen editors** – dedicated sheets for Pokémon stats, moves, evolutions, trainers, encounters, items, types, palettes, and more. Gen 3 support now mirrors the Gen 4 editor, and the UI adapts to game-specific data automatically.
- **Sprite & icon improvements** – bundled mini-icons cover later generations without requiring ROM extraction, and DS/3DS sheets gracefully degrade when in-ROM sprites are unavailable.
- **Quality-of-life tooling** – CSV import/export on every major table, copy/paste workflows, searchable combo boxes, inline find, and manual-edit logging so you can track custom changes before saving.
- **ROM data safety nets** – per-sheet backups, reload/restore commands, and guarded editors that block partial writes or invalid combinations before they reach the ROM handler.

## What you can do

1. **Randomize** like FVX: choose your settings, hit randomize, and patch the ROM. The underlying randomization pipeline is unchanged.
2. **Inspect & fine-tune**: walk through the editors to tweak evolution rules, rebalance stats, force egg moves, tune trainer parties, or handcraft encounter tables.
3. **Iterate quickly**: export a sheet to CSV for bulk edits, re-import, preview icon/tooltips instantly, and keep a log of manual adjustments via the Manual Edit Registry.

## Supported games

All main-series titles from Gen 1 through Gen 7 (Game Boy → Nintendo 3DS) are supported. Gen 6/7 panels include platform-specific conveniences such as version-exclusive evolution rules and weather/terrain triggers.

## Getting started

1. Install a Java 8+ runtime (FVX compatibility is maintained).  
2. Launch the editor (`java -jar UniversalRandomizer.jar`) or use the platform launcher scripts in `launcher/`.  
3. Open a supported ROM and pick a workflow: randomize immediately or dive into the editors before saving.  
4. Review the Manual Edit log and perform a final save or randomize pass. Your changes are written when you save the ROM in the main window.

## Key differences from FVX

- Editors now show sprite/icon previews even when the source game lacks in-ROM assets (we bundle the missing icons).
- Gen 3 data sheets include the full Personal/Moves/Evolution fields, matching later generations feature-for-feature.
- UI caps, column visibility, and validation rules are aligned with pk3DS where applicable to reduce manual mistakes.
- Logging hooks record any manual adjustments so you can track deltas alongside randomized changes.

## Development notes

- Randomizer classes under `src/com/dabomstew/pkrandom/randomizers` remain untouched to preserve FVX parity.  
- Editor utilities live under `src/com/dabomstew/pkrandom/pokemon/editors` and are where most of the new work happens.  
- `PokemonIconCache` contains the sprite fallback logic (bundled assets live in `src/com/dabomstew/pkrandom/pokemon/icons`).

We’re actively iterating on the editor experience—expect additional panels, validation passes, and helper scripts in future updates.

## Questions & feedback

We’re building this tool to streamline ROM customization workflows. If you spot an issue, need a feature, or want to contribute, open a discussion or issue in this repository. Please include the generation, ROM version, and a description of the steps you took so we can reproduce the problem.
