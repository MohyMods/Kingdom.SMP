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
    static String validate(ServerPlayer player,String id,BlockPos center){
        var level=player.serverLevel();
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
        if(id.endsWith("j05"))return gradedRoute(player,center);
        if(id.endsWith("j06"))return commissions(player);
        if(id.endsWith("j08")){
            var facade=RetainedBuildingService.inspectCompletedProject(player,"kingdom_smp:builder/j02");if(facade.count()<RetainedBuildingRules.get(player.server).goal("kingdom_smp:builder/j02"))throw new IllegalStateException("Maintain the supplied masonry/glass facade for handover");
            String maintained=validate(player,"kingdom_smp:builder/j04",center)+"|"+gradedRoute(player,center)+"|"+commissions(player);return "handover="+hash(facade.hash()+"|"+maintained);
        }
        throw new IllegalArgumentException("No validated Builder inspection geometry");
    }
    private static String gradedRoute(ServerPlayer player,BlockPos center){
        var level=player.serverLevel();int goal=BuilderInspectionRules.route(player.server);Set<BlockPos> remaining=new HashSet<>();
        var footprints=ConstructionService.store(player.server).projects().stream().filter(p->p.origin()!=null).map(ConstructionService::bounds).toList();
        for(var pos:BlockPos.betweenClosed(center.offset(-25,-2,-25),center.offset(25,2,25))){var state=level.getBlockState(pos);boolean material=state.is(Blocks.DIRT_PATH)||state.is(Blocks.COBBLESTONE)||state.is(Blocks.STONE_BRICKS)||state.is(net.minecraft.tags.BlockTags.PLANKS)||state.getBlock() instanceof StairBlock||state.getBlock() instanceof SlabBlock;
            if(material&&deck(level,pos)&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)&&footprints.stream().noneMatch(b->b.contains(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)))remaining.add(pos.immutable());}
        while(!remaining.isEmpty()){
            var queue=new ArrayDeque<BlockPos>();var first=remaining.iterator().next();queue.add(first);remaining.remove(first);var component=new HashSet<BlockPos>();
            while(!queue.isEmpty()){var pos=queue.removeFirst();component.add(pos);for(var side:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){var next=pos.relative(side).offset(0,dy,0);if(remaining.remove(next))queue.addLast(next);}}
            if(component.size()<goal)continue;int minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE,drain=0,minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
            for(var pos:component){minY=Math.min(minY,pos.getY());maxY=Math.max(maxY,pos.getY());minX=Math.min(minX,pos.getX());maxX=Math.max(maxX,pos.getX());minZ=Math.min(minZ,pos.getZ());maxZ=Math.max(maxZ,pos.getZ());for(var side:Direction.Plane.HORIZONTAL){var channel=pos.relative(side);if(!component.contains(channel)&&clear(level,channel)&&level.getBlockState(channel.below()).isFaceSturdy(level,channel.below(),Direction.UP)){drain++;break;}}}
            if(maxY>minY&&Math.max(maxX-minX,maxZ-minZ)>=12&&drain>=8)return "gradedRoute="+component.size()+",levels="+(maxY-minY+1)+",drainage="+drain;
        }
        throw new IllegalStateException("Inspect"+goal+"connected supported route cells, safe one-block grades across2heights and8shallow side-drain cells; attest consent and no unauthorized excavation");
    }
    private record Project(BuilderWorksite site,RetainedBuildingService.Snapshot proof) { }
    static boolean separate(BuilderWorksite a,BuilderWorksite b,BuilderWorksite c){return !a.id().equals(b.id())&&!a.id().equals(c.id())&&!b.id().equals(c.id())&&!a.bounds().intersects(b.bounds())&&!a.bounds().intersects(c.bounds())&&!b.bounds().intersects(c.bounds());}
    private static String commissions(ServerPlayer player){
        final String id="kingdom_smp:builder/j06";int minimum=RetainedBuildingRules.get(player.server).goal(id);var projects=new ArrayList<Project>();
        for(var site:com.mohymods.kingdomsmp.economy.CommerceService.store(player.server).builderAuthorizationHistory().stream().filter(s->s.player().equals(player.getUUID())&&s.quest().equals(id)).limit(32).toList()){
            // Each former current authorization keeps only its own native positions; no evidence is transferred.
            try{projects.add(new Project(site,RetainedBuildingService.historical(player,site)));}catch(IllegalStateException unavailable){/* Unloaded, revoked or reserved old sites cannot contribute. */}
        }
        for(var timber:projects)if(timber.proof().categories().get("timber")>=minimum)for(var stone:projects)if(stone.proof().categories().get("stone")>=minimum)for(var glass:projects)if(glass.proof().categories().get("glass")>=minimum&&separate(timber.site(),stone.site(),glass.site()))return "commissions=3,native="+hash(timber.site().id()+":"+timber.proof().hash()+"|"+stone.site().id()+":"+stone.proof().hash()+"|"+glass.site().id()+":"+glass.proof().hash());
        throw new IllegalStateException("Inspect3separate authorized j06projects with at least"+minimum+"retained native timber/stone/glass blocks respectively; all sites need current consent and loaded support");
    }
    private static String hash(String text){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}

}
