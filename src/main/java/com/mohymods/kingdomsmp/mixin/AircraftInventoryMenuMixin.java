package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.AircraftInventoryAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla checks stillValid before every container click, including quick-move/hotbar swaps. */
@Pseudo @Mixin(targets="immersive_aircraft.screen.VehicleScreenHandler",remap=false)
abstract class AircraftInventoryMenuMixin {
    @Inject(method="stillValid",at=@At("RETURN"),cancellable=true)
    private void kingdom$currentCargoRights(Player player,CallbackInfoReturnable<Boolean> callback){
        if(callback.getReturnValueZ()&&!AircraftInventoryAccess.menu(player,this))callback.setReturnValue(false);
    }
}
