package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.BrewingEvidence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=AbstractContainerMenu.class,remap=false)
abstract class NativeBrewClickMixin {
 @WrapMethod(method="clicked") private void kingdom$click(int slot,int button,ClickType type,Player p,Operation<Void> nativeClick){var proof=BrewingEvidence.transferBefore(p,(AbstractContainerMenu)(Object)this);nativeClick.call(slot,button,type,p);BrewingEvidence.transferAfter(proof);}
}
