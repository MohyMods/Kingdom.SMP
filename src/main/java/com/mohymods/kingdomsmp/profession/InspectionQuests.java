package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Only inspection families with implemented live geometry checks may be activated. */
public final class InspectionQuests {
    private static final Set<String> IDS=Set.of("kingdom_smp:builder/a04","kingdom_smp:builder/a08","kingdom_smp:lumberjack/a06");
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Set<String> closure=new HashSet<>(IDS);var queue=new ArrayDeque<>(IDS);
        while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);
        CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
        for(var id:IDS){var d=ApprovedProfessionContent.get(id);if(!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.INSPECT))||d.minimumTier()!=2)throw new IllegalStateException("Unvalidated inspection content");}
    }
    private InspectionQuests() { }
    public static Set<String> ids(){return IDS;}
    public static boolean contains(String id){return IDS.contains(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated inspection adapter for this objective");return ApprovedProfessionContent.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
}
