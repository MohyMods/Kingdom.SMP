package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.RepairService;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=ItemStack.class,remap=false)
abstract class NativeWearMixin {
 @WrapMethod(method="hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V")
 private void kingdom$wear(int amount,ServerLevel level,LivingEntity entity,Consumer<Item> broken,Operation<Void> original){var stack=(ItemStack)(Object)this;int before;try{before=stack.getDamageValue();}catch(RuntimeException failed){original.call(amount,level,entity,broken);return;}original.call(amount,level,entity,broken);if(entity instanceof ServerPlayer p)RepairService.worn(p,stack,before);}
}
