package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Same authenticated progressive lifecycle, on an actual four-block-deep air/water site. */
@GameTestHolder("kingdom_smp_foundation_tests") @PrefixGameTestTemplate(false)
public final class FoundationRuntimeTests {
    @GameTest(template="empty",timeoutTicks=12000)
    public static void savedRotatedFoundationsBeforeFurnishedHall(GameTestHelper h) throws Exception {
        boolean[] partial={false},finished={false};
        h.onEachTick(()->{
            if(partial[0]||finished[0])return;
            try{
                var server=h.getLevel().getServer();var p=ConstructionService.store(server).reward("market:"+RoyalBountyHallBlueprint.ID).orElse(null);
                if(p==null||p.phase()!=ConstructionStore.Phase.BUILDING||p.placed()<8)return;
                h.assertTrue(p.foundations().size()==17*15*4&&p.placed()<p.foundations().size(),"Four complete support layers precede original hall blocks");
                var reopened=ConstructionStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data")).get(p.id());h.assertTrue(reopened.equals(p),"Partial foundation prefix, proposal, rotation, owner and NPC identity reopen unchanged");
                var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());var low=p.origin().offset(b.rotate(p.foundations().getFirst(),p.rotation()));
                h.assertTrue(ConstructionService.protectedAt(h.getLevel(),low)&&ConstructionService.protectedAt(h.getLevel(),low.below()),"Lowest planned support and original anchor remain reserved");
                h.assertTrue(h.getLevel().getBlockState(p.origin()).isAir(),"No floor appears before supports are completed");
                // A real fluid may enter a not-yet-placed cell after approval. It is the only
                // non-weather material the saved support plan may replace, without drops.
                var water=p.origin().offset(b.rotate(p.foundations().getLast(),p.rotation()));h.getLevel().setBlock(water,Blocks.WATER.defaultBlockState(),18);
                partial[0]=true;
            }catch(Exception e){finished[0]=true;h.runAfterDelay(1,()->h.fail(e.toString()));}
        });
        BountyRuntimeTests.completeBountyHall(h,4,p->{try{
            finished[0]=true;h.assertTrue(partial[0],"Actual partial-foundation persistence was exercised");var level=h.getLevel();var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
            h.assertTrue(p.rotation()==Rotation.CLOCKWISE_180&&p.placed()==p.foundations().size()+b.blocks().size(),"Rotated hall finishes exact combined support/building count");
            for(var pos:p.foundations())h.assertTrue(level.getBlockState(p.origin().offset(b.rotate(pos,p.rotation()))).is(Blocks.COBBLESTONE),"Saved foundation cell actually constructed");
            h.assertTrue(ConstructionService.structureIntact(level,p)&&level.getEntity(p.npc())!=null,"Verified furnished hall and its original permanent Bounty Master remain usable");
            var source=p.origin().offset(b.rotate(p.foundations().getFirst(),p.rotation())).below();h.assertTrue(level.getBlockState(source).is(Blocks.STONE),"Original terrain anchor is retained, never excavated");
            var first=p.origin().offset(b.rotate(p.foundations().getFirst(),p.rotation()));level.setBlock(first,Blocks.AIR.defaultBlockState(),18);
            h.assertTrue(!ConstructionService.structureIntact(level,p),"Missing credited support is detected; services cannot silently accept it");level.setBlock(first,Blocks.COBBLESTONE.defaultBlockState(),18);
            h.assertTrue(ConstructionService.structureIntact(level,p),"Explicit replacement restores the exact saved support, with no duplicate NPC issuance");
            com.mojang.logging.LogUtils.getLogger().info("FOUNDATION_ASSERTIONS_PASSED: real fresh-world Hut/Hall/Tier3/Bank lifecycle, exact authenticated rotated Bounty site, 1020saved support cells, original anchor protection, partial prefix reopen, actual water replacement, progressive supports before furnished building, full prefix verification and single resident; not yet a process-restart claim");h.succeed();
        }catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});
    }
}
