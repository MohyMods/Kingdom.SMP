package com.mohymods.kingdomsmp.equipment;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Additive configuration; no rank/claim/receipt bypass switches. */
public record MasterGearRules(Map<MasterGear,Long> prices,int treeLimit,int veinLimit,int surveyRadius,int surveyCooldown,int wandLimit,int extractionXp,int lapisCost,int codexMaxCost,int codexMaxLevel) {
 private static final Map<MinecraftServer,MasterGearRules> CACHE=new WeakHashMap<>();
 public MasterGearRules {prices=Map.copyOf(prices);}
 public static MasterGearRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-master-equipment.json"));}catch(IOException e){throw new IllegalStateException("Master equipment settings require recovery",e);}});}
 public static MasterGearRules load(Path path)throws IOException{
  var defaults=new LinkedHashMap<String,Long>();for(var gear:MasterGear.values())defaults.put(gear.path()+"_price",gear.base());defaults.put("tree_limit",256L);defaults.put("vein_limit",64L);defaults.put("survey_radius",6L);defaults.put("survey_cooldown_ticks",200L);defaults.put("wand_limit",128L);defaults.put("extraction_xp_levels",8L);defaults.put("lapis_cost",4L);defaults.put("codex_max_cost",80L);defaults.put("codex_max_level",10L);
  if(!Files.exists(path)){var json=new JsonObject();json.addProperty("schema",1);defaults.forEach(json::addProperty);Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json),StandardOpenOption.CREATE_NEW);}
  try{var json=JsonParser.parseString(Files.readString(path)).getAsJsonObject();var keys=new HashSet<>(defaults.keySet());keys.add("schema");if(!json.keySet().equals(keys)||!json.get("schema").isJsonPrimitive()||!json.get("schema").getAsJsonPrimitive().isNumber()||json.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Unknown settings");var values=new HashMap<String,Long>();for(var key:defaults.keySet()){if(!json.get(key).isJsonPrimitive()||!json.get(key).getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Numeric setting required");long n=json.get(key).getAsBigDecimal().longValueExact();if(n<1||n>(key.endsWith("_price")?1_000_000_000: key.equals("survey_radius")?8:key.equals("codex_max_level")?10:key.equals("survey_cooldown_ticks")?12000:1024))throw new IllegalArgumentException("Out of bounds: "+key);values.put(key,n);}var prices=new EnumMap<MasterGear,Long>(MasterGear.class);for(var gear:MasterGear.values())prices.put(gear,values.get(gear.path()+"_price"));return new MasterGearRules(prices,values.get("tree_limit").intValue(),values.get("vein_limit").intValue(),values.get("survey_radius").intValue(),values.get("survey_cooldown_ticks").intValue(),values.get("wand_limit").intValue(),values.get("extraction_xp_levels").intValue(),values.get("lapis_cost").intValue(),values.get("codex_max_cost").intValue(),values.get("codex_max_level").intValue());}catch(RuntimeException e){throw new IOException("Invalid bounded Master equipment config",e);}
 }
}
