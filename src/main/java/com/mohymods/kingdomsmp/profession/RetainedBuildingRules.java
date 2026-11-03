package com.mohymods.kingdomsmp.profession;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Bounded ordinary quantity defaults, not authority to change quest meanings or tier gates. */
public final class RetainedBuildingRules {
    private static final Map<MinecraftServer,RetainedBuildingRules> CACHE=new WeakHashMap<>();
    private final Map<String,Integer> goals;
    private RetainedBuildingRules(Map<String,Integer> goals){this.goals=Map.copyOf(goals);}
    public int goal(String id){RetainedBuildingQuests.get(id);return goals.get(id);}
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static RetainedBuildingRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-retained-buildings.json"));}catch(IOException e){throw new IllegalStateException("Retained-building rules invalid; no objective awarded",e);}});}
    public static RetainedBuildingRules load(Path path)throws IOException{
        var defaults=new JsonObject();defaults.addProperty("schema",1);var quantities=new JsonObject();defaults.add("goals",quantities);
        RetainedBuildingQuests.ids().stream().sorted().forEach(id->quantities.addProperty(id,RetainedBuildingQuests.goal(id)));
        if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
        try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(!root.keySet().equals(Set.of("schema","goals"))||number(root,"schema")!=1||!quantities.keySet().containsAll(root.getAsJsonObject("goals").keySet())||!root.getAsJsonObject("goals").keySet().containsAll(Set.of("kingdom_smp:builder/a01","kingdom_smp:builder/a02","kingdom_smp:builder/a03","kingdom_smp:builder/a06")))throw new IllegalArgumentException("Invalid retained rules");
            var rows=root.getAsJsonObject("goals");Map<String,Integer> result=new HashMap<>();for(var id:quantities.keySet()){int n=rows.has(id)?number(rows,id):RetainedBuildingQuests.goal(id);if(n<1||n>2048||id.endsWith("j02")&&n<2||id.endsWith("m02")&&n<3)throw new IllegalArgumentException("Retained quantities must be1..2048");result.put(id,n);}return new RetainedBuildingRules(result);
        }catch(RuntimeException e){throw new IOException("Malformed retained-building goals",e);}
    }
    private static int number(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer goal required");return value.getAsBigDecimal().intValueExact();}
}
