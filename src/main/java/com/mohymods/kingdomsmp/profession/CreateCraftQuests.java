package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
/** Exact Create6.0.10 primary handcraft recipes. Decompression/recycling is never new production. */
public final class CreateCraftQuests {
 public static final String ALLOY="kingdom_smp:engineer/a01",COMPONENTS="kingdom_smp:engineer/a02";
 public static final Map<String,String> METRICS=Map.of("create:andesite_alloy","andesite_alloy","create:shaft","shafts","create:cogwheel","small_cogs","create:large_cogwheel","large_cogs");
 private static final Map<String,Set<String>> RECIPES=Map.of("create:andesite_alloy",Set.of("create:crafting/materials/andesite_alloy","create:crafting/materials/andesite_alloy_from_zinc"),"create:shaft",Set.of("create:crafting/kinetics/shaft"),"create:cogwheel",Set.of("create:crafting/kinetics/cogwheel"),"create:large_cogwheel",Set.of("create:crafting/kinetics/large_cogwheel","create:crafting/kinetics/large_cogwheel_from_little"));
 private CreateCraftQuests(){}
 public static boolean contains(String quest){return ALLOY.equals(quest)||COMPONENTS.equals(quest);}
 public static boolean supported(){var mods=ModList.get();return mods!=null&&mods.getModContainerById("create").map(c->c.getModInfo().getVersion().toString().equals("6.0.10")).orElse(false)&&METRICS.keySet().stream().allMatch(id->BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)));}
 public static boolean valid(ServerPlayer p,Container container,ItemStack output){
  if(!supported()||!(container instanceof CraftingContainer crafting))return false;String id=BuiltInRegistries.ITEM.getKey(output.getItem()).toString();if(!RECIPES.containsKey(id))return false;
  var recipe=p.server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,crafting.asCraftInput(),p.serverLevel()).orElse(null);if(recipe==null||!RECIPES.get(id).contains(recipe.id().toString())||!ItemStack.matches(recipe.value().assemble(crafting.asCraftInput(),p.registryAccess()),output))return false;
  Map<String,Integer> inputs=new HashMap<>();for(int slot=0;slot<crafting.getContainerSize();slot++){var stack=crafting.getItem(slot);if(!stack.isEmpty()){String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();if(stack.is(net.minecraft.tags.ItemTags.PLANKS))item="planks";inputs.merge(item,1,Integer::sum);}}
  return primary(id,output.getCount(),inputs);
 }
 static boolean primary(String item,int amount,Map<String,Integer> inputs){return switch(item){
  case "create:andesite_alloy"->amount==1&&(inputs.equals(Map.of("minecraft:andesite",2,"minecraft:iron_nugget",2))||inputs.equals(Map.of("minecraft:andesite",2,"create:zinc_nugget",2)));
  case "create:shaft"->amount==8&&inputs.equals(Map.of("create:andesite_alloy",2));
  case "create:cogwheel"->amount==1&&inputs.equals(Map.of("create:shaft",1,"planks",1));
  case "create:large_cogwheel"->amount==1&&(inputs.equals(Map.of("create:shaft",1,"planks",2))||inputs.equals(Map.of("create:cogwheel",1,"planks",1)));
  default->false;
 };}
}
