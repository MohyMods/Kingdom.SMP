package com.mohymods.kingdomsmp.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** Pixel-aligned geometry avoids stretched borders at different Minecraft GUI scales. */
public final class RoyalPanel {
    private RoyalPanel() { }

    public static void frame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, RoyalPalette.BRONZE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, RoyalPalette.STONE);
        // Quiet slate courses give the frame a material surface without noisy text backgrounds.
        for (int row = 0; row * 24 < height - 12; row++) {
            int top = y + 6 + row * 24;
            for (int column = 0; column * 64 < width; column++) {
                int left = x + 5 + column * 64 - (row % 2) * 32;
                graphics.fill(Math.max(x + 5, left), top, Math.min(x + width - 5, left + 62),
                        Math.min(y + height - 6, top + 22), ((row + column) & 1) == 0 ? 0xFF1E2027 : 0xFF1C1E25);
            }
        }
        graphics.fill(x + 4, y + 4, x + width - 4, y + 5, RoyalPalette.BRONZE);
        for (int cornerX : new int[]{x + 3, x + width - 11}) {
            graphics.fill(cornerX, y + 3, cornerX + 8, y + 5, RoyalPalette.GOLD);
            graphics.fill(cornerX, y + height - 5, cornerX + 8, y + height - 3, RoyalPalette.GOLD);
        }
    }

    public static void parchment(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, RoyalPalette.BRONZE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, RoyalPalette.PARCHMENT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0xFFE8D7B5);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0xFFC3AC82);
        for (int row = 8; row < height - 4; row += 17) {
            for (int column = 6 + row % 13; column < width - 5; column += 43) {
                graphics.fill(x + column, y + row, Math.min(x + width - 4, x + column + 5), y + row + 1, 0xFFD4C19C);
            }
        }
    }
}
