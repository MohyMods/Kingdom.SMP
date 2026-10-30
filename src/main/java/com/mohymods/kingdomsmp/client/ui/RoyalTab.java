package com.mohymods.kingdomsmp.client.ui;

import net.minecraft.network.chat.Component;

/** Semantic reusable selected tab; shares button input, tooltip and focus behavior. */
public final class RoyalTab extends RoyalButton {
    public RoyalTab(int x, int y, int width, Component text, boolean selected, OnPress onPress, Component help) {
        super(x, y, width, 22, text, onPress, help);
        selected(selected);
    }
}
