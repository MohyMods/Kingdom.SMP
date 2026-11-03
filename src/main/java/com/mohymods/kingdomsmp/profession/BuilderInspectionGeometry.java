package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.building.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Actual loaded-world functional geometry for approved vanilla Builder inspections. Never edits blocks. */
final class BuilderInspectionGeometry {
    private BuilderInspectionGeometry() { }
    private static boolean clear(ServerLevel level,BlockPos pos){return level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty();}
    private static boolean deck(ServerLevel level,BlockPos pos){var state=level.getBlockState(pos);return (state.isCollisionShapeFullBlock(level,pos)||state.is(Blocks.DIRT_PATH)||state.getBlock() instanceof SlabBlock||state.getBlock() instanceof StairBlock)&&clear(level,pos.above());}
    static boolean twelveSpan(boolean[] decks,boolean[] grounded,boolean[] gap){
        if(decks.length!=14||grounded.length!=14||gap.length!=14)throw new IllegalArgumentException("Fourteen inspected crossing cells required");
        for(boolean cell:decks)if(!cell)return false;if(!grounded[0]||!grounded[13])return false;for(int n=1;n<13;n++)if(!gap[n])return false;return true;
    }
    private static boolean bridge(ServerLevel level,BlockPos center){
        for(int y=-2;y<=2;y++)for(int cross=-6;cross<=6;cross++)for(var axis:List.of(Direction.EAST,Direction.SOUTH)){
            var start=center.offset(axis==Direction.EAST?-7:cross,y,axis==Direction.SOUTH?-7:cross);boolean[] decks=new boolean[14],ground=new boolean[14],gap=new boolean[14];
            for(int n=0;n<14;n++){var pos=start.relative(axis,n);decks[n]=deck(level,pos);ground[n]=level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);gap[n]=clear(level,pos.below(2));}
            if(twelveSpan(decks,ground,gap))return true;
        }return false;
    }
    private static String station(BlockState state){
        if(state.is(Blocks.CRAFTING_TABLE)||state.is(Blocks.STONECUTTER))return "construction";
        if(state.getBlock() instanceof AbstractFurnaceBlock)return "cooking";
        if(state.getBlock() instanceof ChestBlock||state.is(Blocks.BARREL))return "storage";
        if(state.is(Blocks.LECTERN))return "study";
        if(state.getBlock() instanceof BedBlock&&state.getValue(BedBlock.PART)==net.minecraft.world.level.block.state.properties.BedPart.FOOT)return "rest";
        if(state.is(Blocks.LOOM)||state.is(Blocks.CARTOGRAPHY_TABLE)||state.is(Blocks.GRINDSTONE))return "artisan";return null;
    }
    private static boolean access(ServerLevel level,BlockPos pos){
        for(var side:Direction.Plane.HORIZONTAL){var step=pos.relative(side);if(clear(level,step)&&level.getBlockState(step.below()).isFaceSturdy(level,step.below(),Direction.UP))return true;}return false;
    }
    private static boolean roof(ServerLevel level,BlockPos pos){for(int y=2;y<=4;y++){var above=pos.above(y);var state=level.getBlockState(above);if(state.isCollisionShapeFullBlock(level,above)||state.getBlock() instanceof SlabBlock||state.getBlock() instanceof StairBlock)return true;}return false;}
    static boolean zones(List<List<BlockPos>> groups,int index,List<BlockPos> selected,int[] budget){
        if(index==groups.size())return true;
        for(var pos:groups.get(index)){if(--budget[0]<0)return false;if(selected.stream().anyMatch(old->Math.abs(old.getX()-pos.getX())<2&&Math.abs(old.getZ()-pos.getZ())<2))continue;selected.add(pos);if(zones(groups,index+1,selected,budget))return true;selected.removeLast();}return false;
    }
    static String validate(ServerLevel level,String id,BlockPos center){
        if(id.endsWith("j01")){if(!bridge(level,center))throw new IllegalStateException("Inspect a clear crossing with12open span cells,14connected deck cells and grounded approaches at both ends");return "bridgeSpan=12,approaches=2";}
        if(id.endsWith("j03")){
            Set<BlockPos> path=new HashSet<>();var footprints=ConstructionService.store(level.getServer()).projects().stream().filter(p->p.origin()!=null).map(ConstructionService::bounds).toList();
            for(var pos:BlockPos.betweenClosed(center.offset(-8,-2,-8),center.offset(8,2,8)))if(deck(level,pos)&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))path.add(pos.immutable());
            int connected=RetainedBuildingService.connectedRoute(level,path,pos->footprints.stream().noneMatch(b->b.contains(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)));if(connected<8)throw new IllegalStateException("Inspect8supported clear exterior approach cells connected to an actual intact kingdom service doorway; attest the player's access improvement");return "publicAccess="+connected;
        }
        if(id.endsWith("j04")){
            Map<String,List<BlockPos>> stations=new HashMap<>();int doors=0;
            for(var pos:BlockPos.betweenClosed(center.offset(-8,-2,-8),center.offset(8,2,8))){var state=level.getBlockState(pos);if(state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER&&access(level,pos))doors++;
                String type=station(state);if(type!=null&&access(level,pos)&&roof(level,pos)){var list=stations.computeIfAbsent(type,k->new ArrayList<>());if(list.size()<16)list.add(pos.immutable());}}
            var groups=stations.values().stream().sorted(Comparator.comparingInt(List::size)).toList();if(doors<1||groups.size()<6||!zones(groups,0,new ArrayList<>(),new int[]{4096}))throw new IllegalStateException("Provide an accessible roofed room with an entrance and6spaced usable zones: construction, cooking, storage, study, rest and artisan work; no new service unlock");return "interiorZones=6,roof=present,entry=usable";
        }
        throw new IllegalArgumentException("No validated Builder inspection geometry");
    }
}
