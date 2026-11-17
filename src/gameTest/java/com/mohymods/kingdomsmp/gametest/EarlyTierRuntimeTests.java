package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.progression.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

public final class EarlyTierRuntimeTests {
    public static void naturalMilestones(GameTestHelper h)throws Exception {
        if(ContributionService.kingdom(h.getLevel().getServer()).attainedTier()==6){restore(h);return;}
        BankRuntimeTests.completeBankLoop(h,()->{try{advance(h);}catch(Exception e){h.fail(e.toString());}});
    }
    private static void advance(GameTestHelper h)throws Exception {
        var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);var p=server.getPlayerList().getPlayer(hall.owner());
        var bank=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().equals(RoyalBankBlueprint.ID)).findFirst().orElseThrow();
        var originalMoney=CommerceService.store(server).balance(p.getUUID());int projectCount=ConstructionService.store(server).projects().size();
        h.assertTrue(ContributionService.kingdom(server).attainedTier()==3,"Real consumed First Hearth/Tier2/road/paid Bank, no forced tier");
        for(int tier=3;tier<=5;tier++) {
            var ledger=EarlyTierService.ledger(server,tier);
            var first=ledger.requirements().keySet().stream().sorted().findFirst().orElseThrow();give(p,first,1);
            if(tier==3){
                var economy=CommerceService.store(server);long oldMoney=economy.balance(p.getUUID());economy.prepareTransfer(UUID.randomUUID(),p.getUUID(),-1);
                boolean refused=false;try{TierTwoService.handleReview(p,first,1);}catch(IllegalStateException expected){refused=true;}
                h.assertTrue(refused&&ledger.history().isEmpty()&&p.getInventory().getItem(0).getCount()==1,"Prepared real Bank conversion blocks the independent milestone inventory operation");
                h.assertTrue(!economy.recoverTransfer(CommerceService.receipt(p.saveWithoutId(new net.minecraft.nbt.CompoundTag())))&&economy.balance(p.getUUID())==oldMoney,"Actual unchanged native receipt aborts preparation without any money or resource mint");
            }
            var block=BlueprintRegistry.get(bank.blueprint(),bank.blueprintVersion()).blocks().getFirst();var pos=bank.origin().offset(block.offset());var saved=h.getLevel().getBlockState(pos);h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            boolean denied=false;try{TierTwoService.handleReview(p,first,1);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied&&ledger.history().isEmpty()&&p.getInventory().getItem(0).getCount()==1,"Damaged Bank refuses without material consumption");h.getLevel().setBlock(pos,saved,18);
            var stale=TierTwoService.handleReview(p,first,1);p.getInventory().getItem(0).shrink(1);denied=false;try{TierTwoService.confirm(p,stale);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied&&ledger.history().isEmpty(),"Changed native inventory refuses before prepare");
            for(var e:ledger.requirements().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                int remaining=e.getValue();while(remaining>0) {
                    int batch=Math.min(2304,remaining);give(p,e.getKey(),batch);long prior=ledger.revision();var q=TierTwoService.handleReview(p,e.getKey(),batch);h.assertTrue(TierTwoService.quote(p).orElseThrow().nonce().equals(q),"Normal Royal community interface supplies real review");TierTwoService.confirm(p,q);
                    h.assertTrue(p.getInventory().isEmpty()&&ledger.revision()==prior+1&&ledger.pending().isEmpty(),"Full exact physical materials consumed once and native receipt committed");
                    boolean replay=false;try{TierTwoService.confirm(p,q);}catch(IllegalStateException|IllegalArgumentException expected){replay=true;}h.assertTrue(replay&&ledger.revision()==prior+1,"Replayed confirmation cannot consume or credit twice");remaining-=batch;
                }
            }
            h.assertTrue(ContributionService.kingdom(server).attainedTier()==tier+1&&ledger.resourcesComplete(),"Sequential earned next tier after full eight-category milestone");
            h.assertTrue(ledger.standings().getFirst().player().equals(p.getUUID()),"Persistent normalized contributor recognition");
        }
        h.assertTrue(CommerceService.store(server).balance(p.getUUID())==originalMoney&&ConstructionService.store(server).projects().size()==projectCount,"Milestones mint no money or duplicate blueprints/buildings");
        h.assertTrue(TierTwoService.entries(p).stream().anyMatch(e->e.detail().contains("Coming Soon")),"Future tiers visibly deferred");
        h.assertTrue(!com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess.aerodromeComplete(server),"Tier6alone cannot bypass completed Aerodrome");
        EarlyTierService.verifyAll(p);LogUtils.getLogger().info("EARLY_TIERS_NATIVE_PASSED: actual fresh Hut/Hall/fullTier1+2/road/paidBank, all24fullresourcefamilies/three real transitions/no forced tier/changedinventory/replay/damagedBankrefusals/native receipts/normalized recognition/no currency or blueprint mint; raw resource fixture, not gathering duration or full professions/Aerodrome acceptance");h.succeed();
    }
    private static void restore(GameTestHelper h)throws Exception {
        var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);var p=ClaimRuntimeTests.player(h.getLevel(),hall.owner(),hall.ownerName());
        for(var project:ConstructionService.store(server).projects()){var bounds=ConstructionService.bounds(project);for(int x=((int)bounds.minX)>>4;x<=((int)bounds.maxX)>>4;x++)for(int z=((int)bounds.minZ)>>4;z<=((int)bounds.maxZ)>>4;z++)h.getLevel().getChunk(x,z);}
        h.runAfterDelay(40,()->{try{
            EarlyTierService.verifyAll(p);h.assertTrue(ContributionService.kingdom(server).attainedTier()==6,"All three new stages restore from real native receipts");
            for(int tier=3;tier<=5;tier++){var ledger=EarlyTierService.ledger(server,tier);h.assertTrue(ledger.resourcesComplete()&&ledger.pending().isEmpty()&&ledger.standings().getFirst().player().equals(p.getUUID()),"Original pinned costs/history/normalized winner restore");}
            h.assertTrue(ConstructionService.structureIntact(h.getLevel(),hall)&&h.getLevel().getEntity(hall.npc())!=null,"Original Hall and Steward survive");
            var hut=BuilderHutSavedData.get(server).hut().orElseThrow();h.assertTrue(BuilderHutService.structureIntact(h.getLevel(),hut)&&h.getLevel().getEntity(hut.npcId())!=null,"Original Hut and Aldric survive");
            LogUtils.getLogger().info("EARLY_TIERS_RESTART_PASSED: natural Tier6/all24resource goals/three pinned journals/native receipts/once-only winners/original buildings and residents restore");h.succeed();
        }catch(Exception e){h.fail(e.toString());}});
    }
    private static void give(ServerPlayer p,String id,int count){p.getInventory().clearContent();for(int slot=0;count>0;slot++){int n=Math.min(count,64);p.getInventory().setItem(slot,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)),n));count-=n;}}
}
