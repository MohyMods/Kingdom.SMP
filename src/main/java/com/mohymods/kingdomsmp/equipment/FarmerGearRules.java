package com.mohymods.kingdomsmp.equipment;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
/** Numerical shop defaults only. No option bypasses rank, active profession, local NPC or purchase escalation. */
public final class FarmerGearRules {
 private final Map<FarmerGear,Long> prices;
 private static final Map<MinecraftServer,FarmerGearRules> CACHE=new WeakHashMap<>();
 private FarmerGearRules(Map<FarmerGear,Long> prices){this.prices=Map.copyOf(prices);}
 public long base(FarmerGear gear){return prices.get(gear);}
 public static void clear(MinecraftServer server){CACHE.remove(server);}
 public static FarmerGearRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-farmer-equipment.json"));}catch(IOException e){throw new IllegalStateException("Farmer equipment shop configuration invalid",e);}});}
 public static FarmerGearRules load(Path path)throws IOException{
  var data=new JsonObject();data.addProperty("schema",1);var prices=new JsonObject();for(var gear:FarmerGear.values())prices.addProperty(gear.id(),gear.base());data.add("basePrices",prices);if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(data),StandardOpenOption.CREATE_NEW);}
  try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!root.keySet().equals(Set.of("schema","basePrices"))||number(root,"schema")!=1||!root.getAsJsonObject("basePrices").keySet().equals(prices.keySet()))throw new IllegalArgumentException("Unknown equipment price settings");Map<FarmerGear,Long> result=new EnumMap<>(FarmerGear.class);for(var gear:FarmerGear.values()){long value=number(root.getAsJsonObject("basePrices"),gear.id());if(value<1000||value>1_000_000_000)throw new IllegalArgumentException("Equipment base price out of bounds");result.put(gear,value);}return new FarmerGearRules(result);}catch(RuntimeException e){throw new IOException("Invalid bounded Farmer equipment schema",e);}
 }
 private static long number(JsonObject data,String key){var value=data.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return value.getAsBigDecimal().longValueExact();}
}
