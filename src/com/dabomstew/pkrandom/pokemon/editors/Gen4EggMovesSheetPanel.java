package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.romhandlers.RomHandler;

/**
 * Egg move editor for Generation 4 games (DP/Pt/HGSS).
 * Delegates to the Generation 5 implementation so the UX stays consistent.
 */
public class Gen4EggMovesSheetPanel extends Gen5EggMovesSheetPanel {

    public Gen4EggMovesSheetPanel(RomHandler romHandler) {
        super(romHandler);
    }
}
