package com.mohymods.kingdomsmp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftInventoryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Initial public appearance sync must not disclose private cargo to arbitrary viewers. */
@Pseudo @Mixin(targets="immersive_aircraft.network.c2s.InventoryRequest",remap=false)
abstract class AircraftInventorySyncMixin {
    @Shadow @Final private int vehicleId;
    @Inject(method="receiveServer",at=@At("HEAD"),cancellable=true)
    private void kingdom$currentViewer(ServerPlayer p,CallbackInfo callback){if(!AircraftInventoryAccess.current(p))callback.cancel();}
    @WrapOperation(method="receiveServer",at=@At(value="INVOKE",target="Limmersive_aircraft/entity/inventory/SparseSimpleInventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),require=1)
    private ItemStack kingdom$publicEquipment(@Coerce Object inventory,int index,Operation<ItemStack> original,ServerPlayer p){
        return AircraftInventoryAccess.publicSlot(p,vehicleId,index)?original.call(inventory,index):ItemStack.EMPTY;
    }
}
