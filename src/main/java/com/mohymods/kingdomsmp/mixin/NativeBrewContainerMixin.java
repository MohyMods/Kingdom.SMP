package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.BrewingEvidence;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=BaseContainerBlockEntity.class,remap=false)
abstract class NativeBrewContainerMixin {
 private ItemStack kingdom$before(int slot){return (Object)this instanceof BrewingStandBlockEntity stand&&slot>=0&&slot<4?stand.getItem(slot).copy():null;}
 private void kingdom$after(int slot,ItemStack before){if(before!=null)BrewingEvidence.inputChanged((BrewingStandBlockEntity)(Object)this,slot,before);}
 @WrapMethod(method="setItem") private void kingdom$set(int slot,ItemStack stack,Operation<Void> nativeSet){var before=kingdom$before(slot);nativeSet.call(slot,stack);kingdom$after(slot,before);}
 @WrapMethod(method="removeItem") private ItemStack kingdom$remove(int slot,int count,Operation<ItemStack> nativeRemove){var before=kingdom$before(slot);var output=nativeRemove.call(slot,count);kingdom$after(slot,before);return output;}
 @WrapMethod(method="removeItemNoUpdate") private ItemStack kingdom$removeAll(int slot,Operation<ItemStack> nativeRemove){var before=kingdom$before(slot);var output=nativeRemove.call(slot);kingdom$after(slot,before);return output;}
 @WrapMethod(method="clearContent") private void kingdom$clear(Operation<Void> nativeClear){var before=kingdom$before(3);nativeClear.call();kingdom$after(3,before);}
}
