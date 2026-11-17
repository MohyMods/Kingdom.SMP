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
  for(var role:Profession.values())for(var cycle:List.of("daily","weekly")){boolean daily=cycle.equals("daily");int quantity;long reward;
   switch(role){case FARMER,BUILDER->{quantity=daily?64:512;reward=daily?80:800;}case LUMBERJACK->{quantity=daily?32:256;reward=daily?72:768;}case BLACKSMITH,MINER->{quantity=daily?16:128;reward=daily?160:1280;}case MERCHANT,LIBRARIAN->{quantity=daily?32:256;reward=daily?16:128;}case ENGINEER,MECHANIC,MAGE->{quantity=daily?16:128;reward=daily?80:640;}case ADVENTURER->{quantity=daily?32:256;reward=daily?40:320;}case ALCHEMIST->{quantity=daily?16:128;reward=daily?16:128;}default->throw new IllegalArgumentException("Missing profession contract defaults");}
   var row=new JsonObject();row.addProperty("quantity",quantity);row.addProperty("reward",reward);rows.add(role.id()+"/"+cycle,row);
  }
  if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
  try{var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!data.keySet().equals(Set.of("schema","dailyRewardPool","weeklyRewardPool","deliveries"))||n(data,"schema")!=1)throw new IllegalArgumentException("Invalid contract schema");long dailyPool=n(data,"dailyRewardPool"),weeklyPool=n(data,"weeklyRewardPool");if(dailyPool<1||dailyPool>1_000_000_000||weeklyPool<1||weeklyPool>1_000_000_000)throw new IllegalArgumentException("Invalid contract pool");var configured=data.getAsJsonObject("deliveries");if(!rows.keySet().containsAll(configured.keySet())||!configured.keySet().containsAll(Set.of("farmer/daily","farmer/weekly","builder/daily","builder/weekly","lumberjack/daily","lumberjack/weekly")))throw new IllegalArgumentException("Unknown or missing contract definitions");Map<String,Terms> terms=new HashMap<>();
   for(var id:rows.keySet()){var row=configured.has(id)?configured.getAsJsonObject(id):rows.getAsJsonObject(id);if(!row.keySet().equals(Set.of("quantity","reward")))throw new IllegalArgumentException("Unknown contract option");var parts=id.split("/");var role=Profession.byId(parts[0]);long count=n(row,"quantity"),reward=n(row,"reward"),pool=parts[1].equals("daily")?dailyPool:weeklyPool;if(count<1||count>2304||reward<1||reward>1_000_000||reward>pool)throw new IllegalArgumentException("Invalid contract quantity/reward");Item item=switch(role){case FARMER->Items.WHEAT;case BUILDER->Items.COBBLESTONE;case LUMBERJACK->Items.OAK_LOG;case BLACKSMITH,MINER->Items.IRON_INGOT;case MERCHANT,LIBRARIAN->Items.PAPER;case ENGINEER->Items.REDSTONE;case MECHANIC->Items.COPPER_INGOT;case MAGE->Items.LAPIS_LAZULI;case ADVENTURER->Items.STRING;case ALCHEMIST->Items.GLASS_BOTTLE;};terms.put(id,new Terms(role,parts[1],item,(int)count,reward,pool));}
   return new ContractRules(terms);
  }catch(RuntimeException e){throw new IOException("Malformed bounded schema1contract rules",e);}
 }
 private static long n(JsonObject data,String key){var value=data.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return value.getAsBigDecimal().longValueExact();}
}
