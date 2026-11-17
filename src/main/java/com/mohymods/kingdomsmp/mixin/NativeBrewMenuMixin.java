package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.BrewingEvidence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=BrewingStandMenu.class,remap=false)
abstract class NativeBrewMenuMixin {
 @WrapMethod(method="quickMoveStack") private ItemStack kingdom$shift(Player p,int slot,Operation<ItemStack> nativeMove){var proof=BrewingEvidence.transferBefore(p,(BrewingStandMenu)(Object)this);var result=nativeMove.call(p,slot);BrewingEvidence.transferAfter(proof);return result;}
}
