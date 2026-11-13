package com.mohymods.kingdomsmp.mixin;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=BrewingStandBlockEntity.class,remap=false)
public interface NativeBrewingAccess {@Accessor("brewTime") int kingdom$brewTime();@Accessor("fuel") int kingdom$fuel();}
