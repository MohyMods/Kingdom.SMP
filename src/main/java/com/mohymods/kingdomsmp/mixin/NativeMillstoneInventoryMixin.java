package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.MillingService;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity$MillstoneInventoryHandler",remap=false)
abstract class NativeMillstoneInventoryMixin {
 @WrapMethod(method="insertItem") private ItemStack kingdom$insert(int slot,ItemStack stack,boolean simulate,Operation<ItemStack> nativeInsert){var be=MillingService.machine(this);var before=simulate?null:MillingService.input(be);var result=nativeInsert.call(slot,stack,simulate);MillingService.changed(be,before);return result;}
 @WrapMethod(method="extractItem") private ItemStack kingdom$extract(int slot,int amount,boolean simulate,Operation<ItemStack> nativeExtract){var be=MillingService.machine(this);var before=simulate?null:MillingService.input(be);var result=nativeExtract.call(slot,amount,simulate);MillingService.changed(be,before);return result;}
}
