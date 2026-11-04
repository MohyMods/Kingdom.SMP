package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CraftDeliveryStoreTest {
    @TempDir Path directory;final UUID player=UUID.randomUUID();
    EconomyStore enrolled()throws Exception{var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");return s;}
    @Test void oldReceiptCancelsAndMatchingReceiptConsumesAndAwardsExactlyOnce()throws Exception{
        var s=enrolled();var input=CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY);
        s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"Actual crafted outputs",input);
        assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
        var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"Actual crafted outputs",input);
        var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        assertTrue(s.recoverItem(receipt));assertFalse(s.recoverItem(receipt));
        assertEquals(receipt.sequence(),s.expectedReceipt(player));assertEquals(100,s.professions(player).track(Profession.BUILDER).xp());
        assertEquals(0,s.balance(player));assertTrue(s.mailbox(player).isEmpty());assertTrue(s.itemMailbox(player).isEmpty());
        s.archive(player,"Private crafted delivery archive");var reopened=EconomyStore.open(directory);
        assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(receipt.sequence(),reopened.expectedReceipt(player));
        assertThrows(IllegalStateException.class,()->reopened.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"repeat",input));
    }
    @Test void wrongMaterialsMissingInputsNegativeQuantitiesAndOtherAuthorityKindsFailBeforePreparing()throws Exception{
        var s=enrolled();long revision=s.revision();
        for(var input:List.of(Map.of("minecraft:diamond",32),Map.of("minecraft:stone_brick_stairs",32),Map.of("minecraft:stone_brick_stairs",-1,"minecraft:stone_brick_slab",32))){
            assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"forged input",input));
        }
        assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,"kingdom_smp:farmer/a07","foreign",Map.of("minecraft:bread",32)));
        assertThrows(IllegalArgumentException.class,()->s.completeFarmerQuest(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"not consumed"));
        assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"",CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY)));
        assertEquals(revision,s.revision());assertTrue(s.pendingItem().isEmpty());
    }
    @Test void afterRenameFaultRecoversOneAwardAndOneNativeReceipt()throws Exception{
        var s=enrolled();var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"Verified native recipe and consumed outputs",CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY));
        var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        var faulty=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Private after-rename craft receipt fault");});
        assertThrows(IOException.class,()->faulty.recoverItem(receipt));var reopened=EconomyStore.open(directory);
        assertFalse(reopened.recoverItem(receipt));assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(receipt.sequence(),reopened.expectedReceipt(player));
    }
    @Test void inactiveProfessionCannotPrepareConsumptionOrAwardXp()throws Exception{
        var s=EconomyStore.open(directory);assertThrows(IllegalStateException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.MASONRY,"no active profession",CraftDeliveryQuests.defaults(CraftDeliveryQuests.MASONRY)));
        assertTrue(s.pendingItem().isEmpty());assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void mixedTimberReceiptCancellationCommitAndReplayPreserveExactMaterialsAndOneAward()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","lumberjack");
        var input=Map.of("minecraft:oak_planks",64,"minecraft:birch_planks",64,"minecraft:stripped_oak_log",16,"minecraft:stripped_birch_log",16);
        assertEquals(CraftDeliveryQuests.defaults(CraftDeliveryQuests.TIMBER),CraftDeliveryQuests.totals(CraftDeliveryQuests.TIMBER,input));
        s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Actual mixed timber produced",input);
        assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));assertEquals(0,s.professions(player).track(Profession.LUMBERJACK).xp());
        var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Actual mixed timber produced",input);var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        assertTrue(s.recoverItem(receipt));assertFalse(s.recoverItem(receipt));assertEquals(100,s.professions(player).track(Profession.LUMBERJACK).xp());
        var reopened=EconomyStore.open(directory);assertEquals(s.professions(player),reopened.professions(player));assertEquals(receipt.sequence(),reopened.expectedReceipt(player));
        assertThrows(IllegalStateException.class,()->reopened.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Replay",input));assertEquals(0,reopened.balance(player));assertTrue(reopened.mailbox(player).isEmpty());
    }
    @Test void mixedTimberAfterRenameFailureRecoversWithoutSecondAward()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","lumberjack");
        var input=Map.of("minecraft:cherry_planks",128,"minecraft:stripped_mangrove_log",32);
        var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Real native mixed production",input);var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        var faulty=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Timber after rename");});assertThrows(IOException.class,()->faulty.recoverItem(receipt));
        var reopened=EconomyStore.open(directory);assertFalse(reopened.recoverItem(receipt));assertEquals(100,reopened.professions(player).track(Profession.LUMBERJACK).xp());
    }
    @Test void unknownNetherClientCategoriesAndOversizedTimberCannotEnterJournal()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","lumberjack");long revision=s.revision();
        for(var input:List.of(Map.of("planks",128,"stripped_logs",32),Map.of("minecraft:warped_planks",128,"minecraft:stripped_oak_log",32),Map.of("minecraft:oak_planks",128),Map.of("minecraft:oak_planks",2304,"minecraft:stripped_oak_log",32),Map.of("minecraft:oak_planks",-1,"minecraft:stripped_oak_log",32)))assertThrows(IllegalArgumentException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Forged timber",input));
        assertEquals(revision,s.revision());assertTrue(s.pendingItem().isEmpty());
    }

    @Test void mixedFittingsRequireAllRealWoodCategoriesAndRejectForeignOrClientCategoryIds(){
        var input=Map.of("minecraft:oak_fence",18,"minecraft:birch_fence",14,"minecraft:oak_stairs",16,"minecraft:birch_trapdoor",16);
        assertEquals(CraftDeliveryQuests.defaults(CraftDeliveryQuests.FITTINGS),CraftDeliveryQuests.totals(CraftDeliveryQuests.FITTINGS,input));
        for(var wrong:List.of(Map.of("minecraft:nether_brick_fence",32),Map.of("minecraft:oak_planks",64),Map.of("fences",32),Map.of("minecraft:oak_fence",2305)))assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(CraftDeliveryQuests.FITTINGS,wrong));
        var incomplete=new net.minecraft.nbt.CompoundTag();incomplete.putInt("minecraft:oak_fence",32);assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.validate(CraftDeliveryQuests.FITTINGS,incomplete));
    }
    @Test void advancedProductionCannotConsumeBeforeEarlierBandOrThroughNonProductionAuthority()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","lumberjack");long revision=s.revision();
        assertThrows(IllegalStateException.class,()->s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.CHARCOAL,"Native charcoal",Map.of("minecraft:charcoal",64)));
        assertEquals(revision,s.revision());assertTrue(s.pendingItem().isEmpty());
    }
    void earnedApprenticeModel(EconomyStore s)throws Exception{
        // Canonical model fixture, not a native gameplay proof. Dedicated test earns this band normally.
        for(int n=1;n<=4;n++){String id=Profession.LUMBERJACK.questId(ProfessionRank.APPRENTICE,n);int goal=TreeQuestService.get(id).goal();s.completeTreeQuest(UUID.randomUUID(),player,id,goal,goal);}
        var delivery=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Model native boundary",Map.of("minecraft:oak_planks",128,"minecraft:stripped_oak_log",32));s.recoverItem(new EconomyStore.Receipt(s.id(),delivery.sequence()));
        s.approveProfessionInspection(UUID.randomUUID(),UUID.randomUUID(),player,"kingdom_smp:lumberjack/a06","Model inspected route");
        for(long day=10;day<13;day++){var item=new net.minecraft.nbt.CompoundTag();item.putString("id","minecraft:oak_log");item.putInt("count",1);var pending=s.prepareContract(UUID.randomUUID(),player,"lumberjack","daily",day,64,80,2000,item);s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence()));}
        s.completeCommissionQuest(UUID.randomUUID(),player,"kingdom_smp:lumberjack/a07",3);s.approveProfessionInspection(UUID.randomUUID(),UUID.randomUUID(),player,"kingdom_smp:lumberjack/a08","Model woodland assessment");
    }
    @Test void advancedMixedAndSmeltReceiptsCancelRecoverAfterRenameAndReopenWithoutDuplicateRewards()throws Exception{
        var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","lumberjack");earnedApprenticeModel(s);
        var fittings=Map.of("minecraft:oak_fence",18,"minecraft:birch_fence",14,"minecraft:oak_stairs",16,"minecraft:birch_trapdoor",16);
        s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.FITTINGS,"Native mixed production boundary",fittings);assertFalse(s.recoverItem(new EconomyStore.Receipt(s.id(),s.expectedReceipt(player))));assertEquals(800,s.professions(player).track(Profession.LUMBERJACK).xp());
        var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.FITTINGS,"Native mixed production boundary",fittings);var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        var faulty=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Advanced production after rename");});assertThrows(IOException.class,()->faulty.recoverItem(receipt));s=EconomyStore.open(directory);assertFalse(s.recoverItem(receipt));assertEquals(1000,s.professions(player).track(Profession.LUMBERJACK).xp());
        var charcoal=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.CHARCOAL,"Native charcoal production boundary",Map.of("minecraft:charcoal",64));var charcoalReceipt=new EconomyStore.Receipt(s.id(),charcoal.sequence());assertTrue(s.recoverItem(charcoalReceipt));assertFalse(s.recoverItem(charcoalReceipt));s.archive(player,"Advanced forestry production recovery");var restored=EconomyStore.open(directory);assertEquals(1200,restored.professions(player).track(Profession.LUMBERJACK).xp());assertEquals(charcoalReceipt.sequence(),restored.expectedReceipt(player));assertEquals(0,restored.balance(player));
        assertThrows(IllegalStateException.class,()->restored.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.CHARCOAL,"Duplicate",Map.of("minecraft:charcoal",64)));
    }
 @Test void smithFuelAndIronUseExistingReceiptRecoveryWithoutAwardingToolsBeforeTheirPrerequisite()throws Exception{
  var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","blacksmith");var fuel=Map.of("minecraft:coal",32,"minecraft:charcoal",32);assertEquals(CraftDeliveryQuests.defaults(CraftDeliveryQuests.FUEL),CraftDeliveryQuests.totals(CraftDeliveryQuests.FUEL,fuel));
  s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.FUEL,"Reviewed exact mixed fuel",fuel);assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));assertEquals(0,s.professions(player).track(Profession.BLACKSMITH).xp());
  var pending=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.FUEL,"Reviewed exact mixed fuel",fuel);var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());assertTrue(s.recoverItem(receipt));assertFalse(s.recoverItem(receipt));
  var current=s;assertThrows(IllegalStateException.class,()->current.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TOOLS,"Missing native iron qualification",CraftDeliveryQuests.defaults(CraftDeliveryQuests.TOOLS)));
  var iron=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.IRON,"Model native processed iron boundary",CraftDeliveryQuests.defaults(CraftDeliveryQuests.IRON));var ironReceipt=new EconomyStore.Receipt(s.id(),iron.sequence());var faulty=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Smith after rename");});assertThrows(IOException.class,()->faulty.recoverItem(ironReceipt));s=EconomyStore.open(directory);assertFalse(s.recoverItem(ironReceipt));assertEquals(200,s.professions(player).track(Profession.BLACKSMITH).xp());s.archive(player,"Smith delivery recovery");assertEquals(s.professions(player),EconomyStore.open(directory).professions(player));assertEquals(0,s.balance(player));
 }
 @Test void coalOreAndUnverifiedFuelCannotMasqueradeAsCoalOrCharcoal(){for(String id:List.of("minecraft:coal_ore","minecraft:coal_block","minecraft:blaze_rod","fuel"))assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(CraftDeliveryQuests.FUEL,Map.of(id,64)));}
}
