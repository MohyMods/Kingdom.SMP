package com.mohymods.kingdomsmp.profession;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Strict per-world balancing quantities; canonical deliveries pin the reviewed inputs for replay. */
public final class CraftDeliveryRules {
    private static final Map<MinecraftServer,CraftDeliveryRules> CACHE=new WeakHashMap<>();
    private final Map<String,Map<String,Integer>> inputs;
    private CraftDeliveryRules(Map<String,Map<String,Integer>> inputs){this.inputs=Map.copyOf(inputs);}
    public Map<String,Integer> inputs(String id){CraftDeliveryQuests.get(id);return inputs.get(id);}
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static CraftDeliveryRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{
        try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-craft-deliveries.json"));}
        catch(IOException e){throw new IllegalStateException("Craft delivery configuration invalid; nothing consumed",e);}
    });}
    public static CraftDeliveryRules load(Path path)throws IOException{
        var defaults=new JsonObject();defaults.addProperty("schema",1);var rows=new JsonObject();defaults.add("deliveries",rows);
        for(var id:CraftDeliveryQuests.catalog().definitions().keySet()){
            var row=new JsonObject();CraftDeliveryQuests.defaults(id).forEach(row::addProperty);rows.add(id,row);
        }
        if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
        try{
            var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(!data.keySet().equals(Set.of("schema","deliveries"))||number(data,"schema")!=1)throw new IllegalArgumentException("Invalid craft delivery schema");
            var configured=data.getAsJsonObject("deliveries");if(!rows.keySet().containsAll(configured.keySet())||!configured.has(CraftDeliveryQuests.MASONRY))throw new IllegalArgumentException("Unknown or missing delivery objective");
            Map<String,Map<String,Integer>> result=new HashMap<>();
            for(var id:rows.keySet()){
                var row=configured.has(id)?configured.getAsJsonObject(id):rows.getAsJsonObject(id);var counts=new HashMap<String,Integer>();var tag=new CompoundTag();
                for(var item:row.keySet()){int count=number(row,item);tag.putInt(item,count);counts.put(item,count);}
                CraftDeliveryQuests.validateGoals(id,tag);result.put(id,Map.copyOf(counts));
            }
            return new CraftDeliveryRules(result);
        }catch(RuntimeException e){throw new IOException("Malformed bounded craft-delivery configuration",e);}
    }
    private static int number(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Numeric quantity required");return value.getAsBigDecimal().intValueExact();}
}
