package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Deferred successful native placements/tool transforms only; no world writes, nearby-machine credit or chunk loads. */
public final class RetainedBuildingEvents {
    private static final Map<MinecraftServer,Long> FAILURES=new WeakHashMap<>();
    private static final Map<MinecraftServer,ArrayDeque<Runnable>> PENDING=new WeakHashMap<>();
    private RetainedBuildingEvents() { }
    public static void register(IEventBus bus){
        bus.addListener(EventPriority.LOWEST,false,BlockEvent.EntityPlaceEvent.class,RetainedBuildingEvents::place);
        bus.addListener(EventPriority.LOWEST,false,BlockEvent.BlockToolModificationEvent.class,RetainedBuildingEvents::path);
        bus.addListener(EventPriority.LOWEST,false,BlockDropsEvent.class,RetainedBuildingEvents::removed);
        bus.addListener(RetainedBuildingEvents::tick);
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->{PENDING.remove(e.getServer());FAILURES.remove(e.getServer());RetainedBuildingService.initialize(e.getServer());});
    }
    public static void initialize(MinecraftServer server){PENDING.remove(server);RetainedBuildingService.initialize(server);}
    private static List<BuilderWorksite> sites(ServerLevel level,BlockPos pos){
        var store=CommerceService.initializedStore(level.getServer()).orElse(null);if(store==null||store.quarantined())return List.of();
        return store.builderWorksites().stream().filter(s->s.dimension().equals(level.dimension().location().toString())&&s.contains(pos)).toList();
    }
    private static void invalidate(ServerLevel level,BlockPos pos){
        var sites=sites(level,pos);if(sites.isEmpty())return;var data=RetainedBuildingSavedData.get(level.getServer());
        for(var site:sites)if(data.evidence().invalidate(site.id(),pos)){data.setDirty();RetainedBuildingService.changed(site.player());}
    }
    private static void later(MinecraftServer server,Runnable action){var q=PENDING.computeIfAbsent(server,s->new ArrayDeque<>());if(q.size()<8192)q.addLast(action);}
    private static void failure(MinecraftServer server,RuntimeException error){
        long tick=server.getTickCount();var old=FAILURES.get(server);
        if(old==null||tick-old>=200){FAILURES.put(server,tick);com.mojang.logging.LogUtils.getLogger().error("Retained evidence unavailable; native item/block transaction continues and no quest is awarded",error);}
    }
    private static void safely(ServerLevel level,Runnable action){try{action.run();}catch(RuntimeException failure){failure(level.getServer(),failure);}}
    private static void place(BlockEvent.EntityPlaceEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        safely(level,()->{var pos=event.getPos().immutable();var before=event.getBlockSnapshot().getState();var after=event.getPlacedBlock();
            if(before.is(net.minecraft.tags.BlockTags.LOGS)&&after.is(net.minecraft.tags.BlockTags.LOGS)&&!before.equals(after)){
                if(event.getEntity() instanceof ServerPlayer p)capture(p,level,pos,after,before.toString());return;
            }
            invalidate(level,pos);if(event.getEntity() instanceof ServerPlayer p)capture(p,level,pos,after,null);
        });
    }
    private static void path(BlockEvent.BlockToolModificationEvent event){
        if(event.isSimulated()||!(event.getPlayer() instanceof ServerPlayer p)||!(event.getLevel() instanceof ServerLevel level))return;
        safely(level,()->{
            if(event.getItemAbility()==ItemAbilities.SHOVEL_FLATTEN&&!event.getState().is(Blocks.DIRT_PATH))capture(p,level,event.getPos().immutable(),Blocks.DIRT_PATH.defaultBlockState(),null);
            else if(event.getItemAbility()==ItemAbilities.AXE_STRIP&&event.getFinalState()!=null){var after=event.getFinalState().equals(event.getState())?net.minecraft.world.item.AxeItem.getAxeStrippingState(event.getState()):event.getFinalState();if(after!=null&&!after.equals(event.getState()))capture(p,level,event.getPos().immutable(),after,event.getState().toString());}
        });
    }
    private static void capture(ServerPlayer player,ServerLevel level,BlockPos pos,BlockState expected,String originalProof){
        if(!RetainedBuildingService.real(player))return;
        var store=CommerceService.initializedStore(player.server).orElse(null);if(store==null||store.quarantined())return;
        var roster=store.professions(player.getUUID());if(!roster.isActive(Profession.BUILDER))return;
        var authorizations=sites(level,pos).stream().filter(s->s.player().equals(player.getUUID())&&roster.progressBand(Profession.BUILDER)==RetainedBuildingQuests.get(s.quest()).rank()&&RetainedBuildingService.candidate(s,pos,expected)).filter(s->originalProof==null||originalProof.equals(RetainedBuildingSavedData.get(player.server).evidence().positions(s.id()).get(pos.asLong()))).sorted(Comparator.comparing(BuilderWorksite::quest)).limit(1).toList();
        if(authorizations.isEmpty())return;
        later(player.server,()->{
            if(!RetainedBuildingService.real(player)||player.serverLevel()!=level||!level.hasChunkAt(pos)||!RetainedBuildingService.sameProof(level.getBlockState(pos),expected.toString())||PlacementProtection.check(level,player,pos,pos)!=null)return;
            var current=store.professions(player.getUUID());if(!current.isActive(Profession.BUILDER))return;
            var data=RetainedBuildingSavedData.get(player.server);
            for(var site:authorizations){
                if(current.progressBand(Profession.BUILDER)!=RetainedBuildingQuests.get(site.quest()).rank()||!store.builderWorksite(player.getUUID(),site.quest()).map(s->s.id().equals(site.id())).orElse(false))continue;
                if(data.evidence().record(site.id(),pos,level.getBlockState(pos).toString())){data.setDirty();RetainedBuildingService.changed(player.getUUID());}
            }
        });
    }
    private static void removed(BlockDropsEvent event){safely(event.getLevel(),()->invalidate(event.getLevel(),event.getPos()));}
    private static void tick(ServerTickEvent.Post event){var queue=PENDING.remove(event.getServer());if(queue==null)return;while(!queue.isEmpty())try{queue.removeFirst().run();}catch(RuntimeException failure){failure(event.getServer(),failure);}}
}
