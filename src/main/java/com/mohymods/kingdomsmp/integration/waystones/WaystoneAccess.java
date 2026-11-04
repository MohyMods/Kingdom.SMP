package com.mohymods.kingdomsmp.integration.waystones;

import com.mojang.logging.LogUtils;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.KingdomTravelRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Optional exact-API boundary. No Waystones/Balm types are linked when they are absent. */
public final class WaystoneAccess {
    public static final String VERIFIED_WAYSTONES="21.1.46",VERIFIED_BALM="21.0.66";
    private static Method contextOf,entityOf,targetOf,dimensionOf,commitEntity,commitLevel,commitOriginal,overrideResult,failedResult;
    private static java.lang.reflect.Constructor<?> cancelledError;
    private static String status="Waystones absent; optional use gate inactive",failure;
    private WaystoneAccess() { }
    public static String status(){return status;}
    @SuppressWarnings({"unchecked","rawtypes"})
    public static void register(IEventBus bus){
        var mod=ModList.get().getModContainerById("waystones");if(mod.isEmpty())return;
        String version=mod.orElseThrow().getModInfo().getVersion().toString();
        String balm=ModList.get().getModContainerById("balm").map(c->c.getModInfo().getVersion().toString()).orElse("absent");
        if(!VERIFIED_WAYSTONES.equals(version)||!VERIFIED_BALM.equals(balm)){
            failure="Waystones progression requires verified Waystones "+VERIFIED_WAYSTONES+" / Balm "+VERIFIED_BALM+"; installed "+version+" / "+balm+". World startup refused rather than opening an unverified travel bypass.";
            status=failure;bus.addListener(EventPriority.HIGHEST,WaystoneAccess::checkStartup);return;
        }
        try{
            Class<?> pre=Class.forName("net.blay09.mods.waystones.api.event.WaystoneTeleportEvent$Pre"),context=Class.forName("net.blay09.mods.waystones.api.WaystoneTeleportContext"),waystone=Class.forName("net.blay09.mods.waystones.api.Waystone");
            if(!Event.class.isAssignableFrom(pre)||!ICancellableEvent.class.isAssignableFrom(pre))throw new IllegalStateException("Waystones Pre is not a cancellable NeoForge event");
            contextOf=pre.getMethod("getContext");entityOf=context.getMethod("getEntity");targetOf=context.getMethod("getTargetWaystone");dimensionOf=waystone.getMethod("getDimension");
            // Balm's verified fireEvent posts to this actual NeoForge bus. Pre precedes
            // Waystones' requirement validation/consumption in normal paths.
            bus.addListener(EventPriority.LOWEST,false,(Class)pre,(Consumer<Event>)WaystoneAccess::teleport);
            // forceTeleport deliberately skips the cost/start event, but every native entity
            // transfer still passes this commit event. Deny its override branch as well.
            Class<?> commit=Class.forName("net.blay09.mods.waystones.api.event.WaystoneTeleportEntityEvent$Pre"),result=Class.forName("net.blay09.mods.waystones.api.EntityTeleportResult"),destination=Class.forName("net.blay09.mods.waystones.api.TeleportDestination"),error=Class.forName("net.blay09.mods.waystones.api.error.WaystoneTeleportError");
            commitEntity=commit.getMethod("getEntity");commitLevel=commit.getMethod("getTargetLevel");commitOriginal=commit.getMethod("getOriginalDestination");overrideResult=commit.getMethod("overrideResult",result);failedResult=result.getMethod("failed",Entity.class,destination,error);
            cancelledError=Class.forName("net.blay09.mods.waystones.api.error.WaystoneTeleportError$CancelledByEvent").getConstructor();
            bus.addListener(EventPriority.LOWEST,true,(Class)commit,(Consumer<Event>)WaystoneAccess::commit);

            bus.addListener(EventPriority.HIGHEST,WaystoneAccess::blockUse);
            bus.addListener(EventPriority.HIGHEST,WaystoneAccess::itemUse);
            status="Waystones "+version+" / Balm "+balm+"; Tier9 server use and pre-cost teleport gate registered";
            LogUtils.getLogger().info(status);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Verified Waystones travel API could not bind; refusing an unprotected progression integration",error);}
    }
    private static void checkStartup(ServerAboutToStartEvent event){if(failure!=null)throw new IllegalStateException(failure);}
    private static void teleport(Event event){
        try{
            var context=contextOf.invoke(event);var entity=(Entity)entityOf.invoke(context);var server=entity.getServer();if(server==null)return;
            if(!server.isSameThread()){
                ((ICancellableEvent)event).setCanceled(true);
                server.execute(()->notice(entity,"Waystone travel must be reviewed on the server thread. Try again."));return;
            }
            int tier=ContributionService.kingdom(server).attainedTier();
            if(tier<WaystoneRules.UNLOCK_TIER){((ICancellableEvent)event).setCanceled(true);notice(entity,locked(WaystoneRules.UNLOCK_TIER,tier));return;}
            var target=targetOf.invoke(context);if(target==null)throw new IllegalStateException("Missing Waystone destination");
            var dimension=(ResourceKey<?>)dimensionOf.invoke(target);String id=dimension.location().toString();
            if(!WaystoneRules.allowed(tier,id)){((ICancellableEvent)event).setCanceled(true);notice(entity,KingdomTravelRules.name(id)+" opens at kingdom Tier "+WaystoneRules.requiredTier(id)+". Current tier: "+tier+".");}
        }catch(ReflectiveOperationException|RuntimeException error){((ICancellableEvent)event).setCanceled(true);LogUtils.getLogger().error("Waystone travel refused because authoritative validation failed",error);}
    }
    private static void commit(Event event){
        try{
            var entity=(Entity)commitEntity.invoke(event);var server=entity.getServer();if(server==null)return;
            var target=(net.minecraft.server.level.ServerLevel)commitLevel.invoke(event);
            int tier=server.isSameThread()?ContributionService.kingdom(server).attainedTier():1;
            String id=target.dimension().location().toString();if(WaystoneRules.allowed(tier,id))return;
            ((ICancellableEvent)event).setCanceled(true);
            overrideResult.invoke(event,failedResult.invoke(null,entity,commitOriginal.invoke(event),cancelledError.newInstance()));
            if(server.isSameThread())notice(entity,WaystoneRules.requiredTier(id)==9?locked(9,tier):KingdomTravelRules.name(id)+" opens at kingdom Tier "+WaystoneRules.requiredTier(id)+". Current tier: "+tier+".");
        }catch(ReflectiveOperationException|RuntimeException error){((ICancellableEvent)event).setCanceled(true);LogUtils.getLogger().error("Waystone entity commit refused because authoritative validation failed",error);}
    }
    private static void blockUse(PlayerInteractEvent.RightClickBlock event){
        if(event.getLevel().isClientSide())return;
        if(!BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).getNamespace().equals("waystones"))return;
        denyEarlyUse(event);
    }
    private static void itemUse(PlayerInteractEvent.RightClickItem event){
        if(event.getLevel().isClientSide()||event.getItemStack().isEmpty())return;
        if(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).getNamespace().equals("waystones"))denyEarlyUse(event);
    }
    private static void denyEarlyUse(PlayerInteractEvent event){
        var server=event.getEntity().getServer();if(server==null)return;
        int tier=ContributionService.kingdom(server).attainedTier();if(tier>=WaystoneRules.UNLOCK_TIER)return;
        ((ICancellableEvent)event).setCanceled(true);notice(event.getEntity(),locked(WaystoneRules.UNLOCK_TIER,tier));
    }
    private static String locked(int required,int current){return "Waystones open at kingdom Tier "+required+". Current tier: "+current+".";}
    private static void notice(Entity entity,String message){if(entity instanceof ServerPlayer player&&player.server.getPlayerList().getPlayer(player.getUUID())==player)player.displayClientMessage(Component.literal(message),true);}
}
