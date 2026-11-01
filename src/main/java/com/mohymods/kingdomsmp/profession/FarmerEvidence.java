package com.mohymods.kingdomsmp.profession;

import net.minecraft.nbt.*;
import java.util.*;

/** Bounded non-monetary event evidence. Canonical quest completions remain in EconomyStore. */
public final class FarmerEvidence {
    public static final int MAX_PLAYERS=128, MAX_UNIQUE=8192, MAX_COUNTER=1_000_000;
    private static final Set<String> METRICS=Set.of("soil","wheat","carrot","potato","beetroot","melon","pumpkin","cow","sheep","chicken","pig","bread","cake","replant");
    private final Map<UUID,Proof> players=new HashMap<>();
    private static final class Proof {
        final Map<String,Integer> counters=new HashMap<>();
        final Set<String> unique=new HashSet<>();
    }
    public int count(UUID player,ProfessionRank band,String metric){checkMetric(metric);var p=players.get(player);return p==null?0:p.counters.getOrDefault(key(band,metric),0);}
    /** Distinct identities are capped and never evicted into countable state. */
    public boolean record(UUID player,ProfessionRank band,String metric,int amount,String identity){
        Objects.requireNonNull(player);Objects.requireNonNull(band);checkMetric(metric);
        if(amount<1||amount>4096||identity!=null&&(identity.isBlank()||identity.length()>160))throw new IllegalArgumentException("Invalid Farmer event evidence");
        if(!players.containsKey(player)&&players.size()>=MAX_PLAYERS)return false;
        var p=players.computeIfAbsent(player,k->new Proof());String unique=identity==null?null:metric+":"+identity;
        if(unique!=null&&(p.unique.contains(unique)||p.unique.size()>=MAX_UNIQUE))return false;
        String key=key(band,metric);int before=p.counters.getOrDefault(key,0);if(before>=MAX_COUNTER)return false;
        if(unique!=null)p.unique.add(unique);p.counters.put(key,(int)Math.min(MAX_COUNTER,(long)before+amount));return true;
    }
    private static String key(ProfessionRank band,String metric){return band.id()+"/"+metric;}
    private static void checkMetric(String metric){if(!METRICS.contains(metric))throw new IllegalArgumentException("Unknown Farmer evidence metric");}
    public CompoundTag save(){
        var result=new CompoundTag();result.putInt("schema",1);var rows=new ListTag();
        players.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{var row=new CompoundTag();row.putUUID("player",entry.getKey());var counters=new CompoundTag();entry.getValue().counters.forEach(counters::putInt);row.put("counters",counters);var seen=new ListTag();entry.getValue().unique.stream().sorted().forEach(v->seen.add(StringTag.valueOf(v)));row.put("unique",seen);rows.add(row);});
        result.put("players",rows);return result;
    }
    public static FarmerEvidence load(CompoundTag tag){
        if(!tag.getAllKeys().equals(Set.of("schema","players"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!(tag.get("players") instanceof ListTag rows)||rows.size()>MAX_PLAYERS||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid Farmer evidence schema");
        var result=new FarmerEvidence();
        for(var value:rows){var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("player","counters","unique"))||!row.hasUUID("player")||!row.contains("counters",Tag.TAG_COMPOUND)||!(row.get("unique") instanceof ListTag seen)||seen.size()>MAX_UNIQUE||!seen.isEmpty()&&seen.getElementType()!=Tag.TAG_STRING)throw new IllegalArgumentException("Invalid Farmer proof");
            var proof=new Proof();var counters=row.getCompound("counters");if(counters.size()>METRICS.size()*3)throw new IllegalArgumentException("Too many Farmer counters");
            for(var key:counters.getAllKeys()){String[] parts=key.split("/",-1);if(parts.length!=2||Arrays.stream(ProfessionRank.values()).noneMatch(r->r.id().equals(parts[0]))||!METRICS.contains(parts[1])||!counters.contains(key,Tag.TAG_INT)||counters.getInt(key)<1||counters.getInt(key)>MAX_COUNTER)throw new IllegalArgumentException("Invalid Farmer counter");proof.counters.put(key,counters.getInt(key));}
            for(var item:seen){String identity=item.getAsString();if(identity.length()>180||!identity.contains(":"))throw new IllegalArgumentException("Invalid Farmer identity");checkMetric(identity.substring(0,identity.indexOf(':')));if(!proof.unique.add(identity))throw new IllegalArgumentException("Duplicate Farmer identity");}
            if(result.players.put(row.getUUID("player"),proof)!=null)throw new IllegalArgumentException("Duplicate Farmer player");
        }
        return result;
    }
}
