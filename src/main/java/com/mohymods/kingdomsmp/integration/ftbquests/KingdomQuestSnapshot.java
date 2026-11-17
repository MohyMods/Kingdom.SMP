package com.mohymods.kingdomsmp.integration.ftbquests;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable shared-kingdom facts, supplied by already initialized authoritative services. */
public record KingdomQuestSnapshot(int tier, Set<String> completedBuildingIds, Set<String> purchasedBuildingIds,
                                   boolean contributionsStarted, boolean publicTradeCompleted) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public KingdomQuestSnapshot {
        if (tier < 1 || tier > 12) throw new IllegalArgumentException("Invalid projected kingdom tier");
        completedBuildingIds = checked(completedBuildingIds);
        purchasedBuildingIds = checked(purchasedBuildingIds);
    }

    private static Set<String> checked(Set<String> ids) {
        Objects.requireNonNull(ids);
        if (ids.size() > 128) throw new IllegalArgumentException("Too many projected buildings");
        for (String id : ids) if (id == null || id.length() > 160 || !ID.matcher(id).matches())
            throw new IllegalArgumentException("Invalid projected building identifier");
        return Set.copyOf(ids);
    }
}
