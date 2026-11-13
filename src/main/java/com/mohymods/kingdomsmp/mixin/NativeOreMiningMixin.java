package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.NativeOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
@Mixin(value=ServerPlayerGameMode.class,remap=false)
abstract class NativeOreMiningMixin {
 @Shadow @Final protected ServerPlayer player;
 @WrapMethod(method="removeBlock")
 private boolean kingdom$mined(BlockPos pos,BlockState state,boolean harvest,Operation<Boolean> original){var proof=NativeOreProvenance.capture(player,pos,harvest);boolean success=original.call(pos,state,harvest);NativeOreProvenance.mined(proof,success);return success;}
}
