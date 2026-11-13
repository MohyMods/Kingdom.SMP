package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
/** Native small wheel, real fluid flow and connected native shaft; no speed/network/quest injection. */
final class CreateDriveRuntime {
 static void run(GameTestHelper h,Runnable done)throws Exception{
  var level=h.getLevel();var server=level.getServer();var id=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().equals(BlueprintRegistry.HUT)).findFirst().orElseThrow().owner();var actor=server.getPlayerList().getPlayer(id);if(actor==null)actor=ClaimRuntimeTests.vanillaPlayer(level,id,"HallOwner");final var p=actor;server.getPlayerList().deop(p.getGameProfile());var store=CommerceService.store(server);var track=store.professions(id).track(Profession.ENGINEER);h.assertTrue(track.completedQuestIds().containsAll(Set.of(CreateCraftQuests.ALLOY,CreateCraftQuests.COMPONENTS)),"Use actually earned native alloy/component source, no injected rank");var center=new BlockPos(512,85,512);
  for(int x=31;x<=32;x++)for(int z=31;z<=32;z++){level.getChunk(x,z);var at=new BlockPos(x*16,85,z*16);ClaimRuntimeTests.ftbUnclaim(level,at);ClaimRuntimeTests.ftbClaim(p,at);ClaimRuntimeTests.opacUnclaim(level,at);ClaimRuntimeTests.opacClaim(level,id,at);}
  BlacksmithRepairRuntime.near(p,center);
  if(track.completedQuestIds().contains(CreateDriveInspection.QUEST)){
   h.runAfterDelay(80,()->{try{h.assertTrue(CreateDriveInspection.validate(p,center).startsWith("waterwheel=")&&track.xp()==400&&track.completedQuestIds().size()==4,"Second JVM restores actual flowing drive/native network/4earned400XP and previous proofs");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("CREATE_DRIVE_RESTART_PASSED: actual native fluid-powered wheel and connected live shaft resume after restart with same4earned400XP/canonical inspection/enrollment/premise-Ivo");done.run();}catch(Exception failure){h.fail(failure.toString());}});return;
  }
  h.assertTrue(track.xp()==300,"Native operating-drive trial starts at real300XP");for(var pos:BlockPos.betweenClosed(center.offset(-3,-1,-3),center.offset(3,3,3)))h.assertTrue(level.getBlockState(pos).isAir(),"Private test site is empty; never overwrite an existing building or terrain");
  level.setBlockAndUpdate(center.below(),Blocks.STONE.defaultBlockState()); // Explicit private scaffold input in this void fixture, not quest/build proof.
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(x!=0||z!=0)level.setBlockAndUpdate(center.offset(x,-1,z),Blocks.STONE.defaultBlockState()); // Private source terrain.
  place(h,p,center,new ItemStack(CreateCraftRuntime.item("create:water_wheel")),center.below(),Direction.UP);
  var facing=level.getBlockState(center).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);h.assertTrue(facing.getAxis().isHorizontal(),"Actual native wheel axis is horizontal");var shaft=center.relative(facing);place(h,p,shaft,new ItemStack(CreateCraftRuntime.item("create:shaft")),center,facing);
  boolean denied=false;try{CreateDriveInspection.validate(p,center);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"A dry idle wheel and adjacent furniture shaft cannot pass inspection");
  var side=facing.getAxis()==Direction.Axis.Z?Direction.EAST:Direction.SOUTH;var water=center.relative(side).above(2);h.assertTrue(level.getBlockState(water).isAir(),"Private source water input replaces only air");level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
  h.runAfterDelay(120,()->{try{inspect(h,p,center);done.run();}catch(Exception failure){var wheel=level.getBlockEntity(center);h.fail("Native flowing drive: "+failure+" state="+level.getBlockState(center)+" wheel="+wheel+(wheel==null?"":" generated="+number(wheel,"getGeneratedSpeed")+" speed="+number(wheel,"getSpeed")));}});
 }
 static Object number(Object object,String method){try{return object.getClass().getMethod(method).invoke(object);}catch(Exception failure){return failure.toString();}}
 static void place(GameTestHelper h,ServerPlayer p,BlockPos at,ItemStack input,BlockPos clicked,Direction face){var held=p.getMainHandItem().copy();BlacksmithRepairRuntime.near(p,at);p.setYRot(0);p.setXRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,input);p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,clicked,false));h.assertTrue(p.getMainHandItem().isEmpty()&&!p.serverLevel().getBlockState(at).isAir(),"Actual native permitted machinery placement consumes physical input");p.setItemInHand(InteractionHand.MAIN_HAND,held);}
 static void inspect(GameTestHelper h,ServerPlayer p,BlockPos center)throws Exception{
  var server=p.server;var level=p.serverLevel();var buyer=ConstructionService.store(server).projects().stream().filter(q->ProfessionPremises.contains(q.blueprint())).findFirst().orElseThrow().owner();var admin=server.getPlayerList().getPlayer(buyer);h.assertTrue(admin!=null&&!buyer.equals(p.getUUID()),"Original independent owner attends as inspector");server.getPlayerList().op(admin.getGameProfile());try{
   admin.moveTo(center.getX()+1.5,center.getY(),center.getZ()-2.5);p.moveTo(center.getX()+.5,center.getY(),center.getZ()-2.5);String live=CreateDriveInspection.validate(p,center);h.assertTrue(live.startsWith("waterwheel="),"Native world fluid flow drives the actual wheel/source/network and connected shaft, without setSpeed/setFlowScore");var review=ProfessionInspectionService.review(admin,p,CreateDriveInspection.QUEST,center,"Inspected real water power connected transmission safe access and present owner consent");ClaimRuntimeTests.ftbUnclaim(level,center);ClaimRuntimeTests.ftbClaim(admin,center);boolean denied=false;try{ProfessionInspectionService.confirm(admin,review);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Actual after-review foreign claim revocation blocks approval/XP");ClaimRuntimeTests.ftbUnclaim(level,center);ClaimRuntimeTests.ftbClaim(p,center);
   review=ProfessionInspectionService.review(admin,p,CreateDriveInspection.QUEST,center,"Inspected real water power connected transmission safe access and present owner consent");ProfessionInspectionService.confirm(admin,review);var track=CommerceService.store(server).professions(p.getUUID()).track(Profession.ENGINEER);h.assertTrue(track.xp()==400&&track.completedQuestIds().size()==4,"Actual confirmed working drive adds100XPonce; unfinished functions/processing still block Journeyman");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("CREATE_DRIVE_NATIVE_PASSED: actual consumed wheel/shaft native placement, dry furniture refusal, real source water native flow/native generated speed and same-network shaft, OP2current-site approval/after-review FTBclaim revocation refusal/4earned400XP/original workshop-Ivo; explicit private terrain-water/tier supplies only");
  }finally{server.getPlayerList().deop(admin.getGameProfile());}
 }
}
