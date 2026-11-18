package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.equipment.MasterToolService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;
@Mixin(value=ServerPlayerGameMode.class,remap=false)
abstract class MasterToolMiningMixin {
 @Shadow @Final protected ServerPlayer player;
 @WrapMethod(method="destroyBlock")
 private boolean kingdom$master(BlockPos pos,Operation<Boolean> original){var plan=MasterToolService.capture(player,pos);boolean success=original.call(pos);try{MasterToolService.completed(player,pos,plan,success);}catch(RuntimeException ignored){}return success;}
}
