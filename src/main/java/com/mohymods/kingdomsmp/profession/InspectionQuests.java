package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Only inspection families with implemented live geometry checks may be activated. */
public final class InspectionQuests {
    private static final Set<String> IDS=Set.of("kingdom_smp:builder/a04","kingdom_smp:lumberjack/a06");
    private static final ProfessionQuestCatalog CATALOG=ProfessionQuestCatalog.approved(IDS.stream().map(id->{
        var d=ApprovedProfessionContent.get(id);
        if(!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.INSPECT))||d.minimumTier()!=2)throw new IllegalStateException("Unvalidated inspection content");
        return d.slot();
    }).toList());
    private InspectionQuests() { }
    public static boolean contains(String id){return IDS.contains(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated inspection adapter for this objective");return ApprovedProfessionContent.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
}
