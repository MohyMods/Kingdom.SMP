package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.equipment.*;
import com.mohymods.kingdomsmp.economy.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("kingdom_smp_farmer_gear_tests") @PrefixGameTestTemplate(false)
public final class FarmerGearRuntimeTests {
 @GameTest(template="empty",timeoutTicks=800)
 public static void actualFortuneHoeAbilities(GameTestHelper h)throws Exception{
  var level=h.getLevel();level.setDayTime(6000);var p=ClaimRuntimeTests.player(level,"HoeBorrower");var center=h.absolutePos(new BlockPos(-8,3,-8));
  for(var pos:BlockPos.betweenClosed(center.offset(-5,0,-5),center.offset(5,0,5))){level.setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);level.setBlock(pos,Math.abs(pos.getX()-center.getX())<=4&&Math.abs(pos.getZ()-center.getZ())<=4?((CropBlock)Blocks.WHEAT).getStateForAge(7):Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);}
  p.moveTo(center.getX()+.5,center.getY(),center.getZ()+.5);p.getInventory().clearContent();p.getInventory().selected=0;var tool=FarmerGearItems.stack(p,FarmerGear.FORTUNE_X_HOE);p.getInventory().setItem(0,tool);p.getInventory().setItem(1,new ItemStack(Items.WHEAT_SEEDS,64));p.getInventory().setItem(2,new ItemStack(Items.WHEAT_SEEDS,17));
  h.assertTrue(tool.get(DataComponents.ENCHANTMENTS).getLevel(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE))==10,"Real item carries Fortune X through verified1.21.1 enchantment components");h.assertTrue(CommerceService.store(p.server).professions(p.getUUID()).active().isEmpty(),"Borrower has no profession: approved Master item use is transferable");AuctionService.validateItem(tool);h.assertTrue(ItemStack.isSameItemSameComponents(tool,BuyOrderService.template(tool))&&FarmerGearItems.master(tool),"Pristine legitimate Master item is supported by exact-component buy orders and10%classification");use(p,center);
  h.runAfterDelay(8,()->guard(h,()->{
   int planted=0;for(var pos:BlockPos.betweenClosed(center.offset(-4,0,-4),center.offset(4,0,4)))if(level.getBlockState(pos).is(Blocks.WHEAT)&&((CropBlock)Blocks.WHEAT).getAge(level.getBlockState(pos))==0)planted++;
   h.assertTrue(planted==81,"Actual bounded9×9native break/use actions harvest and replant all81crops");h.assertTrue(p.getInventory().getItem(1).isEmpty()&&p.getInventory().getItem(2).isEmpty()&&tool.getDamageValue()==81,"Exactly81real seeds and81durability consumed; no free replanting");
   var protectedPos=new BlockPos(Math.floorDiv(center.getX()+48,16)*16,center.getY(),center.getZ());level.setBlock(protectedPos.below(),Blocks.FARMLAND.defaultBlockState(),3);level.setBlock(protectedPos,((CropBlock)Blocks.WHEAT).getStateForAge(7),3);var defender=ClaimRuntimeTests.player(level,"HoeDefender");ClaimRuntimeTests.ftbClaim(defender,protectedPos);ClaimRuntimeTests.opacClaim(level,defender.getUUID(),protectedPos);var legal=protectedPos.west();level.setBlock(legal.below(),Blocks.FARMLAND.defaultBlockState(),3);level.setBlock(legal,((CropBlock)Blocks.WHEAT).getStateForAge(7),3);p.moveTo(legal.getX()+.5,legal.getY(),legal.getZ()+.5);var fresh=FarmerGearItems.stack(p,FarmerGear.FORTUNE_X_HOE);p.getInventory().setItem(0,fresh);p.getInventory().setItem(1,new ItemStack(Items.WHEAT_SEEDS,2));use(p,legal);
   h.runAfterDelay(8,()->guard(h,()->{
    h.assertTrue(((CropBlock)Blocks.WHEAT).isMaxAge(level.getBlockState(protectedPos))&&p.getInventory().getItem(1).getCount()==1&&fresh.getDamageValue()==1&&((CropBlock)Blocks.WHEAT).getAge(level.getBlockState(legal))==0,"Legal center is harvested/replanted once while both real protection APIs retain the adjacent foreign crop; no foreign seed or durability spent");
    var unsupported=center.west(32);level.setBlock(unsupported.below(),Blocks.FARMLAND.defaultBlockState(),3);level.setBlock(unsupported,((CropBlock)Blocks.WHEAT).getStateForAge(7),3);p.moveTo(unsupported.getX()+.5,unsupported.getY(),unsupported.getZ()+.5);p.getInventory().setItem(1,ItemStack.EMPTY);use(p,unsupported);
    h.runAfterDelay(8,()->guard(h,()->{h.assertTrue(((CropBlock)Blocks.WHEAT).isMaxAge(level.getBlockState(unsupported))&&fresh.getDamageValue()==1,"Missing supplied seeds leaves mature crop and tool untouched");com.mojang.logging.LogUtils.getLogger().info("FARMER_GEAR_ASSERTIONS_PASSED: real FortuneX component, transferable use,81native harvests/replants, exact seed/durability cost, both claims, no-seed refusal, pristine order/tax classification");h.succeed();}));
   }));
  }));
 }
 private static void use(ServerPlayer p,BlockPos pos){p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
