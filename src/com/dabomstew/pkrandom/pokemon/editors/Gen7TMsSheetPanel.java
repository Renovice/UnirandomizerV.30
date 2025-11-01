package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.romhandlers.RomHandler;

/**
 * Generation 7 TM/HM compatibility sheet panel.
 * Delegates to the Generation 6 implementation to keep feature parity.
 */
public class Gen7TMsSheetPanel extends Gen6TMsSheetPanel {

    public Gen7TMsSheetPanel(RomHandler romHandler) {
        super(romHandler);
    }
}
