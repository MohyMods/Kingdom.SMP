package com.mohymods.kingdomsmp.profession;

import java.util.Objects;

/** Stable domain identity. Unregistered building references do not grant an unlock. */
public enum Profession {
    FARMER("farmer", "kingdom_smp:farmers_house"),
    BLACKSMITH("blacksmith"), MINER("miner"),
    BUILDER("builder", "kingdom_smp:builders_workshop"),
    LUMBERJACK("lumberjack", "kingdom_smp:lumberjacks_lodge"),
    MERCHANT("merchant"), ENGINEER("engineer"), MECHANIC("mechanic"),
    MAGE("mage"), ADVENTURER("adventurer"), ALCHEMIST("alchemist"), LIBRARIAN("librarian");

    private final String id, buildingId;
    Profession(String id) { this(id, "kingdom_smp:profession/" + id); }
    Profession(String id, String buildingId) { this.id = id;this.buildingId = buildingId; }
    public String id() { return id; }
    public String buildingId() { return buildingId; }
    public static Profession byId(String id) {
        Objects.requireNonNull(id);
        for (var profession : values()) if (profession.id.equals(id)) return profession;
        throw new IllegalArgumentException("Unknown profession: " + id);
    }
    public String questId(ProfessionRank rank, int ordinal) {
        Objects.requireNonNull(rank);
        if (ordinal < 1 || ordinal > 8) throw new IllegalArgumentException("Profession quest ordinal must be 1–8");
        return "kingdom_smp:" + id + "/" + rank.questPrefix() + "0" + ordinal;
    }
    public boolean ownsQuestId(String quest) {
        if (quest == null) return false;
        for (var rank : ProfessionRank.values()) for (int n = 1; n <= 8; n++) if (questId(rank, n).equals(quest)) return true;
        return false;
    }
}
