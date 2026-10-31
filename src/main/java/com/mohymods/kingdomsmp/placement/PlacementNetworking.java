package com.mohymods.kingdomsmp.placement;

import com.mohymods.kingdomsmp.building.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.*;
import java.util.function.Consumer;

public final class PlacementNetworking {
    private static final Map<ServerPlayer,Long> LAST=new WeakHashMap<>();
    private static Consumer<PlacementResult> receiver;
    private PlacementNetworking(){}
    public static void install(Consumer<PlacementResult> r){receiver=Objects.requireNonNull(r);}
    public static void register(PayloadRegistrar registrar){
        registrar.playToClient(PlacementResult.TYPE,PlacementResult.STREAM_CODEC,(p,c)->{if(receiver!=null)receiver.accept(p);});
        registrar.playToServer(PlacementProbe.TYPE,PlacementProbe.STREAM_CODEC,(p,c)->{if(c.player() instanceof ServerPlayer player){long now=System.nanoTime();Long last=LAST.get(player);if(last!=null&&now-last<250_000_000L)return;LAST.put(player,now);c.reply(inspect(player,p));}});
    }
    public static PlacementResult inspect(ServerPlayer player,PlacementProbe probe){
        try{
            var project=ConstructionService.lodge(player.server);
            if(project==null||!ConstructionService.hasPlan(player,project)||!ConstructionService.heldPlan(player,project))throw new IllegalStateException("Hold your current unused blueprint. Recover a lost or stale plan through Construction.");
            if(project.phase().ordinal()>=com.mohymods.kingdomsmp.persistence.ConstructionStore.Phase.BUILDING.ordinal())throw new IllegalStateException("This construction right has already been redeemed.");
            // Existing foreign claim data must never be treated as unclaimed land.
            String protection=PlacementProtection.denial();if(protection!=null)throw new IllegalStateException(protection);
            var preview=ConstructionService.inspectCandidate(player,probe.origin(),Rotation.values()[probe.rotation()]);
            var candidate=project.propose(probe.origin(),Rotation.values()[probe.rotation()]);var level=player.serverLevel();
            List<BlockPos> collisions=new ArrayList<>(),unsupported=new ArrayList<>();
            if(level.dimension()==Level.OVERWORLD&&ConstructionService.loaded(level,candidate)&&!level.isOutsideBuildHeight(probe.origin().below())&&!level.isOutsideBuildHeight(probe.origin().offset(0,BlueprintRegistry.lodge().height()-1,0))){
                var definition=BlueprintRegistry.lodge();int width=definition.width(candidate.rotation()),depth=definition.depth(candidate.rotation());
                for(int x=0;x<width;x++)for(int z=0;z<depth;z++){
                    var support=probe.origin().offset(x,-1,z);if(unsupported.size()<32&&!level.getBlockState(support).isFaceSturdy(level,support,Direction.UP))unsupported.add(support.immutable());
                }
                for(var pos:BlockPos.betweenClosed(probe.origin(),probe.origin().offset(width-1,definition.height()-1,depth-1)))if(collisions.size()<32&&!level.getBlockState(pos).isAir())collisions.add(pos.immutable());
            }
            return new PlacementResult(probe,preview.denial().isBlank(),preview.denial().isBlank()?"Valid site — use the blueprint to review placement.":preview.denial(),collisions,unsupported);
        }catch(IllegalArgumentException|IllegalStateException e){String reason=Objects.toString(e.getMessage(),"Placement unavailable");return new PlacementResult(probe,false,reason.substring(0,Math.min(512,reason.length())),List.of(),List.of());}
    }
}
