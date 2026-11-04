package com.mohymods.kingdomsmp.profession;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact D-062 content baseline. Definition availability never grants runtime activation or evidence. */
public final class ApprovedProfessionContent {
    public static final String BASELINE="9004b3ea9e92dfeebcb8b7105ad6b440fb8f24e8";
    public enum Validation { NAT, MAKE, DEL, JOB, BUILD, INSPECT, TREE, GROW, CONTRACT, REPAIR, SMITH, ENCHANT, BREW, EXPLORE, COMBAT, MAGIC, TRADE, BANK, HUNT, FLIGHT }
    public record Definition(String id,Profession profession,ProfessionRank rank,int ordinal,String name,
            String objective,String quantity,Set<Validation> validation,int minimumTier,Set<String> prerequisites,
            String availability,long xp,String rewards) {
        public Definition {
            Objects.requireNonNull(profession);Objects.requireNonNull(rank);
            if(profession==Profession.FARMER||!profession.questId(rank,ordinal).equals(id))throw new IllegalArgumentException("Invalid remaining quest identity");
            for(String s:List.of(name,objective,quantity,availability,rewards))if(s==null||s.isBlank()||s.length()>1024)throw new IllegalArgumentException("Invalid quest description");
            validation=Set.copyOf(validation);prerequisites=Set.copyOf(prerequisites);
            if(validation.isEmpty()||validation.size()>4||minimumTier<2||minimumTier>12||xp!=(rank.ordinal()+1)*100L)throw new IllegalArgumentException("Invalid approved objective metadata");
            new ProfessionQuestCatalog.Definition(id,profession,rank,ordinal,prerequisites);
        }
        public ProfessionQuestCatalog.Definition slot(){return new ProfessionQuestCatalog.Definition(id,profession,rank,ordinal,prerequisites);}
    }
    private static final Map<String,Definition> DEFINITIONS=loadBundled();
    private ApprovedProfessionContent() { }
    public static Map<String,Definition> definitions(){return DEFINITIONS;}
    public static Definition get(String id){var d=DEFINITIONS.get(id);if(d==null)throw new IllegalArgumentException("No approved profession objective: "+id);return d;}
    private static Map<String,Definition> loadBundled(){
        try(var input=ApprovedProfessionContent.class.getResourceAsStream("/data/kingdom_smp/professions/approved_remaining_quests.json")){
            if(input==null)throw new IOException("Missing approved profession content");
            byte[] bytes=input.readNBytes(1024*1024+1);
            if(bytes.length>1024*1024)throw new IOException("Approved profession content is too large");
            return parse(new String(bytes,StandardCharsets.UTF_8));
        }catch(IOException|RuntimeException e){throw new ExceptionInInitializerError(e);}
    }
    static Map<String,Definition> parse(String text){
        var root=JsonParser.parseString(text).getAsJsonObject();
        if(!root.keySet().equals(Set.of("schema","approvedBaseline","quests"))||number(root,"schema")!=1||!BASELINE.equals(string(root,"approvedBaseline")))throw new IllegalArgumentException("Unapproved profession content baseline");
        var rows=root.getAsJsonArray("quests");if(rows.size()!=264)throw new IllegalArgumentException("Exactly264remaining objectives required");
        Map<String,Definition> definitions=new HashMap<>();
        for(var element:rows){
            var row=element.getAsJsonObject();
            if(!row.keySet().equals(Set.of("id","profession","rank","ordinal","name","objective","quantity","validation","minimumTier","prerequisites","availability","xp","rewards")))throw new IllegalArgumentException("Unknown objective content field");
            var validators=EnumSet.noneOf(Validation.class);
            for(var code:row.getAsJsonArray("validation"))if(!code.isJsonPrimitive()||!code.getAsJsonPrimitive().isString()||!validators.add(Validation.valueOf(code.getAsString())))throw new IllegalArgumentException("Invalid duplicate validator");
            Set<String> prerequisites=new HashSet<>();
            for(var prior:row.getAsJsonArray("prerequisites"))if(!prior.isJsonPrimitive()||!prior.getAsJsonPrimitive().isString()||!prerequisites.add(prior.getAsString()))throw new IllegalArgumentException("Invalid duplicate prerequisite");
            var definition=new Definition(string(row,"id"),Profession.byId(string(row,"profession")),ProfessionRank.byId(string(row,"rank")),number(row,"ordinal"),string(row,"name"),string(row,"objective"),string(row,"quantity"),validators,number(row,"minimumTier"),prerequisites,string(row,"availability"),number(row,"xp"),string(row,"rewards"));
            if(definitions.put(definition.id(),definition)!=null)throw new IllegalArgumentException("Duplicate approved quest identity");
        }
        // Validates every explicit edge, including foreign/missing/forward/cyclic dependencies.
        ProfessionQuestCatalog.approved(definitions.values().stream().map(Definition::slot).toList());
        for(var profession:Profession.values())if(profession!=Profession.FARMER)for(var rank:ProfessionRank.values())for(int n=1;n<=8;n++)if(!definitions.containsKey(profession.questId(rank,n)))throw new IllegalArgumentException("Missing profession quest slot");
        return Map.copyOf(definitions);
    }
    private static String string(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Text required: "+key);return value.getAsString();}
    private static int number(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required: "+key);return value.getAsBigDecimal().intValueExact();}
}
