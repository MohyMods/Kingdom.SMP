package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BrewingStandBlockEntity.class,remap=false)
abstract class NativeBrewingMixin implements BrewJobHolder {
 @Unique private CompoundTag kingdom$job=new CompoundTag();
 public CompoundTag kingdom$brewJob(){return kingdom$job.copy();}
 public void kingdom$brewJob(CompoundTag job){kingdom$job=job.copy();}
 @Inject(method="saveAdditional",at=@At("TAIL")) private void kingdom$save(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci){if(!kingdom$job.isEmpty())tag.put(BrewingEvidence.DATA,kingdom$job.copy());}
 @Inject(method="loadAdditional",at=@At("TAIL")) private void kingdom$load(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci){try{kingdom$job=BrewingEvidence.checked(tag.getCompound(BrewingEvidence.DATA)).copy();}catch(RuntimeException invalid){kingdom$job=new CompoundTag();com.mojang.logging.LogUtils.getLogger().warn("Invalid optional Kingdom brewing provenance discarded; native stand inventory retained");}}
 @WrapMethod(method="serverTick") private static void kingdom$tick(Level level,BlockPos pos,BlockState state,BrewingStandBlockEntity stand,Operation<Void> nativeTick){var proof=BrewingEvidence.before(stand);nativeTick.call(level,pos,state,stand);BrewingEvidence.after(proof);}

}
