package com.mohymods.kingdomsmp.economy;

import com.google.gson.*;
import com.mohymods.kingdomsmp.commerce.CommerceCatalog;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** New configurable defaults; original shared budget, personal allowance and buy quotas remain authoritative. */
public final class MarketCatalog {
    public record Rule(long minimumBuy,long minimumSell,long maximumSell,int stock){
        public Rule {if(minimumBuy<1||minimumSell<1||maximumSell<minimumSell||maximumSell>1_000_000||stock<1||stock>1_000_000)throw new IllegalArgumentException("Invalid market rule");}
    }
    private final Map<String,Rule> rules;
    private MarketCatalog(Map<String,Rule> rules){this.rules=Map.copyOf(rules);}
    public Rule rule(String id){return Objects.requireNonNull(rules.get(id),"Unknown market resource");}
    public static MarketCatalog load(Path file,CommerceCatalog commerce)throws IOException{
        var defaults=new JsonObject();defaults.addProperty("schema",1);var rows=new JsonObject();defaults.add("resources",rows);
        for(var supply:commerce.supplies()){var row=new JsonObject();row.addProperty("minimumBuyPrice",Math.max(1,supply.price()/2));row.addProperty("minimumSellPrice",supply.price()*2);row.addProperty("maximumSellPrice",supply.price()*3);row.addProperty("dailyStock",Math.max(1,supply.quota()/8));rows.add(supply.id(),row);}
        if(!Files.exists(file)){Files.createDirectories(file.getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
        try{
            var data=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(!data.keySet().equals(Set.of("schema","resources"))||number(data,"schema")!=1)throw new IllegalArgumentException("Invalid market schema");
            var configured=data.getAsJsonObject("resources");if(!configured.keySet().equals(rows.keySet()))throw new IllegalArgumentException("Market resources must match the actual supplies catalogue");
            Map<String,Rule> rules=new HashMap<>();
            for(var supply:commerce.supplies()){
                var row=configured.getAsJsonObject(supply.id());if(!row.keySet().equals(Set.of("minimumBuyPrice","minimumSellPrice","maximumSellPrice","dailyStock")))throw new IllegalArgumentException("Unknown market option");
                long stock=number(row,"dailyStock");if(stock>1_000_000||stock<1)throw new IllegalArgumentException("Daily stock must be 1–1000000");
                var rule=new Rule(number(row,"minimumBuyPrice"),number(row,"minimumSellPrice"),number(row,"maximumSellPrice"),(int)stock);
                if(rule.minimumBuy()>supply.price()||rule.minimumSell()<=supply.price())throw new IllegalArgumentException("NPC selling floor must exceed its maximum buying price; buying floor cannot exceed its ceiling");
                rules.put(supply.id(),rule);
            }
            return new MarketCatalog(rules);
        }catch(RuntimeException e){throw new IOException("Market configuration requires valid schema-1 bounded prices and an arbitrage-safe spread",e);}
    }
    private static long number(JsonObject data,String key){var v=data.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return v.getAsBigDecimal().longValueExact();}
}
