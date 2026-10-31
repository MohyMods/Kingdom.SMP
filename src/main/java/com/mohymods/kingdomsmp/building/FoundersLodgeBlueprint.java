package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** Original timber meeting lodge, generous porch, communal table and Reeve's study. */
public final class FoundersLodgeBlueprint {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("kingdom_smp", "founders_lodge");
    private FoundersLodgeBlueprint() { }
    public static ConstructionBlueprint create() {
        Map<BlockPos, BlockState> b = new LinkedHashMap<>();
        for(int x=0;x<17;x++) for(int z=0;z<13;z++) {
            var floor=x>1&&x<15&&z>2&&z<10?Blocks.SPRUCE_PLANKS.defaultBlockState():
                    ((x*31+z*17)%9==0?Blocks.ANDESITE:Blocks.STONE_BRICKS).defaultBlockState();
            put(b,x,0,z,floor);
        }
        put(b,8,0,0,stairs(Direction.SOUTH));
        for(int y=1;y<=4;y++) for(int x=1;x<=15;x++) for(int z=2;z<=10;z++) {
            if(x!=1&&x!=15&&z!=2&&z!=10)continue;
            if(x==8&&z==2&&y<=2)continue;
            boolean pillar=(x==1||x==15)&&(z==2||z==6||z==10)|| (z==2||z==10)&&(x==4||x==12);
            boolean window=y>=2&&y<=3&&((z==2||z==10)&&(x==3||x==6||x==10||x==13)
                    ||(x==1||x==15)&&(z==4||z==8));
            put(b,x,y,z,pillar?log(Direction.Axis.Y):window?Blocks.GLASS.defaultBlockState():
                    y==1?Blocks.STONE_BRICKS.defaultBlockState():Blocks.BIRCH_PLANKS.defaultBlockState());
        }
        for(int x=1;x<=15;x++) for(int z:new int[]{2,4,8,10}) put(b,x,5,z,log(Direction.Axis.X));
        for(int z=3;z<10;z++) for(int x:new int[]{1,15}) put(b,x,5,z,log(Direction.Axis.Z));
        for(int z=2;z<=10;z++)for(int y=6;y<5+Math.min(z,12-z);y++)for(int x:new int[]{1,15})
            put(b,x,y,z,Blocks.SPRUCE_PLANKS.defaultBlockState());
        for(int z=0;z<13;z++)for(int x=0;x<17;x++)put(b,x,5+Math.min(z,12-z),z,
                z==6?Blocks.SPRUCE_SLAB.defaultBlockState():stairs(z<6?Direction.SOUTH:Direction.NORTH));
        for(int x:new int[]{4,12})for(int y=1;y<5;y++)put(b,x,y,0,log(Direction.Axis.Y));
        for(int x=4;x<=12;x++)put(b,x,5,0,log(Direction.Axis.X));

        // Dependency-sensitive furniture follows the supporting shell and roof.
        for(int x=5;x<=11;x++) {
            put(b,x,1,6,x==5||x==11?Blocks.SPRUCE_FENCE.defaultBlockState():Blocks.AIR.defaultBlockState());
            put(b,x,2,6,Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        for(int x:new int[]{5,7,9,11}) {
            put(b,x,1,4,stairs(Direction.NORTH)); put(b,x,1,8,stairs(Direction.SOUTH));
        }
        for(int z:new int[]{8,9})put(b,14,1,z,Blocks.BOOKSHELF.defaultBlockState());
        put(b,13,1,9,Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        put(b,13,2,9,Blocks.POTTED_FERN.defaultBlockState());
        put(b,12,1,9,stairs(Direction.EAST));
        for(int z:new int[]{9,10})put(b,2,1,z,Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.EAST));
        put(b,2,1,7,Blocks.CRAFTING_TABLE.defaultBlockState());
        put(b,11,1,3,Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        BlockState bed=Blocks.BLUE_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH);
        put(b,2,1,4,bed.setValue(BlockStateProperties.BED_PART,BedPart.FOOT));
        put(b,2,1,5,bed.setValue(BlockStateProperties.BED_PART,BedPart.HEAD));
        BlockState door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
        put(b,8,1,2,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));
        put(b,8,2,2,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));
        for(int x:new int[]{4,8,12})for(int z:new int[]{4,8})put(b,x,4,z,
                Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
        put(b,8,4,0,Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING,true));
        for(int z=1;z<=3;z++)put(b,8,1,z,Blocks.RED_CARPET.defaultBlockState());
        // Keep the actual door clear: carpets do not replace its lower half.
        put(b,8,1,2,door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));
        List<BuilderHutBlueprint.Placement> blocks=b.entrySet().stream().filter(e->!e.getValue().isAir())
                .map(e->new BuilderHutBlueprint.Placement(e.getKey(),e.getValue())).toList();
        return new ConstructionBlueprint(ID,1,17,13,12,blocks,new BlockPos(12,1,8),new BlockPos(12,1,8),new BlockPos(11,1,3));
    }
    private static void put(Map<BlockPos,BlockState>b,int x,int y,int z,BlockState s){b.put(new BlockPos(x,y,z),s);}
    private static BlockState log(Direction.Axis a){return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,a);}
    private static BlockState stairs(Direction d){return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,d);}
}
