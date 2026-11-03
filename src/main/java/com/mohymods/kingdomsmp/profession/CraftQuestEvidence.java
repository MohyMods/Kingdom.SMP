package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Actual server crafting result events, never packets or renamed items. Delivery also consumes outputs. */
public final class CraftQuestEvidence {
    private CraftQuestEvidence() { }
    public static void register(IEventBus bus){bus.addListener(CraftQuestEvidence::crafted);}
    private static void crafted(PlayerEvent.ItemCraftedEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p instanceof FakePlayer||p.isCreative()||p.isSpectator()||!p.isAlive()||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return;
        var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return;
        var roster=store.professions(p.getUUID());var output=event.getCrafting();
        if(output.isEmpty()||output.getCount()>4096)return;
        String id=BuiltInRegistries.ITEM.getKey(output.getItem()).toString();
        for(var role:roster.active()){
            var metric=CraftDeliveryQuests.metrics(role).get(id);if(metric==null)continue;
            var data=ProfessionEvidenceSavedData.get(p.server);
            if(data.evidence(role).record(p.getUUID(),roster.progressBand(role),metric,output.getCount(),null))data.setDirty();
        }
    }
    public static int count(ServerPlayer p,Profession role,ProfessionRank band,String item){
        var metric=CraftDeliveryQuests.metrics(role).get(item);if(metric==null)throw new IllegalArgumentException("No validated crafting metric for output");
        return ProfessionEvidenceSavedData.get(p.server).evidence(role).count(p.getUUID(),band,metric);
    }
}
