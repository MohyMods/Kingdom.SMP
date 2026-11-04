package com.mohymods.kingdomsmp.mixin;

import com.mohymods.kingdomsmp.integration.space.SpaceAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Before native rocket removal, passenger changes, station ingredients or block placement. */
@Pseudo @Mixin(targets={"earth.terrarium.adastra.common.network.packets.ServerboundLandPacket$Type","earth.terrarium.adastra.common.network.packets.ServerboundConstructSpaceStationPacket$Type","earth.terrarium.adastra.common.network.packets.ServerboundLandOnSpaceStationPacket$Type"},remap=false)
abstract class SpaceLandingMixin {
    @Inject(method="lambda$handle$0",at=@At("HEAD"),cancellable=true)
    private static void kingdom$beforeHandover(@Coerce Object packet,Player player,CallbackInfo callback){if(!SpaceAccess.landing(packet,player))callback.cancel();}
}
