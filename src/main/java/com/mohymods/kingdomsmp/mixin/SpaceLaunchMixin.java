package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.space.SpaceAccess;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="earth.terrarium.adastra.common.entities.vehicles.Rocket",remap=false)
abstract class SpaceLaunchMixin {
    @Inject(method="initiateLaunchSequence",at=@At("HEAD"),cancellable=true)
    private void kingdom$beforeFuel(CallbackInfo callback){if(!SpaceAccess.launch((Entity)(Object)this))callback.cancel();}
}
