# Uni Randomizer (Working Title)

Uni Randomizer is a all-in-one in one sanbox tool for pokemon randomization, allowing you to edit moves, pokemon, evolutions, type charts etc... all in one program without having to download seperate software just to make small edits to pokemon or moves.


## Highlights

- **Integrated multi-gen editors** – dedicated sheets for Pokémon stats, moves, evolutions, trainers, encounters, items, types.
- **Quality-of-life tooling** – CSV import/export on every major table, copy/paste workflows, searchable combo boxes, inline find, and manual-edit logging so you can track custom changes before saving.

## What you can do

1. **Randomize** like the regular universal randomizer: choose your settings, hit randomize, and patch the ROM. The underlying randomization logic is the same.
2. **Inspect & fine-tune**: walk through the editors to tweak evolution rules, rebalance stats, force egg moves or change the type chart. 
3. **Iterate quickly**: export a sheet to CSV for bulk edits, re-import, preview icon/tooltips instantly, and keep a log of manual adjustments via the Manual Edit Registry.

## Supported games

- All main-series titles from Gen 3 through Gen 7 (Gameboy → Nintendo 3DS) are supported. 
- Partial support for gen 5 fairy type romhacks (experimental doenst yet support romhacks like volt white 2 redux, just regular roms with the fairy type patch)

## Getting started

1. Install a Java 8+ runtime (JRE is provided in the release) 
2. Launch the editor (`java -jar UniversalRandomizer.jar`) or use the platform launcher scripts in `launcher/`.  
3. Open a supported ROM and pick your settings.
4. Review the Manual Edit log and perform a final save or randomize pass. Your changes are written when you save the ROM in the main window.

## Key differences from FVX

- Editors that show sprite/icon for easy UI accesability. 
- Type chart editors for each gen from 1-7
- Allow for "change name to type" when randomizing all types of pokemon, so for example ivysaur becomes fairy/rock hsi name changes to fai/roc to signify his new types saving you the trouble of having to look up each pkmn individually. 
- Logging records any manual adjustments so you can track changes acros roms with logs. 

## Development notes

- Randomizer classes under `src/com/dabomstew/pkrandom/randomizers` remain untouched to preserve FVX parity.  
- Editor utilities live under `src/com/dabomstew/pkrandom/pokemon/editors` and are where most of the new work happens.  


## Questions & feedback

i am currently building this tool to streamline ROM randomizations. If you spot an issue, need a feature, or want to contribute, open a discussion or issue in this repository. Please include the generation, ROM version, and a description of the steps you took so we can reproduce the problem.
