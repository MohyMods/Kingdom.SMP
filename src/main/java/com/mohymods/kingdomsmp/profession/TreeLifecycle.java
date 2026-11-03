package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.block.state.BlockState;

/** Exact successful native feature output; off-thread capture never accesses SavedData or players. */
public final class TreeLifecycle {
    private static final ConcurrentHashMap<MinecraftServer,State> STATES=new ConcurrentHashMap<>();
    private static final class State {
        final AtomicLong sequence=new AtomicLong(),epoch=new AtomicLong();final AtomicInteger queued=new AtomicInteger();
        final ConcurrentLinkedQueue<Fact> facts=new ConcurrentLinkedQueue<>();final Map<TreeEvidence.Cell,Long> mutations=new HashMap<>();
        final ArrayList<Ticket> tickets=new ArrayList<>();volatile boolean used;int auditCursor;long failureTick=Long.MIN_VALUE;
    }
    private record Ticket(BlockPos pos,Object feature,TreeEvidence.Plant plant,long tick) { }
    private record Fact(UUID id,String dimension,BlockPos origin,Map<Long,TreeEvidence.Log> logs,Map<Long,Long> writes,TreeEvidence.Plant plant,long epoch,boolean transactional,long tick,long completion,String finalState) { }
    public static final class Batch {
        final MinecraftServer server;final WorldGenLevel level;final BlockPos origin;final String dimension;final long epoch;final TreeEvidence.Plant plant;
        final Map<Long,TreeEvidence.Log> logs=new HashMap<>();final Map<Long,Long> writes=new HashMap<>();int leaves;boolean overflow;
        Batch(WorldGenLevel level,BlockPos origin,State state,TreeEvidence.Plant plant){this.server=level.getLevel().getServer();this.level=level;this.origin=origin.immutable();this.dimension=level.getLevel().dimension().location().toString();this.epoch=state.epoch.get();this.plant=plant;}
    }
    private TreeLifecycle() { }
    private static State state(MinecraftServer server){return STATES.computeIfAbsent(server,s->new State());}
    public static void stop(MinecraftServer server){STATES.remove(server);}
    public static void used(MinecraftServer server){state(server).used=true;}
    public static void failure(MinecraftServer server,RuntimeException failure){var s=state(server);synchronized(s){long tick=server.getTickCount();if(s.failureTick==Long.MIN_VALUE||tick-s.failureTick>=200){s.failureTick=tick;com.mojang.logging.LogUtils.getLogger().error("Tree evidence refused; native worldgen/item transaction continues without profession credit",failure);}}}
    public static String species(BlockState state){
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if(!id.startsWith("minecraft:"))return null;String name=id.substring(10);if(name.startsWith("stripped_"))name=name.substring(9);
        for(String suffix:List.of("_log","_wood"))if(name.endsWith(suffix)){String species=name.substring(0,name.length()-suffix.length());return TreeEvidence.SPECIES.contains(species)?species:null;}
        return null;
    }
    public static Batch begin(FeaturePlaceContext<TreeConfiguration> context){
        var level=context.level();var server=level.getLevel().getServer();
        try{
            var s=state(server);TreeEvidence.Plant plant=null;boolean natural=level instanceof WorldGenRegion;
            if(!natural){if(!server.isSameThread())return null;long tick=server.getTickCount();var feature=context.config();
                for(var iterator=s.tickets.iterator();iterator.hasNext();){var ticket=iterator.next();if(tick-ticket.tick()>1){iterator.remove();continue;}
                    if(ticket.feature()==feature&&ticket.pos().getY()==context.origin().getY()&&Math.abs(ticket.pos().getX()-context.origin().getX())<=1&&Math.abs(ticket.pos().getZ()-context.origin().getZ())<=1){plant=ticket.plant();iterator.remove();break;}
                }
                if(plant==null)return null;
            }
            return new Batch(level,context.origin(),s,plant);
        }catch(RuntimeException failed){failure(server,failed);return null;}
    }
    public static void trunk(Batch b,BlockPos pos,BlockState block){
        if(b==null||b.overflow)return;
        try{String species=species(block);if(species==null)return;if(b.logs.size()>=2048&&!b.logs.containsKey(pos.asLong())){b.overflow=true;return;}
            if(!b.level.getBlockState(pos).equals(block))return;b.logs.put(pos.asLong(),new TreeEvidence.Log(block.toString(),species));b.writes.put(pos.asLong(),state(b.server).sequence.incrementAndGet());
        }catch(RuntimeException failed){b.overflow=true;failure(b.server,failed);}
    }
    public static void leaf(Batch b,BlockState block){if(b==null)return;try{if(block.is(BlockTags.LEAVES))b.leaves=Math.min(4096,b.leaves+1);}catch(RuntimeException failed){b.overflow=true;failure(b.server,failed);}}
    public static java.util.function.BiConsumer<BlockPos,BlockState> trunkRecorder(Batch batch,java.util.function.BiConsumer<BlockPos,BlockState> original){return (pos,state)->{original.accept(pos,state);trunk(batch,pos,state);};}
    public static net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageSetter foliageRecorder(Batch batch,net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageSetter original){
        return new net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageSetter(){public void set(BlockPos pos,BlockState state){original.set(pos,state);leaf(batch,state);}public boolean isSet(BlockPos pos){return original.isSet(pos);}};
    }
    public static void finish(Batch b,boolean success){
        if(b==null||!success||b.overflow||b.leaves==0||b.logs.isEmpty())return;
        try{var s=state(b.server);if(b.epoch!=s.epoch.get())return;
            var fact=new Fact(UUID.randomUUID(),b.dimension,b.origin,Map.copyOf(b.logs),Map.copyOf(b.writes),b.plant,b.epoch,b.server.isSameThread()&&b.level.getLevel().captureBlockSnapshots,b.server.getTickCount(),b.plant==null?b.origin.asLong():b.plant.position(),b.level.getBlockState(b.plant==null?b.origin:BlockPos.of(b.plant.position())).toString());
            if(b.server.isSameThread()&&!fact.transactional()){apply(b.server,s,fact);return;}
            int cells=b.logs.size();if(s.queued.addAndGet(cells)>65536){s.queued.addAndGet(-cells);return;}s.facts.add(fact);
        }catch(RuntimeException failed){failure(b.server,failed);}
    }
    public static void beforeGrowth(ServerLevel level,BlockPos pos,Object feature){
        try{var server=level.getServer();var plant=TreeSavedData.get(server).evidence().plantAt(level.dimension().location().toString(),pos).orElse(null);
            if(plant==null||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals(plant.sapling()))return;
            var s=state(server);s.tickets.removeIf(t->server.getTickCount()-t.tick()>1);if(s.tickets.size()<8192)s.tickets.add(new Ticket(pos.immutable(),feature,plant,server.getTickCount()));
        }catch(RuntimeException failed){failure(level.getServer(),failed);}
    }
    /** Rotation invalidates every old outstanding batch instead of evicting a placement exclusion into countable state. */
    public static void mutation(ServerLevel level,BlockPos pos){
        var s=state(level.getServer());var key=new TreeEvidence.Cell(level.dimension().location().toString(),pos.asLong());
        synchronized(s.mutations){if(s.mutations.size()>=65536&&!s.mutations.containsKey(key)){s.epoch.incrementAndGet();s.mutations.clear();}s.mutations.put(key,s.sequence.incrementAndGet());}
    }
    public static boolean featureOutput(ServerLevel level,BlockPos pos,BlockState state){
        for(var fact:state(level.getServer()).facts)if(fact.transactional()&&fact.dimension().equals(level.dimension().location().toString())){
            var log=fact.logs().get(pos.asLong());if(log!=null&&log.state().equals(state.toString())||fact.completion()==pos.asLong()&&fact.finalState().equals(state.toString()))return true;
        }return false;
    }
    public static void drain(MinecraftServer server,int limit){
        var s=state(server);int available=Math.min(limit,s.facts.size());
        for(int n=0;n<available;n++){
            var fact=s.facts.poll();if(fact==null)break;
            if(fact.transactional()&&server.getTickCount()<=fact.tick()){s.facts.add(fact);continue;}
            s.queued.addAndGet(-fact.logs().size());try{apply(server,s,fact);}catch(RuntimeException failed){failure(server,failed);}
        }
        if(s.used&&server.getTickCount()%20==0)auditGrowth(server);
    }
    private static void apply(MinecraftServer server,State s,Fact fact){
        if(!server.isSameThread())throw new IllegalStateException("Tree facts apply on the server thread only");if(fact.epoch()!=s.epoch.get())return;
        Map<Long,TreeEvidence.Log> accepted=new HashMap<>();synchronized(s.mutations){for(var e:fact.logs().entrySet())if(s.mutations.getOrDefault(new TreeEvidence.Cell(fact.dimension(),e.getKey()),0L)<=fact.writes().get(e.getKey()))accepted.put(e.getKey(),e.getValue());}
        if(accepted.isEmpty())return;
        if(fact.transactional()){
            var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(fact.dimension())));
            if(level==null||accepted.size()!=fact.logs().size()||!level.hasChunkAt(BlockPos.of(fact.completion()))||!level.getBlockState(BlockPos.of(fact.completion())).toString().equals(fact.finalState()))return;
            for(var entry:accepted.entrySet()){var pos=BlockPos.of(entry.getKey());if(!level.hasChunkAt(pos)||!level.getBlockState(pos).toString().equals(entry.getValue().state()))return;}
        }
        var data=TreeSavedData.get(server);s.used=true;boolean credit=false;
        if(fact.plant()!=null&&fact.plant().band()!=null){var store=CommerceService.initializedStore(server).orElse(null);if(store!=null&&!store.quarantined()){var roster=store.professions(fact.plant().player());credit=roster.isActive(Profession.LUMBERJACK)&&roster.progressBand(Profession.LUMBERJACK)==fact.plant().band();}}
        if(data.evidence().register(fact.id(),fact.dimension(),fact.origin(),accepted,fact.plant(),credit)){
            data.setDirty();if(fact.plant()!=null){var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(fact.dimension())));
                if(level!=null)for(int x=0;x<2;x++)for(int z=0;z<2;z++){var pos=fact.origin().offset(x,0,z);if(level.hasChunkAt(pos)&&!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.SaplingBlock))data.evidence().discardPlant(fact.dimension(),pos);}
            }
        }
    }
    /** Native forestry never earns progress inside a kingdom structure/reserved foundation. */
    public static boolean consent(ServerLevel level,ServerPlayer player,BlockPos pos){
        if(PlacementProtection.check(level,player,pos,pos)!=null)return false;
        var cell=new net.minecraft.world.phys.AABB(pos);
        for(var project:com.mohymods.kingdomsmp.building.ConstructionService.store(level.getServer()).projects())if(project.origin()!=null&&cell.intersects(com.mohymods.kingdomsmp.building.ConstructionService.bounds(project)))return false;
        var hut=com.mohymods.kingdomsmp.persistence.BuilderHutSavedData.get(level.getServer()).hut().orElse(null);
        if(hut!=null){var blueprint=com.mohymods.kingdomsmp.building.BuilderHutBlueprint.create();var origin=hut.origin();if(cell.intersects(new net.minecraft.world.phys.AABB(origin.getX(),origin.getY()-4,origin.getZ(),origin.getX()+blueprint.width(hut.rotation()),origin.getY()+com.mohymods.kingdomsmp.building.BuilderHutBlueprint.HEIGHT,origin.getZ()+blueprint.depth(hut.rotation()))))return false;}
        return true;
    }
    public static void auditGrowth(MinecraftServer server){
        try{var store=CommerceService.initializedStore(server).orElse(null);if(store==null||store.quarantined())return;var data=TreeSavedData.get(server);int checked=0;
            var receipts=data.evidence().growth();if(receipts.isEmpty())return;var state=state(server);int start=Math.floorMod(state.auditCursor,receipts.size());
            for(int n=0;n<Math.min(128,receipts.size());n++){
                var receipt=receipts.get((start+n)%receipts.size());state.auditCursor=(start+n+1)%receipts.size();var p=server.getPlayerList().getPlayer(receipt.player());if(p==null||!TreeQuestService.real(p)||!store.professions(p.getUUID()).isActive(Profession.LUMBERJACK)||!p.serverLevel().dimension().location().toString().equals(receipt.dimension()))continue;
                var pos=BlockPos.of(receipt.position());if(!p.serverLevel().hasChunkAt(pos)||!consent(p.serverLevel(),p,pos))continue;
                if(data.evidence().confirmGrowth(receipt.id(),p.getUUID()))data.setDirty();
            }
        }catch(RuntimeException failed){failure(server,failed);}
    }
}
