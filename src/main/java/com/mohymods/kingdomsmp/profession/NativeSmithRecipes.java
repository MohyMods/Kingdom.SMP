package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
/** Master refined goods require their actual registered primary recipe, not ingot/block recycling. */
public final class NativeSmithRecipes {
 private NativeSmithRecipes(){}
 public static boolean guarded(Item item){return item==Items.NETHERITE_INGOT||item==Items.POLISHED_BASALT||item==Items.QUARTZ_BLOCK;}
 public static boolean valid(ServerPlayer p,Container container,ItemStack output){
  if(!(container instanceof CraftingContainer crafting)||!guarded(output.getItem()))return false;
  var recipe=p.server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,crafting.asCraftInput(),p.serverLevel()).orElse(null);if(recipe==null||!recipe.id().getNamespace().equals("minecraft"))return false;var actual=recipe.value().assemble(crafting.asCraftInput(),p.registryAccess());if(!ItemStack.matches(actual,output))return false;
  Map<Item,Integer> ingredients=new HashMap<>();for(int slot=0;slot<crafting.getContainerSize();slot++){var stack=crafting.getItem(slot);if(!stack.isEmpty())ingredients.merge(stack.getItem(),1,Integer::sum);}
  if(output.is(Items.NETHERITE_INGOT))return output.getCount()==1&&ingredients.equals(Map.of(Items.NETHERITE_SCRAP,4,Items.GOLD_INGOT,4));
  if(output.is(Items.POLISHED_BASALT))return output.getCount()==4&&ingredients.equals(Map.of(Items.BASALT,4));
  return output.getCount()==1&&ingredients.equals(Map.of(Items.QUARTZ,4));
 }
}
