package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.AircraftSales;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native upgrade recipes otherwise strip registration when consuming a purchased airframe. */
@Mixin(Ingredient.class)
abstract class RegisteredAircraftIngredientMixin {
  @Inject(
      method = "test(Lnet/minecraft/world/item/ItemStack;)Z",
      at = @At("HEAD"),
      cancellable = true)
  private void kingdom$preserveRegistration(
      ItemStack stack, CallbackInfoReturnable<Boolean> result) {
    if (stack != null && AircraftSales.registered(stack)) result.setReturnValue(false);
  }
}
