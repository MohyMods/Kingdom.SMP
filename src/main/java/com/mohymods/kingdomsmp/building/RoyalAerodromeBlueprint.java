package com.mohymods.kingdomsmp.building;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** Original broad landing apron, open-front timber hangar and furnished Flight Engineer office. */
public final class RoyalAerodromeBlueprint {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath("kingdom_smp","royal_aerodrome");
    private RoyalAerodromeBlueprint() { }
    public static ConstructionBlueprint create(){
        Map<BlockPos,BlockState> at=new LinkedHashMap<>();
        for(int x=0;x<27;x++)for(int z=0;z<31;z++)put(at,x,0,z,(z<12?((x==0||x==26||z==0)?Blocks.STONE_BRICKS:Blocks.SMOOTH_STONE):Blocks.SPRUCE_PLANKS).defaultBlockState());
        // Apron markings and approach lights remain flat and leave an eleven-block entrance.
        for(int z=1;z<12;z+=3)for(int x=12;x<=14;x++)put(at,x,0,z,Blocks.YELLOW_TERRACOTTA.defaultBlockState());
        for(int x:new int[]{1,25})for(int z:new int[]{2,7,11}){put(at,x,1,z,Blocks.STONE_BRICK_WALL.defaultBlockState());put(at,x,2,z,Blocks.LANTERN.defaultBlockState());}
        for(int x=2;x<=24;x++)for(int z=12;z<=29;z++)for(int y=1;y<=6;y++)if(x==2||x==24||z==12||z==29){
            if(z==12&&x>=8&&x<=18)continue;
            boolean frame=(x==2||x==24)&&(z==12||z==18||z==24||z==29)||(z==12||z==29)&&(x==2||x==7||x==19||x==24);
            var block=frame?Blocks.STRIPPED_SPRUCE_LOG:y==1?Blocks.STONE_BRICKS:Blocks.WHITE_TERRACOTTA;
            if(!frame&&(y==2||y==3)&&(x==2||x==24)&&(z==15||z==21||z==27))block=Blocks.GLASS;
            put(at,x,y,z,block.defaultBlockState());
        }
        // Full sloping slate roof, supporting purlins and solid end gables.
        for(int rise=0;rise<=6;rise++)for(int z=11;z<=30;z++){
            int left=1+rise*2,right=25-rise*2;
            for(int x=left;x<=Math.min(left+1,13);x++)put(at,x,7+rise,z,Blocks.DEEPSLATE_TILES.defaultBlockState());
            for(int x=Math.max(right-1,14);x<=right;x++)put(at,x,7+rise,z,Blocks.DEEPSLATE_TILES.defaultBlockState());
            if(z==12||z==29)for(int x=left+2;x<right-1;x++)put(at,x,7+rise,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        for(int z:new int[]{12,18,24,29})for(int x=2;x<=24;x++)put(at,x,6,z,Blocks.SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X));
        // Office partition: accessible doorway; no loot or supplied aircraft hidden in containers.
        for(int z=19;z<=28;z++)for(int y=1;y<=4;y++)if(z!=23||y>2)put(at,20,y,z,(y==1?Blocks.SPRUCE_PLANKS:Blocks.GLASS).defaultBlockState());
        for(int x=21;x<=23;x++)put(at,x,1,28,Blocks.BARREL.defaultBlockState());
        put(at,21,1,25,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        put(at,23,1,20,Blocks.CARTOGRAPHY_TABLE.defaultBlockState());put(at,23,1,21,Blocks.SMITHING_TABLE.defaultBlockState());
        put(at,21,1,20,Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        for(int x:new int[]{5,13,21})for(int z:new int[]{18,24})put(at,x,5,z,Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
        put(at,23,2,28,Blocks.POTTED_FERN.defaultBlockState());
        return new ConstructionBlueprint(ID,1,27,31,14,at.entrySet().stream().map(e->new BuilderHutBlueprint.Placement(e.getKey(),e.getValue())).toList(),new BlockPos(22,1,23),new BlockPos(22,1,23),new BlockPos(21,1,25));
    }
    private static void put(Map<BlockPos,BlockState> at,int x,int y,int z,BlockState state){at.put(new BlockPos(x,y,z),state);}
}
