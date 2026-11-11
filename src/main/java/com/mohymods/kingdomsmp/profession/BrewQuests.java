package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
/** Validated vanilla native brewing routes; modded and later-band routes stay unavailable. */
public final class BrewQuests {
 public record Recipe(String quest,String input,String reagent,String output){}
 private static final List<Recipe> RECIPES=List.of(new Recipe("kingdom_smp:alchemist/a02","water","nether_wart","awkward"),new Recipe("kingdom_smp:alchemist/a03","awkward","glistering_melon_slice","healing"),new Recipe("kingdom_smp:alchemist/a04","awkward","sugar","swiftness"),new Recipe("kingdom_smp:alchemist/a05","awkward","blaze_powder","strength"),new Recipe("kingdom_smp:alchemist/a06","awkward","magma_cream","fire_resistance"),new Recipe("kingdom_smp:alchemist/j01","fire_resistance","redstone","long_fire_resistance"),new Recipe("kingdom_smp:alchemist/j02","healing","glowstone_dust","strong_healing"));
 private BrewQuests(){}
 public static Set<String> ids(){return RECIPES.stream().map(Recipe::quest).collect(java.util.stream.Collectors.toUnmodifiableSet());}
 public static boolean contains(String id){return id!=null&&RECIPES.stream().anyMatch(r->r.quest().equals(id));}
 public static Recipe get(String id){return RECIPES.stream().filter(r->r.quest().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unvalidated brewing objective"));}
 public static Optional<Recipe> route(String input,String reagent,String output){return RECIPES.stream().filter(r->("minecraft:"+r.input()).equals(input)&&("minecraft:"+r.reagent()).equals(reagent)&&("minecraft:"+r.output()).equals(output)).findFirst();}
 public static void validateEncoded(String quest,CompoundTag stack){var contents=stack.getCompound("components").getCompound("minecraft:potion_contents");if(!stack.getString("id").equals("minecraft:potion")||stack.getInt("count")!=1||!contents.getAllKeys().equals(Set.of("potion"))||!contents.getString("potion").equals("minecraft:"+get(quest).output()))throw new IllegalArgumentException("Wrong native commissioned potion");}
}
