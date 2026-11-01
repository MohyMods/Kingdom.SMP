package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;
/** Dedicated event/API tests. Fixture seeds an active roster, and accelerates growth/AI waiting, never quest completion. */
@GameTestHolder("kingdom_smp_farmer_events_tests") @PrefixGameTestTemplate(false)
public final class FarmerEventsRuntimeTests {
 @GameTest(template="empty",timeoutTicks=600)
 public static void actualStemBreedingAndSustainableCycles(GameTestHelper h)throws Exception{
  var level=h.getLevel();var server=level.getServer();var p=ClaimRuntimeTests.player(level,"FarmerEvents");CommerceService.store(server).changeProfession(UUID.randomUUID(),p.getUUID(),"enroll","","farmer");level.setDayTime(6000);var soil=h.absolutePos(new BlockPos(-8,2,-8));
  for(var pos:BlockPos.betweenClosed(soil.offset(-2,0,-2),soil.offset(5,0,2))){level.setBlock(pos,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);for(int y=1;y<4;y++)level.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),3);}p.moveTo(soil.getX()+.5,soil.getY()+1,soil.getZ()+2.5);p.getInventory().selected=0;
  p.getInventory().setItem(0,new ItemStack(Items.WHEAT_SEEDS));use(p,soil);p.getInventory().setItem(0,new ItemStack(Items.MELON_SEEDS));use(p,soil.east(3));
  h.runAfterDelay(3,()->guard(h,()->{
   h.assertTrue(level.getBlockState(soil.above()).is(Blocks.WHEAT)&&level.getBlockState(soil.east(3).above()).is(Blocks.MELON_STEM),"Real native seed planting establishes crop and stem provenance");level.setBlock(soil.above(),((CropBlock)Blocks.WHEAT).getStateForAge(7),3);p.getInventory().setItem(0,ItemStack.EMPTY);p.gameMode.destroyBlock(soil.above());
   var stemPos=soil.east(3).above();level.setBlock(stemPos,Blocks.MELON_STEM.defaultBlockState().setValue(StemBlock.AGE,7),3);var random=RandomSource.create(8612);for(int n=0;n<500&&level.getBlockState(stemPos).is(Blocks.MELON_STEM);n++)level.getBlockState(stemPos).randomTick(level,stemPos,random);h.assertTrue(level.getBlockState(stemPos).is(Blocks.ATTACHED_MELON_STEM),"Actual vanilla random-tick API generates fruit and attached stem");var fruit=stemPos.relative(level.getBlockState(stemPos).getValue(AttachedStemBlock.FACING));h.assertTrue(p.gameMode.destroyBlock(fruit),"Native harvest removes actual stem-grown melon");
   var a=EntityType.COW.create(level);var b=EntityType.COW.create(level);a.moveTo(soil.getX(),soil.getY()+1,soil.getZ()+1);b.moveTo(soil.getX()+1,soil.getY()+1,soil.getZ()+1);level.addFreshEntity(a);level.addFreshEntity(b);p.getInventory().setItem(0,new ItemStack(Items.WHEAT,2));a.mobInteract(p,InteractionHand.MAIN_HAND);b.mobInteract(p,InteractionHand.MAIN_HAND);h.assertTrue(p.getInventory().getItem(0).isEmpty()&&a.isInLove()&&b.isInLove(),"Real feeding consumes two wheat and attributes both breeding parents");a.spawnChildFromBreeding(level,b);
   h.runAfterDelay(3,()->guard(h,()->{
    h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"wheat")==1&&FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"melon")==1&&FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"cow")==1,"Real crop, actual generated fruit and actual unique baby spawn recorded");h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"replant")==0,"Harvest alone is not sustainable replanting");
    p.getInventory().setItem(0,new ItemStack(Items.WHEAT_SEEDS));use(p,soil);var copied=soil.offset(-2,0,0);p.getInventory().setItem(0,new ItemStack(Items.MELON));use(p,copied);
    h.runAfterDelay(3,()->guard(h,()->{
     h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"replant")==1,"Same player's real post-harvest seed replant completes exactly one saved cycle");p.getInventory().setItem(0,ItemStack.EMPTY);h.assertTrue(p.gameMode.destroyBlock(copied.above()),"Player-placed melon is actually harvested");
     h.runAfterDelay(3,()->guard(h,()->{h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"melon")==1&&FarmerQuestService.count(p,ProfessionRank.JOURNEYMAN,"melon")==0,"Placed fruit cannot gain credit and no progress leaks into a future band");craftBread(h,p,soil.offset(-1,0,1));com.mojang.logging.LogUtils.getLogger().info("FARMER_EVENTS_ASSERTIONS_PASSED: real native seeds, accelerated actual stem-growth API, copied-fruit refusal, real feeding/birth, saved harvest-then-replant cycles, actual recipe bread crafting and band isolation");h.succeed();}));
    }));
   }));
  }));
 }
 private static void craftBread(GameTestHelper h,ServerPlayer p,BlockPos table){
  var level=p.serverLevel();level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);var menu=new net.minecraft.world.inventory.CraftingMenu(42,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,table));p.containerMenu=menu;
  for(int slot=1;slot<=3;slot++)menu.getSlot(slot).set(new ItemStack(Items.WHEAT));
  h.assertTrue(menu.getSlot(0).getItem().is(Items.BREAD),"Actual vanilla three-wheat recipe resolves through the server crafting menu");
  var crafted=menu.quickMoveStack(p,0);h.assertTrue(crafted.is(Items.BREAD)&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(2).getItem().isEmpty()&&menu.getSlot(3).getItem().isEmpty(),"Real craft consumes all three ingredients and delivers its actual output");
  h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"bread")==1&&FarmerQuestService.count(p,ProfessionRank.JOURNEYMAN,"bread")==0,"Actual ItemCraftedEvent credits one personally crafted bread in the current band only");p.closeContainer();
 }
 private static void use(ServerPlayer p,BlockPos pos){p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
