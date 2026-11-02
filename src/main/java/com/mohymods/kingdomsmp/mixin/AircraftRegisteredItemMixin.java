package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.aircraft.AircraftSales;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "immersive_aircraft.item.VehicleItem", remap = false)
abstract class AircraftRegisteredItemMixin {
  @Inject(method = "use", at = @At("HEAD"), cancellable = true)
  private void kingdom$registeredDeployment(
      Level level,
      Player player,
      InteractionHand hand,
      CallbackInfoReturnable<InteractionResultHolder<ItemStack>> result) {
    if (player instanceof ServerPlayer p && AircraftSales.registered(p.getItemInHand(hand)))
      result.setReturnValue(AircraftSales.deploy(p, hand));
  }
}
