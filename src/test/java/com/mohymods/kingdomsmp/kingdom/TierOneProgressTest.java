package com.mohymods.kingdomsmp.kingdom;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource.*;
import static org.junit.jupiter.api.Assertions.*;

class TierOneProgressTest {
    private static final UUID ALICE = new UUID(0, 1);
    private static final UUID BOB = new UUID(0, 2);
    private static final UUID CAROL = new UUID(0, 3);

    @Test
    void freshProgressHasNoInventedContributionsOrWinner() {
        TierOneProgress progress = TierOneProgress.initial();
        assertEquals(0, progress.revision());
        assertTrue(progress.contributors().isEmpty());
        assertFalse(progress.complete());
        assertTrue(progress.winner().isEmpty());
        assertTrue(progress.leader().isEmpty());
        for (TierOneProgress.Resource resource : TierOneProgress.Resource.values()) {
            assertEquals(0, progress.total(resource));
        }
    }

    @Test
    void acceptedQuantityIsCappedAndExcessRemainsUnconsumed() {
        TierOneProgress first = TierOneProgress.initial().contribute(ALICE, LOGS, 1_000).progress();
        TierOneProgress.ContributionResult result = first.contribute(BOB, LOGS, 100);
        assertEquals(24, result.acceptedCount());
        assertEquals(1024, result.progress().total(LOGS));
        assertEquals(24, result.progress().contributors().get(BOB).logs());
        assertEquals(1000, first.total(LOGS), "Prior snapshots must remain immutable");
        assertEquals(2, result.progress().revision());
    }

    @Test
    void maliciousMaximumLongRequestsAreBoundedWithoutOverflow() {
        TierOneProgress progress = TierOneProgress.initial();
        for (TierOneProgress.Resource resource : TierOneProgress.Resource.values()) {
            TierOneProgress.ContributionResult result = progress.contribute(ALICE, resource, Long.MAX_VALUE);
            assertEquals(resource.required(), result.acceptedCount());
            progress = result.progress();
        }
        assertEquals(8192, progress.contributors().get(ALICE).scoreUnits());
        assertEquals(Optional.of(ALICE), progress.winner());
        assertTrue(progress.complete());
    }

    @Test
    void everyResourceHasEqualWeightWhenItsWholeRequirementIsContributed() {
        for (TierOneProgress.Resource resource : TierOneProgress.Resource.values()) {
            TierOneProgress progress = TierOneProgress.initial()
                    .contribute(ALICE, resource, resource.required()).progress();
            assertEquals(2048, progress.contributors().get(ALICE).scoreUnits());
        }
    }

    @Test
    void scarceResourcesAreNotOutrankedByLargerRawCounts() {
        TierOneProgress progress = TierOneProgress.initial().contribute(ALICE, IRON_INGOTS, 10).progress();
        progress = progress.contribute(BOB, COBBLESTONE, 100).progress();
        assertEquals(Optional.of(ALICE), progress.leader());
        assertEquals(160, progress.contributors().get(ALICE).scoreUnits());
        assertEquals(100, progress.contributors().get(BOB).scoreUnits());
    }

    @Test
    void zeroAndFullRequirementSubmissionsDoNotConsumeOrRetimestamp() {
        TierOneProgress progress = TierOneProgress.initial().contribute(ALICE, LOGS, 1024).progress();
        assertSame(progress, progress.contribute(ALICE, LOGS, 0).progress());
        TierOneProgress.ContributionResult capped = progress.contribute(BOB, LOGS, 99);
        assertSame(progress, capped.progress());
        assertEquals(0, capped.acceptedCount());
        assertEquals(1, progress.revision());
        assertEquals(1, progress.contributors().get(ALICE).lastPositiveSequence());
        assertFalse(progress.contributors().containsKey(BOB));
    }

    @Test
    void negativeRequestsRejectInsteadOfUndoingContributions() {
        TierOneProgress progress = TierOneProgress.initial().contribute(ALICE, WHEAT, 100).progress();
        assertThrows(IllegalArgumentException.class, () -> progress.contribute(ALICE, WHEAT, -1));
        assertThrows(IllegalArgumentException.class, () -> progress.contribute(ALICE, WHEAT, Long.MIN_VALUE));
        assertEquals(100, progress.total(WHEAT));
    }

    @Test
    void tiedCurrentScoresPreferWhoFirstReachedThatScoreRatherThanWhoJoinedFirst() {
        TierOneProgress progress = TierOneProgress.initial().contribute(ALICE, COBBLESTONE, 10).progress();
        progress = progress.contribute(BOB, COBBLESTONE, 11).progress();
        progress = progress.contribute(ALICE, COBBLESTONE, 1).progress();
        assertEquals(Optional.of(BOB), progress.leader());
        assertEquals(11, progress.contributors().get(ALICE).scoreUnits());
        assertEquals(11, progress.contributors().get(BOB).scoreUnits());
    }

    @Test
    void interleavedContributorsFinalizeOneWinnerWithStableTieOrdering() {
        TierOneProgress progress = TierOneProgress.initial();
        for (TierOneProgress.Resource resource : TierOneProgress.Resource.values()) {
            progress = progress.contribute(ALICE, resource, resource.required() / 2).progress();
            TierOneProgress.ContributionResult result = progress.contribute(BOB, resource, resource.required() / 2);
            progress = result.progress();
            assertEquals(resource == IRON_INGOTS, result.completedNow());
        }
        assertEquals(4096, progress.contributors().get(ALICE).scoreUnits());
        assertEquals(4096, progress.contributors().get(BOB).scoreUnits());
        assertEquals(Optional.of(ALICE), progress.winner());
        assertEquals(8, progress.revision());
        for (TierOneProgress.Resource resource : TierOneProgress.Resource.values()) {
            TierOneProgress.ContributionResult replay = progress.contribute(CAROL, resource, Long.MAX_VALUE);
            assertEquals(0, replay.acceptedCount());
            assertFalse(replay.completedNow());
            assertSame(progress, replay.progress());
            assertEquals(Optional.of(ALICE), replay.progress().winner());
        }
    }

    @Test
    void deterministicUuidFallbackMakesRestoredTiesIndependentOfMapOrder() {
        Map<UUID, TierOneProgress.Contributor> first = new HashMap<>();
        first.put(BOB, new TierOneProgress.Contributor(1, 0, 0, 0, 1));
        first.put(ALICE, new TierOneProgress.Contributor(1, 0, 0, 0, 1));
        TierOneProgress progress = new TierOneProgress(1, first, Optional.empty());
        assertEquals(Optional.of(ALICE), progress.leader());
    }

    @Test
    void inputsAndReturnedMapsCannotMutateAnInstalledSnapshot() {
        Map<UUID, TierOneProgress.Contributor> mutable = new HashMap<>();
        mutable.put(ALICE, new TierOneProgress.Contributor(1, 0, 0, 0, 1));
        TierOneProgress progress = new TierOneProgress(1, mutable, Optional.empty());
        mutable.clear();
        assertEquals(1, progress.total(LOGS));
        assertThrows(UnsupportedOperationException.class, () -> progress.contributors().clear());
    }

    @Test
    void invalidRestoredStateCannotMasqueradeAsLegitimateProgress() {
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress(-1, Map.of(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress.Contributor(-1, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress.Contributor(0, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress.Contributor(1025, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress.Contributor(1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress(0,
                Map.of(ALICE, new TierOneProgress.Contributor(1, 0, 0, 0, 1)), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress(2,
                Map.of(ALICE, new TierOneProgress.Contributor(1024, 0, 0, 0, 1),
                        BOB, new TierOneProgress.Contributor(1, 0, 0, 0, 2)), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress(0, Map.of(), Optional.of(ALICE)));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress(1,
                Map.of(ALICE, new TierOneProgress.Contributor(1024, 2048, 512, 128, 1)), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new TierOneProgress(1,
                Map.of(ALICE, new TierOneProgress.Contributor(1024, 2048, 512, 128, 1)), Optional.of(BOB)));
    }

    @Test
    void exhaustedRevisionFailsBeforeChangingState() {
        TierOneProgress progress = new TierOneProgress(Long.MAX_VALUE, Map.of(), Optional.empty());
        assertThrows(IllegalStateException.class, () -> progress.contribute(ALICE, LOGS, 1));
        assertEquals(0, progress.total(LOGS));
    }

    @Test
    void independentlyPlannedConcurrentRequestsDoNotMutateEachOthersInputs() {
        TierOneProgress base = TierOneProgress.initial();
        TierOneProgress alicePlan = base.contribute(ALICE, LOGS, 1024).progress();
        TierOneProgress bobPlan = base.contribute(BOB, LOGS, 1024).progress();
        assertEquals(0, base.total(LOGS));
        assertEquals(1, alicePlan.revision());
        assertEquals(1, bobPlan.revision());
        assertEquals(Optional.of(ALICE), alicePlan.leader());
        assertEquals(Optional.of(BOB), bobPlan.leader());
        // An authoritative adapter must serialize/CAS installation, not merge
        // these independently based plans or consume both inventories blindly.
    }
}
