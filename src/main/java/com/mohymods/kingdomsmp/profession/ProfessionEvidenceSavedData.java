package com.mohymods.kingdomsmp.profession;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Bounded ordinary event evidence; canonical quest/financial receipts remain the award authority. */
public final class ProfessionEvidenceSavedData extends SavedData {
    private static final String NAME="kingdomsmp_profession_evidence_v1";
    private final Map<Profession,ProfessionEvidence> evidence=new EnumMap<>(Profession.class);
    public ProfessionEvidence evidence(Profession role){
        var metrics=new HashSet<>(CraftDeliveryQuests.metrics(role).values());
        if(metrics.isEmpty())throw new IllegalArgumentException("No implemented evidence producer for profession");
        return evidence.computeIfAbsent(role,p->new ProfessionEvidence(role,metrics));
    }
    public static ProfessionEvidenceSavedData get(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("Profession evidence is server-thread only");
        var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{
            try{Files.readAttributes(path,BasicFileAttributes.class);}
            catch(NoSuchFileException absent){var fresh=new ProfessionEvidenceSavedData();fresh.setDirty();return fresh;}
            catch(IOException failure){throw new IllegalStateException("Cannot inspect profession evidence",failure);}
            throw new IllegalStateException("Existing profession evidence cannot load; restore matching save before continuing");
        },ProfessionEvidenceSavedData::load),NAME);
    }
    public static ProfessionEvidenceSavedData load(CompoundTag tag,HolderLookup.Provider registries){
        if(!tag.getAllKeys().equals(Set.of("schema","professions"))||!tag.contains("schema",Tag.TAG_INT)||tag.getInt("schema")!=1||!tag.contains("professions",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid shared profession evidence schema");
        var result=new ProfessionEvidenceSavedData();var rows=tag.getCompound("professions");
        if(rows.size()>11)throw new IllegalArgumentException("Too many profession evidence scopes");
        for(var id:rows.getAllKeys()){
            var role=Profession.byId(id);var metrics=new HashSet<>(CraftDeliveryQuests.metrics(role).values());
            if(metrics.isEmpty()||!rows.contains(id,Tag.TAG_COMPOUND))throw new IllegalArgumentException("Unknown producer or malformed evidence scope");
            result.evidence.put(role,ProfessionEvidence.load(rows.getCompound(id),role,metrics));
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        tag.putInt("schema",1);var rows=new CompoundTag();
        evidence.forEach((role,proofs)->rows.put(role.id(),proofs.save()));tag.put("professions",rows);return tag;
    }
}
