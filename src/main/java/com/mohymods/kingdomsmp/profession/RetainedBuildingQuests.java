package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Approved Builder retained-material objectives, separate from inspections and crafting deliveries. */
public final class RetainedBuildingQuests {
    private static final Map<String,Integer> GOALS=Map.of("kingdom_smp:builder/a01",128,"kingdom_smp:builder/a02",64,"kingdom_smp:builder/a03",64,"kingdom_smp:builder/a06",48,"kingdom_smp:builder/j02",80,"kingdom_smp:builder/m01",384,"kingdom_smp:builder/m02",96,"kingdom_smp:builder/j06",16);
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Set<String> closure=new HashSet<>(GOALS.keySet());
        for(var id:GOALS.keySet()){var d=ApprovedProfessionContent.get(id);if(!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.BUILD))&&!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.BUILD,ApprovedProfessionContent.Validation.INSPECT)))throw new IllegalStateException("Unvalidated retained definition");for(var rank:ProfessionRank.values())if(rank.ordinal()<d.rank().ordinal())for(int n=1;n<=8;n++)closure.add(d.profession().questId(rank,n));}
        var queue=new ArrayDeque<>(closure);while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);
        CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
    }
    public static Set<String> ids(){return GOALS.keySet();}
    private RetainedBuildingQuests() { }
    public static boolean tracked(String id){return GOALS.containsKey(id);}
    public static boolean contains(String id){return tracked(id)&&!id.endsWith("j06");}
    public static ApprovedProfessionContent.Definition get(String id){if(!tracked(id))throw new IllegalArgumentException("No validated retained building adapter for objective");return ApprovedProfessionContent.get(id);}
    public static int goal(String id){get(id);return GOALS.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
}
