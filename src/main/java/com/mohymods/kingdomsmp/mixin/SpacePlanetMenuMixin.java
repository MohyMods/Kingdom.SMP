package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.space.SpaceAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="earth.terrarium.adastra.common.menus.base.PlanetsMenuProvider",remap=false)
abstract class SpacePlanetMenuMixin {
    @Inject(method="createContent(Lnet/minecraft/server/level/ServerPlayer;)Learth/terrarium/adastra/common/menus/content/PlanetsContent;",at=@At("RETURN"),cancellable=true)
    private void kingdom$showCurrentLocks(ServerPlayer p,CallbackInfoReturnable<Object> callback){callback.setReturnValue(SpaceAccess.content(p,callback.getReturnValue()));}
}
