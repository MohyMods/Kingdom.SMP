package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.NativeDropReceipts;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=ServerPlayer.class,remap=false)
abstract class NativeProductionDropMixin {
 @WrapMethod(method="drop(Z)Z") private boolean kingdom$fundedDrop(boolean all,Operation<Boolean> nativeDrop){var drop=NativeDropReceipts.begin((ServerPlayer)(Object)this,all);var previous=NativeDropReceipts.enter(drop);try{boolean success=nativeDrop.call(all);NativeDropReceipts.finish(drop,success);return success;}finally{NativeDropReceipts.enter(previous);}}
}
