package com.mohymods.kingdomsmp.profession;

import java.util.*;

/** Only inspection families with implemented live geometry checks may be activated. */
public final class InspectionQuests {
    private static final Set<String> IDS=Set.of(CreateDriveInspection.QUEST,LibraryFieldGuide.QUEST,"kingdom_smp:alchemist/a01","kingdom_smp:alchemist/a08","kingdom_smp:merchant/a08","kingdom_smp:builder/a04","kingdom_smp:builder/a08","kingdom_smp:lumberjack/a06","kingdom_smp:lumberjack/a08","kingdom_smp:builder/j01","kingdom_smp:builder/j03","kingdom_smp:builder/j04","kingdom_smp:builder/j05","kingdom_smp:builder/j06","kingdom_smp:builder/j08","kingdom_smp:lumberjack/j08","kingdom_smp:blacksmith/a08","kingdom_smp:blacksmith/j08","kingdom_smp:librarian/a02","kingdom_smp:librarian/a03","kingdom_smp:librarian/a06","kingdom_smp:librarian/a08");
    private static final ProfessionQuestCatalog CATALOG;
    static {
        Set<String> closure=new HashSet<>(IDS);for(var id:IDS){var d=ApprovedProfessionContent.get(id);for(var rank:ProfessionRank.values())if(rank.ordinal()<d.rank().ordinal())for(int n=1;n<=8;n++)closure.add(d.profession().questId(rank,n));}var queue=new ArrayDeque<>(closure);
        while(!queue.isEmpty())for(var prior:ApprovedProfessionContent.get(queue.removeFirst()).prerequisites())if(closure.add(prior))queue.addLast(prior);
        CATALOG=ProfessionQuestCatalog.approved(closure.stream().map(id->ApprovedProfessionContent.get(id).slot()).toList());
        for(var id:IDS){var d=ApprovedProfessionContent.get(id);if((!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.INSPECT))&&!d.validation().equals(Set.of(ApprovedProfessionContent.Validation.BUILD,ApprovedProfessionContent.Validation.INSPECT))&&!(id.equals(LibraryFieldGuide.QUEST)&&d.validation().equals(Set.of(ApprovedProfessionContent.Validation.EXPLORE,ApprovedProfessionContent.Validation.INSPECT))))||d.minimumTier()!=(d.profession()==Profession.ENGINEER?4:d.profession()==Profession.ALCHEMIST?5:d.profession()==Profession.MERCHANT?3:d.profession()==Profession.LIBRARIAN?(d.rank()==ProfessionRank.APPRENTICE?4:6):d.profession()==Profession.BLACKSMITH?(d.rank()==ProfessionRank.APPRENTICE?3:4):d.rank()==ProfessionRank.APPRENTICE?2:3))throw new IllegalStateException("Unvalidated inspection content");}
    }
    private InspectionQuests() { }
    public static Set<String> ids(){return IDS;}
    public static boolean contains(String id){return IDS.contains(id);}
    public static ApprovedProfessionContent.Definition get(String id){if(!contains(id))throw new IllegalArgumentException("No validated inspection adapter for this objective");return ApprovedProfessionContent.get(id);}
    public static ProfessionQuestCatalog catalog(){return CATALOG;}
}
