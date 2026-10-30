package com.mohymods.kingdomsmp.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Vanilla button input/focus/narration with a Royal Gilded renderer. */
public class RoyalButton extends Button {
    private boolean selected;

    public RoyalButton(int x, int y, int width, int height, Component text, OnPress onPress, Component tooltip) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(tooltip));
    }

    public RoyalButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int edge = selected || isHoveredOrFocused() ? RoyalPalette.GOLD : RoyalPalette.BRONZE;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), edge);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1,
                isHoveredOrFocused() ? RoyalPalette.HOVER : RoyalPalette.PANEL);
        if (selected) graphics.fill(getX() + 3, getY() + getHeight() - 3,
                getX() + getWidth() - 3, getY() + getHeight() - 2, RoyalPalette.GOLD);
        var font = Minecraft.getInstance().font;
        String text = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, getWidth() - 8));
        graphics.drawCenteredString(font, text, getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, active ? RoyalPalette.TEXT : RoyalPalette.DISABLED);
    }
}
