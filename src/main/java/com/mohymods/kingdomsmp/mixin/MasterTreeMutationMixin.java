package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.equipment.MasterGrownTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=LevelChunk.class,remap=false)
abstract class MasterTreeMutationMixin {
 @WrapMethod(method="setBlockState")
 private BlockState kingdom$changed(BlockPos pos,BlockState state,boolean moving,Operation<BlockState> original){var chunk=(LevelChunk)(Object)this;BlockState before=original.call(pos,state,moving);if(before!=null&&chunk.getLevel() instanceof ServerLevel level)MasterGrownTrees.changed(level,pos,before,state);return before;}
}
