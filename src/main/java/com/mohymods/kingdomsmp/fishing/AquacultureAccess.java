package com.mohymods.kingdomsmp.fishing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModList;
import java.util.*;

/** Optional exact-version boundary: core never links an absent Aquaculture class or guesses registered items. */
public final class AquacultureAccess {
    public static final String VERSION="2.7.21",ROD="aquaculture:iron_fishing_rod";
    public static final List<String> FISH=List.of("bluegill","smallmouth_bass","brown_trout","carp","catfish","perch");
    private AquacultureAccess() { }
    public static String unavailable() {
        var mods=ModList.get();if(mods==null||!mods.isLoaded("aquaculture"))return "Requires the installed Aquaculture 2 fishing mod.";
        String version=mods.getModContainerById("aquaculture").orElseThrow().getModInfo().getVersion().toString();
        if(!VERSION.equals(version))return "Aquaculture version "+version+" is not verified; required "+VERSION+".";
        for(var fish:FishingCatchBook.SPECIES){var id=ResourceLocation.parse("aquaculture:"+fish);if(!BuiltInRegistries.ITEM.containsKey(id)||!BuiltInRegistries.ITEM.get(id).getClass().getName().equals("com.teammetallurgy.aquaculture.item.FishItem"))return "Aquaculture fish registry does not match the supported version.";}
        var rod=ResourceLocation.parse(ROD);if(!BuiltInRegistries.ITEM.containsKey(rod)||!BuiltInRegistries.ITEM.get(rod).getClass().getName().equals("com.teammetallurgy.aquaculture.item.AquaFishingRodItem"))return "Aquaculture equipment registry does not match the supported version.";
        return "";
    }
    public static boolean available(){return unavailable().isEmpty();}
    public static void require(){String why=unavailable();if(!why.isEmpty())throw new IllegalStateException(why);}
    public static Item fish(String path){require();if(!FISH.contains(path))throw new IllegalArgumentException("Unknown verified fish species");return BuiltInRegistries.ITEM.get(ResourceLocation.parse("aquaculture:"+path));}
    public static Item rod(){require();return BuiltInRegistries.ITEM.get(ResourceLocation.parse(ROD));}
    /** Initially only a pristine, default empty rod is supported; attached equipment is never silently erased. */
    public static boolean plainRod(ItemStack stack){return !stack.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(ROD)&&available()&&ItemStack.isSameItemSameComponents(stack,new ItemStack(rod()));}
}
