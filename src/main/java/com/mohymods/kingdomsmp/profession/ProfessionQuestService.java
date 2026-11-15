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
        BrewQuests.ids().forEach(id->definitions.add(ApprovedProfessionContent.get(id).slot()));
        MerchantTransactionQuests.ids().forEach(id->definitions.add(ApprovedProfessionContent.get(id).slot()));
        definitions.addAll(CommissionQuests.catalog().definitions().values().stream().filter(d->CommissionQuests.contains(d.id())).toList());
        definitions.addAll(InspectionQuests.catalog().definitions().values().stream().filter(d->InspectionQuests.contains(d.id())).toList());
        definitions.addAll(CraftDeliveryQuests.catalog().definitions().values().stream().filter(d->CraftDeliveryQuests.contains(d.id())).toList());
        definitions.addAll(RetainedBuildingQuests.catalog().definitions().values().stream().filter(d->RetainedBuildingQuests.contains(d.id())).toList());
        definitions.add(ApprovedProfessionContent.get(com.mohymods.kingdomsmp.economy.ServiceWorkState.SMITH).slot());
        definitions.add(ApprovedProfessionContent.get(com.mohymods.kingdomsmp.economy.ServiceWorkState.LIBRARY).slot());
        definitions.add(ApprovedProfessionContent.get(LibraryGlyphCatalogue.QUEST).slot());
        definitions.add(ApprovedProfessionContent.get(RepairService.QUEST).slot());
        definitions.add(ApprovedProfessionContent.get(RepairService.JOURNEYMAN).slot());
        definitions.add(ApprovedProfessionContent.get(EnchantedToolService.QUEST).slot());
        definitions.add(ApprovedProfessionContent.get(SmithingQuestService.QUEST).slot());
        definitions.add(ApprovedProfessionContent.get(NativeOutputQuests.LIBRARY_ENCHANT).slot());
        definitions.add(ApprovedProfessionContent.get(NativeOutputQuests.LIBRARY_BOOKS).slot());
        definitions.add(ApprovedProfessionContent.get(NativeOutputQuests.LIBRARY_FAMILIES).slot());
        definitions.add(ApprovedProfessionContent.get(NativeOutputQuests.LIBRARY_MAXIMUM).slot());
        definitions.addAll(TreeQuestService.catalog().definitions().values().stream().filter(d->TreeQuestService.contains(d.id())).toList());
        definitions.addAll(EncounterQuests.catalog().definitions().values());
        definitions.addAll(MineAccessService.catalog().definitions().values());
        definitions.addAll(NativeOreQuests.catalog().definitions().values().stream().filter(d->NativeOreQuests.contains(d.id())).toList());
        CATALOG=ProfessionQuestCatalog.approved(definitions);
    }
    private ProfessionQuestService() { }
    /** Presentation only. These slots must never become the rank or evidence catalogue. */
    public static ProfessionQuestCatalog presentationCatalog() {
        var rows=new ArrayList<>(FarmerQuests.catalog().definitions().values());
        rows.addAll(ApprovedProfessionContent.definitions().values().stream().map(ApprovedProfessionContent.Definition::slot).toList());
        return ProfessionQuestCatalog.approved(rows);
    }
    public static boolean implemented(String id) { return CATALOG.definitions().containsKey(id); }

    public static ProfessionQuestCatalog catalog() { return CATALOG; }
    public static String title(String id) { if(LibraryGlyphCatalogue.QUEST.equals(id))return ApprovedProfessionContent.get(id).name(); if(ServiceWorkQuests.contains(id))return ApprovedProfessionContent.get(id).name(); if(!implemented(id)&&ApprovedProfessionContent.definitions().containsKey(id))return ApprovedProfessionContent.get(id).name(); if(MineAccessService.QUEST.equals(id))return ApprovedProfessionContent.get(id).name(); if(EncounterQuests.QUEST.equals(id))return ApprovedProfessionContent.get(id).name(); if(NativeOreQuests.contains(id))return ApprovedProfessionContent.get(id).name(); if(MerchantTransactionQuests.contains(id))return MerchantTransactionQuests.get(id).name(); if(NativeOutputQuests.contains(id))return NativeOutputQuests.get(id).name(); if(EnchantedToolService.QUEST.equals(id)||SmithingQuestService.QUEST.equals(id))return ApprovedProfessionContent.get(id).name(); if(RepairService.contains(id))return ApprovedProfessionContent.get(id).name(); if(TreeQuestService.contains(id))return ApprovedProfessionContent.get(id).name(); if(RetainedBuildingQuests.contains(id))return RetainedBuildingQuests.get(id).name(); return CraftDeliveryQuests.contains(id)?CraftDeliveryQuests.get(id).name():InspectionQuests.contains(id)?InspectionQuests.get(id).name():CommissionQuests.contains(id)?CommissionQuests.get(id).title():FarmerQuests.title(id); }
    public static long xp(String id) { if(LibraryGlyphCatalogue.QUEST.equals(id))return 200; if(ServiceWorkQuests.contains(id))return ApprovedProfessionContent.get(id).xp(); if(MineAccessService.QUEST.equals(id))return 100; if(EncounterQuests.QUEST.equals(id))return 100; if(NativeOreQuests.contains(id))return ApprovedProfessionContent.get(id).xp(); if(MerchantTransactionQuests.contains(id))return MerchantTransactionQuests.get(id).xp(); if(NativeOutputQuests.contains(id))return NativeOutputQuests.get(id).xp(); if(SmithingQuestService.QUEST.equals(id))return 300; if(EnchantedToolService.QUEST.equals(id))return 200; if(RepairService.contains(id))return ApprovedProfessionContent.get(id).xp(); if(TreeQuestService.contains(id))return ApprovedProfessionContent.get(id).xp(); if(RetainedBuildingQuests.contains(id))return RetainedBuildingQuests.get(id).xp(); return CraftDeliveryQuests.contains(id)?CraftDeliveryQuests.get(id).xp():InspectionQuests.contains(id)?InspectionQuests.get(id).xp():CommissionQuests.contains(id)?CommissionQuests.xp(id):FarmerQuests.xp(id); }
    public static String status(ServerPlayer player,String id) {
        if(LibraryGlyphCatalogue.QUEST.equals(id))return LibraryGlyphCatalogueService.status();
        if(ServiceWorkQuests.contains(id))return ServiceWorkService.status(player,id);
        if(id==null||!CATALOG.definitions().containsKey(id))return "COMING SOON — QUEST NOT YET AVAILABLE. This objective has no validated runtime implementation yet. Existing progress is preserved.";
        if(com.mohymods.kingdomsmp.kingdom.AlphaScope.futureQuest(id))return com.mohymods.kingdomsmp.kingdom.AlphaScope.futureMessage();
        if(ForestReserveInspection.contains(id))return "Operator inspection of an authorized accessible community forest reserve: two maintained genuine native-planted species, current working two-species productive apiary, safe paths/lighting/replacement saplings, and your recorded six-species maturity/renewal history. Earlier earned supply and service proofs remain canonical prerequisites; placed logs and old unknown trees never substitute.";
        if(id.equals(ApiaryInspection.QUEST))return "Operator inspection: functioning flowering woodland apiary, accessible occupied native advanced hives, iron/copper forage and genuine recent production by both permitted pre-End species. Previously stocked combs never prove operation.";
        if(MineExcavation.contains(id))return "Request OP2 on-site excavation authorization before digging. Select uncleared stone/dirt away from structures and storage, retain the protected surrounding margin, then request a confirmed inspection at the original tracked mine entrance. Keep the route/lights/tool/sorting/plan and current claims intact. Final assessment also verifies your saved geology, specimens and Nether expeditions. No blocks are removed automatically.";
        if(MinePlan.QUEST.equals(id))return "Carry a book and quill with separate lines access=x,y,z (your tracked entrance), ore=x,y,z (a stocked sorting bin) and support=x,y,z (another retained route support). Keep the zones at least three blocks apart. Request an on-site operator review after all seven Journeyman objectives; the full current route, lights, tool, storage, claims and plan are rechecked. The book is retained.";
        if(MineInspection.contains(id))return "Request an on-site operator review at your recorded mine entrance. Keep the genuine walked route and "+MineAccessRules.get(player.server).lights()+" distinct supported lights permitted and loaded. Final assessment also checks a functional pickaxe and two separately stocked accessible ore-sorting bins. No inventory is consumed.";
        if(id.equals(BuilderCivicInspection.ROUTES))return "Operator inspection: two different grounded public route connections between at least three intact kingdom services. Visit the roads and all endpoint buildings. Center the review on the network; loaded supported/dry routes and current claims are rechecked, with no chunk tickets.";
        if(BuilderCivicInspection.building(id))return "Operator inspection of your maintained supplied civic project: current Master frame/decorative masonry, two-floor eight-zone interior, working verified furnishings, public access and consent. Earlier earned roads/contracts remain canonical prerequisites; no service unlock or free equipment.";
        if(id.equals(SupplementariesInspection.QUEST))return "Operator inspection of four working verified Supplementaries fittings: stocked sack, readable notice-board book, lit sconce and anchored climbable rope. Enable their native recipes; keep menus closed and the site permitted. Other optional furnishings remain unvalidated.";
        if(EncounterQuests.QUEST.equals(id))return EncounterQuests.status(player);
        if(MineAccessService.QUEST.equals(id))return MineAccessService.status(player);
        if(NativeOreQuests.contains(id))return NativeOreQuests.status(player,id);
        if(MerchantTransactionQuests.contains(id))return MerchantTransactionQuests.status(player,id);
        if(BrewQuests.EXPEDITION.equals(id))return "Personally brew and permanently deliver "+BrewQuestRules.goal(player.server,id)/3+" each of healing II, swiftness II and fire resistance. Actual native reagent/fuel costs and distinct authenticated outputs required; one family cannot replace another.";
        if(BrewQuests.UNDERWATER.equals(id))return "Deliver "+BrewQuestRules.goal(player.server,id)/2+" authenticated water-breathing and "+BrewQuestRules.goal(player.server,id)/2+" night-vision potions. Personally brew both families at an idle permitted stand, pay actual reagents/fuel and collect each batch.";
        if(BrewQuests.SPLASH.equals(id))return "Deliver "+BrewQuestRules.goal(player.server,id)+" authenticated beneficial splash potions. Use native gunpowder/fuel on a supported beneficial brew and collect your own batch. Harmful, lingering and untracked potions do not count.";
        if(BrewQuests.contains(id))return "Brew and deliver "+BrewQuestRules.goal(player.server,id)+" native "+BrewQuests.get(id).output()+" potions. Load bottles, insert reagent last into an idle permitted stand, then collect your batch. Native fuel/ingredients must be paid; keep inputs unchanged until completion.";
        if(NativeOutputQuests.journeymanBook(id))return (id.equals(NativeOutputQuests.LIBRARY_FAMILIES)?"Deliver distinct books from protection, tool, weapon and durability families; "+LibraryQuestRules.get(player.server).familyCopies()+" each.":"Deliver "+LibraryQuestRules.get(player.server).maximumBooks()+" books newly combined to a native maximum level.")+" Choose commission here before producing; existing outputs retain their purpose.";
        if(NativeOutputQuests.LIBRARY_BOOKS.equals(id))return "Combine and deliver "+LibraryQuestRules.get(player.server).enchantedBooks()+" distinct ordinary enchanted books through a permitted native anvil. Preserve compatible enchantments and pay actual books/XP; copied, stripped or recycled outputs do not count.";
        if(NativeOutputQuests.LIBRARY_ENCHANT.equals(id))return LibraryEnchantService.status(player);
        if(SmithingQuestService.QUEST.equals(id))return SmithingQuestService.status(player);
        if(EnchantedToolService.QUEST.equals(id))return EnchantedToolService.status(player);
        if(RepairService.contains(id))return RepairService.status(player,id);
        if(TreeQuestService.contains(id))return TreeQuestService.status(player,id);
        if(RetainedBuildingQuests.contains(id))return RetainedBuildingService.status(player,id);
        if(CraftDeliveryQuests.contains(id))return CraftDeliveryService.status(player,id);
        if(InspectionQuests.contains(id)){
            if(SmithCommunityForge.FINAL.equals(id))return "After all seven Master proofs, request final on-site assessment of the running community forge, safe maintenance and canonical returned customer services. Original paid premises must remain intact. No equipment is minted.";
            if(SmithCommunityForge.QUEST.equals(id))return "Community forge: two accessible furnaces (one running), anvil, two storage containers, water, lighting and safe access outside kingdom reservations. An on-site operator confirms authorship, community use and current permissions.";
            if(id.equals(CreateWorkshopInspection.QUEST))return "After all7personal proofs, request a live workshop assessment: safe water power, transmission, press/millstone, stopped clutch output, lighting and storage. Keep your maintained premises and permitted machines loaded; inspection never loads chunks.";
            if(id.equals(CreateTransmissionInspection.QUEST))return "Administrator reviews a working native gear ratio/reversal and live clutch output. Then turn on the reviewed adjacent lever and stop only that output; confirm within30seconds. Keep source, gearing, site geometry and permissions unchanged.";
            if(id.equals(LibraryFieldGuide.QUEST))return "Personally visit an unlocked Aether or Twilight biome during Journeyman trials. Write dimension=namespace:id biome=namespace:id in your own field guide, add readable observations, and request an on-site administrator inspection at the Library.";
            if(id.equals("kingdom_smp:librarian/a06"))return "Place a written catalogue on your lectern: "+LibraryQuestRules.get(player.server).catalogueEntries()+" native declarations 'minecraft:id max=N'. List every incompatible pair as 'minecraft:a ! minecraft:b'; an administrator confirms knowledge and authorship.";
            var d=InspectionQuests.get(id);var roster=CommerceService.store(player.server).professions(player.getUUID());
            if(roster.track(d.profession()).completedQuestIds().contains(id))return "Complete. Your inspected project and earned XP are recorded.";
            if(id.endsWith("builder/j05"))return "Administrator inspection: maintain"+BuilderInspectionRules.route(player.server)+"connected supported route cells, safe grading, shallow drainage and consent. Center the review on the route; no unauthorized excavation.";
            if(id.endsWith("builder/j06"))return "Complete3separate authorized j06sites: "+RetainedBuildingRules.get(player.server).goal(id)+"native timber, stone and glass blocks in their respective projects. Each site's own saved proof and current consent are inspected; earlier evidence never transfers.";
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
        if(LibraryGlyphCatalogue.QUEST.equals(id))return false;
        if(ServiceWorkQuests.contains(id))return ServiceWorkService.ready(player,id);
        if(com.mohymods.kingdomsmp.kingdom.AlphaScope.futureQuest(id))return false;
        if(id==null||!CATALOG.definitions().containsKey(id))return false;
        if(EncounterQuests.QUEST.equals(id))return EncounterQuests.ready(player);
        if(MineAccessService.QUEST.equals(id))return MineAccessService.ready(player);
        if(NativeOreQuests.contains(id))return NativeOreQuests.ready(player,id);
        if(MerchantTransactionQuests.contains(id))return MerchantTransactionQuests.ready(player,id);
        if(NativeOutputQuests.contains(id))return false;
        if(RepairService.contains(id))return RepairService.ready(player,id);
        if(TreeQuestService.contains(id))return TreeQuestService.ready(player,id);
        if(RetainedBuildingQuests.contains(id))return RetainedBuildingService.ready(player,id);
        if(InspectionQuests.contains(id)||CraftDeliveryQuests.contains(id))return false;
        if (!CommissionQuests.contains(id)) return FarmerQuestService.ready(player,id);
        var d=CommissionQuests.get(id);var store=CommerceService.store(player.server);var roster=store.professions(player.getUUID());
        return com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(player.server).attainedTier()>=com.mohymods.kingdomsmp.kingdom.AlphaScope.questTier(id) && roster.isActive(d.profession()) && !roster.track(d.profession()).completedQuestIds().contains(id)
            && CATALOG.mayAttempt(d.profession(),id,roster.track(d.profession()).completedQuestIds())
            && ProfessionService.available(player,d.profession())
            && store.commissionContractPeriods(player.getUUID(),id) >= CommissionQuestRules.get(player.server).goal(id);
    }
    public static void complete(ServerPlayer player,UUID operation,String id) throws IOException {
        if(LibraryGlyphCatalogue.QUEST.equals(id))throw new IllegalStateException("Request an on-site OP2glyph catalogue review at the Library; physical delivery and inspection must commit together");
        if(ServiceWorkQuests.contains(id)){ServiceWorkService.complete(player,operation,id);return;}
        if(id==null||!CATALOG.definitions().containsKey(id))throw new IllegalArgumentException("No validated runtime objective");
        if(com.mohymods.kingdomsmp.kingdom.AlphaScope.futureQuest(id))throw new IllegalStateException(com.mohymods.kingdomsmp.kingdom.AlphaScope.futureMessage());
        if(EncounterQuests.QUEST.equals(id)){EncounterQuests.complete(player,operation);return;}
        if(MineAccessService.QUEST.equals(id)){MineAccessService.complete(player,operation);return;}
        if(NativeOreQuests.contains(id)){NativeOreQuests.complete(player,operation,id);return;}
        if(MerchantTransactionQuests.contains(id)){MerchantTransactionQuests.complete(player,operation,id);return;}
        if(NativeOutputQuests.contains(id))throw new IllegalStateException("Use the authenticated enchanted-tool delivery");
        if(RepairService.contains(id)){RepairService.complete(player,operation,id);return;}
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
