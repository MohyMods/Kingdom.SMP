package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
public final class RepairQuestRules {
 private static final Map<MinecraftServer,Goals> CACHE=new WeakHashMap<>();private RepairQuestRules(){}
 public static void clear(MinecraftServer server){CACHE.remove(server);}
 public record Goals(int ordinary,int enchanted,int tools,int upgrades){}
 public static int upgrades(MinecraftServer server){goal(server);return CACHE.get(server).upgrades();}
 public static int enchantedTools(MinecraftServer server){goal(server);return CACHE.get(server).tools();}
 public static int goal(MinecraftServer server){return goal(server,RepairService.QUEST);}
 public static int goal(MinecraftServer server,String quest){var goals=CACHE.computeIfAbsent(server,s->{try{return loadGoals(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-repair-quests.json"));}catch(IOException e){throw new IllegalStateException("Native repair requirements need valid configuration",e);}});return RepairService.JOURNEYMAN.equals(quest)?goals.enchanted():goals.ordinary();}
 public static int load(Path path)throws IOException{return loadGoals(path).ordinary();}
 public static Goals loadGoals(Path path)throws IOException{if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,"{\"schema\":1,\"distinctIronItems\":4,\"enchantedIronItems\":4,\"enchantedTools\":4,\"netheriteUpgrades\":1}\n",StandardOpenOption.CREATE_NEW);}try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!root.keySet().containsAll(Set.of("schema","distinctIronItems"))||!Set.of("schema","distinctIronItems","enchantedIronItems","enchantedTools","netheriteUpgrades").containsAll(root.keySet()))throw new IllegalArgumentException("Unknown repair rules");for(var key:root.keySet())if(!root.get(key).isJsonPrimitive()||!root.getAsJsonPrimitive(key).isNumber())throw new IllegalArgumentException("Integer required");if(root.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid repair schema");int n=root.get("distinctIronItems").getAsBigDecimal().intValueExact();if(n<1||n>128)throw new IllegalArgumentException("Repair count must be1..128");int enchanted=root.has("enchantedIronItems")?root.get("enchantedIronItems").getAsBigDecimal().intValueExact():4;if(enchanted<1||enchanted>128)throw new IllegalArgumentException("Enchanted repair count must be1..128");int tools=root.has("enchantedTools")?root.get("enchantedTools").getAsBigDecimal().intValueExact():4;if(tools<1||tools>36)throw new IllegalArgumentException("Enchanted tool count must fit1..36inventory slots");int upgrades=root.has("netheriteUpgrades")?root.get("netheriteUpgrades").getAsBigDecimal().intValueExact():1;if(upgrades<1||upgrades>36)throw new IllegalArgumentException("Upgrade count must fit1..36inventory slots");return new Goals(n,enchanted,tools,upgrades);}catch(RuntimeException failed){throw new IOException("Invalid native repair rules",failed);}}
}
