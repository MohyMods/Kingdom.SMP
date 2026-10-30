package com.mohymods.kingdomsmp.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** Pixel-aligned geometry avoids stretched borders at different Minecraft GUI scales. */
public final class RoyalPanel {
    private RoyalPanel() { }

    public static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, RoyalPalette.BRONZE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, RoyalPalette.STONE);
        graphics.fill(x + 4, y + 4, x + width - 4, y + 5, RoyalPalette.BRONZE);
        for (int cornerX : new int[]{x + 3, x + width - 11}) {
            graphics.fill(cornerX, y + 3, cornerX + 8, y + 5, RoyalPalette.GOLD);
            graphics.fill(cornerX, y + height - 5, cornerX + 8, y + height - 3, RoyalPalette.GOLD);
        }
    }

    public static void parchment(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, RoyalPalette.BRONZE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, RoyalPalette.PARCHMENT);
    }
}
