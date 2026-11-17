package com.mohymods.kingdomsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Native aircraft implement powered lift without vanilla mayfly or NoGravity. */
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
abstract class AircraftVehicleMovementMixin {
    @Shadow public ServerPlayer player;
    // Exactly the vehicle-floating predicate. Player movement, speed/collision checks,
    // ordinary vehicles and the global server flight policy are left unchanged.
    @WrapOperation(method="handleMoveVehicle",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;isFlightAllowed()Z"),require=1)
    private boolean kingdom$nativeLift(MinecraftServer server,Operation<Boolean> original){
        return original.call(server)||AircraftAccess.nativeLift(player);
    }
}
