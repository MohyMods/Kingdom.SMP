package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.LibraryEnchantService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=EnchantmentMenu.class,remap=false)
abstract class NativeLibraryEnchantMixin {
 @WrapMethod(method="clickMenuButton")
 private boolean kingdom$enchant(Player player,int button,Operation<Boolean> original){var proof=LibraryEnchantService.capture(player,(EnchantmentMenu)(Object)this,button);boolean result=original.call(player,button);LibraryEnchantService.commit(proof,result);return result;}
}
