package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Real claim-aware construction in a dark cavern, then resource-backed vanilla seed repair. */
@GameTestHolder("kingdom_smp_crop_tests") @PrefixGameTestTemplate(false)
public final class CropRecoveryRuntimeTests {
    private static long requests;
    @GameTest(template="empty",timeoutTicks=5000)
    public static void darkGardenAndRecordedCropRepair(GameTestHelper h)throws Exception{
        TownHallRuntimeTests.newWorldLoop(h,()->{try{garden(h);}catch(Exception e){h.fail(e.toString());}});
    }
    private static void garden(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var level=h.getLevel();var hall=ConstructionService.lodge(server);var p=server.getPlayerList().getPlayer(hall.owner());var stranger=server.getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseThrow();
        var hb=BlueprintRegistry.get(hall.blueprint(),hall.blueprintVersion());var home=hall.origin().offset(hb.home());p.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);
        var money=CommerceService.store(server);money.adminSet(UUID.randomUUID(),new UUID(0,0),p.getUUID(),1000,"Isolated original Farmer crop recovery fixture");
        var buy=review(p,0,"kingdom_smp:farmers_house",1);confirm(p,buy,0,"kingdom_smp:farmers_house");
        var project=ConstructionService.store(server).reward("market:kingdom_smp:farmers_house").orElseThrow();ConstructionService.select(p,project.id());var claim=ConstructionService.review(p,1);ConstructionService.confirm(p,claim.nonce(),2);
        var b=BlueprintRegistry.get(project.blueprint(),project.blueprintVersion());var site=hall.origin().offset(0,0,40);
        // Explicit isolated terrain fixture: roof is outside the validated building volume.
        for(var pos:BlockPos.betweenClosed(site.offset(-20,-1,-20),site.offset(35,-1,30)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
        for(var pos:BlockPos.betweenClosed(site.offset(-20,14,-20),site.offset(35,14,30)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
        for(int x=(site.getX()-2)>>4;x<=(site.getX()+b.width()+1)>>4;x++)for(int z=(site.getZ()-2)>>4;z<=(site.getZ()+b.depth()+1)>>4;z++){var point=new BlockPos(x*16,site.getY(),z*16);ClaimRuntimeTests.ftbClaim(p,point);ClaimRuntimeTests.opacClaim(level,p.getUUID(),point);}
        p.moveTo(site.getX()-2,site.getY()+1,site.getZ());var preview=ConstructionService.preview(p,site,Rotation.NONE);h.assertTrue(preview.denial().isEmpty(),"Dark garden volume remains otherwise valid: "+preview.denial());var placement=ConstructionService.view(p,1,false,"");ConstructionService.confirm(p,placement.review(),4);var start=ConstructionService.review(p,7);ConstructionService.confirm(p,start.nonce(),8);
        var hut=BuilderHutSavedData.get(server).hut().orElseThrow();var builder=level.getEntity(hut.npcId());var post=b.workPost(site,Rotation.NONE);builder.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);
        int[] stage={0},wait={0};long[] paused={-1};boolean[] done={false};
        h.onEachTick(()->{if(done[0])return;try{
            var store=ConstructionService.store(server);var saved=store.get(project.id());
            if(stage[0]==0){
                for(int n=0;n<4;n++){ConstructionService.step(server,project.id());if(ConstructionService.activity(project.id()).contains("Garden light"))break;}
                if(!ConstructionService.activity(project.id()).contains("Garden light"))return;
                paused[0]=store.get(project.id()).placed();h.assertTrue(!b.blocks().get((int)paused[0]).state().getBlock().equals(Blocks.WHEAT),"Dark-site pause precedes soil/crop pair rather than losing a placed plant");stage[0]=1;wait[0]=0;return;
            }
            if(stage[0]==1){
                ConstructionService.step(server,project.id());h.assertTrue(store.get(project.id()).placed()==paused[0],"Dark garden records no consumed planting/progress");if(++wait[0]<20)return;
                var light=site.offset(15,1,5);level.setBlock(light.below(),Blocks.STONE.defaultBlockState(),18);level.setBlock(light,Blocks.TORCH.defaultBlockState(),3);stage[0]=2;return;
            }
            if(stage[0]==2){
                if(saved.placed()<b.blocks().size()){ConstructionService.step(server,project.id());saved=store.get(project.id());if(saved.placed()<b.blocks().size())return;}
                // Simulates the saved600/600 world from the reported bug. No repair grant is made.
                for(var block:b.blocks())if(block.state().getBlock() instanceof CropBlock)level.setBlock(site.offset(block.offset()),Blocks.AIR.defaultBlockState(),3);
                ConstructionService.step(server,project.id());h.assertTrue(store.get(project.id()).phase()==ConstructionStore.Phase.BUILDING,"Strict prefix withholds completion when recorded crops are missing");
                var crop=site.offset(11,1,3);h.assertTrue(!ConstructionService.cropRepairAllowed(stranger,crop,Blocks.WHEAT.defaultBlockState()),"Unrelated player cannot repair/modify the reservation");
                h.assertTrue(!ConstructionService.cropRepairAllowed(p,site.offset(3,1,3),Blocks.WHEAT.defaultBlockState()),"Owner cannot replace furnishing with a crop");
                p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.WHEAT_SEEDS,15));p.getInventory().selected=0;
                for(var block:b.blocks())if(block.state().getBlock() instanceof CropBlock){
                    var cell=site.offset(block.offset());p.moveTo(cell.getX()+.5,cell.getY(),cell.getZ()-1.5);var soil=cell.below();var hit=new BlockHitResult(Vec3.atCenterOf(soil).add(0,.5,0),Direction.UP,soil,false);
                    var result=p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);h.assertTrue(result.consumesAction()&&level.getBlockState(cell).is(Blocks.WHEAT),"Owner replants through real native seed interaction: "+result);
                }
                h.assertTrue(p.getMainHandItem().isEmpty(),"Exactly15actualseeds consumed; no free crop regeneration");p.moveTo(site.getX()-2,site.getY()+1,site.getZ());stage[0]=3;return;
            }
            if(stage[0]==3){
                ConstructionService.step(server,project.id());saved=store.get(project.id());if(saved.phase()!=ConstructionStore.Phase.COMPLETE)return;
                h.assertTrue(saved.placed()==600&&saved.npcIssued(),"Same original project completes once after resource-backed repair");h.assertTrue(ConstructionService.structureIntact(level,saved),"Walls, roof, furnishing and repaired farm are intact");
                var reopened=ConstructionStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.get(project.id()).equals(saved),"Repaired completion/identity survives reopen");
                done[0]=true;h.succeed();com.mojang.logging.LogUtils.getLogger().info("CROP_RECOVERY_ASSERTIONS_PASSED: dark-site no-progress pause, real light then all original placements, strict missing-prefix refusal, owner-only native seed consumption, no regeneration, same NPC identity/completion/reopen");
            }
        }catch(Exception failure){done[0]=true;h.fail(failure.toString());}});
    }
    private static CommerceView review(ServerPlayer p,int tab,String id,int count){CommerceService.handle(p,new CommerceRequest(++requests,1,tab,id,count,CommerceRequest.NONE));return CommerceService.snapshot(p,++requests,tab,"");}
    private static void confirm(ServerPlayer p,CommerceView v,int tab,String id){if(!v.reviewing())throw new IllegalStateException("Review unavailable");CommerceService.handle(p,new CommerceRequest(++requests,2,tab,id,1,v.review()));}
}
