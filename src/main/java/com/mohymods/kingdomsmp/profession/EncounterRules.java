package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Approved configurable starting quantities; never overwrite malformed or existing server rules. */
public record EncounterRules(int total,int perSpecies) {
    private static final Map<MinecraftServer,EncounterRules> CACHE=new WeakHashMap<>();
    public EncounterRules{if(total<4||total>2048||perSpecies<1||perSpecies>512||total<4*perSpecies)throw new IllegalArgumentException("Invalid encounter quantities");}
    public int speciesLimit(){return total-3*perSpecies;}
    public static EncounterRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-encounters.json"));}catch(IOException e){throw new IllegalStateException("Invalid encounter configuration",e);}});}
    public static EncounterRules load(Path path)throws IOException{
        if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,"{\"schema\":1,\"total\":24,\"perSpecies\":4}",StandardOpenOption.CREATE_NEW);}
        try{var row=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!row.keySet().equals(Set.of("schema","total","perSpecies"))||number(row,"schema")!=1)throw new IllegalArgumentException("Invalid encounter rules");return new EncounterRules(number(row,"total"),number(row,"perSpecies"));}catch(RuntimeException e){throw new IOException("Malformed encounter rules",e);}
    }
    private static int number(JsonObject row,String key){var v=row.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return v.getAsBigDecimal().intValueExact();}
}
