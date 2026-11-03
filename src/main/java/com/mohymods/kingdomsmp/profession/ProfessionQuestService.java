package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;
import java.util.*;

/** Routes only implemented approved objectives; client quest IDs never provide evidence or XP. */
public final class ProfessionQuestService {
    private static final ProfessionQuestCatalog CATALOG;
    static {
        var definitions=new ArrayList<>(FarmerQuests.catalog().definitions().values());
        definitions.addAll(CommissionQuests.catalog().definitions().values().stream().filter(d->CommissionQuests.contains(d.id())).toList());
        definitions.addAll(InspectionQuests.catalog().definitions().values().stream().filter(d->InspectionQuests.contains(d.id())).toList());
        definitions.addAll(CraftDeliveryQuests.catalog().definitions().values());
        definitions.addAll(RetainedBuildingQuests.catalog().definitions().values().stream().filter(d->RetainedBuildingQuests.contains(d.id())).toList());
        definitions.addAll(TreeQuestService.catalog().definitions().values());
        CATALOG=ProfessionQuestCatalog.approved(definitions);
    }
    private ProfessionQuestService() { }
    public static ProfessionQuestCatalog catalog() { return CATALOG; }
    public static String title(String id) { if(TreeQuestService.contains(id))return ApprovedProfessionContent.get(id).name(); if(RetainedBuildingQuests.contains(id))return RetainedBuildingQuests.get(id).name(); return CraftDeliveryQuests.contains(id)?CraftDeliveryQuests.get(id).name():InspectionQuests.contains(id)?InspectionQuests.get(id).name():CommissionQuests.contains(id)?CommissionQuests.get(id).title():FarmerQuests.title(id); }
    public static long xp(String id) { if(TreeQuestService.contains(id))return ApprovedProfessionContent.get(id).xp(); if(RetainedBuildingQuests.contains(id))return RetainedBuildingQuests.get(id).xp(); return CraftDeliveryQuests.contains(id)?CraftDeliveryQuests.get(id).xp():InspectionQuests.contains(id)?InspectionQuests.get(id).xp():CommissionQuests.contains(id)?CommissionQuests.xp(id):FarmerQuests.xp(id); }
    public static String status(ServerPlayer player,String id) {
        if(TreeQuestService.contains(id))return TreeQuestService.status(player,id);
        if(RetainedBuildingQuests.contains(id))return RetainedBuildingService.status(player,id);
        if(CraftDeliveryQuests.contains(id))return CraftDeliveryService.status(player,id);
        if(InspectionQuests.contains(id)){
            var d=InspectionQuests.get(id);var roster=CommerceService.store(player.server).professions(player.getUUID());
            if(roster.track(d.profession()).completedQuestIds().contains(id))return "Complete. Your inspected project and earned XP are recorded.";
            return "Administrator inspection: "+d.objective()+". "+d.quantity()+". Confirm authorship, working access and claim consent on site.";
        }
        if (!CommissionQuests.contains(id)) return FarmerQuestService.status(player,id);
        var d=CommissionQuests.get(id); var store=CommerceService.store(player.server); var roster=store.professions(player.getUUID());
        if(roster.track(d.profession()).completedQuestIds().contains(id)) return "Complete. Your profession XP is recorded.";
        if(!roster.isActive(d.profession())) return "Activate "+ProfessionService.name(d.profession())+" at its completed premises.";
        long count=store.commissionContractPeriods(player.getUUID(),id);
        return count+" / "+CommissionQuestRules.get(player.server).goal(id)+" distinct "+d.cycle()+" contracts completed during this objective band. Deliver at the assigned profession resident; payment claims do not create extra progress.";
    }
    public static boolean ready(ServerPlayer player,String id) {
        if(TreeQuestService.contains(id))return TreeQuestService.ready(player,id);
        if(RetainedBuildingQuests.contains(id))return RetainedBuildingService.ready(player,id);
        if(InspectionQuests.contains(id)||CraftDeliveryQuests.contains(id))return false;
        if (!CommissionQuests.contains(id)) return FarmerQuestService.ready(player,id);
        var d=CommissionQuests.get(id);var store=CommerceService.store(player.server);var roster=store.professions(player.getUUID());
        return com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(player.server).attainedTier()>=ApprovedProfessionContent.get(id).minimumTier() && roster.isActive(d.profession()) && !roster.track(d.profession()).completedQuestIds().contains(id)
            && CATALOG.mayAttempt(d.profession(),id,roster.track(d.profession()).completedQuestIds())
            && ProfessionService.available(player,d.profession())
            && store.commissionContractPeriods(player.getUUID(),id) >= CommissionQuestRules.get(player.server).goal(id);
    }
    public static void complete(ServerPlayer player,UUID operation,String id) throws IOException {
        if(TreeQuestService.contains(id)){TreeQuestService.complete(player,operation,id);return;}
        if(RetainedBuildingQuests.contains(id)){RetainedBuildingService.complete(player,operation,id);return;}
        if(CraftDeliveryQuests.contains(id))throw new IllegalStateException("Use the reviewed crafted-material delivery; an event completion cannot consume materials");
        if(InspectionQuests.contains(id))throw new IllegalStateException("An authenticated administrator must inspect and confirm this site");
        if (!CommissionQuests.contains(id)) { FarmerQuestService.complete(player,operation,id); return; }
        var d=CommissionQuests.get(id);
        ContractService.requireCounter(player,d.profession());
        if(!ready(player,id))throw new IllegalStateException(status(player,id));
        CommerceService.store(player.server).completeCommissionQuest(operation,player.getUUID(),id,CommissionQuestRules.get(player.server).goal(id));
    }
}
