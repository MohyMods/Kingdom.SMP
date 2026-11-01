package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Invoked in three separately booted dedicated JVMs against the same private test world. */
@GameTestHolder("kingdom_smp_foundation_resume_tests") @PrefixGameTestTemplate(false)
public final class FoundationRestartRuntimeTests {
    @GameTest(template="empty",timeoutTicks=12000)
    public static void resumeActualServerSavedFoundation(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();var project=ConstructionService.store(server).reward("market:"+RoyalBountyHallBlueprint.ID).orElse(null);
        if(project==null){prepare(h);return;}
        h.assertTrue(!project.foundations().isEmpty(),"Saved restart fixture has a real foundation plan");
        var owner=ClaimRuntimeTests.player(h.getLevel(),project.owner(),project.ownerName());
        var hut=ConstructionService.store(server).projects().stream().filter(p->p.blueprint().toString().equals(BuilderHutBlueprint.ID)).findFirst().orElseThrow();
        var keeper=ClaimRuntimeTests.player(h.getLevel(),hut.owner(),hut.ownerName());
        move(owner,BlueprintRegistry.get(project.blueprint(),project.blueprintVersion()).workPost(project.origin(),project.rotation()).offset(1,0,0));
        move(keeper,hut.origin().offset(-1,0,-1));
        awaitLoaded(h,project.id(),owner,keeper,0);
    }
    private static void prepare(GameTestHelper h) throws Exception {
        boolean[] stopped={false};
        h.onEachTick(()->guard(h,()->{
            if(stopped[0])return;var p=ConstructionService.store(h.getLevel().getServer()).reward("market:"+RoyalBountyHallBlueprint.ID).orElse(null);
            if(p==null||p.phase()!=ConstructionStore.Phase.BUILDING||p.placed()<16)return;
            h.assertTrue(p.placed()<p.foundations().size()&&!p.npcIssued(),"Stop with only a real partial foundation prefix, before resident issuance");
            stopped[0]=true;
            var hut=BuilderHutSavedData.get(h.getLevel().getServer()).hut().orElseThrow();var aldric=h.getLevel().getEntity(hut.npcId());h.assertTrue(aldric!=null,"Original Aldric remains present at the saved interruption");// GameTest success discards non-player entities inside its template. Keep the
            // original Aldric on prepared ground outside that region and away from his work post.
            aldric.moveTo(p.origin().getX()-2.5,p.origin().getY()-4,p.origin().getZ()-2.5);
            com.mojang.logging.LogUtils.getLogger().info("FOUNDATION_RESTART_PREPARED: project={} proposal={} cells={} placed={} npc={} — flush and stop this actual dedicated process",p.id(),p.proposal(),p.foundations().size(),p.placed(),p.npc());h.succeed();
        }));
        BountyRuntimeTests.completeBountyHall(h,4,p->h.fail("Restart fixture must stop during its foundations, not after completion"));
    }
    private static void awaitLoaded(GameTestHelper h,UUID id,ServerPlayer owner,ServerPlayer keeper,int tries) {
        guard(h,()->{
            var server=h.getLevel().getServer();var level=h.getLevel();var p=ConstructionService.store(server).get(id);var hut=BuilderHutSavedData.get(server).hut().orElseThrow();
            if(!ConstructionService.loaded(level,p)||!BuilderHutService.structureIntact(level,hut)||level.getEntity(hut.npcId())==null||!level.isPositionEntityTicking(p.origin())){
                if(tries%40==0){String missing="none";for(var block:BuilderHutBlueprint.create().placements(hut.rotation())){var pos=hut.origin().offset(block.offset());if(!level.hasChunkAt(pos)||!ConstructionBlueprint.matches(level.getBlockState(pos),block.state())){missing=pos.toShortString()+" expected "+block.state()+" actual "+level.getBlockState(pos);break;}}com.mojang.logging.LogUtils.getLogger().info("FOUNDATION_RESTART_WAIT tries={} siteLoaded={} hutIntact={} aldric={} originTicking={} ownerPos={} keeperPos={} hutMismatch={}",tries,ConstructionService.loaded(level,p),BuilderHutService.structureIntact(level,hut),level.getEntity(hut.npcId()),level.isPositionEntityTicking(p.origin()),owner.position(),keeper.position(),missing);}
                h.assertTrue(tries<120,"Saved building and Hut become loaded through real registered player presence");h.runAfterDelay(5,()->awaitLoaded(h,id,owner,keeper,tries+1));return;
            }
            h.assertTrue(ConstructionService.verifyPrefix(level,p)==null,"Actual process restart retains every credited foundation cell and original structure prefix");
            if(p.phase()==ConstructionStore.Phase.COMPLETE){verifyResident(h,p);return;}
            h.assertTrue(p.phase()==ConstructionStore.Phase.BUILDING&&p.placed()>=16&&p.placed()<p.foundations().size(),"Second process resumes the saved partial foundation entitlement");
            // Explicit fixture acceleration of the approach only; no new Aldric or building blocks.
            var post=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion()).workPost(p.origin(),p.rotation());var aldric=level.getEntity(hut.npcId());aldric.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);
            var authoritativeCells=List.copyOf(p.foundations());boolean[] complete={false};
            h.onEachTick(()->guard(h,()->{
                if(complete[0])return;var current=ConstructionService.store(server).get(id);
                h.assertTrue(current.foundations().equals(authoritativeCells),"Restart never replans or changes approved foundation cells");
                for(int n=0;n<2&&current.phase()==ConstructionStore.Phase.BUILDING;n++){ConstructionService.step(server,id);current=ConstructionService.store(server).get(id);}
                if(current.phase()!=ConstructionStore.Phase.COMPLETE)return;
                complete[0]=true;verifyResident(h,current);
            }));
        });
    }
    private static void verifyResident(GameTestHelper h,ConstructionStore.Project p) {
        var level=h.getLevel();var npc=level.getEntity(p.npc());
        if(npc==null){h.runAfterDelay(5,()->verifyResident(h,p));return;}
        h.assertTrue(p.npcIssued()&&!p.npcDead()&&npc.isAlive()&&ConstructionService.structureIntact(level,p),"The permanent saved resident serves the intact supported hall");
        long residents=level.getEntitiesOfClass(com.mohymods.kingdomsmp.npc.ReeveEntity.class,ConstructionService.bounds(p).inflate(2)).stream().filter(Entity::isAlive).count();
        h.assertTrue(residents==1&&npc.getName().getString().equals("Garrick Thornfield"),"Exactly one original named Bounty Master exists after construction/restart");
        // First completion is in process two; an exact fixture marker survives only in SavedData's
        // ordinary world save, never inside the playable mod or as a gameplay entitlement.
        var data=level.getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(RestartMarker::new,(tag,provider)->new RestartMarker()),"kingdomsmp_foundation_test_completed");
        boolean prior=java.nio.file.Files.exists(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/kingdomsmp_foundation_test_completed.dat"));data.setDirty();
        com.mojang.logging.LogUtils.getLogger().info(prior?"FOUNDATION_RESTART_VERIFIED: same complete project={}, npc={}, foundation={}, placed={} after second real server restart":"FOUNDATION_RESTART_RESUMED: partial saved project={} finished with original npc={}, foundation={}, placed={}; flush and stop for a second real restart",p.id(),p.npc(),p.foundations().size(),p.placed());h.succeed();
    }
    private static final class RestartMarker extends net.minecraft.world.level.saveddata.SavedData {
        @Override public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider ignored){tag.putInt("schema",1);return tag;}
    }
    private static void move(ServerPlayer player,BlockPos pos){player.teleportTo(player.serverLevel(),pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),0,0);player.serverLevel().getChunkSource().move(player);}
    @FunctionalInterface private interface Step{void run()throws Exception;}
    private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
