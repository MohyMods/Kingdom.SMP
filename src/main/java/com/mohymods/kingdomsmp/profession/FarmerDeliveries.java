package com.mohymods.kingdomsmp.profession;
import net.minecraft.nbt.*;
import java.util.*;
/** Version-one approved food families; quantities are pinned by the server review, never a client item payload. */
public final class FarmerDeliveries {
 private FarmerDeliveries(){ }
 public static Set<String> family(String quest){return switch(quest){
  case "kingdom_smp:farmer/a07"->Set.of("minecraft:bread");
  case "kingdom_smp:farmer/j05"->Set.of("minecraft:egg","minecraft:milk_bucket");
  case "kingdom_smp:farmer/j06"->Set.of("minecraft:cake");
  case "kingdom_smp:farmer/m03"->Set.of("minecraft:bread","minecraft:cake","minecraft:pumpkin_pie","minecraft:beetroot_soup");
  default->throw new IllegalArgumentException("Not an approved Farmer food delivery");};}
 public static void validate(String quest,CompoundTag inputs){if(!inputs.getAllKeys().equals(family(quest)))throw new IllegalArgumentException("Food delivery differs from approved objective family");for(var key:inputs.getAllKeys())if(!inputs.contains(key,Tag.TAG_INT)||inputs.getInt(key)<1||inputs.getInt(key)>2304)throw new IllegalArgumentException("Invalid Farmer food quantity");}
 public static Map<String,Integer> containers(Map<String,Integer> inputs){var result=new HashMap<String,Integer>();if(inputs.containsKey("minecraft:milk_bucket"))result.put("minecraft:bucket",inputs.get("minecraft:milk_bucket"));if(inputs.containsKey("minecraft:beetroot_soup"))result.put("minecraft:bowl",inputs.get("minecraft:beetroot_soup"));return Map.copyOf(result);}
}
