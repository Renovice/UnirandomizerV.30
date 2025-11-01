package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.romhandlers.RomHandler;

/**
 * Generation 7 moves sheet panel. We currently share the Generation 6 move
 * editor implementation since the underlying data layout remains the same.
 */
public class Gen7MovesSheetPanel extends Gen6MovesSheetPanel {

    public Gen7MovesSheetPanel(RomHandler romHandler) {
        super(romHandler);
    }
}
