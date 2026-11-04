package com.mohymods.kingdomsmp.mixin;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ItemCombinerMenu.class,remap=false)
public interface NativeCombinerAccess {@Accessor("access") ContainerLevelAccess kingdom$access();}
