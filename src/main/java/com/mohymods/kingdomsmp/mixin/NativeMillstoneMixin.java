package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.MillingService;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity",remap=false)
abstract class NativeMillstoneMixin {
 @WrapMethod(method="process") private void kingdom$mill(Operation<Void> nativeProcess){var impact=MillingService.capture((BlockEntity)(Object)this);nativeProcess.call();MillingService.commit(impact);}
 @WrapMethod(method="destroy") private void kingdom$destroy(Operation<Void> nativeDestroy){MillingService.invalidate((BlockEntity)(Object)this);nativeDestroy.call();}
}
