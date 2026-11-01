package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** D-034: approved Farmer objective identities and the 8/8/8 rank policy. */
public final class FarmerQuests {
    private FarmerQuests() { }
    private static final String[] TITLES = {
        "Prepare the soil", "Wheat harvest", "Carrot harvest", "Potato harvest", "First livestock", "The village baker", "Bread for the community", "A working farm",
        "Beetroot and wheat", "Roots for winter", "From stem to harvest", "Varied livestock", "Eggs and milk", "Celebration cakes", "Daily provisioner", "Shared irrigation",
        "Four-field harvest", "Master husbandry", "The kingdom banquet", "Weekly provisioner", "Community food contracts", "Sustainable cultivation", "Agricultural estate", "Farmer stewardship"
    };
    private static final ProfessionQuestCatalog CATALOG;
    static {
        var definitions = new ArrayList<ProfessionQuestCatalog.Definition>();
        for (var rank : ProfessionRank.values()) for (int ordinal = 1; ordinal <= 8; ordinal++) {
            var prerequisites = new HashSet<String>();
            // The final inspection also requires all seven other Master proofs.
            if (rank == ProfessionRank.MASTER && ordinal == 8) for (int n=1;n<8;n++) prerequisites.add(id(ProfessionRank.MASTER,n));
            definitions.add(new ProfessionQuestCatalog.Definition(id(rank,ordinal),Profession.FARMER,rank,ordinal,prerequisites));
        }
        CATALOG = ProfessionQuestCatalog.approved(definitions);
    }
    public static ProfessionRank band(ProfessionRoster roster) {var ids=roster.track(Profession.FARMER).completedQuestIds();if(!CATALOG.completedBand(Profession.FARMER,ProfessionRank.APPRENTICE,ids))return ProfessionRank.APPRENTICE;return CATALOG.completedBand(Profession.FARMER,ProfessionRank.JOURNEYMAN,ids)?ProfessionRank.MASTER:ProfessionRank.JOURNEYMAN;}
    public static ProfessionQuestCatalog catalog() { return CATALOG; }
    public static String id(ProfessionRank rank,int ordinal) { return Profession.FARMER.questId(rank,ordinal); }
    public static boolean inspection(String id) { return Set.of(id(ProfessionRank.APPRENTICE,8),id(ProfessionRank.JOURNEYMAN,8),id(ProfessionRank.MASTER,7),id(ProfessionRank.MASTER,8)).contains(id); }
    public static boolean food(String id) { return Set.of(id(ProfessionRank.APPRENTICE,7),id(ProfessionRank.JOURNEYMAN,5),id(ProfessionRank.JOURNEYMAN,6),id(ProfessionRank.MASTER,3)).contains(id); }
    public static String title(String id) {
        var d=CATALOG.definition(id).orElseThrow(()->new IllegalArgumentException("Unknown approved Farmer quest"));
        return TITLES[d.rank().ordinal()*8+d.ordinal()-1];
    }
    /** XP is recognition; objective completion, not an XP shortcut, determines rank. */
    public static long xp(String id) { return (CATALOG.definition(id).orElseThrow(()->new IllegalArgumentException("Unknown approved Farmer quest")).rank().ordinal()+1)*100L; }
}
