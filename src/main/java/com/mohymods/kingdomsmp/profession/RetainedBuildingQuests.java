package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Approved Builder retained-material objectives, separate from inspections and crafting deliveries. */
public final class RetainedBuildingQuests {
    private static final Map<String,Integer> GOALS=Map.of("kingdom_smp:builder/a01",128,"kingdom_smp:builder/a02",64,"kingdom_smp:builder/a03",64,"kingdom_smp:builder/a06",48);
    private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(GOALS.keySet().stream().map(id->{
        var d=ApprovedProfessionContent.get(id);if(d.minimumTier()!=2||!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.BUILD)))throw new IllegalStateException("Unvalidated retained building definition");return d.slot();
    }).toList());
    private RetainedBuildingQuests() { }
    public static boolean contains(String id){return GOALS.containsKey(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated retained building adapter for objective");return ApprovedProfessionContent.get(id);}
    public static int goal(String id){get(id);return GOALS.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
}
