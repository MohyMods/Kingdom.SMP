package com.mohymods.kingdomsmp.commerce;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import com.mohymods.kingdomsmp.building.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Explicit versioned numerical defaults; fixed gameplay gates are not configurable here. */
public final class CommerceCatalog {
    public record Building(String id,String title,int tier,long price,String resident,String founder) { }
    public record Supply(String id,String title,Item item,long price,int quota) { }
    private final List<Building> buildings;
    private final List<Supply> supplies;
    private final long budget,allowance;
    private CommerceCatalog(List<Building>b,List<Supply>s,long budget,long allowance){buildings=List.copyOf(b);supplies=List.copyOf(s);this.budget=budget;this.allowance=allowance;}
    public List<Building> buildings(){return buildings;} public List<Supply> supplies(){return supplies;}
    public long budget(){return budget;} public long allowance(){return allowance;}
    public Building building(String id){return buildings.stream().filter(b->b.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown building listing."));}
    public Supply supply(String id){return supplies.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown supplies order."));}
    public static CommerceCatalog load(Path config)throws IOException{
        JsonObject defaults=new JsonObject();defaults.addProperty("schema",1);defaults.addProperty("sharedDailyBudget",20000);defaults.addProperty("playerDailyAllowance",2000);
        var prices=new JsonObject();prices.addProperty("farmers_house",1000);prices.addProperty("builders_workshop",1250);prices.addProperty("lumberjacks_lodge",1000);prices.addProperty("royal_bank",2500);prices.addProperty("royal_bounty_hall",1500);prices.addProperty("fishermans_wharf",1750);prices.addProperty("royal_aerodrome",7500);for(var d:ProfessionPremises.definitions())prices.addProperty(d.priceKey(),d.price());defaults.add("blueprintPrices",prices);
        var orders=new JsonObject();defaults.add("supplies",orders);
        for(var s:List.of(new Supply("logs","Oak logs",Items.OAK_LOG,2,2048),new Supply("stone","Cobblestone",Items.COBBLESTONE,1,4096),new Supply("wheat","Wheat",Items.WHEAT,1,2048),new Supply("iron","Iron ingots",Items.IRON_INGOT,8,256))){var row=new JsonObject();row.addProperty("price",s.price());row.addProperty("dailyQuota",s.quota());orders.add(s.id(),row);}
        if(!Files.exists(config)){Files.createDirectories(config.getParent());Files.writeString(config,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
        JsonObject data;
        try{data=JsonParser.parseString(Files.readString(config)).getAsJsonObject();}catch(RuntimeException e){throw new IOException("Invalid commerce configuration",e);}
        try{
            if(integer(data,"schema",1,1)!=1)throw new IllegalArgumentException("Unsupported commerce schema");
            long budget=integer(data,"sharedDailyBudget",1,1_000_000_000),allowance=integer(data,"playerDailyAllowance",1,budget);
            JsonObject bp=data.getAsJsonObject("blueprintPrices"),sp=data.getAsJsonObject("supplies");
            if(!bp.has("royal_bounty_hall"))bp.addProperty("royal_bounty_hall",1500); // Optional new schema-one listing; never rewrite existing world settings.
            if(!bp.has("fishermans_wharf"))bp.addProperty("fishermans_wharf",1750); // Keep old world files byte-identical.
            if(!bp.has("royal_aerodrome"))bp.addProperty("royal_aerodrome",7500); // In-memory default; never rewrite an existing world file.
            List<Building> buildings=new ArrayList<>();
            buildings.add(building(bp,"farmers_house","Farmer's House",2,"Rowan Fenwick","Founder of the First Fields"));
            buildings.add(building(bp,"builders_workshop","Builder's Workshop",2,"Cedric Ashlar","Founder of the Builders' Hearth"));
            buildings.add(building(bp,"lumberjacks_lodge","Lumberjack's Lodge",2,"Brenna Oakfall","Founder of the Timber Hall"));
            buildings.add(building(bp,"royal_bank","Royal Bank",3,"Oswin Fairmark","Founder of the Royal Bank"));
            buildings.add(building(bp,"royal_bounty_hall","Royal Bounty Hall",3,"Garrick Thornfield","Founder of the Royal Bounty Hall"));
            buildings.add(building(bp,"fishermans_wharf","Fisherman's Wharf",3,"Tomas Reedwater","Founder of the Royal Quay"));
            buildings.add(building(bp,"royal_aerodrome","Royal Aerodrome",6,"Corin Windlass","Founder of the Royal Aerodrome"));
            for(var d:ProfessionPremises.definitions()){if(!bp.has(d.priceKey()))bp.addProperty(d.priceKey(),d.price());buildings.add(new Building(d.id().toString(),d.title(),d.tier(),integer(bp,d.priceKey(),500,15000),d.resident(),"Founder of the "+d.title()));}
            List<Supply> supplies=new ArrayList<>();
            for(var base:List.of(new Supply("logs","Oak logs",Items.OAK_LOG,0,0),new Supply("stone","Cobblestone",Items.COBBLESTONE,0,0),new Supply("wheat","Wheat",Items.WHEAT,0,0),new Supply("iron","Iron ingots",Items.IRON_INGOT,0,0))){var row=sp.getAsJsonObject(base.id());supplies.add(new Supply(base.id(),base.title(),base.item(),integer(row,"price",1,1000),(int)integer(row,"dailyQuota",1,1_000_000)));}
            return new CommerceCatalog(buildings,supplies,budget,allowance);
        }catch(RuntimeException e){throw new IOException("Commerce configuration requires valid bounded schema-1 values",e);}
    }
    private static Building building(JsonObject prices,String path,String title,int tier,String resident,String founder){return new Building("kingdom_smp:"+path,title,tier,integer(prices,path,500,15000),resident,founder);}
    private static long integer(JsonObject o,String key,long min,long max){var v=o.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);long n=v.getAsBigDecimal().longValueExact();if(n<min||n>max)throw new IllegalArgumentException(key);return n;}
    public static boolean marketBuilding(ResourceLocation id){return ProfessionPremises.contains(id)||Set.of("kingdom_smp:farmers_house","kingdom_smp:builders_workshop","kingdom_smp:lumberjacks_lodge","kingdom_smp:royal_bank","kingdom_smp:royal_bounty_hall","kingdom_smp:fishermans_wharf","kingdom_smp:royal_aerodrome").contains(id.toString());}
}
