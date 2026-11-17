package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mohymods.kingdomsmp.profession.NetherOres;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=ScatteredOreFeature.class,remap=false)
abstract class NativeScatteredOreMixin {
 @WrapOperation(method="place",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
 private boolean kingdom$scattered(WorldGenLevel world,BlockPos pos,BlockState state,int flags,Operation<Boolean> original,@Local BlockState before){boolean success=original.call(world,pos,state,flags);if(success)NetherOres.generated(world,pos,before,state);return success;}
}
