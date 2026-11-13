package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.space.SpaceAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Exact1.16.26/CommonStorage0.0.10 compatibility: commit extraction and preserve countdown key. */
@Pseudo @Mixin(targets="earth.terrarium.adastra.common.entities.vehicles.Rocket",remap=false)
abstract class SpaceFuelCommitMixin {
    @Inject(method="consumeFuel",at=@At("RETURN"))
    private void kingdom$commitFuel(boolean simulate,CallbackInfoReturnable<Boolean> callback){
        if(!simulate)SpaceAccess.commitFuel((Entity)(Object)this);
    }
    @Inject(method="readAdditionalSaveData",at=@At("HEAD"))
    private void kingdom$readSavedCountdown(CompoundTag data,CallbackInfo callback){
        // Native writer uses LaunchTicks; native reader expects launchTicks. Retain
        // any older explicit lower-case field instead of replacing historical data.
        if(data.contains("LaunchTicks",net.minecraft.nbt.Tag.TAG_INT)&&!data.contains("launchTicks",net.minecraft.nbt.Tag.TAG_INT))data.putInt("launchTicks",data.getInt("LaunchTicks"));
    }
}
