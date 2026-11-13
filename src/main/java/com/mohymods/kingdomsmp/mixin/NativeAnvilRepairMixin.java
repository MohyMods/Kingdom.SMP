package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.RepairService;
import com.mohymods.kingdomsmp.profession.EnchantedToolService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
@Mixin(value=AnvilMenu.class,remap=false)
abstract class NativeAnvilRepairMixin {
 @Shadow private int repairItemCountCost;
 @WrapMethod(method="createResult")
 private void kingdom$preview(Operation<Void> original){original.call();var menu=(AnvilMenu)(Object)this;EnchantedToolService.preview(((NativeCombinerAccess)menu).kingdom$player(),menu);}
 @WrapMethod(method="onTake")
 private void kingdom$repair(Player player,ItemStack output,Operation<Void> original){var proof=RepairService.capture(player,(AnvilMenu)(Object)this,output,repairItemCountCost);var enchanted=EnchantedToolService.capture(player,(AnvilMenu)(Object)this,output,repairItemCountCost);original.call(player,output);RepairService.commit(proof);EnchantedToolService.commit(enchanted);}
}
