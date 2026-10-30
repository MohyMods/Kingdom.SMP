package com.mohymods.kingdomsmp.kingdom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class KingdomStateTest {
    @Test
    void freshWorldBeginsAtApprovedTierOne() {
        KingdomState fresh = KingdomState.initial();
        assertEquals(1, fresh.schemaVersion());
        assertEquals(0, fresh.revision());
        assertEquals(1, fresh.attainedTier());
        assertTrue(fresh.established());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void allTwelveEstablishedTiersCanBeRepresented(int tier) {
        KingdomState state = new KingdomState(1, 25, tier);
        assertTrue(state.established());
        assertEquals(tier, state.attainedTier());
        assertEquals(25, state.revision());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 13, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void outOfRangePersistedTiersFailRatherThanBeingClamped(int tier) {
        assertThrows(IllegalArgumentException.class, () -> new KingdomState(1, 0, tier));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2, Integer.MAX_VALUE})
    void oldOrFutureSchemaRequiresExplicitMigration(int schema) {
        assertThrows(IllegalArgumentException.class, () -> new KingdomState(schema, 0, 1));
    }

    @Test
    void invalidRevisionCannotBePresentedAsAuthoritativeState() {
        assertThrows(IllegalArgumentException.class, () -> new KingdomState(1, -1, 1));
    }

    @Test
    void highestRevisionRemainsRepresentableWithoutAnImplicitIncrement() {
        assertEquals(Long.MAX_VALUE, new KingdomState(1, Long.MAX_VALUE, 12).revision());
    }
}
