package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.SmeltQuestEvidence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
/** Exact successful native result extraction; evidence failures cannot interrupt inventory transfer. */
@Mixin(value=FurnaceResultSlot.class,remap=false)
abstract class FurnaceProductionMixin {
    @Shadow @Final private Player player;
    @Shadow private int removeCount;
    @WrapMethod(method="checkTakeAchievements")
    private void kingdom$production(ItemStack output,Operation<Void> original){
        var proof=SmeltQuestEvidence.capture(player,((Slot)(Object)this).container,output,removeCount);
        original.call(output);
        SmeltQuestEvidence.commit(proof);
    }
}
