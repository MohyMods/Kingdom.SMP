package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
/** Repeatable economic deliveries, distinct from approval-gated profession advancement quests. */
public final class ContractRules {
 public record Terms(Profession profession,String cycle,Item item,int quantity,long reward,long pool){ }
 private static final Map<MinecraftServer,ContractRules> CACHE=new WeakHashMap<>();
 private final Map<String,Terms> terms;
 private ContractRules(Map<String,Terms> terms){this.terms=Map.copyOf(terms);}
 public List<Terms> all(){return terms.values().stream().sorted(Comparator.comparing((Terms t)->t.profession().id()).thenComparing(Terms::cycle)).toList();}
 public Terms get(String id){var value=terms.get(id);if(value==null)throw new IllegalArgumentException("Unknown profession delivery contract");return value;}
 public static String id(Terms t){return t.profession().id()+"/"+t.cycle();}
 public static long period(String cycle){long day=Math.floorDiv(System.currentTimeMillis(),86_400_000L);return switch(cycle){case "daily"->day;case "weekly"->Math.floorDiv(day+3,7);default->throw new IllegalArgumentException("Unknown contract cycle");};}
 public static void clear(MinecraftServer server){CACHE.remove(server);}
 public static ContractRules get(MinecraftServer server){var saved=CACHE.get(server);if(saved!=null)return saved;try{var value=load(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-contracts.json"));CACHE.put(server,value);return value;}catch(IOException e){throw new IllegalStateException("Contract definitions require bounded valid server configuration.",e);}}
 public static ContractRules load(Path path)throws IOException{
  var defaults=new JsonObject();defaults.addProperty("schema",1);defaults.addProperty("dailyRewardPool",2000);defaults.addProperty("weeklyRewardPool",10000);var rows=new JsonObject();defaults.add("deliveries",rows);
  for(var role:List.of(Profession.FARMER,Profession.BUILDER,Profession.LUMBERJACK,Profession.BLACKSMITH))for(var cycle:List.of("daily","weekly")){boolean daily=cycle.equals("daily"),logs=role==Profession.LUMBERJACK;boolean smith=role==Profession.BLACKSMITH;var row=new JsonObject();row.addProperty("quantity",smith?(daily?16:128):logs?(daily?32:256):(daily?64:512));row.addProperty("reward",smith?(daily?160:1280):logs?(daily?72:768):(daily?80:800));rows.add(role.id()+"/"+cycle,row);}
  if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
  try{var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!data.keySet().equals(Set.of("schema","dailyRewardPool","weeklyRewardPool","deliveries"))||n(data,"schema")!=1)throw new IllegalArgumentException("Invalid contract schema");long dailyPool=n(data,"dailyRewardPool"),weeklyPool=n(data,"weeklyRewardPool");if(dailyPool<1||dailyPool>1_000_000_000||weeklyPool<1||weeklyPool>1_000_000_000)throw new IllegalArgumentException("Invalid contract pool");var configured=data.getAsJsonObject("deliveries");if(!rows.keySet().containsAll(configured.keySet())||!configured.keySet().containsAll(rows.keySet().stream().filter(k->!k.startsWith("blacksmith/")).toList()))throw new IllegalArgumentException("Unknown or missing contract definitions");Map<String,Terms> terms=new HashMap<>();
   for(var id:rows.keySet()){var row=configured.has(id)?configured.getAsJsonObject(id):rows.getAsJsonObject(id);if(!row.keySet().equals(Set.of("quantity","reward")))throw new IllegalArgumentException("Unknown contract option");var parts=id.split("/");var role=Profession.byId(parts[0]);long count=n(row,"quantity"),reward=n(row,"reward"),pool=parts[1].equals("daily")?dailyPool:weeklyPool;if(count<1||count>2304||reward<1||reward>1_000_000||reward>pool)throw new IllegalArgumentException("Invalid contract quantity/reward");Item item=switch(role){case FARMER->Items.WHEAT;case BUILDER->Items.COBBLESTONE;case LUMBERJACK->Items.OAK_LOG;case BLACKSMITH->Items.IRON_INGOT;default->throw new IllegalArgumentException("No implemented contract for profession");};terms.put(id,new Terms(role,parts[1],item,(int)count,reward,pool));}
   return new ContractRules(terms);
  }catch(RuntimeException e){throw new IOException("Malformed bounded schema1contract rules",e);}
 }
 private static long n(JsonObject data,String key){var value=data.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return value.getAsBigDecimal().longValueExact();}
}
