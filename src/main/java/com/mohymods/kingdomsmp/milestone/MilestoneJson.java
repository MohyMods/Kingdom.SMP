package com.mohymods.kingdomsmp.milestone;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import static com.mohymods.kingdomsmp.milestone.MilestoneCatalog.*;

/** Strict, bounded JSON: unknown/duplicate fields, fractional numbers and trailing data fail closed. */
public final class MilestoneJson {
    public static final int MAX_CHARACTERS=262144;
    private MilestoneJson(){}
    public static MilestoneCatalog read(Reader input)throws IOException{
        StringBuilder data=new StringBuilder();char[] b=new char[4096];int count;
        while((count=input.read(b))!=-1){data.append(b,0,count);if(data.length()>MAX_CHARACTERS)throw new IOException("Milestone file exceeds 256 KiB character limit");}
        try(JsonReader reader=new JsonReader(new StringReader(data.toString()))){
            reader.setLenient(false);JsonObject root=object(element(reader,0),"schema","capabilities","milestones");
            if(reader.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT||integer(root,"schema")!=1)throw new IllegalArgumentException("Unsupported schema or trailing data");
            List<Capability> caps=new ArrayList<>();for(JsonElement e:array(root,"capabilities",128)){
                JsonObject o=object(e,"id","available_from_tier","requires","after_milestones");
                caps.add(new Capability(string(o,"id"),Math.toIntExact(integer(o,"available_from_tier")),strings(o,"requires"),strings(o,"after_milestones")));
            }
            List<Milestone> milestones=new ArrayList<>();for(JsonElement e:array(root,"milestones",12)){
                JsonObject o=object(e,"id","tier","next_tier","approval","title","scoring_policy","requirements","objectives","reward");
                List<Requirement> requirements=new ArrayList<>();for(JsonElement r:array(o,"requirements",16)){
                    JsonObject v=object(r,"id","item","tag","quantity","category","requires","tracking","display");
                    if(v.has("item")==v.has("tag")||!string(v,"tracking").equals("shared_consumed"))throw new IllegalArgumentException("Use exactly one item or tag, and shared_consumed tracking");
                    JsonObject display=v.has("display")?object(v.get("display"),"name","description"):new JsonObject();
                    requirements.add(new Requirement(string(v,"id"),v.has("tag")?"#"+string(v,"tag"):string(v,"item"),integer(v,"quantity"),string(v,"category"),strings(v,"requires"),optional(display,"name"),optional(display,"description")));
                }
                List<Objective> objectives=new ArrayList<>();for(JsonElement x:array(o,"objectives",8)){
                    JsonObject v=object(x,"id","kind","target","requires","description");
                    objectives.add(new Objective(string(v,"id"),string(v,"kind"),string(v,"target"),strings(v,"requires"),optional(v,"description")));
                }
                milestones.add(new Milestone(string(o,"id"),Math.toIntExact(integer(o,"tier")),Math.toIntExact(integer(o,"next_tier")),string(o,"approval"),string(o,"title"),string(o,"scoring_policy"),requirements,objectives,string(o,"reward")));
            }
            return new MilestoneCatalog(caps,milestones);
        }catch(RuntimeException e){throw new IOException("Invalid milestone data: "+e.getMessage(),e);}
    }
    private static JsonElement element(JsonReader r,int depth)throws IOException{
        if(depth>32)throw new IOException("Milestone nesting exceeds limit");
        return switch(r.peek()){
            case BEGIN_OBJECT->{r.beginObject();JsonObject o=new JsonObject();while(r.hasNext()){String key=r.nextName();if(o.has(key))throw new IOException("Duplicate JSON field: "+key);o.add(key,element(r,depth+1));}r.endObject();yield o;}
            case BEGIN_ARRAY->{r.beginArray();JsonArray a=new JsonArray();while(r.hasNext()){if(a.size()>=256)throw new IOException("Array exceeds limit");a.add(element(r,depth+1));}r.endArray();yield a;}
            case STRING->new JsonPrimitive(r.nextString());
            case NUMBER->new JsonPrimitive(new BigDecimal(r.nextString()));
            default->throw new IOException("Only objects, arrays, strings and numbers are supported");
        };
    }
    private static JsonObject object(JsonElement e,String...keys){if(e==null||!e.isJsonObject())throw new IllegalArgumentException("Expected object");JsonObject o=e.getAsJsonObject();for(String key:o.keySet())if(!Set.of(keys).contains(key))throw new IllegalArgumentException("Unknown field: "+key);return o;}
    private static JsonArray array(JsonObject o,String key,int limit){JsonElement e=o.get(key);if(e==null||!e.isJsonArray()||e.getAsJsonArray().size()>limit)throw new IllegalArgumentException("Missing/oversized array: "+key);return e.getAsJsonArray();}
    private static String string(JsonObject o,String key){JsonElement e=o.get(key);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Expected string: "+key);return e.getAsString();}
    private static String optional(JsonObject o,String key){return o.has(key)?string(o,key):"";}
    private static long integer(JsonObject o,String key){JsonElement e=o.get(key);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Expected integer: "+key);return e.getAsBigDecimal().longValueExact();}
    private static List<String> strings(JsonObject o,String key){List<String> result=new ArrayList<>();for(JsonElement e:array(o,key,32)){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Expected dependency identifier");result.add(e.getAsString());}return result;}
}
