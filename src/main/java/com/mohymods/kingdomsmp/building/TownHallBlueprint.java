package com.mohymods.kingdomsmp.building;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** Original 19x15 two-storey civic hall, with usable stairs, meeting room and steward gallery. */
public final class TownHallBlueprint {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("kingdom_smp","royal_town_hall");
    private TownHallBlueprint(){}
    public static ConstructionBlueprint create(){
        Map<BlockPos,BlockState> b=new LinkedHashMap<>();
        for(int y=0;y<=10;y++)for(int x=0;x<19;x++)for(int z=0;z<15;z++){
            BlockState s=null;
            if(y==0)s=Blocks.STONE_BRICKS.defaultBlockState();
            else if(y==5)s=Blocks.SPRUCE_PLANKS.defaultBlockState();
            else if(x==0||x==18||z==0||z==14){
                s=y==1?Blocks.STONE_BRICKS.defaultBlockState():Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
                if(((x==0||x==18?z:x)%4!=0)&&(y==2||y==3||y==7||y==8))s=Blocks.GLASS.defaultBlockState();
                else if(x!=0&&x!=18&&z!=0&&z!=14)s=Blocks.BIRCH_PLANKS.defaultBlockState();
            }
            if(s!=null)put(b,x,y,z,s);
        }
        // Gabled roof with closed ends. Upper gallery has a continuous ceiling.
        for(int x=0;x<19;x++)for(int z=0;z<15;z++)put(b,x,11,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int step=0;step<=7;step++)for(int x=0;x<19;x++){
            put(b,x,11+step,step,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
            put(b,x,11+step,14-step,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
            if(x==0||x==18)for(int z=step+1;z<14-step;z++)put(b,x,11+step,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        // Stairs run west along the rear side; clear headroom through the upper floor.
        for(int i=0;i<5;i++){
            for(int y=1+i;y<=Math.min(10,4+i);y++)put(b,15-i,y,11,Blocks.AIR.defaultBlockState());
            put(b,15-i,1+i,11,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));
        }
        for(int x=9;x<=10;x++)put(b,x,5,11,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int x=5;x<=12;x++){put(b,x,1,6,Blocks.SPRUCE_FENCE.defaultBlockState());put(b,x,2,6,Blocks.SPRUCE_SLAB.defaultBlockState());}
        for(int x:new int[]{5,7,9,11})for(int z:new int[]{4,8})put(b,x,1,z,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,z==4?Direction.NORTH:Direction.SOUTH));
        for(int floor:new int[]{1,6}){
            for(int x:new int[]{3,9,15})for(int z:new int[]{3,9})put(b,x,floor==1?4:10,z,Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
            for(int x=2;x<=5;x++)put(b,x,floor,13,Blocks.BOOKSHELF.defaultBlockState());
            put(b,2,floor,10,Blocks.BARREL.defaultBlockState());put(b,2,floor,11,Blocks.BARREL.defaultBlockState());
        }
        put(b,14,1,7,Blocks.CARTOGRAPHY_TABLE.defaultBlockState());put(b,14,2,7,Blocks.POTTED_FERN.defaultBlockState());
        for(int x:new int[]{5,9,13})put(b,x,1,2,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        put(b,14,6,7,Blocks.CARTOGRAPHY_TABLE.defaultBlockState());put(b,14,7,7,Blocks.POTTED_FERN.defaultBlockState());
        for(int x=6;x<=12;x++)put(b,x,6,6,Blocks.SPRUCE_SLAB.defaultBlockState());
        for(int z=0;z<=3;z++)put(b,9,1,z,Blocks.RED_CARPET.defaultBlockState());
        var door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
        put(b,9,1,0,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));put(b,9,2,0,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
        // Ensure the permanent Steward has a clear verified home and walking space.
        put(b,15,1,8,Blocks.AIR.defaultBlockState());put(b,15,2,8,Blocks.AIR.defaultBlockState());
        var blocks=b.entrySet().stream().filter(e->!e.getValue().isAir()).map(e->new BuilderHutBlueprint.Placement(e.getKey(),e.getValue())).toList();
        return new ConstructionBlueprint(ID,1,19,15,19,blocks,new BlockPos(15,1,8),new BlockPos(15,1,8),new BlockPos(5,1,2));
    }
    private static void put(Map<BlockPos,BlockState>b,int x,int y,int z,BlockState state){b.put(new BlockPos(x,y,z),state);}
}
