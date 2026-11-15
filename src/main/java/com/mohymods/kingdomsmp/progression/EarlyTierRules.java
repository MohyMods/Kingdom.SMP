package com.mohymods.kingdomsmp.progression;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Versioned alpha balancing defaults. No End or pre-unlock Nether materials. */
public final class EarlyTierRules {
    private EarlyTierRules() { }
    public static Map<String,Integer> defaults(int tier) {
        return switch(tier) {
            case 3 -> Map.of("minecraft:iron_ingot",256,"minecraft:copper_ingot",256,"minecraft:stone_bricks",512,"minecraft:glass",256,"minecraft:bread",128,"minecraft:redstone",128,"minecraft:diamond",8,"minecraft:gold_ingot",32);
            case 4 -> Map.of("minecraft:diamond",16,"minecraft:gold_ingot",96,"minecraft:redstone",256,"minecraft:lapis_lazuli",128,"minecraft:book",48,"minecraft:obsidian",64,"minecraft:cooked_beef",128,"minecraft:stone_bricks",768);
            case 5 -> Map.of("minecraft:nether_bricks",256,"minecraft:quartz",128,"minecraft:blaze_rod",16,"minecraft:magma_cream",32,"minecraft:glowstone_dust",96,"minecraft:iron_ingot",512,"minecraft:copper_ingot",512,"minecraft:glass",384);
            default -> throw new IllegalArgumentException("Alpha milestones only cover Tier 3 through 5");
        };
    }
    public static Map<String,Integer> load(Path file,int tier)throws IOException {
        var defaults=defaults(tier);
        if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS)) {
            var root=new JsonObject();root.addProperty("schema",1);root.addProperty("tier",tier);
            root.addProperty("status","0.13.0-alpha configurable balance defaults; not historical finalized costs");
            var rows=new JsonObject();defaults.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->rows.addProperty(e.getKey(),e.getValue()));root.add("quantities",rows);
            Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);
        }
        try {
            if(Files.isSymbolicLink(file)||Files.size(file)>16384)throw new IllegalArgumentException("Unsafe milestone file");
            var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(root.get("schema").getAsBigDecimal().intValueExact()!=1||root.get("tier").getAsBigDecimal().intValueExact()!=tier)throw new IllegalArgumentException("Wrong milestone schema/tier");
            var rows=root.getAsJsonObject("quantities");if(!rows.keySet().equals(defaults.keySet()))throw new IllegalArgumentException("Expected exact accessible resource families");
            Map<String,Integer> result=new HashMap<>();for(var key:defaults.keySet()){var n=rows.get(key);if(!n.isJsonPrimitive()||!n.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Non-numeric quantity");int amount=n.getAsBigDecimal().intValueExact();if(amount<1||amount>1_000_000)throw new IllegalArgumentException("Unbounded quantity");result.put(key,amount);}return Map.copyOf(result);
        }catch(RuntimeException e){throw new IOException("Invalid Tier "+tier+" configuration; existing credit is never reinterpreted",e);}
    }
}
