package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.equipment.MasterGrownTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=TreeGrower.class,remap=false)
abstract class MasterTreeGrowerMixin {
 @WrapMethod(method="growTree")
 private boolean kingdom$grown(ServerLevel level,ChunkGenerator generator,BlockPos pos,BlockState state,RandomSource random,Operation<Boolean> original){MasterGrownTrees.Batch batch=null;try{batch=MasterGrownTrees.begin(level,pos,state);}catch(RuntimeException ignored){}boolean success=false;try{success=original.call(level,generator,pos,state,random);return success;}finally{try{MasterGrownTrees.finish(batch,success);}catch(RuntimeException ignored){}}}
}
