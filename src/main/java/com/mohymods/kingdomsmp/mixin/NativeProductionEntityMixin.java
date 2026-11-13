package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.NativeDropReceipts;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=ItemEntity.class,remap=false)
abstract class NativeProductionEntityMixin {
 @WrapMethod(method="setItem") private void kingdom$changed(ItemStack stack,Operation<Void> nativeSet){nativeSet.call(stack);NativeDropReceipts.changed((ItemEntity)(Object)this);}
 @WrapMethod(method="playerTouch") private void kingdom$pickup(Player player,Operation<Void> nativeTouch){nativeTouch.call(player);NativeDropReceipts.changed((ItemEntity)(Object)this);}
 @WrapMethod(method="tryToMerge") private void kingdom$merge(ItemEntity other,Operation<Void> nativeMerge){var self=(ItemEntity)(Object)this;int before=self.getItem().getCount(),otherBefore=other.getItem().getCount();nativeMerge.call(other);NativeDropReceipts.merged(self,other,before,otherBefore);}
}
