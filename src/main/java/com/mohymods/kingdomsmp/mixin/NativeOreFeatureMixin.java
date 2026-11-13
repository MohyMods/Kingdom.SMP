package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mohymods.kingdomsmp.profession.NativeOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=OreFeature.class,remap=false)
abstract class NativeOreFeatureMixin {
 @WrapOperation(method="doPlace",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
 private BlockState kingdom$natural(LevelChunkSection section,int x,int y,int z,BlockState state,boolean locks,Operation<BlockState> original,@Local(argsOnly=true)WorldGenLevel world,@Local BlockPos.MutableBlockPos pos){var before=original.call(section,x,y,z,state,locks);NativeOreProvenance.generated(world,pos,before,state);return before;}
}
