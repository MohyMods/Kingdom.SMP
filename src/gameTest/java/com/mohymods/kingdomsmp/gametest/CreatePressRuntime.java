package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
/** Native physical player drops and powered real Create impact/recipe/input conversion. Only private environment/supplies/time accelerated. */
final class CreatePressRuntime {
 static final BlockPos PRESS=new BlockPos(544,88,512);
 static void run(GameTestHelper h,Runnable done)throws Exception{
  var level=h.getLevel();var server=level.getServer();var id=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().equals(BlueprintRegistry.HUT)).findFirst().orElseThrow().owner();var actor=server.getPlayerList().getPlayer(id);if(actor==null)actor=ClaimRuntimeTests.vanillaPlayer(level,id,"HallOwner");final var p=actor;server.getPlayerList().deop(p.getGameProfile());var store=CommerceService.store(server);var track=store.professions(id).track(Profession.ENGINEER);h.assertTrue(track.completedQuestIds().contains(CreateDriveInspection.QUEST),"Real prior native working-drive source required");
  for(int x=33;x<=34;x++)for(int z=31;z<=32;z++){level.getChunk(x,z);var at=new BlockPos(x*16,88,z*16);ClaimRuntimeTests.ftbUnclaim(level,at);ClaimRuntimeTests.ftbClaim(p,at);ClaimRuntimeTests.opacUnclaim(level,at);ClaimRuntimeTests.opacClaim(level,id,at);}
  BlacksmithRepairRuntime.near(p,PRESS.below());int count=CreatePressService.count(p);
  if(track.completedQuestIds().contains(CreatePressService.QUEST)){h.assertTrue(count==32&&track.xp()==500&&track.completedQuestIds().size()==5,"Third JVM restores actual32native sheets/5earned500XP/incomplete Apprentice band");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("CREATE_PRESS_RESTART_PASSED: actual native32press impacts/physical32sheetDEL/5earned500XP/canonical receipt and original Engineer Workshop-Ivo/enrollment reopen without replay");done.run();return;}
  boolean fresh=count==0;h.assertTrue(fresh||count==16,"Only genuine native partial16or fresh0proofs can run this fixture");
  if(fresh)build(h,p);
  h.runAfterDelay(160,()->{try{
   var press=level.getBlockEntity(PRESS);h.assertTrue(press!=null&&((Number)press.getClass().getMethod("getKineticSpeed").invoke(press)).floatValue()!=0,"Actual flowing waterwheel transmits real kinetic power to the native press; no setSpeed/network injection");
   if(fresh){
    // Exact optional evidence type fault only on this private loaded copy; original matching object restored before save.
    var matching=ProfessionEvidenceSavedData.get(server);level.getDataStorage().set("kingdomsmp_profession_evidence_v1",new SavedData(){public CompoundTag save(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){return tag;}});
    try{impact(h,p,press);h.assertTrue(p.getInventory().countItem(CreateCraftRuntime.item("create:iron_sheet"))>0,"Evidence failure cannot interrupt native ingot consumption/ordinary sheet output/pickup");}finally{level.getDataStorage().set("kingdomsmp_profession_evidence_v1",matching);matching.setDirty();}
    h.assertTrue(CreatePressService.count(p)==0,"Faulted optional evidence grants no proof or XP");
    for(int n=0;n<16;n++)impact(h,p,press);h.assertTrue(CreatePressService.count(p)==16&&store.professions(id).track(Profession.ENGINEER).xp()==400,"Actual16successful native drops/press impacts only; no premature delivery or XP");CommerceService.checkpoint(p);com.mojang.logging.LogUtils.getLogger().info("CREATE_PRESS_PENDING_PASSED: actual water-driven native machine/input-player drops/material conversion, corrupt optional evidence leaves vanilla transaction intact/no false credit;16real impact proofs/physical outputs saved,400XPunchanged");done.run();return;
   }
   h.assertTrue(p.getInventory().countItem(CreateCraftRuntime.item("create:iron_sheet"))>=16,"Partial native physical outputs survive real second JVM");for(int n=0;n<16;n++)impact(h,p,press);h.assertTrue(CreatePressService.count(p)==32,"Remaining16genuine native impacts complete32saved production proof");var project=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().toString().equals(Profession.ENGINEER.buildingId())).findFirst().orElseThrow();var bounds=ConstructionService.bounds(project);for(int x=((int)bounds.minX)>>4;x<=((int)bounds.maxX)>>4;x++)for(int z=((int)bounds.minZ)>>4;z<=((int)bounds.maxZ)>>4;z++)level.getChunk(x,z);
   ProfessionPremisesRuntimeTests.whenResidents(h,List.of(project),()->{try{CreateCraftRuntime.deliver(h,p,project,CreatePressService.QUEST);var earned=store.professions(id).track(Profession.ENGINEER);h.assertTrue(earned.xp()==500&&earned.completedQuestIds().size()==5&&p.getInventory().countItem(CreateCraftRuntime.item("create:iron_sheet"))==1,"Actual32plain sheets consumed for100XPonce; the uncredited fault-case sheet remains ordinary inventory");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("CREATE_PRESS_NATIVE_PASSED: real second JVM retains16native impact/physical outputs,16new real paid-input drops/powered native impact conversions,32plain physicalDEL/canonical receipt/5earned500XP/original Workshop-Ivo, optional failure sheet not retroactively credited");done.run();}catch(Exception failure){h.fail(failure.toString());}});
  }catch(Exception failure){h.fail("Native press pipeline: "+failure);}});
 }
 static void build(GameTestHelper h,ServerPlayer p){var level=p.serverLevel();for(var pos:BlockPos.betweenClosed(PRESS.offset(-4,-2,-4),PRESS.offset(4,3,7)))h.assertTrue(level.getBlockState(pos).isAir(),"New private press test site is empty; never replace player terrain/buildings");for(int x=-4;x<=4;x++)for(int z=-4;z<=7;z++)level.setBlockAndUpdate(PRESS.offset(x,-2,z),Blocks.STONE.defaultBlockState()); // Explicit private void terrain input, no construction proof.
  for(int x=-4;x<=4;x++)level.setBlockAndUpdate(PRESS.offset(x,-1,2),Blocks.STONE.defaultBlockState()); // Isolate actual water stream from processing floor.
  var wheel=PRESS.south(4);level.setBlockAndUpdate(wheel.below(),Blocks.STONE.defaultBlockState());CreateDriveRuntime.place(h,p,wheel,new ItemStack(CreateCraftRuntime.item("create:water_wheel")),wheel.below(),Direction.UP);
  for(int z=3;z>=1;z--)CreateDriveRuntime.place(h,p,PRESS.south(z),new ItemStack(CreateCraftRuntime.item("create:shaft")),PRESS.south(z+1),Direction.NORTH);
  CreateDriveRuntime.place(h,p,PRESS,new ItemStack(CreateCraftRuntime.item("create:mechanical_press")),PRESS.south(),Direction.NORTH);level.setBlockAndUpdate(wheel.east().above(2),Blocks.WATER.defaultBlockState()); // Explicit private source water; native fluid/kinetics do all processing.
 }
 static void impact(GameTestHelper h,ServerPlayer p,BlockEntity press)throws Exception{
  p.closeContainer();BlacksmithRepairRuntime.near(p,PRESS.below());var held=p.getMainHandItem().copy();ItemEntity dropped;try{p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_INGOT));var before=new HashSet<UUID>();for(var e:p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(PRESS).inflate(8)))before.add(e.getUUID());h.assertTrue(p.drop(false)&&p.getMainHandItem().isEmpty(),"Native ServerPlayer drop debits one real supplied inventory ingot");dropped=p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(PRESS).inflate(8)).stream().filter(e->!before.contains(e.getUUID())&&e.getItem().is(Items.IRON_INGOT)&&e.getOwner()==p).findFirst().orElseThrow();}finally{p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);}
  dropped.setPos(PRESS.getX()+.5,PRESS.getY()-.98,PRESS.getZ()+.5);dropped.setDeltaMovement(Vec3.ZERO); // Private aim/time acceleration of that actual dropped entity; no source/owner/ground/proof injection.
  var tick=press.getClass().getMethod("tick");for(int n=0;n<1000&&dropped.getItem().is(Items.IRON_INGOT);n++){dropped.tick();tick.invoke(press);}h.assertTrue(dropped.getItem().is(CreateCraftRuntime.item("create:iron_sheet"))&&dropped.getItem().getCount()==1,"Native moving press impact consumes actual input and produces one actual registered sheet");dropped.setPickUpDelay(0);int before=p.getInventory().countItem(CreateCraftRuntime.item("create:iron_sheet"));dropped.playerTouch(p);h.assertTrue(dropped.isRemoved()&&p.getInventory().countItem(CreateCraftRuntime.item("create:iron_sheet"))==before+1,"Actual native player pickup retains the real produced sheet");
 }
}
