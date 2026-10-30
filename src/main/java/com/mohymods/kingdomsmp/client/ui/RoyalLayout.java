package com.mohymods.kingdomsmp.client.ui;

/** GUI-scaled pixel geometry, independent of Minecraft rendering. */
public record RoyalLayout(int x, int y, int width, int height) {
    public static RoyalLayout fit(int screenWidth, int screenHeight) {
        if (screenWidth < 160 || screenHeight < 160) {
            throw new IllegalArgumentException("Royal screens need at least 160 GUI pixels per axis");
        }
        int width = Math.min(720, screenWidth - 16);
        int height = Math.min(430, screenHeight - 16);
        return new RoyalLayout((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    public int contentX() { return x + 14; }
    public int contentWidth() { return width - 28; }
    public int bodyY() { return y + 80; }
    public int bodyHeight() { return height - 124; }
    public int footerY() { return y + height - 30; }
    public int tierWidth() { return (contentWidth() - 5 * 4) / 6; }
    public int tierX(int tier) { return contentX() + ((tier - 1) % 6) * (tierWidth() + 4); }
    public int tierY(int tier) { return bodyY() + ((tier - 1) / 6) * 26; }
}
