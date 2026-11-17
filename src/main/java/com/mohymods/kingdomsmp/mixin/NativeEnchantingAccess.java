package com.mohymods.kingdomsmp.mixin;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=EnchantmentMenu.class,remap=false)
public interface NativeEnchantingAccess {@Accessor("access") ContainerLevelAccess kingdom$access();}
