package com.mohymods.kingdomsmp.integration.space;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.KingdomTravelRules;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Exact native pre-cost boundaries. Generic dimension hooks alone run too late for rocket handover. */
public final class SpaceAccess {
    public static final String VERSION="1.16.26";
    private static Map<Class<?>,Method> destinations=Map.of();
    private static Method fluidContainer,fluidUpdate,disabledPlanets,spaceStations,spawnLocations;
    private static java.lang.reflect.Constructor<?> contentFactory;
    private static final Map<ServerPlayer,Long> lastNotice=new WeakHashMap<>();
    private static String status="Ad Astra absent; optional native launch/landing adapter inactive";
    private SpaceAccess() { }
    public static String status(){return status;}
    public static void register(IEventBus bus){
        if(!ModList.get().isLoaded("ad_astra"))return;
        var versions=Map.of("ad_astra",VERSION,"resourcefullib","3.0.12","resourcefulconfig","3.0.11","common_storage_lib","0.0.10");
        for(var version:versions.entrySet()){
            var mod=ModList.get().getModContainerById(version.getKey());
            if(mod.isEmpty()||!mod.orElseThrow().getModInfo().getVersion().toString().equals(version.getValue())){
                status="Unverified space API: requires "+version.getKey()+" "+version.getValue()+". Startup refused.";
                bus.addListener((ServerAboutToStartEvent event)->{throw new IllegalStateException(status);});return;
            }
        }
        try{
            var bound=new HashMap<Class<?>,Method>();
            for(String name:List.of("ServerboundLandPacket","ServerboundConstructSpaceStationPacket","ServerboundLandOnSpaceStationPacket")){
                var packet=Class.forName("earth.terrarium.adastra.common.network.packets."+name,false,SpaceAccess.class.getClassLoader());bound.put(packet,packet.getMethod("dimension"));
            }
            var rocket=Class.forName("earth.terrarium.adastra.common.entities.vehicles.Rocket",false,SpaceAccess.class.getClassLoader());rocket.getMethod("initiateLaunchSequence");
            var content=Class.forName("earth.terrarium.adastra.common.menus.content.PlanetsContent",false,SpaceAccess.class.getClassLoader());
            contentFactory=content.getConstructor(Set.class,Map.class,Set.class);disabledPlanets=content.getMethod("disabledPlanets");spaceStations=content.getMethod("spaceStations");spawnLocations=content.getMethod("spawnLocations");
            fluidContainer=rocket.getMethod("fluidContainer");fluidUpdate=Class.forName("earth.terrarium.common_storage_lib.fluid.impl.SimpleFluidStorage",false,SpaceAccess.class.getClassLoader()).getMethod("update");destinations=Map.copyOf(bound);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Verified space API unavailable; refusing unprotected native handover",failure);}
        status="Ad Astra "+VERSION+": Tier10 Moon/Earth orbit, Tier11 advanced planets; native pre-cost launch/landing guards";
    }
    /** Outgoing DTO copy: retain native disabled planets/stations/spawns and add current kingdom locks. */
    @SuppressWarnings("unchecked") public static Object content(ServerPlayer p,Object original){
        if(!p.server.isSameThread())throw new IllegalStateException("Planet menu requires the server thread");
        try{
            var nativeDisabled=(Set<net.minecraft.resources.ResourceLocation>)disabledPlanets.invoke(original);var combined=new HashSet<>(nativeDisabled);int tier=ContributionService.kingdom(p.server).attainedTier();
            for(var realm:SpaceTravelRules.realms())if(tier<realm.tier())combined.add(net.minecraft.resources.ResourceLocation.parse(realm.dimension()));
            for(var realm:KingdomTravelRules.realms())if(tier<realm.tier())combined.add(net.minecraft.resources.ResourceLocation.parse(realm.dimension()));
            return combined.equals(nativeDisabled)?original:contentFactory.newInstance(Set.copyOf(combined),spaceStations.invoke(original),spawnLocations.invoke(original));
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Verified native planet menu could not preserve current locks",failure);}
    }
    /** Native extraction does not commit its changed fluid snapshot on this exact API pair. */
    public static void commitFuel(Entity rocket){
        if(rocket.level().isClientSide)return;
        if(rocket.getServer()==null||!rocket.getServer().isSameThread())throw new IllegalStateException("Native launch fuel commit requires the server thread");
        try{fluidUpdate.invoke(fluidContainer.invoke(rocket));}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Verified native launch fuel could not be committed",failure);}
    }
    public static boolean launch(Entity rocket){
        if(rocket.level().isClientSide)return true;
        var server=rocket.getServer();if(server==null||!server.isSameThread())return false;
        // A historical already-inside player may leave space to return home. Landing
        // guards still refuse every currently locked destination; no forced relocation.
        if(SpaceTravelRules.space(rocket.level().dimension().location().toString()))return true;
        int tier=ContributionService.kingdom(server).attainedTier();if(tier>=SpaceTravelRules.MINIMUM_TIER)return true;
        for(var passenger:rocket.getPassengers())if(passenger instanceof ServerPlayer p)notice(p,"Space launches require at least kingdom Tier 10. Current tier: "+tier+".");
        return false;
    }
    public static boolean landing(Object packet,Player actor){
        if(!(actor instanceof ServerPlayer p)||!p.server.isSameThread()||p.hasDisconnected()||!p.isAlive()||p.isSpectator()||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return false;
        try{
            var getter=destinations.get(packet.getClass());if(getter==null)return false;
            var target=(ResourceKey<?>)getter.invoke(packet);String id=target.location().toString();
            int tier=ContributionService.kingdom(p.server).attainedTier();
            if(KingdomTravelRules.allowed(tier,id))return true;
            notice(p,"Destination requires at least kingdom Tier "+KingdomTravelRules.requiredTier(id)+". Current tier: "+tier+".");return false;
        }catch(ReflectiveOperationException|RuntimeException failure){return false;}
    }
    private static void notice(ServerPlayer p,String text){long now=p.server.getTickCount();Long previous=lastNotice.get(p);if(previous==null||now-previous>=20){lastNotice.put(p,now);p.displayClientMessage(Component.literal(text),true);}}
}
