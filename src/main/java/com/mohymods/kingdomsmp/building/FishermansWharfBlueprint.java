package com.mohymods.kingdomsmp.building;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** Original stone quay, covered landing, provision barrels and a furnished Fishmonger's counting house. */
public final class FishermansWharfBlueprint {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("kingdom_smp","fishermans_wharf");
    private FishermansWharfBlueprint() { }
    public static ConstructionBlueprint create() {
        Map<BlockPos,BlockState> at=new LinkedHashMap<>();
        for(int x=0;x<17;x++)for(int z=0;z<19;z++)put(at,x,0,z,(x==0||x==16||z==0||z==18?Blocks.STONE_BRICKS:Blocks.SPRUCE_PLANKS).defaultBlockState());
        // Quay bollards/railings leave a broad north approach and open fishing frontage.
        for(int x=0;x<17;x++)if(x<7||x>9){put(at,x,1,0,Blocks.SPRUCE_FENCE.defaultBlockState());put(at,x,1,18,Blocks.SPRUCE_FENCE.defaultBlockState());}
        for(int z=1;z<18;z++){put(at,0,1,z,Blocks.SPRUCE_FENCE.defaultBlockState());put(at,16,1,z,Blocks.SPRUCE_FENCE.defaultBlockState());}
        for(int x:new int[]{1,15})for(int z:new int[]{1,7,17}){put(at,x,1,z,Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());put(at,x,2,z,Blocks.LANTERN.defaultBlockState());}
        // Covered outdoor landing, without placing water, filled storage or modded loot blocks.
        for(int x:new int[]{3,13})for(int z:new int[]{2,6})for(int y=1;y<=3;y++)put(at,x,y,z,Blocks.SPRUCE_FENCE.defaultBlockState());
        for(int x=2;x<=14;x++)for(int z=1;z<=7;z++)put(at,x,4,z,((x+z)%2==0?Blocks.SPRUCE_SLAB:Blocks.DARK_OAK_SLAB).defaultBlockState());
        for(int x:new int[]{4,12}){put(at,x,1,3,Blocks.BARREL.defaultBlockState());put(at,x,1,5,Blocks.BARREL.defaultBlockState());}
        for(int z=3;z<=5;z++)put(at,2,1,z,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        // Rear counting house: plaster, timber, windows and steep slate roof.
        for(int x=3;x<=13;x++)for(int z=9;z<=17;z++)for(int y=1;y<=4;y++)if(x==3||x==13||z==9||z==17){
            boolean frame=(x==3||x==13)&&(z==9||z==13||z==17)||(z==9||z==17)&&(x==3||x==8||x==13);
            BlockState state=(frame?Blocks.STRIPPED_SPRUCE_LOG:y==1?Blocks.STONE_BRICKS:Blocks.WHITE_TERRACOTTA).defaultBlockState();
            if(!frame&&(y==2||y==3)&&(x==3||x==13?z==11||z==15:x==5||x==11))state=Blocks.GLASS.defaultBlockState();put(at,x,y,z,state);
        }
        var door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
        put(at,8,1,9,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));put(at,8,2,9,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
        for(int x=3;x<=13;x++)for(int z=9;z<=17;z++)put(at,x,5,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int rise=0;rise<=5;rise++)for(int z=8;z<=18;z++){
            put(at,3+rise,5+rise,z,Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
            put(at,13-rise,5+rise,z,Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));
            if(z==9||z==17)for(int x=4+rise;x<13-rise;x++)put(at,x,5+rise,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        // Clear central aisle and resident home; every decoration has physical support.
        put(at,6,1,12,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        for(int x:new int[]{4,5,11,12})put(at,x,1,16,Blocks.BARREL.defaultBlockState());
        put(at,4,1,11,Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        put(at,12,1,11,Blocks.CRAFTING_TABLE.defaultBlockState());put(at,12,2,11,Blocks.POTTED_FERN.defaultBlockState());
        put(at,7,1,14,Blocks.CARTOGRAPHY_TABLE.defaultBlockState());put(at,9,1,14,Blocks.BARREL.defaultBlockState());
        for(int x:new int[]{5,11}){put(at,x,4,12,Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));put(at,x,4,15,Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));}
        put(at,7,4,9,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());put(at,9,4,9,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        return new ConstructionBlueprint(ID,1,17,19,11,at.entrySet().stream().map(e->new BuilderHutBlueprint.Placement(e.getKey(),e.getValue())).toList(),new BlockPos(8,1,15),new BlockPos(8,1,15),new BlockPos(6,1,12));
    }
    private static void put(Map<BlockPos,BlockState> at,int x,int y,int z,BlockState state){at.put(new BlockPos(x,y,z),state);}
}
