package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
/** Numeric perk defaults only; fixed3slot/15000coin rules are not configurable here. */
public record ProfessionRules(long apprentice,long journeyman,long master) {
    private static final Map<MinecraftServer,ProfessionRules> CACHE=new WeakHashMap<>();
    public ProfessionRules {if(apprentice<0||journeyman<apprentice||master<journeyman||master>1_000_000)throw new IllegalArgumentException("Invalid profession allowance bonuses");}
    public long bonus(ProfessionRank rank){return switch(rank){case APPRENTICE->apprentice;case JOURNEYMAN->journeyman;case MASTER->master;};}
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static ProfessionRules get(MinecraftServer server){var saved=CACHE.get(server);if(saved!=null)return saved;try{var value=load(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-professions.json"));CACHE.put(server,value);return value;}catch(IOException e){throw new IllegalStateException("Profession allowance configuration needs recovery.",e);}}
    public static ProfessionRules load(Path path)throws IOException{
        if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,"{\n  \"schema\": 1,\n  \"apprenticeAllowanceBonus\": 500,\n  \"journeymanAllowanceBonus\": 1500,\n  \"masterAllowanceBonus\": 3000\n}\n",StandardOpenOption.CREATE_NEW);}
        try{var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!data.keySet().equals(Set.of("schema","apprenticeAllowanceBonus","journeymanAllowanceBonus","masterAllowanceBonus"))||n(data,"schema")!=1)throw new IllegalArgumentException("Invalid profession rules schema");return new ProfessionRules(n(data,"apprenticeAllowanceBonus"),n(data,"journeymanAllowanceBonus"),n(data,"masterAllowanceBonus"));}catch(RuntimeException e){throw new IOException("Invalid bounded schema1profession allowance rules",e);}
    }
    private static long n(JsonObject data,String key){var value=data.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return value.getAsBigDecimal().longValueExact();}
}
