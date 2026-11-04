package com.mohymods.kingdomsmp.mixin;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read-only exact1.21.1 native completed-recipe accounting, before vanilla clears it. */
@Mixin(value=AbstractFurnaceBlockEntity.class,remap=false)
public interface NativeFurnaceRecipesAccessor {
    @Accessor("recipesUsed") Object2IntOpenHashMap<ResourceLocation> kingdom$recipes();
}
