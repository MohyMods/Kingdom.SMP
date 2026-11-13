package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
/** Native use of the actual Hut table, including Visual Workbench's block conversion and retained grid. */
final class HutFurnitureRegression {
 static void use(GameTestHelper h,ServerPlayer owner,BuilderHutSavedData.Hut hut)throws Exception{
  var level=h.getLevel();var b=BuilderHutBlueprint.create();var table=hut.origin().offset(b.rotateOffset(new BlockPos(8,1,2),hut.rotation()));owner.moveTo(table.getX()-1.5,table.getY(),table.getZ()+.5);level.getChunkSource().move(owner);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  for(int i=0;i<2&&owner.containerMenu==owner.inventoryMenu;i++)owner.gameMode.useItemOn(owner,level,owner.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(table),Direction.UP,table,false));
  h.assertTrue(owner.containerMenu!=owner.inventoryMenu,"Native Hut crafting table opens a usable crafting menu");owner.containerMenu.getSlot(1).set(new ItemStack(Items.DIAMOND,3));owner.closeContainer();
  if(net.neoforged.fml.ModList.get().isLoaded("visualworkbench")){h.assertTrue(!level.getBlockState(table).is(Blocks.CRAFTING_TABLE),"Actual Visual Workbench use converts the vanilla table identity");h.assertTrue(level.getBlockEntity(table)!=null,"Converted table retains its own native inventory entity");}
  h.assertTrue(BuilderHutService.structureIntact(level,hut),"Normal native table use preserves Hut integrity: "+BuilderHutService.integrityReport(level,hut));
  var npc=(com.mohymods.kingdomsmp.npc.AldricEntity)level.getEntity(hut.npcId());owner.moveTo(npc.getX()+1,npc.getY(),npc.getZ());h.assertTrue(BuilderHutService.canInteract(owner,npc),"Original registered Aldric remains available after actual table use");
  if(net.neoforged.fml.ModList.get().isLoaded("visualworkbench")){owner.moveTo(table.getX()-1.5,table.getY(),table.getZ()+.5);owner.gameMode.useItemOn(owner,level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(table),Direction.UP,table,false));h.assertTrue(owner.containerMenu.getSlot(1).getItem().is(Items.DIAMOND)&&owner.containerMenu.getSlot(1).getItem().getCount()==3,"Integrity comparison does not replace converted table or lose retained grid items");owner.closeContainer();}
 }
}
