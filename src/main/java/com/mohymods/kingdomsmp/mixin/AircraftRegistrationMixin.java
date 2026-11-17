package com.mohymods.kingdomsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftSales;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "immersive_aircraft.entity.VehicleEntity", remap = false)
abstract class AircraftRegistrationMixin {
  @WrapOperation(
      method = "drop",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Limmersive_aircraft/entity/VehicleEntity;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
  private ItemEntity kingdom$returnRegisteredFrame(
      @Coerce Entity entity, ItemStack stack, Operation<ItemEntity> original) {
    return AircraftSales.nativeDrop(entity, stack) ? null : original.call(entity, stack);
  }

  @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
  private void kingdom$pendingBodyIsInert(
      DamageSource source, float amount, CallbackInfoReturnable<Boolean> result) {
    if (AircraftSales.validateBody((Entity) (Object) this)) result.setReturnValue(false);
  }

  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void kingdom$checkGeneration(CallbackInfo result) {
    if (AircraftSales.validateBody((Entity) (Object) this)) result.cancel();
  }
}
