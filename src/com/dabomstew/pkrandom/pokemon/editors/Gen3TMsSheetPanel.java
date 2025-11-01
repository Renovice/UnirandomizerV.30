package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.romhandlers.RomHandler;

/**
 * Gen 3 TM/HM sheet backed by the Gen 5 table so alternate formes are listed.
 */
public class Gen3TMsSheetPanel extends Gen5TMsSheetPanel {

    public Gen3TMsSheetPanel(RomHandler romHandler) {
        super(romHandler);
    }
}
