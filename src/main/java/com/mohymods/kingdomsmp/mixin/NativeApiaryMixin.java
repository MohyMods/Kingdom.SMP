package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.ApiaryProduction;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntity",remap=false)
abstract class NativeApiaryMixin {
 @WrapMethod(method="beeReleasePostAction") private void kingdom$production(Level level,Bee bee,BlockState state,BeehiveBlockEntity.BeeReleaseStatus status,Operation<Void> original){var proof=ApiaryProduction.capture((BlockEntity)(Object)this,bee,status);original.call(level,bee,state,status);ApiaryProduction.commit(proof);}
}
