package com.mohymods.kingdomsmp.fishing;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Numerical fish orders share the existing kingdom NPC buying budget and personal sales allowance. */
public record FishingMarketRules(Map<String,Order> orders,long rodPrice,int rodStock) {
    public record Order(int floor,int ceiling,int quota) { }
    private static final Map<MinecraftServer,FishingMarketRules> CACHE=new WeakHashMap<>();
    public FishingMarketRules { orders=Map.copyOf(orders); }
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static FishingMarketRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-fishing-market.json"));}catch(IOException e){throw new IllegalStateException("Fishing market configuration requires recovery",e);}});}
    public static FishingMarketRules load(Path path)throws IOException{
        var defaults=new JsonObject();defaults.addProperty("schema",1);defaults.addProperty("ironRodPrice",1500);defaults.addProperty("dailyRodStock",10);var fish=new JsonObject();
        int[][] values={{1,4,256},{2,6,128},{2,6,128},{1,4,256},{3,9,64},{2,5,128}};
        for(int n=0;n<AquacultureAccess.FISH.size();n++){var row=new JsonObject();row.addProperty("minimumBuy",values[n][0]);row.addProperty("maximumBuy",values[n][1]);row.addProperty("dailyQuota",values[n][2]);fish.add(AquacultureAccess.FISH.get(n),row);}defaults.add("fish",fish);
        if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
        try{
            var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(!data.keySet().equals(Set.of("schema","ironRodPrice","dailyRodStock","fish"))||number(data,"schema")!=1||!data.getAsJsonObject("fish").keySet().equals(new HashSet<>(AquacultureAccess.FISH)))throw new IllegalArgumentException("Invalid fishing market schema");
            long price=number(data,"ironRodPrice"),stock=number(data,"dailyRodStock");if(price<1||price>1_000_000||stock<1||stock>1000)throw new IllegalArgumentException("Invalid rod price/stock");
            Map<String,Order> orders=new LinkedHashMap<>();
            for(var id:AquacultureAccess.FISH){var row=data.getAsJsonObject("fish").getAsJsonObject(id);if(!row.keySet().equals(Set.of("minimumBuy","maximumBuy","dailyQuota")))throw new IllegalArgumentException("Unexpected fishing term");long min=number(row,"minimumBuy"),max=number(row,"maximumBuy"),quota=number(row,"dailyQuota");if(min<1||max<min||max>1000||quota<1||quota>100_000)throw new IllegalArgumentException("Invalid fish price/quota");orders.put(id,new Order((int)min,(int)max,(int)quota));}
            return new FishingMarketRules(orders,price,(int)stock);
        }catch(RuntimeException e){throw new IOException("Invalid bounded fishing market configuration",e);}
    }
    private static long number(JsonObject object,String key){var value=object.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return value.getAsBigDecimal().longValueExact();}
}
