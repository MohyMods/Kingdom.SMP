package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.SaplingBlock;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native successful planting, transformations and removals. Evidence failures never interrupt item/block transactions. */
public final class TreeEvents {
    private static final Map<MinecraftServer,ArrayDeque<Runnable>> PENDING=new WeakHashMap<>();
    private record PistonKey(String dimension,long position) { }
    private record Movement(long tick,List<net.minecraft.core.BlockPos> cells) { }
    private static final Map<MinecraftServer,Map<PistonKey,Movement>> MOVEMENTS=new WeakHashMap<>();
    private TreeEvents() { }
    public static void register(IEventBus bus){
        bus.addListener(EventPriority.LOWEST,false,BlockEvent.EntityPlaceEvent.class,TreeEvents::place);
        bus.addListener(EventPriority.LOWEST,false,BlockGrowFeatureEvent.class,TreeEvents::grow);
        bus.addListener(EventPriority.LOWEST,false,BlockDropsEvent.class,TreeEvents::harvest);
        bus.addListener(EventPriority.LOWEST,false,BlockEvent.BlockToolModificationEvent.class,TreeEvents::strip);
        bus.addListener(EventPriority.LOWEST,false,PistonEvent.Pre.class,TreeEvents::pistonBefore);
        bus.addListener(TreeEvents::pistonAfter);
        bus.addListener(TreeEvents::tick);bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->{PENDING.remove(e.getServer());MOVEMENTS.remove(e.getServer());TreeLifecycle.stop(e.getServer());TreeQuestRules.clear(e.getServer());});
    }
    private static void safe(ServerLevel level,Runnable action){try{action.run();}catch(RuntimeException failure){TreeLifecycle.failure(level.getServer(),failure);}}
    private static void later(MinecraftServer server,Runnable action){var queue=PENDING.computeIfAbsent(server,s->new ArrayDeque<>());if(queue.size()<8192)queue.addLast(action);}
    private static void place(BlockEvent.EntityPlaceEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var pos=event.getPos().immutable();var block=event.getPlacedBlock();
        if(TreeLifecycle.species(block)==null&&!(block.getBlock() instanceof SaplingBlock))return;
        safe(level,()->{
            var before=event.getBlockSnapshot().getState();
            if(before.getBlock() instanceof SaplingBlock&&(before.getBlock()==block.getBlock()||TreeLifecycle.featureOutput(level,pos,block)))return;
            if(TreeLifecycle.species(before)!=null&&Objects.equals(TreeLifecycle.species(before),TreeLifecycle.species(block))&&!before.getBlock().equals(block.getBlock()))return;
            TreeLifecycle.mutation(level,pos);TreeLifecycle.drain(level.getServer(),128);TreeLifecycle.used(level.getServer());var data=TreeSavedData.get(level.getServer());String dim=level.dimension().location().toString();
            if(data.evidence().invalidate(dim,pos)|data.evidence().discardPlant(dim,pos))data.setDirty();
            if(!(block.getBlock() instanceof SaplingBlock)||!(event.getEntity() instanceof ServerPlayer p)||!TreeQuestService.real(p))return;
            String sapling=BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();if(!sapling.matches("minecraft:(oak|birch|spruce|jungle|acacia|dark_oak|cherry)_sapling|minecraft:mangrove_propagule"))return;
            var store=CommerceService.initializedStore(p.server).orElse(null);ProfessionRank band=store!=null&&!store.quarantined()&&store.professions(p.getUUID()).isActive(Profession.LUMBERJACK)?store.professions(p.getUUID()).progressBand(Profession.LUMBERJACK):null;
            later(p.server,()->{if(!TreeQuestService.real(p)||p.serverLevel()!=level||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(block)||!TreeLifecycle.consent(level,p,pos))return;if(data.evidence().plant(p.getUUID(),band,dim,pos,sapling))data.setDirty();});
        });
    }
    private static void grow(BlockGrowFeatureEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getFeature()!=null&&event.getFeature().value().config() instanceof net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration)TreeLifecycle.beforeGrowth(level,event.getPos(),event.getFeature().value().config());
    }
    private static void harvest(BlockDropsEvent event){
        if(TreeLifecycle.species(event.getState())==null&&!(event.getState().getBlock() instanceof SaplingBlock))return;var level=event.getLevel();var pos=event.getPos().immutable();
        safe(level,()->{
            TreeLifecycle.drain(level.getServer(),128);TreeLifecycle.mutation(level,pos);TreeLifecycle.used(level.getServer());var data=TreeSavedData.get(level.getServer());String dim=level.dimension().location().toString();
            boolean credited=false;if(event.getBreaker() instanceof ServerPlayer p&&TreeQuestService.real(p)&&level.getWorldBorder().isWithinBounds(pos)&&TreeLifecycle.consent(level,p,pos)){var store=CommerceService.initializedStore(p.server).orElse(null);if(store!=null&&!store.quarantined()){var roster=store.professions(p.getUUID());if(roster.isActive(Profession.LUMBERJACK)&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=ApprovedProfessionContent.get(Profession.LUMBERJACK.questId(roster.progressBand(Profession.LUMBERJACK),1)).minimumTier())credited=data.evidence().harvest(p.getUUID(),roster.progressBand(Profession.LUMBERJACK),dim,pos,event.getState().toString());}}
            if(credited|data.evidence().invalidate(dim,pos)|data.evidence().discardPlant(dim,pos))data.setDirty();
        });
    }
    private static void strip(BlockEvent.BlockToolModificationEvent event){
        if(event.isSimulated()||event.getItemAbility()!=ItemAbilities.AXE_STRIP||event.getFinalState()==null||!(event.getLevel() instanceof ServerLevel level))return;
        var pos=event.getPos().immutable();var before=event.getState();var reported=event.getFinalState();var after=reported.equals(before)?net.minecraft.world.item.AxeItem.getAxeStrippingState(before):reported;
        if(after==null||before.equals(after))return;
        safe(level,()->{
            ServerPlayer player=event.getPlayer() instanceof ServerPlayer p&&TreeQuestService.real(p)?p:null;
            var store=player==null?null:CommerceService.initializedStore(player.server).orElse(null);
            ProfessionRank band=store!=null&&!store.quarantined()&&store.professions(player.getUUID()).isActive(Profession.LUMBERJACK)?store.professions(player.getUUID()).progressBand(Profession.LUMBERJACK):null;
            boolean eligible=band!=null&&TreeLifecycle.consent(level,player,pos)&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(player.server).attainedTier()>=ApprovedProfessionContent.get(Profession.LUMBERJACK.questId(band,1)).minimumTier();
            TreeLifecycle.mutation(level,pos);later(level.getServer(),()->{
                if(!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(after))return;
                TreeLifecycle.drain(level.getServer(),128);TreeLifecycle.used(level.getServer());var data=TreeSavedData.get(level.getServer());
                if(data.evidence().transform(level.dimension().location().toString(),pos,before.toString(),after.toString()))data.setDirty();
                if(eligible&&"stripped_logs".equals(CraftDeliveryQuests.metrics(Profession.LUMBERJACK).get(BuiltInRegistries.BLOCK.getKey(after.getBlock()).toString()))&&TreeQuestService.real(player)&&TreeLifecycle.consent(level,player,pos)&&TreeLifecycle.species(before)!=null&&Objects.equals(TreeLifecycle.species(before),TreeLifecycle.species(after))){
                    var craft=ProfessionEvidenceSavedData.get(player.server);if(craft.evidence(Profession.LUMBERJACK).record(player.getUUID(),band,"stripped_logs",1,null))craft.setDirty();
                }
            });
        });
    }
    private static PistonKey key(PistonEvent event,ServerLevel level){return new PistonKey(level.dimension().location().toString(),event.getPos().asLong());}
    private static void pistonBefore(PistonEvent.Pre event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        safe(level,()->{var resolver=event.getStructureHelper();if(resolver==null||!resolver.resolve())return;
            var direction=event.getPistonMoveType().isExtend?event.getDirection():event.getDirection().getOpposite();Set<net.minecraft.core.BlockPos> cells=new HashSet<>();
            for(var pos:resolver.getToPush()){cells.add(pos.immutable());cells.add(pos.relative(direction));}for(var pos:resolver.getToDestroy())cells.add(pos.immutable());
            if(cells.isEmpty()||cells.size()>32)return;var plans=MOVEMENTS.computeIfAbsent(level.getServer(),s->new HashMap<>());if(plans.size()<8192)plans.put(key(event,level),new Movement(level.getServer().getTickCount(),List.copyOf(cells)));
        });
    }
    private static void pistonAfter(PistonEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        safe(level,()->{var plans=MOVEMENTS.get(level.getServer());if(plans==null)return;var movement=plans.remove(key(event,level));if(movement==null||movement.tick()!=level.getServer().getTickCount())return;
            TreeLifecycle.drain(level.getServer(),128);var data=TreeSavedData.get(level.getServer());boolean changed=false;
            for(var pos:movement.cells()){TreeLifecycle.mutation(level,pos);changed|=data.evidence().invalidate(level.dimension().location().toString(),pos)|data.evidence().discardPlant(level.dimension().location().toString(),pos);}if(changed)data.setDirty();
        });
    }
    private static void tick(ServerTickEvent.Post event){
        var server=event.getServer();var movements=MOVEMENTS.get(server);if(movements!=null)movements.values().removeIf(m->server.getTickCount()-m.tick()>1);TreeLifecycle.drain(server,128);var queue=PENDING.remove(server);if(queue!=null)while(!queue.isEmpty())try{queue.removeFirst().run();}catch(RuntimeException failed){TreeLifecycle.failure(server,failed);}
    }
}
