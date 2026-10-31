package com.mohymods.kingdomsmp.building;

import com.mohymods.kingdomsmp.network.ConstructionNetworking;
import com.mohymods.kingdomsmp.persistence.ConstructionStore.Phase;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.*;

/** Temporary reservation protections. Completed buildings suspend services on damage. */
public final class ConstructionProtection {
    private ConstructionProtection() { }
    public static void register(IEventBus bus){
        bus.addListener((BlockEvent.BreakEvent e)->{if(e.getLevel() instanceof ServerLevel l&&ConstructionService.protectedAt(l,e.getPos()))e.setCanceled(true);});
        bus.addListener((BlockEvent.EntityPlaceEvent e)->{if(!ConstructionService.placing()&&e.getLevel() instanceof ServerLevel l&&ConstructionService.protectedAt(l,e.getPos()))e.setCanceled(true);});
        bus.addListener((ExplosionEvent.Detonate e)->{if(e.getLevel() instanceof ServerLevel l)e.getAffectedBlocks().removeIf(p->ConstructionService.protectedAt(l,p));});
        bus.addListener((PistonEvent.Pre e)->{
            if(!(e.getLevel() instanceof ServerLevel l))return;
            var resolver=e.getStructureHelper();
            if(ConstructionService.protectedAt(l,e.getPos())||ConstructionService.protectedAt(l,e.getFaceOffsetPos())){e.setCanceled(true);return;}
            if(resolver!=null&&resolver.resolve()){
                var direction=e.getPistonMoveType().isExtend?e.getDirection():e.getDirection().getOpposite();
                if(resolver.getToPush().stream().anyMatch(p->ConstructionService.protectedAt(l,p)||ConstructionService.protectedAt(l,p.relative(direction)))
                        ||resolver.getToDestroy().stream().anyMatch(p->ConstructionService.protectedAt(l,p)))e.setCanceled(true);
            }
        });
        bus.addListener((PlayerInteractEvent.RightClickBlock e)->{
            if(!(e.getEntity() instanceof ServerPlayer player))return;
            if(ConstructionService.protectedAt(player.serverLevel(),e.getPos())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);return;}
            if(!player.isAlive()||player.isSpectator()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||player.serverLevel()!=player.server.overworld()
                    ||player.distanceToSqr(e.getPos().getX()+0.5,e.getPos().getY()+0.5,e.getPos().getZ()+0.5)>64)return;
            for(var p:ConstructionService.store(player.server).projects())if(p.phase()==Phase.COMPLETE){
                var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
                if(e.getPos().equals(p.origin().offset(b.rotate(b.board(),p.rotation())))&&ConstructionService.structureIntact(player.serverLevel(),p)){
                    e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
                    ConstructionService.select(player,p.id());
                    ConstructionNetworking.open(player,ConstructionService.purchased(p)?"Building records — inspect the registered project; visit its resident for services.":"Community milestone board — inspect the kingdom or contribute through the live milestone interface.");
                    return;
                }
            }
        });
    }
}
