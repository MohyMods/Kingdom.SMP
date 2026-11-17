package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
/** Overworld-discovered trim variants with native ingredient dependencies. Eye's End-stone recipe remains Tier12. */
public final class TrimCopies {
 public static final String QUEST="kingdom_smp:blacksmith/j06";
 public record Variant(String id,Set<String> bases,int tier){public Variant{bases=Set.copyOf(bases);}}
 private static final Map<String,Variant> VARIANTS;
 static{var rows=new LinkedHashMap<String,Variant>();add(rows,"sentry","cobblestone",4);add(rows,"coast","cobblestone",4);add(rows,"dune","sandstone",4);add(rows,"wild","mossy_cobblestone",4);add(rows,"vex","cobblestone",4);add(rows,"ward","cobbled_deepslate",4);add(rows,"silence","cobbled_deepslate",4);add(rows,"tide","prismarine",4);for(String name:List.of("wayfinder","shaper","raiser","host"))add(rows,name,"terracotta",4);add(rows,"flow","breeze_rod",4);String bolt=id("bolt");rows.put(bolt,new Variant(bolt,Set.of("minecraft:copper_block","minecraft:waxed_copper_block"),4));add(rows,"eye","end_stone",12);VARIANTS=Map.copyOf(rows);}
 private static String id(String name){return "minecraft:"+name+"_armor_trim_smithing_template";}private static void add(Map<String,Variant> rows,String name,String base,int tier){String id=id(name);rows.put(id,new Variant(id,Set.of("minecraft:"+base),tier));}
 private TrimCopies(){}
 public static boolean contains(String item){return VARIANTS.containsKey(item);}
 public static Map<String,String> metrics(){var result=new HashMap<String,String>();for(var id:VARIANTS.keySet())result.put(id,"trim_"+id.substring(10,id.indexOf("_armor_trim")));return Map.copyOf(result);}
 public static boolean allowed(ServerPlayer p,String item){var d=VARIANTS.get(item);return d!=null&&com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()>=d.tier();}
 public static boolean nativeDuplication(ServerPlayer p,String item,net.minecraft.world.Container input,ItemStack output){
  if(!allowed(p,item)||!(input instanceof CraftingContainer grid)||output.getCount()<2||output.getCount()%2!=0)return false;var holder=p.server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),p.serverLevel()).orElse(null);if(holder==null)return false;var recipe=holder.value();var result=recipe.getResultItem(p.registryAccess());if(!result.is(output.getItem())||result.getCount()!=2||recipe.getIngredients().size()!=9)return false;
  int diamond=0,source=0,base=0;for(var ingredient:recipe.getIngredients()){Set<String> ids=new HashSet<>();for(var stack:ingredient.getItems())ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());if(ids.equals(Set.of("minecraft:diamond")))diamond++;else if(ids.equals(Set.of(item)))source++;else if(!ids.isEmpty()&&VARIANTS.get(item).bases().containsAll(ids))base++;else return false;}return diamond==7&&source==1&&base==1;
 }
 public static Map<String,Integer> totals(Map<String,Integer> inputs){if(inputs.size()!=1)throw new IllegalArgumentException("Two copies must belong to one template kind");var row=inputs.entrySet().iterator().next();if(!contains(row.getKey())||row.getValue()<1||row.getValue()>2304)throw new IllegalArgumentException("Unverified template or quantity");return Map.of("copies",row.getValue());}
 public static boolean proof(ServerPlayer p,String item,int quantity){return allowed(p,item)&&CraftQuestEvidence.count(p,Profession.BLACKSMITH,com.mohymods.kingdomsmp.profession.ProfessionRank.JOURNEYMAN,item)>=quantity;}
 public static Map<String,Integer> resolve(ServerPlayer p,List<ItemStack> inventory,int quantity){for(var id:VARIANTS.keySet().stream().sorted().toList()){if(!proof(p,id,quantity))continue;int count=0;var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));for(var stack:inventory)if(ItemStack.isSameItemSameComponents(stack,new ItemStack(item)))count+=stack.getCount();if(count>=quantity)return Map.of(id,quantity);}throw new IllegalStateException("Craft and bring matching plain copies of one eligible template; mixed kinds cannot substitute");}
 public static boolean ready(ServerPlayer p,int quantity){return VARIANTS.keySet().stream().anyMatch(id->proof(p,id,quantity));}
}
