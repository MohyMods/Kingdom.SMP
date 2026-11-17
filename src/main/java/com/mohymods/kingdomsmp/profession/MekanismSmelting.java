package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import net.neoforged.fml.ModList;
/** Exact installed10.7.19 native vanilla cooking recipes, independent of optional Mekanism Java linkage. */
public final class MekanismSmelting {
 public static final String QUEST="kingdom_smp:mechanic/a01",OUTPUT="mekanism:ingot_osmium",METRIC="smelt_osmium";
 public static final Set<String> INPUTS=Set.of("mekanism:raw_osmium","mekanism:osmium_ore","mekanism:deepslate_osmium_ore");
 public static final Set<String> RECIPES=Set.of("mekanism:processing/osmium/ingot/from_raw_smelting","mekanism:processing/osmium/ingot/from_raw_blasting","mekanism:processing/osmium/ingot/from_ore_smelting","mekanism:processing/osmium/ingot/from_ore_blasting");
 private MekanismSmelting(){}
 public static boolean supported(){var mods=ModList.get();return mods!=null&&mods.getModContainerById("mekanism").map(c->c.getModInfo().getVersion().toString().equals("10.7.19")).orElse(false);}
 public static boolean output(Item item){return OUTPUT.equals(BuiltInRegistries.ITEM.getKey(item).toString());}
 public static boolean recipe(ServerPlayer p,ResourceLocation id,Item result){
  if(!supported()||!output(result)||!RECIPES.contains(id.toString()))return false;var holder=p.server.getRecipeManager().byKey(id).orElse(null);if(holder==null||!(holder.value() instanceof SmeltingRecipe||holder.value() instanceof BlastingRecipe)||!(holder.value() instanceof AbstractCookingRecipe cook)||!cook.getResultItem(p.registryAccess()).is(result)||cook.getResultItem(p.registryAccess()).getCount()!=1||cook.getIngredients().size()!=1)return false;
  var inputs=cook.getIngredients().getFirst().getItems();return inputs.length>0&&inputs.length<=3&&Arrays.stream(inputs).allMatch(i->INPUTS.contains(BuiltInRegistries.ITEM.getKey(i.getItem()).toString()));
 }
}
