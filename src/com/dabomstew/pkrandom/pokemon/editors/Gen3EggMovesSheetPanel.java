package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.romhandlers.RomHandler;

/**
 * Egg move editor for Generation 3 games (RSE/FRLG).
 * Reuses the Generation 5 implementation which already mirrors the
 * spreadsheet-style workflow we want across generations.
 */
public class Gen3EggMovesSheetPanel extends Gen5EggMovesSheetPanel {

    public Gen3EggMovesSheetPanel(RomHandler romHandler) {
        super(romHandler);
    }
}
