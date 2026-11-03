package com.mohymods.kingdomsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mohymods.kingdomsmp.profession.TreeLifecycle;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Verified1.21.1 native trunk/foliage callbacks; no inference from nearby leaves or client data. */
@Mixin(value=TreeFeature.class,remap=false)
abstract class TreeFeatureMixin {
    @Unique private static final ThreadLocal<ArrayDeque<Optional<TreeLifecycle.Batch>>> kingdom$trees=ThreadLocal.withInitial(ArrayDeque::new);
    @WrapMethod(method="place")
    private boolean kingdom$feature(FeaturePlaceContext<TreeConfiguration> context,Operation<Boolean> original){
        var batch=TreeLifecycle.begin(context);var stack=kingdom$trees.get();stack.push(Optional.ofNullable(batch));
        try{boolean success=original.call(context);TreeLifecycle.finish(batch,success);return success;}
        finally{stack.pop();if(stack.isEmpty())kingdom$trees.remove();}
    }
    @WrapOperation(method="place",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/feature/TreeFeature;doPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;Lnet/minecraft/world/level/levelgen/feature/configurations/TreeConfiguration;)Z"),require=1)
    private boolean kingdom$output(TreeFeature self,WorldGenLevel level,RandomSource random,BlockPos origin,BiConsumer<BlockPos,BlockState> roots,BiConsumer<BlockPos,BlockState> trunk,FoliagePlacer.FoliageSetter foliage,TreeConfiguration config,Operation<Boolean> original){
        var stack=kingdom$trees.get();var batch=stack.isEmpty()?null:stack.peek().orElse(null);
        if(batch==null)return original.call(self,level,random,origin,roots,trunk,foliage,config);
        return original.call(self,level,random,origin,roots,TreeLifecycle.trunkRecorder(batch,trunk),TreeLifecycle.foliageRecorder(batch,foliage),config);
    }
}
