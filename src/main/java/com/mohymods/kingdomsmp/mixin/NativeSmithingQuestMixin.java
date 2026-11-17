package com.mohymods.kingdomsmp.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mohymods.kingdomsmp.profession.SmithingQuestService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=SmithingMenu.class,remap=false)
abstract class NativeSmithingQuestMixin {
 @WrapMethod(method="createResult")
 private void kingdom$preview(Operation<Void> original){original.call();var menu=(SmithingMenu)(Object)this;SmithingQuestService.preview(((NativeCombinerAccess)menu).kingdom$player(),menu);}
 @WrapMethod(method="onTake")
 private void kingdom$upgrade(Player player,ItemStack output,Operation<Void> original){var proof=SmithingQuestService.capture(player,(SmithingMenu)(Object)this,output);original.call(player,output);SmithingQuestService.commit(proof);}
}
