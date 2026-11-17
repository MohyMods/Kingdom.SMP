package com.mohymods.kingdomsmp.profession;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import java.util.*;

/** Evidence may lose unsaved events on a crash; it can never independently grant XP, items or money. */
public final class FarmerSavedData extends SavedData {
    private static final String NAME="kingdomsmp_farmer_evidence";
    private static final int MAX_PLANTS=32768;
    public record Plant(String dimension,long position,String block,UUID planter){
        public Plant { if(dimension==null||dimension.length()>128||block==null||block.length()>128||planter==null)throw new IllegalArgumentException("Invalid crop provenance"); }
    }
    private record Cycle(String dimension,long position,UUID player,ProfessionRank band){ }
    private final Map<String,Cycle> replants=new HashMap<>();
    private FarmerEvidence evidence=new FarmerEvidence();
    private final Map<String,Plant> plants=new HashMap<>();
    public FarmerEvidence evidence(){return evidence;}
    private static String key(String dimension,long pos){return dimension+"/"+pos;}
    public boolean plant(Plant plant){String key=key(plant.dimension(),plant.position());if(plants.containsKey(key)||plants.size()<MAX_PLANTS){plants.put(key,plant);setDirty();return true;}return false;}
    public Optional<Plant> provenance(String dimension,long position){return Optional.ofNullable(plants.get(key(dimension,position)));}
    public void awaitReplant(String dimension,long position,UUID player,ProfessionRank band){String key=key(dimension,position);if(replants.containsKey(key)||replants.size()<MAX_PLANTS){replants.put(key,new Cycle(dimension,position,player,band));setDirty();}}
    public boolean replant(String dimension,long position,UUID player,ProfessionRank band){var cycle=replants.remove(key(dimension,position));if(cycle==null)return false;setDirty();return cycle.player().equals(player)&&cycle.band()==band;}
    public Optional<Plant> harvest(String dimension,long position){var plant=plants.remove(key(dimension,position));if(plant!=null)setDirty();return Optional.ofNullable(plant);}
    public static FarmerSavedData get(MinecraftServer server){var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{
        try { Files.readAttributes(path,BasicFileAttributes.class); }
        catch(NoSuchFileException absent){var fresh=new FarmerSavedData();fresh.setDirty();return fresh;}
        catch(IOException inaccessible){throw new IllegalStateException("Cannot inspect Farmer evidence",inaccessible);}
        throw new IllegalStateException("Existing Farmer evidence failed to load; restore before continuing: "+path);
    },FarmerSavedData::load),NAME);}
    public static FarmerSavedData load(CompoundTag tag,HolderLookup.Provider registries){
        if(!tag.getAllKeys().equals(Set.of("schema","evidence","plants","replants"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!tag.contains("evidence",Tag.TAG_COMPOUND)||!(tag.get("plants") instanceof ListTag rows)||rows.size()>MAX_PLANTS||!rows.isEmpty()&&rows.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid Farmer saved schema");
        var data=new FarmerSavedData();data.evidence=FarmerEvidence.load(tag.getCompound("evidence"));
        for(var value:rows){var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("dimension","position","block","planter"))||!row.contains("dimension",Tag.TAG_STRING)||!row.contains("position",Tag.TAG_LONG)||!row.contains("block",Tag.TAG_STRING)||!row.hasUUID("planter"))throw new IllegalArgumentException("Malformed Farmer crop provenance");var plant=new Plant(row.getString("dimension"),row.getLong("position"),row.getString("block"),row.getUUID("planter"));if(data.plants.put(key(plant.dimension(),plant.position()),plant)!=null)throw new IllegalArgumentException("Duplicate Farmer crop provenance");}
        if(!(tag.get("replants") instanceof ListTag cycles)||cycles.size()>MAX_PLANTS||!cycles.isEmpty()&&cycles.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid Farmer replant cycles");
        for(var value:cycles){var row=(CompoundTag)value;if(!row.getAllKeys().equals(Set.of("dimension","position","player","band"))||!row.contains("dimension",Tag.TAG_STRING)||row.getString("dimension").length()>128||!row.contains("position",Tag.TAG_LONG)||!row.hasUUID("player")||!row.contains("band",Tag.TAG_STRING))throw new IllegalArgumentException("Invalid replant cycle");var cycle=new Cycle(row.getString("dimension"),row.getLong("position"),row.getUUID("player"),ProfessionRank.byId(row.getString("band")));if(data.replants.put(key(cycle.dimension(),cycle.position()),cycle)!=null)throw new IllegalArgumentException("Duplicate replant cycle");}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){tag.putInt("schema",1);tag.put("evidence",evidence.save());var rows=new ListTag();plants.values().stream().sorted(Comparator.comparing(p->key(p.dimension(),p.position()))).forEach(p->{var row=new CompoundTag();row.putString("dimension",p.dimension());row.putLong("position",p.position());row.putString("block",p.block());row.putUUID("planter",p.planter());rows.add(row);});tag.put("plants",rows);var cycles=new ListTag();replants.values().stream().sorted(Comparator.comparing(c->key(c.dimension(),c.position()))).forEach(c->{var row=new CompoundTag();row.putString("dimension",c.dimension());row.putLong("position",c.position());row.putUUID("player",c.player());row.putString("band",c.band().id());cycles.add(row);});tag.put("replants",cycles);return tag;}
}
