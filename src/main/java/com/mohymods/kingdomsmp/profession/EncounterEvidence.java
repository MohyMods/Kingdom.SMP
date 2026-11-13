package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.HuntState;
import java.util.*;
import net.minecraft.nbt.*;

/** Permanent bounded victim identities and counters share one save. No retrospective spawn inference. */
public final class EncounterEvidence {
    public static final int LIMIT=131072;
    private record Kill(UUID player,String species) { }
    private final Map<UUID,Kill> victims=new HashMap<>();
    private final Map<UUID,Map<String,Integer>> counters=new HashMap<>();
    public boolean record(UUID victim,UUID player,String species){
        Objects.requireNonNull(victim);Objects.requireNonNull(player);
        if(!HuntState.TARGETS.contains(species)||victims.containsKey(victim)||victims.size()>=LIMIT)return false;
        victims.put(victim,new Kill(player,species));counters.computeIfAbsent(player,k->new HashMap<>()).merge(species,1,Integer::sum);return true;
    }
    public Map<String,Integer> counts(UUID player){
        return Map.copyOf(counters.getOrDefault(player,Map.of()));
    }
    public boolean ready(UUID player,int total,int perSpecies){var counts=counts(player);return counts.values().stream().mapToInt(Integer::intValue).sum()>=total&&HuntState.TARGETS.stream().allMatch(s->counts.getOrDefault(s,0)>=perSpecies);}
    public CompoundTag save(){var tag=new CompoundTag();tag.putInt("schema",1);var list=new ListTag();victims.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->{var row=new CompoundTag();row.putUUID("victim",e.getKey());row.putUUID("player",e.getValue().player());row.putString("species",e.getValue().species());list.add(row);});tag.put("victims",list);return tag;}
    public static EncounterEvidence load(CompoundTag tag){
        if(!tag.getAllKeys().equals(Set.of("schema","victims"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!(tag.get("victims") instanceof ListTag list)||list.size()>LIMIT||!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid encounter evidence");
        var result=new EncounterEvidence();for(var value:list){var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("victim","player","species"))||!row.hasUUID("victim")||!row.hasUUID("player")||!row.contains("species",Tag.TAG_STRING)||!result.record(row.getUUID("victim"),row.getUUID("player"),row.getString("species")))throw new IllegalArgumentException("Invalid or duplicate encounter victim");}return result;
    }
}
