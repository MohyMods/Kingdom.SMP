package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Exact optional aircraft boundary; no aircraft class is linked when the mod is absent. */
public final class AircraftAccess {
    public static final String VERSION="1.7.0+1.21.1";
    private static Class<?> vehicle,item;
    private static java.lang.reflect.Field onWater;
    private static String unavailable="Requires installed Immersive Aircraft "+VERSION+".",failure;
    private record Restoration(ServerPlayer player,Entity vehicle) { }
    private static final ThreadLocal<Restoration> RESTORING=new ThreadLocal<>();
    private static final java.util.Map<ServerPlayer,java.lang.ref.WeakReference<Entity>> RESTORED=new java.util.WeakHashMap<>();
    private static final String LANDING="KingdomAircraftSafeLanding";
    private AircraftAccess() { }
    /** Only vanilla PlayerList's two saved RootVehicle attach sites call this hook. */
    public static boolean restoreSavedVehicle(ServerPlayer p,Entity entity,java.util.function.BooleanSupplier original){
        if(!aircraft(entity))return original.getAsBoolean();
        Restoration previous=RESTORING.get();RESTORING.set(new Restoration(p,entity));
        try{boolean attached=original.getAsBoolean();if(attached&&p.getVehicle()==entity)RESTORED.put(p,new java.lang.ref.WeakReference<>(entity));return attached;}
        finally{if(previous==null)RESTORING.remove();else RESTORING.set(previous);}
    }
    public static boolean savedVehicleRestore(ServerPlayer p,Entity entity){var ref=RESTORED.get(p);return ref!=null&&ref.get()==entity;}
    /** Runs after vanilla cleanup and all PlayerLoggedIn handlers, before further play packets. */
    public static void finishSavedVehicleRestore(ServerPlayer p){
        var ref=RESTORED.get(p);Entity entity=ref==null?null:ref.get();
        try{
            if(entity!=null&&p.getVehicle()==entity){
                String why=denial(p.server);if(why.isEmpty())why=AircraftSales.denial(p,entity);if(why.isEmpty())why=com.mohymods.kingdomsmp.placement.PlacementProtection.restoredVehicleInteraction(p,entity);
                if(why==null||why.isEmpty()){p.getPersistentData().remove(LANDING);return;}
                suspend(p,why);return;
            }
            if(p.getPersistentData().getBoolean(LANDING))suspend(p,"Your previous aircraft landing still needs recovery.");
        }finally{RESTORED.remove(p);}
    }
    private record Landing(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos feet) { }
    private static Landing nearbyGround(ServerPlayer p,net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos center){
        for(int radius=0;radius<=6;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy=2;dy>=-8;dy--){
                var feet=center.offset(dx,dy,dz);if(feet.getY()<=level.getMinBuildHeight()||feet.getY()+2>=level.getMaxBuildHeight()||!level.hasChunkAt(feet))continue;
                var floor=level.getBlockState(feet.below());
                if(!floor.isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)||!floor.getFluidState().isEmpty()||floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)||floor.is(net.minecraft.world.level.block.Blocks.CACTUS)||floor.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)||floor.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE))continue;
                if(java.util.stream.Stream.of(feet,feet.above()).anyMatch(pos->{var state=level.getBlockState(pos);return state.is(net.minecraft.world.level.block.Blocks.FIRE)||state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)||state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)||state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW);}))continue;
                var box=p.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(new net.minecraft.world.phys.Vec3(feet.getX()+.5,feet.getY(),feet.getZ()+.5));
                boolean loaded=true;for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=net.minecraft.util.Mth.floor(Math.nextDown(box.maxX))>>4;cx++)for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=net.minecraft.util.Mth.floor(Math.nextDown(box.maxZ))>>4;cz++)if(!level.hasChunk(cx,cz))loaded=false;
                if(!loaded||!level.getWorldBorder().isWithinBounds(box)||!level.noCollision(p,box)||ConstructionService.protectedAt(level,feet)||!level.getFluidState(feet).isEmpty()||!level.getFluidState(feet.above()).isEmpty())continue;
                return new Landing(level,feet.immutable());
            }
        }
        return null;
    }
    private static void suspend(ServerPlayer p,String reason){
        p.getPersistentData().putBoolean(LANDING,true);
        Landing landing=nearbyGround(p,p.serverLevel(),p.blockPosition());
        if(landing==null){
            var hut=com.mohymods.kingdomsmp.persistence.BuilderHutSavedData.get(p.server).hut().orElse(null);
            if(hut!=null){var home=BuilderHutService.home(hut);p.server.overworld().getChunkAt(home);landing=nearbyGround(p,p.server.overworld(),home);}
        }
        if(landing==null){var spawn=p.server.overworld().getSharedSpawnPos();p.server.overworld().getChunkAt(spawn);landing=nearbyGround(p,p.server.overworld(),spawn);}
        if(landing==null){
            // Stay attached until disconnect/save: never let vanilla discard a legitimate
            // aircraft or save an unaided falling player in an empty/invalid landing area.
            p.connection.disconnect(Component.literal("Saved aircraft preserved; no safe landing could be verified. Ask an administrator to repair the landing area."));return;
        }
        p.stopRiding();p.teleportTo(landing.level(),landing.feet().getX()+.5,landing.feet().getY(),landing.feet().getZ()+.5,p.getYRot(),p.getXRot());p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.fallDistance=0;p.getPersistentData().remove(LANDING);
        p.sendSystemMessage(Component.literal(reason+" Your aircraft was preserved; you were moved to safe ground."));
        com.mojang.logging.LogUtils.getLogger().info("AIRCRAFT_SAFE_LANDING actor={} at={} reason={}",p.getUUID(),landing.feet(),reason);
    }
    public static String unavailable(){return unavailable;}
    public static boolean available(){return unavailable.isEmpty();}
    public static void require(){if(!available())throw new IllegalStateException(unavailable);}
    public static String status(){return available()?"Immersive Aircraft "+VERSION+": Tier6 AND completed verified Royal Aerodrome required for deployment and boarding":unavailable;}
    public static void register(IEventBus bus){
        var mod=ModList.get().getModContainerById("immersive_aircraft");if(mod.isEmpty())return;
        String version=mod.orElseThrow().getModInfo().getVersion().toString();
        if(!VERSION.equals(version)){
            failure=unavailable="Unverified Immersive Aircraft "+version+"; requires "+VERSION+". World startup refused rather than exposing unverified aircraft access.";
            bus.addListener(EventPriority.HIGHEST,(ServerAboutToStartEvent e)->{throw new IllegalStateException(failure);});return;
        }
        try{vehicle=Class.forName("immersive_aircraft.entity.VehicleEntity");item=Class.forName("immersive_aircraft.item.VehicleItem");onWater=item.getDeclaredField("onWater");if(!onWater.trySetAccessible())throw new IllegalStateException("Aircraft deployment fluid policy unavailable");AircraftPacketGuard.bind();AircraftInventoryAccess.bind();AircraftRepairs.bind(vehicle);FlightService.bind();}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Verified aircraft API unavailable; refusing unprotected aircraft access",e);}
        unavailable="";
        bus.addListener(EventPriority.HIGHEST,AircraftAccess::use);
        bus.addListener(EventPriority.HIGHEST,AircraftAccess::useBlock);
        bus.addListener(EventPriority.HIGHEST,AircraftAccess::interact);
        bus.addListener(EventPriority.HIGHEST,AircraftAccess::interactAt);
        bus.addListener(EventPriority.LOWEST,AircraftAccess::mount);
        bus.addListener(AircraftAccess::tick);
    }
    /** Only the verified current controlling rider can use native vehicle lift. No player-flight grant. */
    public static boolean nativeLift(ServerPlayer p){
        if(!p.server.isSameThread()||p.hasDisconnected()||!p.isAlive()||p.isSpectator()||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return false;
        Entity root=p.getRootVehicle();
        return aircraft(root)&&root.getControllingPassenger()==p&&denial(p.server).isEmpty()&&AircraftSales.denial(p,root).isEmpty();
    }
    public static boolean aircraft(Entity e){return vehicle!=null&&vehicle.isInstance(e);}
    public static boolean aircraftItem(ItemStack stack){return item!=null&&item.isInstance(stack.getItem());}
    /** No chunk acquisition: unloaded completed buildings retain their recorded completion. Loaded damage suspends use. */
    public static boolean aerodromeComplete(MinecraftServer server){
        var store=ConstructionService.initializedStore(server).orElse(null);if(store==null||store.quarantined())return false;
        return store.projects().stream().anyMatch(p->p.blueprint().equals(RoyalAerodromeBlueprint.ID)&&p.phase()==ConstructionStore.Phase.COMPLETE&&(!ConstructionService.loaded(server.overworld(),p)||ConstructionService.structureIntact(server.overworld(),p)));
    }
    public static String denial(MinecraftServer server){
        if(!available())return unavailable;
        if(!server.isSameThread())return "Aircraft access must be reviewed on the server thread.";
        int tier=ContributionService.kingdom(server).attainedTier();
        if(tier<6)return "Aircraft need Tier 6 and a completed Aerodrome. Current tier: "+tier+".";
        return aerodromeComplete(server)?"":"Finish or repair the Royal Aerodrome before using aircraft.";
    }
    private static boolean denied(ServerPlayer p){String why=denial(p.server);if(why.isEmpty())return false;p.displayClientMessage(Component.literal(why),true);return true;}
    public static boolean deploymentDenied(ServerPlayer p,ItemStack stack){
        if(denied(p))return true;
        try{
            var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id))throw new IllegalStateException("Aircraft item has no verified native entity type");
            // Match VehicleItem's server-side OUTLINE ray and exact water policy, using native registered dimensions.
            var hit=p.pick(p.blockInteractionRange(),1,onWater.getBoolean(stack.getItem()));
            if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return false; // Native no-target failure remains unchanged.
            var bounds=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id).getDimensions().makeBoundingBox(hit.getLocation());
            if(!p.serverLevel().getWorldBorder().isWithinBounds(bounds))throw new IllegalStateException("Aircraft footprint crosses the world border");
            var from=net.minecraft.core.BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ);var to=net.minecraft.core.BlockPos.containing(Math.nextDown(bounds.maxX),Math.nextDown(bounds.maxY),Math.nextDown(bounds.maxZ));
            if(from.getY()<p.serverLevel().getMinBuildHeight()||to.getY()>=p.serverLevel().getMaxBuildHeight())throw new IllegalStateException("Aircraft footprint exceeds world height");
            for(var pos:net.minecraft.core.BlockPos.betweenClosed(from,to)){
                if(!p.serverLevel().hasChunkAt(pos))throw new IllegalStateException("Load the complete aircraft footprint before deployment");
                if(ConstructionService.protectedAt(p.serverLevel(),pos))throw new IllegalStateException("Aircraft footprint overlaps a construction reservation");
            }
            String why=com.mohymods.kingdomsmp.placement.PlacementProtection.check(p.serverLevel(),p,from,to);
            if(why!=null)throw new IllegalStateException(why);
            return false;
        }catch(ReflectiveOperationException|RuntimeException error){p.displayClientMessage(Component.literal("Aircraft site blocked. See chat for the placement rule."),true);p.sendSystemMessage(Component.literal("Aircraft deployment refused: "+error.getMessage()));return true;}
    }
    public static boolean onWater(ItemStack stack)throws IllegalAccessException{return onWater.getBoolean(stack.getItem());}
    private static void use(PlayerInteractEvent.RightClickItem e){if(e.getEntity() instanceof ServerPlayer p&&aircraftItem(e.getItemStack())&&deploymentDenied(p,e.getItemStack()))e.setCanceled(true);}
    private static void useBlock(PlayerInteractEvent.RightClickBlock e){if(e.getEntity() instanceof ServerPlayer p&&aircraftItem(e.getItemStack())&&deploymentDenied(p,e.getItemStack()))e.setCanceled(true);}
    private static boolean boardingDenied(ServerPlayer p,Entity target){
        if(denied(p))return true;
        String access=AircraftSales.denial(p,target);if(!access.isEmpty()){p.displayClientMessage(Component.literal(access),true);return true;}
        String why=com.mohymods.kingdomsmp.placement.PlacementProtection.entityInteraction(p,target);
        if(why==null)return false;p.displayClientMessage(Component.literal(why.replace("resident","aircraft")),true);return true;
    }
    private static void interact(PlayerInteractEvent.EntityInteract e){if(e.getEntity() instanceof ServerPlayer p&&aircraft(e.getTarget())&&boardingDenied(p,e.getTarget()))e.setCanceled(true);}
    private static void interactAt(PlayerInteractEvent.EntityInteractSpecific e){if(e.getEntity() instanceof ServerPlayer p&&aircraft(e.getTarget())&&boardingDenied(p,e.getTarget()))e.setCanceled(true);}
    private static void mount(EntityMountEvent e){var restore=RESTORING.get();if(restore!=null&&restore.player()==e.getEntityMounting()&&restore.vehicle()==e.getEntityBeingMounted())return;if(e.isMounting()&&e.getEntityMounting() instanceof ServerPlayer p&&aircraft(e.getEntityBeingMounted())&&boardingDenied(p,e.getEntityBeingMounted()))e.setCanceled(true);}
    private static void tick(ServerTickEvent.Post e){
        FlightService.tick(e.getServer());
        // One integrity scan per second for all actual riders, no scans or tickets when nobody is aboard.
        if(e.getServer().getTickCount()%20!=0)return;
        AircraftSales.recover(e.getServer());AircraftRepairs.recover(e.getServer());
        Boolean allowed=null;
        for(var p:java.util.List.copyOf(e.getServer().getPlayerList().getPlayers()))if(!p.hasDisconnected()&&aircraft(p.getVehicle())){
            if(allowed==null)allowed=denial(e.getServer()).isEmpty();
            if(!allowed)suspend(p,"Aircraft access suspended. Tier 6 and an intact Aerodrome are required.");else{String why=AircraftSales.denial(p,p.getVehicle());if(!why.isEmpty())suspend(p,why);}
        }
    }
}
