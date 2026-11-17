package com.mohymods.kingdomsmp.mixin;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Native shift-take passes a zero-count stack; its immutable item identity is still authoritative. */
@Mixin(value=ItemStack.class,remap=false)
public interface NativeItemIdentityAccessor {
    @Accessor("item") Item kingdom$nativeItem();
    @Accessor("components") net.minecraft.core.component.PatchedDataComponentMap kingdom$nativeComponents();
}
