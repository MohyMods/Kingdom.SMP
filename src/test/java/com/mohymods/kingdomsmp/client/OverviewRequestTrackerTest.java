package com.mohymods.kingdomsmp.client;

import org.junit.jupiter.api.Test;

import static com.mohymods.kingdomsmp.client.OverviewRequestTracker.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class OverviewRequestTrackerTest {
    private final Object connection = new Object();
    private final Object screen = new Object();
    private final OverviewRequestTracker tracker = new OverviewRequestTracker(100);

    @Test
    void onlyOneRequestMayBePendingAndSuccessfulReplyIsConsumedOnce() {
        long id = tracker.begin(1_000, connection, screen);
        assertTrue(id > 0);
        assertEquals(0, tracker.begin(1_001, connection, screen));
        assertTrue(tracker.waiting());
        assertTrue(tracker.complete(id, connection, screen, 1_099));
        assertFalse(tracker.waiting());
        assertFalse(tracker.complete(id, connection, screen, 1_099));
    }

    @Test
    void exactTimeoutBoundaryRejectsLateReplyEvenBeforeTickHasRun() {
        long id = tracker.begin(1_000, connection, screen);
        assertFalse(tracker.complete(id, connection, screen, 1_100));
        assertFalse(tracker.waiting());
    }

    @Test
    void expirationIsReportedExactlyOnce() {
        tracker.begin(1_000, connection, screen);
        assertEquals(WAITING, tracker.tick(1_099, connection, screen));
        assertEquals(TIMED_OUT, tracker.tick(1_100, connection, screen));
        assertEquals(IDLE, tracker.tick(1_101, connection, screen));
    }

    @Test
    void oldReplyCannotSatisfyOrCancelRetry() {
        long oldId = tracker.begin(1_000, connection, screen);
        assertEquals(TIMED_OUT, tracker.tick(1_100, connection, screen));
        long newId = tracker.begin(1_101, connection, screen);
        assertTrue(newId > oldId);
        assertFalse(tracker.complete(oldId, connection, screen, 1_102));
        assertTrue(tracker.waiting());
        assertTrue(tracker.complete(newId, connection, screen, 1_103));
    }

    @Test
    void sameIdOnDifferentConnectionCannotOpenScreen() {
        long id = tracker.begin(1_000, connection, screen);
        assertFalse(tracker.complete(id, new Object(), screen, 1_001));
        assertFalse(tracker.waiting());
    }

    @Test
    void navigationCancelsAndLateReplyDoesNotReopenScreen() {
        long id = tracker.begin(1_000, connection, screen);
        assertEquals(CANCELLED, tracker.tick(1_001, connection, new Object()));
        assertFalse(tracker.complete(id, connection, screen, 1_002));
    }

    @Test
    void replyAlsoChecksNavigationWhenNoTickHasRun() {
        long id = tracker.begin(1_000, connection, screen);
        assertFalse(tracker.complete(id, connection, new Object(), 1_001));
        assertFalse(tracker.waiting());
    }

    @Test
    void disconnectCancelsAndIdsRemainUniqueAcrossSessions() {
        long id = tracker.begin(1_000, connection, screen);
        assertEquals(CANCELLED, tracker.tick(1_001, null, null));
        long nextId = tracker.begin(1_002, new Object(), null);
        assertTrue(nextId > id);
    }

    @Test
    void explicitClearDoesNotReuseIds() {
        long id = tracker.begin(1_000, connection, screen);
        tracker.clear();
        assertFalse(tracker.waiting());
        assertTrue(tracker.begin(1_001, connection, screen) > id);
    }

    @Test
    void gameplayNullScreenIsValidOrigin() {
        long id = tracker.begin(1_000, connection, null);
        assertTrue(tracker.complete(id, connection, null, 1_001));
    }

    @Test
    void identityMatchingDoesNotUsePotentiallyEqualObjects() {
        Object first = new String("equal");
        Object second = new String("equal");
        long id = tracker.begin(1_000, first, screen);
        assertEquals(first, second);
        assertFalse(tracker.complete(id, second, screen, 1_001));
    }

    @Test
    void elapsedClockWrapDoesNotSkipTimeout() {
        long start = Long.MAX_VALUE - 50;
        tracker.begin(start, connection, screen);
        assertEquals(WAITING, tracker.tick(start + 99, connection, screen));
        assertEquals(TIMED_OUT, tracker.tick(start + 100, connection, screen));
    }

    @Test
    void invalidConnectionAndTimeoutAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> tracker.begin(0, null, screen));
        assertThrows(IllegalArgumentException.class, () -> new OverviewRequestTracker(0));
        assertThrows(IllegalArgumentException.class, () -> new OverviewRequestTracker(-1));
        assertFalse(tracker.waiting());
    }

    @Test
    void cancellationTakesPrecedenceOverTimeout() {
        tracker.begin(1_000, connection, screen);
        assertEquals(CANCELLED, tracker.tick(2_000, connection, new Object()));
    }
}
