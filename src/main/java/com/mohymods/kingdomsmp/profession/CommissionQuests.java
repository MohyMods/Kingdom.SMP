package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Approved D-062 commissions whose evidence is entirely canonical contract history. */
public final class CommissionQuests {
    public record Definition(String id, Profession profession, ProfessionRank rank, String title, String cycle, int goal) { }
    private static final Map<String, Definition> DEFINITIONS;
    private static final ProfessionQuestCatalog CATALOG;
    static {
        var definitions = new HashMap<String, Definition>();
        for (var role : Profession.values()) for(var rank:ProfessionRank.values()) {
            if(role==Profession.FARMER)continue;
            String id=role.questId(rank,rank==ProfessionRank.MASTER?6:7);var approved=ApprovedProfessionContent.get(id);
            String cycle=rank==ProfessionRank.JOURNEYMAN?"weekly":"daily";int goal=rank==ProfessionRank.APPRENTICE?3:rank==ProfessionRank.JOURNEYMAN?2:5;
            if(!approved.validation().equals(Set.of(ApprovedProfessionContent.Validation.CONTRACT))||!approved.quantity().equals(goal+" "+cycle+" periods"))throw new IllegalStateException("Validated commission differs from approved content");
            definitions.put(id,new Definition(id,role,rank,approved.name(),cycle,goal));
        }
        DEFINITIONS=Map.copyOf(definitions);
        // Earlier bands are prerequisite metadata only; they do not activate their objective adapters.
        Set<String> closure=new HashSet<>(definitions.keySet());
        for(var d:definitions.values())for(var band:ProfessionRank.values())if(band.ordinal()<d.rank().ordinal())for(int n=1;n<=8;n++)closure.add(d.profession().questId(band,n));
        var queue=new ArrayDeque<>(closure);while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);
        CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
    }
    private CommissionQuests() { }
    public static Set<String> ids(){return DEFINITIONS.keySet();}
    public static ProfessionQuestCatalog catalog() { return CATALOG; }
    public static boolean contains(String id) { return DEFINITIONS.containsKey(id); }
    public static Definition get(String id) {
        var d = DEFINITIONS.get(id);
        if (d == null) throw new IllegalArgumentException("This profession commission has no validated implementation");
        return d;
    }
    public static long xp(String id) { return (get(id).rank().ordinal()+1)*100L; }
}
