package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.CreatePressService;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity",remap=false)
abstract class NativeCreatePressMixin {
 @WrapMethod(method="tryProcessInWorld") private boolean kingdom$press(ItemEntity input,boolean simulate,Operation<Boolean> nativePress){var proof=CreatePressService.capture((BlockEntity)(Object)this,input,simulate);boolean success=nativePress.call(input,simulate);CreatePressService.commit(proof,success);return success;}
}
