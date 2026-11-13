package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.npc.AldricEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Three actual dedicated processes: pre-NPC support interruption, native Hut installation, permanent identity. */
@GameTestHolder("kingdom_smp_hut_foundation_tests") @PrefixGameTestTemplate(false)
public final class HutFoundationRuntimeTests {
    @GameTest(template="empty",timeoutTicks=6000)
    public static void initialOwnerSupportsAndAldricSurviveRestarts(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var store=ConstructionService.store(server);var saved=store.reward(ConstructionService.STARTING_HUT).orElse(null);
        if(saved==null){prepare(h);return;}
        var owner=ClaimRuntimeTests.player(h.getLevel(),saved.owner(),saved.ownerName());var b=BlueprintRegistry.get(saved.blueprint(),saved.blueprintVersion());
        move(owner,b.workPost(saved.origin(),saved.rotation()));resume(h,owner,saved.id());
    }
    private static void prepare(GameTestHelper h)throws Exception{
        var level=h.getLevel();var server=level.getServer();var owner=ClaimRuntimeTests.player(level,"HutOwner");var stranger=ClaimRuntimeTests.player(level,"HutOther");
        h.assertTrue(server.getCommands().getDispatcher().execute("kingdom admin owner HutOwner confirm",server.createCommandSourceStack())==1,"One explicitly assigned owner receives the free initial entitlement");
        var store=ConstructionService.store(server);var p=store.reward(ConstructionService.STARTING_HUT).orElseThrow();h.assertTrue(p.owner().equals(owner.getUUID()),"Stranger receives no free duplicate entitlement");
        var claim=ConstructionService.review(owner,1);ConstructionService.confirm(owner,claim.nonce(),2);p=store.get(p.id());var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());var origin=h.absolutePos(new BlockPos(24,5,24));var rotation=Rotation.CLOCKWISE_90;
        for(var pos:BlockPos.betweenClosed(origin.offset(-3,-5,-3),origin.offset(b.width(rotation)+3,-5,b.depth(rotation)+3)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
        var post=b.workPost(origin,rotation);for(int n=1;n<=4;n++)level.setBlock(post.below(n),Blocks.STONE.defaultBlockState(),18);move(owner,post);move(stranger,origin.offset(-2,-4,-2));
        for(int x=origin.getX()>>4;x<=(origin.getX()+b.width(rotation)-1)>>4;x++)for(int z=origin.getZ()>>4;z<=(origin.getZ()+b.depth(rotation)-1)>>4;z++){var chunk=new BlockPos(x*16,origin.getY(),z*16);ClaimRuntimeTests.ftbClaim(owner,chunk);ClaimRuntimeTests.opacClaim(level,owner.getUUID(),chunk);}
        var preview=ConstructionService.preview(owner,origin,rotation);h.assertTrue(preview.denial().isEmpty(),"Rotated starting Hut validates four-deep supports and both actual claim APIs: "+preview.denial());
        ConstructionService.confirm(owner,ConstructionService.view(owner,1,false,"").review(),4);var begin=ConstructionService.review(owner,7);ConstructionService.confirm(owner,begin.nonce(),8);p=store.get(p.id());
        h.assertTrue(p.foundations().size()==b.width()*b.depth()*4&&BuilderHutSavedData.get(server).hut().isEmpty(),"Free Hut includes its saved foundations but no premature native reservation/NPC");
        // Explicit administrator QA role verifies that the old coordinate installer cannot fork an
        // in-progress authenticated Hut. Remove it again before any claim-controlled placement.
        server.getPlayerList().op(owner.getGameProfile());try{int result=server.getCommands().getDispatcher().execute("kingdom hut preview "+origin.getX()+" "+origin.getY()+" "+origin.getZ()+" north Prevent duplicate reserved initial hut",owner.createCommandSourceStack());h.assertTrue(result==0&&BuilderHutSavedData.get(server).hut().isEmpty(),"Legacy administrator preview cannot create a competing native Hut reservation");}finally{server.getPlayerList().deop(owner.getGameProfile());}
        UUID id=p.id();boolean[] stopped={false};h.onEachTick(()->guard(h,()->{
            if(stopped[0])return;var current=store.get(id);if(current.placed()<8){ConstructionService.step(server,id);return;}
            h.assertTrue(!current.npcIssued()&&BuilderHutSavedData.get(server).hut().isEmpty(),"Interruption retains only eight protected support cells, with no Aldric issued");stopped[0]=true;
            com.mojang.logging.LogUtils.getLogger().info("HUT_FOUNDATION_PREPARED: free owner={} project={} cells={} placed={} npc={} — actual process interruption before native Hut installation",current.owner(),id,current.foundations().size(),current.placed(),current.npc());h.succeed();
        }));
    }
    private static void resume(GameTestHelper h,ServerPlayer owner,UUID id){
        boolean[] ready={false},done={false};int[] waiting={0};
        // Register once in the initial test function. Minecraft's task map must not be
        // resized by adding onEachTick from within a running scheduled callback.
        h.onEachTick(()->guard(h,()->{
            if(done[0])return;var level=h.getLevel();var p=ConstructionService.store(owner.server).get(id);var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
            if(!ready[0]){
                if(!ConstructionService.loaded(level,p)||!level.isPositionEntityTicking(p.origin())||!level.isPositionEntityTicking(p.origin().offset(b.width(p.rotation())-1,0,b.depth(p.rotation())-1))){h.assertTrue(waiting[0]++<600,"Owner presence loads all starting-site entity sections");return;}
                h.assertTrue(ConstructionService.verifyPrefix(level,p)==null,"Actual process restart retains the starting Hut's exact protected foundation prefix");
                h.assertTrue(p.phase()==ConstructionStore.Phase.COMPLETE||p.phase()==ConstructionStore.Phase.BUILDING&&p.placed()>=8&&p.placed()<p.foundations().size(),"Second process resumes partial pre-NPC Hut supports");ready[0]=true;
            }
            for(int n=0;n<2&&p.phase()==ConstructionStore.Phase.BUILDING;n++){ConstructionService.step(owner.server,id);p=ConstructionService.store(owner.server).get(id);}
            if(p.phase()!=ConstructionStore.Phase.COMPLETE||level.getEntity(p.npc())==null)return;
            done[0]=true;verify(h,owner,p);
        }));
    }
    private static void verify(GameTestHelper h,ServerPlayer owner,ConstructionStore.Project p){guard(h,()->{
        var level=h.getLevel();var hut=BuilderHutSavedData.get(owner.server).hut().orElseThrow();var entity=level.getEntity(p.npc());
        h.assertTrue(entity instanceof AldricEntity&&entity.isAlive()&&p.npcIssued()&&!p.npcDead()&&hut.npcId().equals(p.npc()),"Native installer and entitlement share exactly one original living Aldric identity");
        h.assertTrue(BuilderHutService.structureIntact(level,hut)&&ConstructionService.structureIntact(level,p)&&p.placed()==p.foundations().size()+348&&hut.placedBlocks()==348,"Original348-block furnished Hut and saved supports are both complete without reinterpreting native progress");
        h.assertTrue(level.getEntitiesOfClass(AldricEntity.class,ConstructionService.bounds(p).inflate(6)).size()==1,"Exactly one permanent Aldric exists after installation/restart");
        var view=ConstructionService.view(owner,9,false,"");h.assertTrue(view.project().equals(p.id())&&view.phase().equals("COMPLETE")&&view.placed()==744&&view.total()==744,"Open/default construction view retains completed Hut until the next legitimate reward exists");
        boolean redeemed=false;try{ConstructionService.review(owner,1);}catch(IllegalStateException expected){redeemed=true;}h.assertTrue(redeemed,"Used initial blueprint cannot be claimed or redeemed again");
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());var cell=p.origin().offset(b.rotate(p.foundations().getFirst(),p.rotation()));level.setBlock(cell,Blocks.AIR.defaultBlockState(),18);h.assertTrue(!BuilderHutService.structureIntact(level,hut)&&!ConstructionService.structureIntact(level,p),"Lost recorded starter support pauses native Hut services without regeneration");level.setBlock(cell,Blocks.COBBLESTONE.defaultBlockState(),18);h.assertTrue(BuilderHutService.structureIntact(level,hut),"Explicit exact support replacement restores existing Hut, not a second NPC");
        var marker=owner.server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_hut_foundation_test_completed.dat");boolean prior=Files.exists(marker);var data=level.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(Marker::new,(tag,registry)->new Marker()),"kingdomsmp_hut_foundation_test_completed");data.setDirty();
        com.mojang.logging.LogUtils.getLogger().info(prior?"HUT_FOUNDATION_VERIFIED: same complete project={} supports={} placed={} original Aldric={} after second actual restart":"HUT_FOUNDATION_RESUMED: same interrupted project={} supports={} placed={} original Aldric={} installed once; save/stop for another actual restart",p.id(),p.foundations().size(),p.placed(),p.npc());h.succeed();
    });}
    private static final class Marker extends net.minecraft.world.level.saveddata.SavedData{
        @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider ignored){tag.putInt("schema",1);return tag;}
    }
    private static void move(ServerPlayer p,BlockPos pos){p.teleportTo(p.serverLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),0,0);p.serverLevel().getChunkSource().move(p);}
    @FunctionalInterface private interface Step{void run()throws Exception;}
    private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){h.fail(e.toString());}}
}
