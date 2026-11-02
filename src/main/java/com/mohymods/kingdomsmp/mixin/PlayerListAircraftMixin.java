package com.mohymods.kingdomsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla discards RootVehicle when reattachment is cancelled: validate after its cleanup branch. */
@Mixin(value=PlayerList.class,remap=false)
abstract class PlayerListAircraftMixin {
    @WrapOperation(method="placeNewPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;startRiding(Lnet/minecraft/world/entity/Entity;Z)Z"),require=2)
    private boolean kingdom$restore(ServerPlayer player,Entity vehicle,boolean force,Operation<Boolean> original){
        return AircraftAccess.restoreSavedVehicle(player,vehicle,()->original.call(player,vehicle,force));
    }
    @Inject(method="placeNewPlayer",at=@At("RETURN"))
    private void kingdom$verifyRestore(Connection connection,ServerPlayer player,CommonListenerCookie cookie,CallbackInfo callback){
        AircraftAccess.finishSavedVehicleRestore(player);
    }
}
