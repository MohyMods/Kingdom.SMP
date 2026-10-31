package com.mohymods.kingdomsmp.building;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;

/** Small loaded-column search for a physically supported intermediate walking target. */
public final class ConstructionRoute {
    private ConstructionRoute(){}
    public static BlockPos supportedWaypoint(ServerLevel level,BlockPos estimate){
        if(!level.hasChunkAt(estimate))return null;
        for(int distance=0;distance<=4;distance++){
            BlockPos lower=estimate.below(distance);if(safe(level,lower))return lower;
            if(distance>0){BlockPos upper=estimate.above(distance);if(safe(level,upper))return upper;}
        }
        return null;
    }
    private static boolean safe(ServerLevel level,BlockPos feet){
        return !level.isOutsideBuildHeight(feet.below())&&!level.isOutsideBuildHeight(feet.above())
                &&level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)
                &&level.getBlockState(feet).getCollisionShape(level,feet).isEmpty()
                &&level.getBlockState(feet.above()).getCollisionShape(level,feet.above()).isEmpty()
                &&level.getFluidState(feet).isEmpty()&&level.getFluidState(feet.above()).isEmpty();
    }
}
