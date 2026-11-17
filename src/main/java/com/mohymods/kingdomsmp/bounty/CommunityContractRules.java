package com.mohymods.kingdomsmp.bounty;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
/** Configurable version-one food provisions. Fixed Tier3 Bounty Hall gate is outside the numerical file. */
public final class CommunityContractRules {
 public record Terms(String id,String title,String item,int goal,int unitReward,long budget){ }
 private final List<Terms> terms;
 private static final Map<MinecraftServer,CommunityContractRules> CACHE=new WeakHashMap<>();
 private CommunityContractRules(List<Terms> terms){this.terms=List.copyOf(terms);}
 public List<Terms> all(){return terms;}
 public Terms get(String id){return terms.stream().filter(t->t.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown community food contract"));}
 public static void clear(MinecraftServer s){CACHE.remove(s);}
 public static CommunityContractRules get(MinecraftServer s){return CACHE.computeIfAbsent(s,server->{try{return load(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-community-contracts.json"));}catch(IOException e){throw new IllegalStateException("Invalid community contract configuration",e);}});}
 public static CommunityContractRules load(Path path)throws IOException{
  var json=new JsonObject();json.addProperty("schema",1);json.addProperty("weeklyRewardPool",4000);var projects=new JsonObject();for(var row:List.of(new Terms("grain","Community granary","minecraft:wheat",1024,1,4000),new Terms("bread","Community bread ovens","minecraft:bread",256,3,4000),new Terms("roots","Community root cellar","minecraft:carrot",512,1,4000))){var data=new JsonObject();data.addProperty("goal",row.goal());data.addProperty("coinsPerItem",row.unitReward());projects.add(row.id(),data);}json.add("projects",projects);
  if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json),StandardOpenOption.CREATE_NEW);}
  try{var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!data.keySet().equals(Set.of("schema","weeklyRewardPool","projects"))||number(data,"schema")!=1||!data.getAsJsonObject("projects").keySet().equals(projects.keySet()))throw new IllegalArgumentException("Invalid community schema");long pool=number(data,"weeklyRewardPool");if(pool<1||pool>1_000_000_000)throw new IllegalArgumentException("Community pool out of bounds");List<Terms> terms=new ArrayList<>();long requiredPool=0;for(var id:List.of("grain","bread","roots")){var row=data.getAsJsonObject("projects").getAsJsonObject(id);if(!row.keySet().equals(Set.of("goal","coinsPerItem")))throw new IllegalArgumentException("Unknown community term");long goal=number(row,"goal"),unit=number(row,"coinsPerItem");if(goal<10||goal>65536||unit<1||unit>100)throw new IllegalArgumentException("Community terms out of bounds");requiredPool=Math.addExact(requiredPool,Math.multiplyExact(goal,unit));terms.add(new Terms("kingdom_smp:community/"+id,switch(id){case "grain"->"Community granary";case "bread"->"Community bread ovens";default->"Community root cellar";},switch(id){case "grain"->"minecraft:wheat";case "bread"->"minecraft:bread";default->"minecraft:carrot";},(int)goal,(int)unit,pool));}if(requiredPool>pool)throw new IllegalArgumentException("Weekly pool must fund all three community project goals; avoid a progression deadlock");return new CommunityContractRules(terms);}catch(RuntimeException e){throw new IOException("Invalid community schema-one quantities",e);}
 }
 private static long number(JsonObject object,String key){var v=object.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return v.getAsBigDecimal().longValueExact();}
}
