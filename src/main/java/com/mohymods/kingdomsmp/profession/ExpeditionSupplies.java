package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
/** Vanilla primary cooked provisions; arbitrary edible items and recycled inventory do not certify cooking. */
public final class ExpeditionSupplies {
 public static final String QUEST="kingdom_smp:adventurer/a05",FOOD="cooked_food",METRIC="smelt_food";
 public static final Map<String,String> RAW=Map.of("minecraft:cooked_beef","minecraft:beef","minecraft:cooked_porkchop","minecraft:porkchop","minecraft:cooked_chicken","minecraft:chicken","minecraft:cooked_mutton","minecraft:mutton","minecraft:cooked_rabbit","minecraft:rabbit","minecraft:cooked_cod","minecraft:cod","minecraft:cooked_salmon","minecraft:salmon","minecraft:baked_potato","minecraft:potato");
 private ExpeditionSupplies(){}
 public static boolean food(Item item){return RAW.containsKey(BuiltInRegistries.ITEM.getKey(item).toString());}
 public static Map<String,String> metrics(){var m=new HashMap<String,String>();RAW.keySet().forEach(id->m.put(id,METRIC));m.put("minecraft:torch","torches");m.put("minecraft:compass","compass");return Map.copyOf(m);}
 public static String category(String item){return RAW.containsKey(item)?FOOD:Set.of("minecraft:torch","minecraft:compass").contains(item)?item:null;}
 public static String metric(String category){return category.equals(FOOD)?METRIC:metrics().get(category);}
}
