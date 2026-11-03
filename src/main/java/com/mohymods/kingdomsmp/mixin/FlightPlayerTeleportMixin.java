package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.FlightService;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Explicit server teleport APIs interrupt unfinished flight evidence; movement packets do not. */
@Mixin(value = net.minecraft.server.level.ServerPlayer.class, remap = false)
abstract class FlightPlayerTeleportMixin {
  @Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
  private void kingdom$teleported(CallbackInfo ci) {
    FlightService.interruptTeleport((Entity) (Object) this);
    com.mohymods.kingdomsmp.integration.aircraft.CargoService.interruptTeleport(
        (Entity) (Object) this);
  }

  @Inject(
      method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",
      at = @At("RETURN"))
  private void kingdom$transferred(CallbackInfoReturnable<Boolean> ci) {
    if (ci.getReturnValue()) {
      FlightService.interruptTeleport((Entity) (Object) this);
      com.mohymods.kingdomsmp.integration.aircraft.CargoService.interruptTeleport(
          (Entity) (Object) this);
    }
  }

  @Inject(method = "changeDimension", at = @At("RETURN"))
  private void kingdom$changedDimension(CallbackInfoReturnable<Entity> ci) {
    if (ci.getReturnValue() != null) {
      FlightService.interruptTeleport((Entity) (Object) this);
      com.mohymods.kingdomsmp.integration.aircraft.CargoService.interruptTeleport(
          (Entity) (Object) this);
    }
  }

  @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("RETURN"))
  private void kingdom$directTransfer(CallbackInfo ci) {
    FlightService.interruptTeleport((Entity) (Object) this);
    com.mohymods.kingdomsmp.integration.aircraft.CargoService.interruptTeleport(
        (Entity) (Object) this);
  }
}
