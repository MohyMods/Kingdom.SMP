package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
/** Controlled native finalize reasons and actual deaths, never injected encounter counters/XP. */
final class EncounterRuntime {
 static final BlockPos SITE=new BlockPos(704,90,512);
 static void run(GameTestHelper h,Runnable done)throws Exception{
  var level=h.getLevel();var server=level.getServer();var project=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().toString().equals(Profession.ADVENTURER.buildingId())).findFirst().orElseThrow();var owner=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().equals(BlueprintRegistry.HUT)).findFirst().orElseThrow().owner();var p=server.getPlayerList().getPlayer(owner);if(p==null)p=ClaimRuntimeTests.vanillaPlayer(level,owner,"HallOwner");server.getPlayerList().deop(p.getGameProfile());var store=CommerceService.store(server);var track=store.professions(p.getUUID()).track(Profession.ADVENTURER);
  if(track.completedQuestIds().contains(EncounterQuests.QUEST)){h.assertTrue(track.xp()==300&&track.completedQuestIds().size()==3&&EncounterSavedData.get(server).evidence().ready(p.getUUID(),24,4)&&ConstructionService.structureIntact(level,project),"Second JVM retains24native encounters/300earnedXP/original guild and Kael");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("ENCOUNTER_RESTART_PASSED: permanent unique species/victim evidence,300earnedXP, original guild/Kael and native inventory receipt restore");done.run();return;}
  h.assertTrue(track.xp()==200&&track.completedQuestIds().size()==2,"Starts from two actually earned supplies/service proofs");DimensionTravelRuntimeTests.seed(server,3);server.setDifficulty(Difficulty.NORMAL,true);level.getChunk(SITE);ClaimRuntimeTests.ftbUnclaim(level,SITE);ClaimRuntimeTests.ftbClaim(p,SITE);ClaimRuntimeTests.opacUnclaim(level,SITE);ClaimRuntimeTests.opacClaim(level,p.getUUID(),SITE);
  for(var pos:BlockPos.betweenClosed(SITE.offset(-4,-1,-4),SITE.offset(4,-1,4)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
  p.teleportTo(level,SITE.getX()+.5,SITE.getY(),SITE.getZ()+.5,Set.of(),0,0);level.getChunkSource().move(p);var player=p;
  h.runAfterDelay(5,()->guard(h,()->waitLoaded(h,player,project,done,0)));
 }
 static void waitLoaded(GameTestHelper h,ServerPlayer p,com.mohymods.kingdomsmp.persistence.ConstructionStore.Project project,Runnable done,int attempt)throws Exception{
  if(!p.serverLevel().isPositionEntityTicking(SITE)){h.assertTrue(attempt<80,"Native destination becomes ticking");h.runAfterDelay(5,()->guard(h,()->waitLoaded(h,p,project,done,attempt+1)));return;}
  for(var reason:List.of(MobSpawnType.COMMAND,MobSpawnType.SPAWNER,MobSpawnType.SPAWN_EGG)){var m=mob(h,p,"minecraft:zombie",reason);m.hurt(p.damageSources().playerAttack(p),1000);}
  var temporary=net.minecraft.world.entity.EntityType.ZOMBIE.create(p.serverLevel());temporary.moveTo(SITE.getX()+2.5,SITE.getY(),SITE.getZ()+.5);EventHooks.finalizeMobSpawn(temporary,p.serverLevel(),p.serverLevel().getCurrentDifficultyAt(temporary.blockPosition()),MobSpawnType.NATURAL,null);h.assertTrue(p.serverLevel().getEntity(temporary.getUUID())==null,"Temporary native entity never entered world tracking");temporary.hurt(p.damageSources().playerAttack(p),1000);
  var indirect=mob(h,p,"minecraft:skeleton",MobSpawnType.NATURAL);indirect.hurt(indirect.damageSources().generic(),1000);
  var canceled=mob(h,p,"minecraft:spider",MobSpawnType.NATURAL);java.util.function.Consumer<LivingDeathEvent> cancel=e->{if(e.getEntity()==canceled)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,LivingDeathEvent.class,cancel);canceled.hurt(p.damageSources().playerAttack(p),1000);
  var signed=mob(h,p,"minecraft:zombie",MobSpawnType.NATURAL);var copied=mob(h,p,"minecraft:zombie",MobSpawnType.COMMAND);copied.getPersistentData().put("kingdomsmp_hunt_origin",signed.getPersistentData().getCompound("kingdomsmp_hunt_origin").copy());copied.hurt(p.damageSources().playerAttack(p),1000);signed.discard();
  h.runAfterDelay(3,()->guard(h,()->{NeoForge.EVENT_BUS.unregister(cancel);canceled.discard();h.assertTrue(EncounterSavedData.get(p.server).evidence().counts(p.getUUID()).isEmpty(),"Command/spawner/egg/indirect/canceled/untracked/copied UUID signature deaths grant no encounters");kill(h,p,project,done,0);}));
 }
 static Mob mob(GameTestHelper h,ServerPlayer p,String type,MobSpawnType reason){var level=p.serverLevel();var m=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(type)).create(level);m.moveTo(SITE.getX()+2.5,SITE.getY(),SITE.getZ()+.5);EventHooks.finalizeMobSpawn(m,level,level.getCurrentDifficultyAt(m.blockPosition()),reason,null);m.setNoAi(true);h.assertTrue(level.addFreshEntity(m)&&level.getEntity(m.getUUID())==m,"Actual finalized mob enters native entity tracking");return m;}
 static void kill(GameTestHelper h,ServerPlayer p,com.mohymods.kingdomsmp.persistence.ConstructionStore.Project project,Runnable done,int n)throws Exception{
  if(n==24){CreateCraftRuntime.counter(p,project);var q=ProfessionService.review(p,"quest:"+EncounterQuests.QUEST);ProfessionService.confirm(p,q.nonce());boolean replay=false;try{ProfessionService.confirm(p,q.nonce());}catch(IllegalStateException expected){replay=true;}h.assertTrue(replay&&CommerceService.store(p.server).professions(p.getUUID()).track(Profession.ADVENTURER).xp()==300,"Normal reviewed completion earns100XP once");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("ENCOUNTER_NATIVE_PASSED: actual24native attributed natural-origin deaths/four species, command/spawner/egg/indirect/canceled/copied signature refusals, ordinary once-only review/300earnedXP; controlled native spawn/damage fixture, no injected counters/XP or natural-duration/fullpack claim");done.run();return;}
  var m=mob(h,p,HuntState.TARGETS.get(n%4),MobSpawnType.NATURAL);h.assertTrue(m.hurt(p.damageSources().playerAttack(p),1000),"Actual native player damage defeats authenticated mob");
  h.runAfterDelay(3,()->guard(h,()->{h.assertTrue(EncounterSavedData.get(p.server).evidence().counts(p.getUUID()).values().stream().mapToInt(Integer::intValue).sum()==n+1,"Each actual successful death creates exactly one personal encounter");kill(h,p,project,done,n+1);}));
 }
 interface Step{void run()throws Exception;}static void guard(GameTestHelper h,Step s){try{s.run();}catch(Exception e){h.fail(e.toString());}}
}
