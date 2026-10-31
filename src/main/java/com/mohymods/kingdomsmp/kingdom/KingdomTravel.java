package com.mohymods.kingdomsmp.kingdom;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import java.util.*;

/** NeoForge's actual Entity/ServerPlayer changeDimension hook, including teleportTo. */
public final class KingdomTravel {
    private static final Map<ServerPlayer,Long> LAST_NOTICE=new WeakHashMap<>();
    private KingdomTravel(){ }
    public static void travel(EntityTravelToDimensionEvent event){
        var entity=event.getEntity();var server=entity.getServer();
        if(server==null||entity.level().dimension().equals(event.getDimension()))return;
        String destination=event.getDimension().location().toString();
        int required=KingdomTravelRules.requiredTier(destination);if(required==1)return;
        int tier=ContributionService.kingdom(server).attainedTier();
        if(KingdomTravelRules.allowed(tier,destination))return;
        event.setCanceled(true);
        if(entity instanceof ServerPlayer player){
            long now=System.nanoTime();Long last=LAST_NOTICE.get(player);
            if(last==null||now-last>=5_000_000_000L){
                LAST_NOTICE.put(player,now);
                String realm=required==5?"The Nether":"The End";
                player.displayClientMessage(Component.literal(realm+" opens at kingdom Tier "+required+". Current tier: "+tier+"."),true);
            }
        }
    }
}
