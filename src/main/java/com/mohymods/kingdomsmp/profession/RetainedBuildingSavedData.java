package com.mohymods.kingdomsmp.profession;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Successful native placements only; canonical authorization and awards remain in the existing journal. */
public final class RetainedBuildingSavedData extends SavedData {
    private static final String NAME="kingdomsmp_retained_buildings_v1";
    private RetainedPlacementEvidence evidence=new RetainedPlacementEvidence();
    public RetainedPlacementEvidence evidence(){return evidence;}
    public static RetainedBuildingSavedData get(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("Retained evidence is server-thread only");
        var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{
            try{Files.readAttributes(path,BasicFileAttributes.class);}
            catch(NoSuchFileException absent){var data=new RetainedBuildingSavedData();data.setDirty();return data;}
            catch(IOException failure){throw new IllegalStateException("Cannot inspect retained building evidence",failure);}
            throw new IllegalStateException("Existing retained evidence failed to load; restore matching checkpoint");
        },RetainedBuildingSavedData::load),NAME);
    }
    public static RetainedBuildingSavedData load(CompoundTag tag,HolderLookup.Provider registries){var result=new RetainedBuildingSavedData();result.evidence=RetainedPlacementEvidence.load(tag);return result;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
