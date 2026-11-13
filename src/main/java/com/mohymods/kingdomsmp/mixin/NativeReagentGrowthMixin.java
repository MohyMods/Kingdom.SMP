package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.ReagentCropService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=CropBlock.class,remap=false)
abstract class NativeReagentGrowthMixin {
 @WrapMethod(method="growCrops") private void kingdom$grow(Level level,BlockPos pos,BlockState state,Operation<Void> nativeGrow){nativeGrow.call(level,pos,state);if(!level.isClientSide&&level.hasChunkAt(pos))ReagentCropService.growth(level,pos,state,level.getBlockState(pos));}
}
