package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
public record MineAccessRules(int routeCells,int lights){
 private static final Map<MinecraftServer,MineAccessRules>CACHE=new WeakHashMap<>();
 public MineAccessRules{if(routeCells<2||routeCells>512||lights<1||lights>128)throw new IllegalArgumentException("Invalid bounded mine access rules");}
 public static MineAccessRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-mine-access.json"));}catch(IOException e){throw new IllegalStateException("Invalid mine access configuration",e);}});}
 public static MineAccessRules load(Path path)throws IOException{if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,"{\"schema\":1,\"routeCells\":16,\"lights\":16}",StandardOpenOption.CREATE_NEW);}try{var row=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!row.keySet().equals(Set.of("schema","routeCells","lights"))||n(row,"schema")!=1)throw new IllegalArgumentException("Invalid mine access rules");return new MineAccessRules(n(row,"routeCells"),n(row,"lights"));}catch(RuntimeException e){throw new IOException("Malformed mine access rules; preserve matching config",e);}}
 private static int n(JsonObject row,String key){var v=row.get(key);if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return v.getAsBigDecimal().intValueExact();}
}
