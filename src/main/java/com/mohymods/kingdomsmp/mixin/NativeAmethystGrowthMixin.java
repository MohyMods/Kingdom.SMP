package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mohymods.kingdomsmp.profession.AmethystGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=BuddingAmethystBlock.class,remap=false)
abstract class NativeAmethystGrowthMixin {
 @WrapOperation(method="randomTick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
 private boolean kingdom$grown(ServerLevel level,BlockPos pos,BlockState state,Operation<Boolean> original){var frame=AmethystGrowth.before(level,pos);boolean success=original.call(level,pos,state);if(success)AmethystGrowth.grown(level,pos,state,frame);return success;}
}
