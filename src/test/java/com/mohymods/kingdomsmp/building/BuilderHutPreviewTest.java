package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class BuilderHutPreviewTest {
    private BuilderHutService.Preview preview(long createdAtNanos) {
        return new BuilderHutService.Preview(BlockPos.ZERO, Rotation.NONE, UUID.randomUUID(), createdAtNanos, "test");
    }

    @Test void remainsValidBeyondTheOldThirtySecondDeadline() {
        long start = TimeUnit.SECONDS.toNanos(17);
        var preview = preview(start);
        assertFalse(preview.expired(start + TimeUnit.SECONDS.toNanos(30)));
        assertFalse(preview.expired(start + TimeUnit.MINUTES.toNanos(4)));
        assertFalse(preview.expired(start + TimeUnit.SECONDS.toNanos(300) - 1));
    }

    @Test void expiresAtExactlyFiveMinutesAndStaysExpired() {
        var preview = preview(0);
        assertTrue(preview.expired(TimeUnit.SECONDS.toNanos(300)));
        assertTrue(preview.expired(TimeUnit.SECONDS.toNanos(301)));
    }

    @Test void monotonicClockWrapDoesNotExpireThePreviewEarlyOrKeepItAlive() {
        long start = Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(10);
        var preview = preview(start);
        assertFalse(preview.expired(start + TimeUnit.SECONDS.toNanos(299)));
        assertTrue(preview.expired(start + TimeUnit.SECONDS.toNanos(300)));
    }
}
