package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.integration.space.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** Exact optional SDK, seeded boundary tiers/fuel/materials, two real JVMs; no planetary split approval. */
@GameTestHolder("kingdom_smp_space_tests") @PrefixGameTestTemplate(false)
public final class SpaceRuntimeTests {
 @GameTest(template="empty",timeoutTicks=1800)
 public static void nativePreCostSpaceTravelRulesAndRestart(GameTestHelper h)throws Exception{
  var level=h.getLevel();var server=level.getServer();var path=server.getWorldPath(LevelResource.ROOT).resolve("space-floor-proof.nbt");boolean restart=Files.exists(path);var proof=restart?AtomicNbt.read(path):new CompoundTag();
  if(restart)h.assertTrue(ContributionService.kingdom(server).attainedTier()==(proof.getInt("phase")==1?10:9),"Actual saved boundary tier survives the second JVM without an award or tier grant");
  else DimensionTravelRuntimeTests.seed(server,9);
  var p=ClaimRuntimeTests.vanillaPlayer(level,UUID.nameUUIDFromBytes("OfflinePlayer:SpacePilot".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"SpacePilot");
  var pos=restart?BlockPos.of(proof.getLong("pos")):h.absolutePos(new BlockPos(24,1,4));
  for(var cell:BlockPos.betweenClosed(pos.offset(-5,-1,-5),pos.offset(5,-1,5)))level.setBlock(cell,Blocks.STONE.defaultBlockState(),18);
  p.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()-3,0,0);level.getChunkSource().move(p);
  h.runAfterDelay(80,()->guard(h,()->{
   h.assertTrue(!p.hasPermissions(2)&&!p.getAbilities().mayfly,"Ordinary authenticated Survival player, no operator/flight bypass");
   Entity rocket;
   if(restart){rocket=level.getEntity(proof.getUUID("rocket"));h.assertTrue(rocket!=null&&rocket.isAlive(),"Original native rocket UUID survives real process restart");h.assertTrue(amount(rocket)==proof.getLong("fuel"),"Exact native fluid storage remains after denied ignition/landing and JVM restart");}
   else{rocket=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("ad_astra:tier_1_rocket")).create(level);h.assertTrue(rocket!=null,"Actual registered native Tier1 rocket");rocket.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(rocket);fill(h,rocket);}
   if(restart&&proof.getInt("phase")==1){finish(h,p,rocket,proof,pos);return;}
   h.assertTrue(p.startRiding(rocket,true),"Native passenger setup retains the original rocket");
   p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ad_astra:iron_plate")),64));p.getInventory().setItem(1,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ad_astra:steel_ingot")),32));p.getInventory().setItem(2,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ad_astra:desh_ingot")),32));p.getInventory().setItem(3,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ad_astra:desh_plate")),32));
   var menu=Class.forName("earth.terrarium.adastra.common.menus.PlanetsMenu").getConstructor(int.class,net.minecraft.world.entity.player.Inventory.class,Set.class,Map.class,Set.class).newInstance(1,p.getInventory(),Set.of(),Map.of(),Set.of());p.containerMenu=(AbstractContainerMenu)menu;
   long fuel=amount(rocket);h.assertTrue(Boolean.TRUE.equals(call(rocket,"hasEnoughFuel")),"Real fuel storage satisfies native launch cost before denial test");
   call(rocket,"initiateLaunchSequence");h.assertTrue(!Boolean.TRUE.equals(call(rocket,"isLaunching"))&&amount(rocket)==fuel,"Real native ignition refuses Tier9 before countdown or fuel consumption");
   menuLocks(h,p,9);
   var moon=key("ad_astra:moon");var orbit=key("ad_astra:moon_orbit");
   // Explicit test-only flight-altitude staging. Native handover otherwise correctly
   // refuses a ground rocket even with a planet menu, masking the progression check.
   altitude(p,rocket);
   var apiType=Class.forName("earth.terrarium.adastra.api.planets.PlanetApi");var api=apiType.getField("API").get(null);var planet=apiType.getMethod("getPlanet",ResourceKey.class).invoke(api,moon);
   h.assertTrue(Boolean.TRUE.equals(Class.forName("earth.terrarium.adastra.common.utils.ModUtils").getMethod("canTeleportToPlanet",net.minecraft.world.entity.player.Player.class,planet.getClass()).invoke(null,p,planet)),"Native rocket, menu, altitude and planet requirements are genuinely eligible before the locked-tier packet test");
   var recipeType=Class.forName("earth.terrarium.adastra.common.recipes.SpaceStationRecipe");var holder=(Optional<?>)recipeType.getMethod("getSpaceStation",Level.class,ResourceKey.class).invoke(null,level,orbit);
   var recipe=((net.minecraft.world.item.crafting.RecipeHolder<?>)holder.orElseThrow()).value();
   h.assertTrue(Boolean.TRUE.equals(recipeType.getMethod("hasIngredients",net.minecraft.world.entity.player.Player.class,Level.class,recipeType).invoke(null,p,server.getLevel(orbit),recipe)),"Real native station recipe is fully supplied before its pre-cost refusal");
   send(p,"ServerboundLandPacket",new Class<?>[]{ResourceKey.class,boolean.class},moon,false);
   send(p,"ServerboundConstructSpaceStationPacket",new Class<?>[]{ResourceKey.class,net.minecraft.network.chat.Component.class},orbit,net.minecraft.network.chat.Component.literal("Private QA Station"));
   send(p,"ServerboundLandOnSpaceStationPacket",new Class<?>[]{ResourceKey.class,ChunkPos.class},orbit,new ChunkPos(0,0));
   h.assertTrue(p.serverLevel()==level&&p.getVehicle()==rocket&&rocket.isAlive()&&amount(rocket)==fuel,"Actual native landing/station handlers refuse before rocket removal, passenger handover or fuel changes");
   h.assertTrue(p.getInventory().getItem(0).getCount()==64&&p.getInventory().getItem(1).getCount()==32&&p.getInventory().getItem(2).getCount()==32&&p.getInventory().getItem(3).getCount()==32,"Denied native station construction retains its actual recipe materials");
   for(String destination:SpaceTravelRules.destinations()){
    var target=server.getLevel(key(destination));h.assertTrue(target!=null,"Exact space destination is really registered: "+destination);
    var transition=new DimensionTransition(target,new Vec3(0,150,0),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING);
    h.assertTrue(p.changeDimension(transition)==null&&p.serverLevel()==level&&p.getVehicle()==rocket,"Direct player travel cannot bypass minimum space floor: "+destination);
    var item=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Items.COPPER_INGOT,7));level.addFreshEntity(item);
    h.assertTrue(item.changeDimension(transition)==null&&item.level()==level&&item.getItem().getCount()==7,"Premature native item transfer retains original stack: "+destination);item.discard();
   }
   h.assertTrue(server.getCommands().getDispatcher().execute("kingdom admin travel inspect",server.createCommandSourceStack())==1,"Actual administrator space boundary diagnostics");
   rocket.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);rocket.setDeltaMovement(Vec3.ZERO);
   if(!restart){
    p.stopRiding();p.closeContainer();p.teleportTo(level,pos.getX()+3.5,pos.getY(),pos.getZ()+.5,0,0);level.getChunkSource().move(p);proof.putUUID("rocket",rocket.getUUID());proof.putLong("pos",pos.asLong());proof.putLong("fuel",fuel);AtomicNbt.write(path,proof);h.succeed();log("SPACE_FLOOR_PREPARED: actual exact SDK ignition and three native pre-cost handlers, all11real destination/player/item denials, original rocket/materials/fuel retained at seeded Tier9");return;
   }
   DimensionTravelRuntimeTests.seed(server,10);menuLocks(h,p,10);call(rocket,"initiateLaunchSequence");h.assertTrue(Boolean.TRUE.equals(call(rocket,"isLaunching"))&&amount(rocket)<fuel,"At the approved minimum, native countdown starts and consumes its actual normal fuel cost");
   var dataManager=Class.forName("earth.terrarium.adastra.common.registry.ModDataManagers").getField("FLUID_CONTENTS").get(null);
   var snapshot=Class.forName("earth.terrarium.common_storage_lib.data.DataManager").getMethod("get",Object.class).invoke(dataManager,rocket);
   var stacks=(List<?>)call(snapshot,"stacks");h.assertTrue(!stacks.isEmpty()&&(long)call(stacks.getFirst(),"amount")==amount(rocket),"Native consumed launch fuel is committed to its actual persistent data manager");
   // Exact destination tiers are now approved; native rocket requirements remain.
   p.stopRiding();p.closeContainer();p.teleportTo(level,pos.getX()+3.5,pos.getY(),pos.getZ()+.5,0,0);level.getChunkSource().move(p);
   proof.putInt("phase",1);proof.putLong("fuel",amount(rocket));AtomicNbt.write(path,proof);h.succeed();log("SPACE_COUNTDOWN_PREPARED: actual second JVM retains original locked rocket/fuel, then normal Tier10 ignition commits fuel and saves the live native countdown for another restart");
  }));
 }
 private static void finish(GameTestHelper h,ServerPlayer p,Entity rocket,CompoundTag proof,BlockPos pos)throws Exception{
  var home=h.getLevel();var server=home.getServer();
  h.assertTrue(Boolean.TRUE.equals(call(rocket,"isLaunching"))&&!Boolean.TRUE.equals(call(rocket,"hasLaunched"))&&(int)call(rocket,"launchTicks")>0&&(int)call(rocket,"launchTicks")<200,"Actual third JVM resumes the saved countdown instead of prematurely launching from a mismatched NBT key");
  h.assertTrue(amount(rocket)==proof.getLong("fuel"),"Consumed launch fuel stays consumed after the real restart");
  h.assertTrue(p.startRiding(rocket,true),"Native restarted rocket remains the original recoverable entity");
  p.containerMenu=(AbstractContainerMenu)Class.forName("earth.terrarium.adastra.common.menus.PlanetsMenu").getConstructor(int.class,net.minecraft.world.entity.player.Inventory.class,Set.class,Map.class,Set.class).newInstance(1,p.getInventory(),Set.of(),Map.of(),Set.of());
  altitude(p,rocket);
  send(p,"ServerboundLandPacket",new Class<?>[]{ResourceKey.class,boolean.class},key("ad_astra:mars"),false);
  h.assertTrue(p.serverLevel()==home&&p.getVehicle()==rocket&&rocket.isAlive()&&amount(rocket)==proof.getLong("fuel"),"Advanced native landing still refuses at Tier10 before handover");
  send(p,"ServerboundLandPacket",new Class<?>[]{ResourceKey.class,boolean.class},key("ad_astra:moon"),false);
  var lander=p.getVehicle();com.mojang.logging.LogUtils.getLogger().info("Native Moon landing diagnostic tier={} world={} vehicle={} originalAlive={} nativeRocketTier={} menu={}",ContributionService.kingdom(server).attainedTier(),p.serverLevel().dimension().location(),lander==null?null:BuiltInRegistries.ENTITY_TYPE.getKey(lander.getType()),rocket.isAlive(),call(rocket,"tier"),p.containerMenu.getClass().getName());h.assertTrue(p.serverLevel().dimension().equals(key("ad_astra:moon"))&&lander!=null&&BuiltInRegistries.ENTITY_TYPE.getKey(lander.getType()).toString().equals("ad_astra:lander")&&!rocket.isAlive(),"Approved Moon10 uses actual native rocket-to-lander handover, retaining original native rocket eligibility");
  var inventory=(net.minecraft.world.Container)call(lander,"inventory");h.assertTrue(inventory.getItem(0).is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ad_astra:tier_1_rocket")))&&inventory.getItem(0).getCount()==1,"Exactly one native rocket item is recovered by the native landing operation");
  p.stopRiding();p.closeContainer();DimensionTravelRuntimeTests.seed(server,9);p.teleportTo(home,pos.getX()+3.5,pos.getY(),pos.getZ()+.5,0,0);lander.discard();
  h.assertTrue(p.serverLevel()==home&&!p.getAbilities().mayfly,"Already-inside player can return to the Overworld below the space threshold without a flight grant");
  var mars=server.getLevel(key("ad_astra:mars"));var transition=new DimensionTransition(mars,new Vec3(0,150,0),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING);
  DimensionTravelRuntimeTests.seed(server,10);h.assertTrue(p.changeDimension(transition)==null&&p.serverLevel()==home,"Advanced direct transition is refused at10");
  DimensionTravelRuntimeTests.seed(server,11);menuLocks(h,p,11);h.assertTrue(p.changeDimension(transition)==p&&p.serverLevel()==mars,"Approved Tier11 permits actual native Mars transition");p.teleportTo(home,pos.getX()+3.5,pos.getY(),pos.getZ()+.5,0,0);
  h.succeed();log("SPACE_FLOOR_RESTART_VERIFIED: actual third JVM preserves consumed fuel/countdown, approved10Moon native handover and exact rocket item, advanced10denial/11Mars transition, safe Overworld return; explicit boundary/menu/fuel seeds only");
 }
 @SuppressWarnings("unchecked") private static void menuLocks(GameTestHelper h,ServerPlayer p,int tier)throws Exception{
  var provider=Class.forName("earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider").getConstructor().newInstance();
  var create=provider.getClass().getMethod("createContent",ServerPlayer.class);var content=create.invoke(provider,p);var disabled=(Set<ResourceLocation>)call(content,"disabledPlanets");
  for(var realm:SpaceTravelRules.realms())h.assertTrue(disabled.contains(ResourceLocation.parse(realm.dimension()))==(tier<realm.tier()),"Real native planet-menu DTO reflects exact current kingdom gate: "+realm.dimension()+" @"+tier);
  h.assertTrue(disabled.contains(ResourceLocation.parse("minecraft:the_end")),"Native menu projection retains End12 even for an added End planet");
  var config=Class.forName("earth.terrarium.adastra.common.config.AdAstraConfig").getField("disabledPlanets");var previous=(String)config.get(null);
  try{
   config.set(null,previous.isBlank()?"ad_astra:moon":previous+",ad_astra:moon");var protectedContent=create.invoke(provider,p);
   h.assertTrue(((Set<ResourceLocation>)call(protectedContent,"disabledPlanets")).contains(ResourceLocation.parse("ad_astra:moon")),"Current kingdom projection never clears an independent native server planet disable");
  }finally{config.set(null,previous);}
 }
 private static void altitude(ServerPlayer p,Entity rocket)throws Exception{int altitude=Class.forName("earth.terrarium.adastra.common.config.AdAstraConfig").getField("atmosphereLeave").getInt(null)+1;rocket.moveTo(rocket.getX(),altitude,rocket.getZ());p.moveTo(rocket.getX(),altitude,rocket.getZ());p.serverLevel().getChunkSource().move(p);}
 private static ResourceKey<Level> key(String id){return ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(id));}
 private static Object call(Object target,String name)throws Exception{return target.getClass().getMethod(name).invoke(target);}
 private static long amount(Entity rocket)throws Exception{var storage=call(rocket,"fluidContainer");return (long)storage.getClass().getMethod("getAmount",int.class).invoke(storage,0);}
 private static void fill(GameTestHelper h,Entity rocket)throws Exception{
  var resource=Class.forName("earth.terrarium.common_storage_lib.resources.fluid.FluidResource");var fuel=BuiltInRegistries.FLUID.get(ResourceLocation.parse("ad_astra:fuel"));
  var nativeFuel=resource.getMethod("of",Fluid.class).invoke(null,fuel);var storage=call(rocket,"fluidContainer");long inserted=(long)storage.getClass().getMethod("insert",resource,long.class,boolean.class).invoke(storage,nativeFuel,10_000L,false);h.assertTrue(inserted>0,"Explicit test supplies enter through the real native fluid storage API");storage.getClass().getMethod("update").invoke(storage);
 }
 @SuppressWarnings("unchecked") private static void send(ServerPlayer p,String name,Class<?>[] types,Object...args)throws Exception{var packet=Class.forName("earth.terrarium.adastra.common.network.packets."+name).getConstructor(types).newInstance(args);var type=packet.getClass().getField("TYPE").get(null);var handle=type.getClass().getMethod("handle",packet.getClass());handle.setAccessible(true);((Consumer<net.minecraft.world.entity.player.Player>)handle.invoke(type,packet)).accept(p);}
 private static void log(String value){com.mojang.logging.LogUtils.getLogger().info(value);}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Native space boundary assertion failed",e);h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
