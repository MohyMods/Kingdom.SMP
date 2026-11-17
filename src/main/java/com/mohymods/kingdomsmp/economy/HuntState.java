package com.mohymods.kingdomsmp.economy;

import java.util.*;

/** Immutable canonical entitlement; kill evidence never owns or grants currency. */
public record HuntState(UUID id, UUID player, long day, String target, int goal, long reward, boolean closed, boolean paid) {
    public static final List<String> TARGETS = List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper");
    public HuntState {
        Objects.requireNonNull(id); Objects.requireNonNull(player);
        if (day<0 || !TARGETS.contains(target) || goal<1 || goal>256 || reward<1 || reward>100_000 || paid&&!closed)
            throw new IllegalArgumentException("Invalid hunt entitlement");
    }
    public static String target(long day) {
        if (day < 0) throw new IllegalArgumentException("Invalid hunt day");
        return TARGETS.get((int) (day % TARGETS.size()));
    }
    public HuntState close(boolean paid) { return new HuntState(id, player, day, target, goal, reward, true, paid); }
}
