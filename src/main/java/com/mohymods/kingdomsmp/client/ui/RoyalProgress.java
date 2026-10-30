package com.mohymods.kingdomsmp.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** A reusable bounded progress indicator; the caller renders its numeric text label. */
public final class RoyalProgress {
    private RoyalProgress() { }
    public static void render(GuiGraphics graphics, int x, int y, int width, int height, double progress) {
        if (width < 3 || height < 3) return;
        double bounded = Double.isFinite(progress) ? Math.clamp(progress, 0.0, 1.0) : 0.0;
        graphics.fill(x, y, x + width, y + height, RoyalPalette.BRONZE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, RoyalPalette.STONE);
        int filled = (int) Math.floor((width - 2) * bounded);
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, RoyalPalette.GOLD);
    }
}
