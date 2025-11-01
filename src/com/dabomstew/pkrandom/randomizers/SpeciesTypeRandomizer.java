package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkromio.gamedata.MegaEvolution;
import com.dabomstew.pkromio.gamedata.Species;
import com.dabomstew.pkromio.gamedata.SpeciesSet;
import com.dabomstew.pkromio.gamedata.Type;
import com.dabomstew.pkromio.gamedata.cueh.BasicSpeciesAction;
import com.dabomstew.pkromio.gamedata.cueh.EvolvedSpeciesAction;
import com.dabomstew.pkromio.romhandlers.RomHandler;

import java.util.List;
import java.util.Random;

public class SpeciesTypeRandomizer extends Randomizer {

    // "Get Secondary Type Chance"s
    private static final double GSTC_NO_EVO = 0.5;
    private static final double GSTC_HAS_EVO = 0.35;
    private static final double GSTC_MIDDLE_EVO = 0.15;
    private static final double GSTC_FINAL_EVO = 0.25;

    public SpeciesTypeRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    public void randomizeSpeciesTypes() {
        boolean evolutionSanity = settings.getSpeciesTypesMod() == Settings.SpeciesTypesMod.RANDOM_FOLLOW_EVOLUTIONS;
        boolean megaEvolutionSanity = settings.isTypesFollowMegaEvolutions();
        boolean dualTypeOnly = settings.isDualTypeOnly();

        BasicSpeciesAction<Species> basicAction = pk -> {
            pk.setPrimaryType(typeService.randomType(random));
            pk.setSecondaryType(null);
            double chance = pk.getEvolutionsFrom().size() == 1 ? GSTC_HAS_EVO : GSTC_NO_EVO;
            assignRandomSecondaryType(pk, chance, dualTypeOnly);
        };
        EvolvedSpeciesAction<Species> evolvedAction = (evFrom, evTo, toMonIsFinalEvo) -> {
            evTo.setPrimaryType(evFrom.getPrimaryType(false));
            evTo.setSecondaryType(evFrom.getSecondaryType(false));

            if (evTo.getSecondaryType(false) == null) {
                double chance = toMonIsFinalEvo ? GSTC_FINAL_EVO : GSTC_MIDDLE_EVO;
                assignRandomSecondaryType(evTo, chance, dualTypeOnly);
            }
        };
        BasicSpeciesAction<Species> noEvoAction = pk -> {
            pk.setPrimaryType(typeService.randomType(random));
            pk.setSecondaryType(null);
            assignRandomSecondaryType(pk, GSTC_NO_EVO, dualTypeOnly);
        };

        copyUpEvolutionsHelper.apply(evolutionSanity, false, basicAction, evolvedAction,
                null, noEvoAction);

        carryTypesToAltFormes();

        carryTypesToMegas(megaEvolutionSanity);

        // Change Pokemon names to their types if the setting is enabled
        if (settings.isChangeNamesToTypes()) {
            changeNamesToTypes();
        }

        changesMade = true;
    }

    private void carryTypesToAltFormes() {
        SpeciesSet allPokes = romHandler.getSpeciesSetInclFormes();
        for (Species sp : allPokes) {
            if (sp != null && sp.isActuallyCosmetic()) {
                sp.setPrimaryType(sp.getBaseForme().getPrimaryType(false));
                sp.setSecondaryType(sp.getBaseForme().getSecondaryType(false));
            }
        }
    }

    private void carryTypesToMegas(boolean megaEvolutionSanity) {
        if (megaEvolutionSanity) {
            List<MegaEvolution> allMegaEvos = romHandler.getMegaEvolutions();
            for (MegaEvolution megaEvo : allMegaEvos) {
                if (megaEvo.getFrom().getMegaEvolutionsFrom().size() > 1)
                    continue;
                megaEvo.getTo().setPrimaryType(megaEvo.getFrom().getPrimaryType(false));
                megaEvo.getTo().setSecondaryType(megaEvo.getFrom().getSecondaryType(false));

                if (megaEvo.getTo().getSecondaryType(false) == null) {
                    if (random.nextDouble() < 0.25) {
                        megaEvo.getTo().setSecondaryType(typeService.randomType(random));
                        while (megaEvo.getTo().getSecondaryType(false) == megaEvo.getTo().getPrimaryType(false)) {
                            megaEvo.getTo().setSecondaryType(typeService.randomType(random));
                        }
                    }
                }
            }
        }
    }

    private void assignRandomSecondaryType(Species sp, double chance, boolean dualTypeOnly) {
        if (random.nextDouble() < chance || dualTypeOnly) {
            sp.setSecondaryType(typeService.randomType(random));
            while (sp.getSecondaryType(false) == sp.getPrimaryType(false)) {
                sp.setSecondaryType(typeService.randomType(random));
            }
        }
    }

    private void changeNamesToTypes() {
        SpeciesSet allPokes = romHandler.getSpeciesSetInclFormes();
        for (Species sp : allPokes) {
            if (sp != null) {
                String typeAbbrev = getTypeAbbreviation(sp.getPrimaryType(false));
                if (sp.getSecondaryType(false) != null) {
                    typeAbbrev += "/" + getTypeAbbreviation(sp.getSecondaryType(false));
                }
                sp.setName(typeAbbrev);
            }
        }
    }

    private String getTypeAbbreviation(Type type) {
        switch (type) {
            case NORMAL:
                return "Nor";
            case FIGHTING:
                return "Fig";
            case FLYING:
                return "Fly";
            case POISON:
                return "Poi";
            case GROUND:
                return "Gro";
            case ROCK:
                return "Roc";
            case BUG:
                return "Bug";
            case GHOST:
                return "Gho";
            case STEEL:
                return "Ste";
            case FIRE:
                return "Fir";
            case WATER:
                return "Wat";
            case GRASS:
                return "Gra";
            case ELECTRIC:
                return "Ele";
            case PSYCHIC:
                return "Psy";
            case ICE:
                return "Ice";
            case DRAGON:
                return "Dra";
            case DARK:
                return "Dar";
            case FAIRY:
                return "Fai";
            default:
                return type.toString().substring(0, 3);
        }
    }
}
