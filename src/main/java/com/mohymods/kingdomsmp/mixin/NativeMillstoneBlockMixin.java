package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.MillingService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="com.simibubi.create.content.kinetics.millstone.MillstoneBlock",remap=false)
abstract class NativeMillstoneBlockMixin {
 @WrapMethod(method="updateEntityAfterFallOn") private void kingdom$input(BlockGetter level,Entity entity,Operation<Void> nativeFall){var input=MillingService.captureInput(entity);nativeFall.call(level,entity);MillingService.inserted(input);}
 @WrapMethod(method="useItemOn") private ItemInteractionResult kingdom$use(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,Operation<ItemInteractionResult> nativeUse){var be=level.getBlockEntity(pos);var before=MillingService.input(be);var result=nativeUse.call(stack,state,level,pos,player,hand,hit);MillingService.changed(be,before);return result;}
}
