package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.MineAccessService;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
/** Observe after vanilla speed/teleport/collision validation; never changes movement acceptance. */
@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
abstract class NativeMineMovementMixin {
 @Shadow public ServerPlayer player;@Shadow private Vec3 awaitingPositionFromClient;
 @WrapMethod(method="handleMovePlayer") private void kingdom$walk(ServerboundMovePlayerPacket packet,Operation<Void> original){var frame=MineAccessService.capture(player,awaitingPositionFromClient);original.call(packet);MineAccessService.moved(frame,awaitingPositionFromClient);}
}
