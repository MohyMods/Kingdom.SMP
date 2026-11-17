package com.mohymods.kingdomsmp.building;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;
/** Original public contract hall: great-room benches, notice lecterns, briefing map and accessible supply room. */
public final class RoyalBountyHallBlueprint {
 public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("kingdom_smp","royal_bounty_hall");
 private RoyalBountyHallBlueprint(){ }
 public static ConstructionBlueprint create(){
  Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
  for(int y=0;y<=4;y++)for(int x=0;x<17;x++)for(int z=0;z<15;z++){
   BlockState state=null;
   if(y==0)state=Blocks.STONE_BRICKS.defaultBlockState();
   else if(x==0||x==16||z==0||z==14){boolean frame=(x==0||x==16)&&(z%7==0)||(z==0||z==14)&&(x%4==0);state=frame?Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState():y==1?Blocks.STONE_BRICKS.defaultBlockState():Blocks.POLISHED_ANDESITE.defaultBlockState();if(!frame&&(y==2||y==3)&&(x==0||x==16?z%3==0:x%4==2))state=Blocks.GLASS.defaultBlockState();}
   if(state!=null)put(blocks,x,y,z,state);
  }
  for(int x=0;x<17;x++)for(int z=0;z<15;z++)put(blocks,x,5,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
  for(int rise=0;rise<=8;rise++)for(int z=0;z<15;z++){
   put(blocks,rise,5+rise,z,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
   put(blocks,16-rise,5+rise,z,Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));
   if(z==0||z==14)for(int x=rise+1;x<16-rise;x++)put(blocks,x,5+rise,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
  }
  // Clear entrance and a wide central aisle through the entire public room.
  var door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
  put(blocks,8,1,0,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));put(blocks,8,2,0,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
  for(int z=1;z<=12;z++)for(int x=7;x<=9;x++)put(blocks,x,1,z,Blocks.RED_CARPET.defaultBlockState());
  for(int z:new int[]{4,7,10}){
   for(int x:new int[]{3,4,5,11,12,13})put(blocks,x,1,z,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,x<8?Direction.EAST:Direction.WEST));
   for(int x:new int[]{3,5,11,13})put(blocks,x,4,z,Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
  }
  for(int z:new int[]{3,6,9}){put(blocks,1,1,z,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));put(blocks,15,1,z,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));}
  // Briefing and dispatch area. All storage is ordinary furnishing, never virtual escrow.
  put(blocks,5,1,12,Blocks.CARTOGRAPHY_TABLE.defaultBlockState());put(blocks,5,2,12,Blocks.POTTED_FERN.defaultBlockState());
  put(blocks,3,1,12,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
  for(int x=11;x<=14;x++){put(blocks,x,1,13,Blocks.BARREL.defaultBlockState());put(blocks,x,2,13,Blocks.BOOKSHELF.defaultBlockState());}
  put(blocks,13,1,11,Blocks.CRAFTING_TABLE.defaultBlockState());put(blocks,14,1,11,Blocks.SMITHING_TABLE.defaultBlockState());
  put(blocks,8,4,0,Blocks.GOLD_BLOCK.defaultBlockState());put(blocks,7,4,0,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());put(blocks,9,4,0,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
  var placements=blocks.entrySet().stream().map(e->new BuilderHutBlueprint.Placement(e.getKey(),e.getValue())).toList();return new ConstructionBlueprint(ID,1,17,15,14,placements,new BlockPos(8,1,13),new BlockPos(8,1,13),new BlockPos(3,1,12));
 }
 private static void put(Map<BlockPos,BlockState> blocks,int x,int y,int z,BlockState state){blocks.put(new BlockPos(x,y,z),state);}
}
