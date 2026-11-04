package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.AircraftPacketGuard;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets={"immersive_aircraft.network.c2s.PilotInputMessage","immersive_aircraft.network.c2s.EnginePowerMessage","immersive_aircraft.network.c2s.SeatChangeMessage","immersive_aircraft.network.c2s.CollisionMessage","immersive_aircraft.network.c2s.FireMessage","immersive_aircraft.network.c2s.CommandMessage"},remap=false)
abstract class AircraftControlMessageMixin {
    @Inject(method="receiveServer",at=@At("HEAD"),cancellable=true)
    private void kingdom$checkControl(ServerPlayer player,CallbackInfo callback){if(!AircraftPacketGuard.permit(player,this))callback.cancel();}
}
