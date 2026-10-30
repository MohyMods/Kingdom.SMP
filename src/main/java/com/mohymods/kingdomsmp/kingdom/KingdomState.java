package com.mohymods.kingdomsmp.kingdom;

/**
 * Immutable persisted kingdom identity for the foundation slice.
 *
 * <p>New kingdoms begin at Tier 1. Tier advancement and rewards are separate
 * operations and are not granted by constructing a snapshot. Revisions identify authoritative snapshots;
 * clients never choose these values.</p>
 */
public record KingdomState(int schemaVersion, long revision, int attainedTier) {
    public static final int CURRENT_SCHEMA = 1;
    public static final int MAX_TIER = 12;

    public KingdomState {
        if (schemaVersion != CURRENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported kingdom schema: " + schemaVersion);
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Kingdom revision must not be negative");
        }
        if (attainedTier < 1 || attainedTier > MAX_TIER) {
            throw new IllegalArgumentException("Kingdom tier must be between 1 and " + MAX_TIER);
        }
    }

    public static KingdomState initial() {
        return new KingdomState(CURRENT_SCHEMA, 0, 1);
    }

    public boolean established() {
        return attainedTier > 0;
    }
}
