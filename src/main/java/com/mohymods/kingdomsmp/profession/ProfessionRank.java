package com.mohymods.kingdomsmp.profession;

public enum ProfessionRank {
    APPRENTICE("apprentice", "a"), JOURNEYMAN("journeyman", "j"), MASTER("master", "m");
    private final String id, questPrefix;
    ProfessionRank(String id, String questPrefix) { this.id = id;this.questPrefix = questPrefix; }
    public String id() { return id; }
    String questPrefix() { return questPrefix; }
    public static ProfessionRank byId(String id) {
        for (var rank : values()) if (rank.id.equals(id)) return rank;
        throw new IllegalArgumentException("Unknown profession rank: " + id);
    }
}
