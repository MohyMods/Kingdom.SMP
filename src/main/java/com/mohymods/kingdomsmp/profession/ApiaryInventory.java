package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
/** Exact supported native species component only; renamed/enchanted/forged extra data never enters delivery. */
public final class ApiaryInventory {
 private ApiaryInventory(){}
 @SuppressWarnings("unchecked") public static String species(ItemStack stack){String type=ApiaryProduction.species(stack);if(!ApiaryEvidence.SPECIES.contains(type==null?"":type))return null;var component=(DataComponentType<ResourceLocation>)BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("productivebees:bee_type"));var plain=new ItemStack(stack.getItem());plain.set(component,ResourceLocation.parse(type));return ItemStack.isSameItemSameComponents(stack,plain)?type:null;}
 public record Plan(List<ItemStack> after,Map<String,Integer> consumed){}
 public static Plan plan(List<ItemStack> before,String quest,Map<String,Integer> goals){if(before.size()!=36||!ApiaryJobs.contains(quest))throw new IllegalArgumentException("Invalid apiary exchange");var after=new ArrayList<>(before.stream().map(ItemStack::copy).toList());var remaining=new HashMap<>(goals);var consumed=new HashMap<String,Integer>();for(var stack:after){String species=species(stack);if(species==null)continue;String key=ApiaryJobs.FOREST.equals(quest)?species:"combs";int take=Math.min(stack.getCount(),remaining.getOrDefault(key,0));if(take>0){stack.shrink(take);remaining.put(key,remaining.get(key)-take);consumed.merge(species,take,Math::addExact);}}if(remaining.values().stream().anyMatch(n->n>0))throw new IllegalStateException("Bring every required native iron/copper resource comb; extra item data and wrong species are refused. Nothing consumed.");return new Plan(List.copyOf(after),Map.copyOf(consumed));}
 public static Map<String,Integer> totals(String quest,Map<String,Integer> consumed){if(!ApiaryJobs.contains(quest)||consumed.isEmpty()||!ApiaryEvidence.SPECIES.containsAll(consumed.keySet()))throw new IllegalArgumentException("Unverified comb species");int total=0;for(int n:consumed.values()){if(n<1||n>2304)throw new IllegalArgumentException("Invalid comb quantity");total=Math.addExact(total,n);}if(total>2304)throw new IllegalArgumentException("Comb delivery exceeds capacity");if(ApiaryJobs.FOREST.equals(quest)){if(!consumed.keySet().equals(ApiaryEvidence.SPECIES))throw new IllegalArgumentException("Both documented apiary species required");return Map.copyOf(consumed);}return Map.of("combs",total);}
}
