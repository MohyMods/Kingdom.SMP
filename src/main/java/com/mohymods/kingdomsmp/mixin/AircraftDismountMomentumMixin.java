package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.AircraftPacketRules;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Primitive fields have their final flags explicitly removed; no unsafe reflective final writes. */
@Pseudo @Mixin(targets="immersive_aircraft.network.c2s.CommandMessage",remap=false)
abstract class AircraftDismountMomentumMixin {
    @Shadow @Final @Mutable private double fx;
    @Shadow @Final @Mutable private double fy;
    @Shadow @Final @Mutable private double fz;
    @Inject(method="receiveServer",at=@At("HEAD"))
    private void kingdom$boundMomentum(ServerPlayer player,CallbackInfo callback){var safe=AircraftPacketRules.dismount(fx,fy,fz);fx=safe.x;fy=safe.y;fz=safe.z;}
}
