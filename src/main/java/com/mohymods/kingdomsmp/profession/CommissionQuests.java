package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Approved D-062 commissions whose evidence is entirely canonical contract history. */
public final class CommissionQuests {
    public record Definition(String id, Profession profession, ProfessionRank rank, String title, String cycle, int goal) { }
    private static final Map<String, Definition> DEFINITIONS;
    private static final ProfessionQuestCatalog CATALOG;
    static {
        var definitions = new HashMap<String, Definition>();
        for (var role : List.of(Profession.BUILDER, Profession.LUMBERJACK)) {
            String id = role.questId(ProfessionRank.APPRENTICE, 7);
            var approved=ApprovedProfessionContent.get(id);
            if(!approved.validation().equals(Set.of(ApprovedProfessionContent.Validation.CONTRACT))||approved.minimumTier()!=2||!approved.quantity().equals("3 daily periods"))throw new IllegalStateException("Validated commission differs from approved content");
            definitions.put(id, new Definition(id, role, approved.rank(),approved.name(), "daily", 3));
        }
        DEFINITIONS = Map.copyOf(definitions);
        CATALOG = ProfessionQuestCatalog.approved(definitions.values().stream().map(d ->
                new ProfessionQuestCatalog.Definition(d.id(), d.profession(), d.rank(), 7, Set.of())).toList());
    }
    private CommissionQuests() { }
    public static ProfessionQuestCatalog catalog() { return CATALOG; }
    public static boolean contains(String id) { return DEFINITIONS.containsKey(id); }
    public static Definition get(String id) {
        var d = DEFINITIONS.get(id);
        if (d == null) throw new IllegalArgumentException("This profession commission has no validated implementation");
        return d;
    }
    public static long xp(String id) { return (get(id).rank().ordinal()+1)*100L; }
}
