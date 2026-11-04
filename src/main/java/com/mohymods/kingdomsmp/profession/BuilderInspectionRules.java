package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Approved48-cell graded route default; only ordinary bounded balancing quantity is configurable. */
final class BuilderInspectionRules {
    private static final Map<MinecraftServer,Integer> CACHE=new WeakHashMap<>();
    static void clear(MinecraftServer server){CACHE.remove(server);}
    static int route(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-builder-inspections.json"));}catch(IOException failure){throw new IllegalStateException("Invalid Builder inspection configuration",failure);}});}
    static int load(Path path)throws IOException{
        if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,"{\"schema\":1,\"gradedRouteCells\":48}\n",StandardOpenOption.CREATE_NEW);}
        try{var row=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!row.keySet().equals(Set.of("schema","gradedRouteCells"))||n(row,"schema")!=1)throw new IllegalArgumentException("Unknown inspection rules");int goal=n(row,"gradedRouteCells");if(goal<16||goal>512)throw new IllegalArgumentException("Route quantity must be16..512");return goal;}catch(RuntimeException failure){throw new IOException("Malformed bounded Builder inspection rules",failure);}
    }
    private static int n(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return value.getAsBigDecimal().intValueExact();}
}
