package com.mohymods.kingdomsmp.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoyalLayoutTest {
    @Test
    void panelAndControlsFitCommonGuiScaledDimensions() {
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {640, 360}, {960, 540}, {1920, 1080}}) {
            RoyalLayout layout = RoyalLayout.fit(size[0], size[1]);
            assertTrue(layout.x() >= 8 && layout.y() >= 8);
            assertTrue(layout.x() + layout.width() <= size[0] - 8);
            assertTrue(layout.y() + layout.height() <= size[1] - 8);
            assertTrue(layout.bodyHeight() > 52, "Tier grid and clipped details need space");
            assertTrue(layout.bodyY() + layout.bodyHeight() < layout.footerY());
            for (int tier = 1; tier <= 12; tier++) {
                assertTrue(layout.tierX(tier) >= layout.contentX());
                assertTrue(layout.tierX(tier) + layout.tierWidth() <= layout.contentX() + layout.contentWidth());
                assertTrue(layout.tierY(tier) + 22 <= layout.bodyY() + layout.bodyHeight());
            }
        }
    }

    @Test
    void largeWindowsDoNotStretchPanelsIndefinitely() {
        assertEquals(720, RoyalLayout.fit(1920, 1080).width());
        assertEquals(430, RoyalLayout.fit(1920, 1080).height());
    }
}
