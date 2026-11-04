package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
public final class RepairQuestRules {
 private static final Map<MinecraftServer,Integer> CACHE=new WeakHashMap<>();private RepairQuestRules(){}
 public static void clear(MinecraftServer server){CACHE.remove(server);}
 public static int goal(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-repair-quests.json"));}catch(IOException e){throw new IllegalStateException("Native repair requirements need valid configuration",e);}});}
 public static int load(Path path)throws IOException{if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,"{\"schema\":1,\"distinctIronItems\":4}\n",StandardOpenOption.CREATE_NEW);}try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!root.keySet().equals(Set.of("schema","distinctIronItems")))throw new IllegalArgumentException("Unknown repair rules");for(var key:root.keySet())if(!root.get(key).isJsonPrimitive()||!root.getAsJsonPrimitive(key).isNumber())throw new IllegalArgumentException("Integer required");if(root.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid repair schema");int n=root.get("distinctIronItems").getAsBigDecimal().intValueExact();if(n<1||n>128)throw new IllegalArgumentException("Repair count must be1..128");return n;}catch(RuntimeException failed){throw new IOException("Invalid native repair rules",failed);}}
}
