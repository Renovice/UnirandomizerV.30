package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.gamedata.Species;
import com.dabomstew.pkromio.romhandlers.PokemonImageGetter;
import com.dabomstew.pkromio.romhandlers.RomHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small shared cache used by the editor panels to display party-style icons for species.
 * Icons are generated lazily from the RomHandler's {@link PokemonImageGetter} implementation.
 */
public final class PokemonIconCache {
    private static final int ICON_SIZE = 48;
    private static final Map<RomHandler, PokemonIconCache> CACHE_BY_HANDLER = new ConcurrentHashMap<>();

    private final RomHandler romHandler;
    private final boolean supported;
    private final Map<Species, ImageIcon> cache = new ConcurrentHashMap<>();

    private PokemonIconCache(RomHandler romHandler) {
        this.romHandler = romHandler;
        this.supported = romHandler != null && romHandler.hasPokemonImageGetter();
    }

    public static PokemonIconCache get(RomHandler romHandler) {
        if (romHandler == null) {
            return new PokemonIconCache(null);
        }
        return CACHE_BY_HANDLER.computeIfAbsent(romHandler, PokemonIconCache::new);
    }

    public boolean hasIcons() {
        return supported;
    }

    public ImageIcon getIcon(Species species) {
        if (!supported || species == null) {
            return null;
        }
        return cache.computeIfAbsent(species, this::createIcon);
    }

    private ImageIcon createIcon(Species species) {
        try {
            PokemonImageGetter getter = romHandler.createPokemonImageGetter(species)
                    .setTransparentBackground(true);
            BufferedImage image = getter.get();
            if (image == null) {
                return null;
            }
            BufferedImage scaled = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0) {
                g2d.dispose();
                return null;
            }
            double scale = Math.min((double) ICON_SIZE / width, (double) ICON_SIZE / height);
            int scaledWidth = Math.max(1, (int) Math.round(width * scale));
            int scaledHeight = Math.max(1, (int) Math.round(height * scale));
            int x = (ICON_SIZE - scaledWidth) / 2;
            int y = (ICON_SIZE - scaledHeight) / 2;
            g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
            g2d.dispose();
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }
}
