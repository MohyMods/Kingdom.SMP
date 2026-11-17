package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.BrewingEvidence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(targets="net.minecraft.world.inventory.BrewingStandMenu$PotionSlot",remap=false)
abstract class NativeBrewTakeMixin {
 @WrapMethod(method="onTake") private void kingdom$take(Player p,ItemStack stack,Operation<Void> nativeTake){nativeTake.call(p,stack);BrewingEvidence.take(p,(Slot)(Object)this,stack);}
}
