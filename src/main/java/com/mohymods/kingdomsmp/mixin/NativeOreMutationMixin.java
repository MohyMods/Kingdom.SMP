package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.NativeOreProvenance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=LevelChunk.class,remap=false)
abstract class NativeOreMutationMixin {
 @WrapMethod(method="setBlockState")
 private BlockState kingdom$invalidate(BlockPos pos,BlockState after,boolean moving,Operation<BlockState> original){var before=original.call(pos,after,moving);NativeOreProvenance.changed((LevelChunk)(Object)this,pos,before,after);com.mohymods.kingdomsmp.profession.ApiaryProduction.changed((LevelChunk)(Object)this,pos,before,after);com.mohymods.kingdomsmp.profession.MineAccessService.changed((LevelChunk)(Object)this,pos,before,after);return before;}
}
