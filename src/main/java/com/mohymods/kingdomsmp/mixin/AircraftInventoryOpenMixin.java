package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.AircraftInventoryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refuse an unauthorized initial full menu synchronization before it can disclose cargo. */
@Pseudo @Mixin(targets="immersive_aircraft.entity.InventoryVehicleEntity",remap=false)
abstract class AircraftInventoryOpenMixin {
    @Inject(method="openInventory",at=@At("HEAD"),cancellable=true)
    private void kingdom$authorizeCargo(ServerPlayer p,CallbackInfo callback){
        if(!AircraftInventoryAccess.entity(p,(Entity)(Object)this))callback.cancel();
    }
}
