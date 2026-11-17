package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Bounded world-local route review defaults; no chunk tickets or client-controlled geometry bounds. */
public record BuilderCivicRules(int radius,int height,int connections){
 private static final Map<MinecraftServer,BuilderCivicRules> CACHE=new WeakHashMap<>();
 public BuilderCivicRules{if(radius<16||radius>64||height<1||height>16||connections<2||connections>12)throw new IllegalArgumentException("Civic route bounds outside supported limits");}
 public static BuilderCivicRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-builder-civic.json"));}catch(IOException bad){throw new IllegalStateException("Civic route config invalid; nothing awarded",bad);}});}
 static BuilderCivicRules load(Path file)throws IOException{if(!Files.exists(file)){Files.createDirectories(file.toAbsolutePath().getParent());Files.writeString(file,"{\"schema\":1,\"radius\":48,\"verticalRange\":2,\"connections\":2}\n",StandardOpenOption.CREATE_NEW);}try{var row=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(!row.keySet().equals(Set.of("schema","radius","verticalRange","connections"))||n(row,"schema")!=1)throw new IllegalArgumentException("Unknown civic route schema");return new BuilderCivicRules(n(row,"radius"),n(row,"verticalRange"),n(row,"connections"));}catch(RuntimeException failure){throw new IOException("Malformed bounded civic route configuration",failure);}}
 private static int n(JsonObject row,String key){var v=row.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return v.getAsBigDecimal().intValueExact();}
}
