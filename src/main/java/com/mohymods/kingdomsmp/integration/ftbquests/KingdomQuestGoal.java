package com.mohymods.kingdomsmp.integration.ftbquests;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/** Versioned, finite projection keys. Unknown tags and future goals never grant progress. */
public enum KingdomQuestGoal {
    HUT("hut", 1), CONTRIBUTION("contribution", 2), TIER_TWO("tier_two", 3),
    HEADQUARTERS("headquarters", 4), PURCHASE("purchase", 5), PUBLIC_BUILDING("public_building", 6),
    TIER_THREE("tier_three", 7), BANK("bank", 8), PUBLIC_TRADE("public_trade", 9);

    public static final String TAG_PREFIX = "kingdom_smp:projection/";
    public static final String VERSIONED_PREFIX = TAG_PREFIX + "v1/";
    public static final long CHAPTER_ID = 0x4B534D5000000001L;
    private static final Set<String> PAID_BUILDINGS = Set.of("kingdom_smp:farmers_house",
            "kingdom_smp:builders_workshop", "kingdom_smp:lumberjacks_lodge", "kingdom_smp:royal_bank");
    private final String key;
    private final int sequence;

    KingdomQuestGoal(String key, int sequence) { this.key = key; this.sequence = sequence; }
    public String tag() { return VERSIONED_PREFIX + key; }
    public long questId() { return 0x4B534D5000000100L + sequence; }
    public long taskId() { return 0x4B534D5000001100L + sequence; }
    public String code() { return String.format("%016X", taskId()); }

    public static Optional<KingdomQuestGoal> parseTag(String tag) {
        if (tag == null || !tag.startsWith(VERSIONED_PREFIX)) return Optional.empty();
        return Arrays.stream(values()).filter(goal -> goal.tag().equals(tag)).findFirst();
    }

    public boolean complete(KingdomQuestSnapshot snapshot) {
        return switch (this) {
            case HUT -> snapshot.completedBuildingIds().contains("kingdom_smp:builders_hut");
            case CONTRIBUTION -> snapshot.contributionsStarted();
            case TIER_TWO -> snapshot.tier() >= 2;
            case HEADQUARTERS -> snapshot.tier() >= 2 && (snapshot.completedBuildingIds().contains("kingdom_smp:royal_town_hall")
                    || snapshot.completedBuildingIds().contains("kingdom_smp:founders_lodge"));
            case PURCHASE -> snapshot.tier() >= 2 && snapshot.purchasedBuildingIds().stream().anyMatch(PAID_BUILDINGS::contains);
            case PUBLIC_BUILDING -> snapshot.tier() >= 2 && snapshot.purchasedBuildingIds().stream()
                    .anyMatch(id -> PAID_BUILDINGS.contains(id) && snapshot.completedBuildingIds().contains(id));
            case TIER_THREE -> snapshot.tier() >= 3;
            case BANK -> snapshot.tier() >= 3 && snapshot.completedBuildingIds().contains("kingdom_smp:royal_bank");
            case PUBLIC_TRADE -> snapshot.tier() >= 3 && snapshot.publicTradeCompleted();
        };
    }
}
