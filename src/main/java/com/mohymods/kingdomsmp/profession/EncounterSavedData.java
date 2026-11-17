package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class EncounterSavedData extends SavedData {
 private static final String NAME="kingdomsmp_encounters_v1";private final EncounterEvidence evidence;
 public EncounterSavedData(){evidence=new EncounterEvidence();}private EncounterSavedData(EncounterEvidence evidence){this.evidence=evidence;}public EncounterEvidence evidence(){return evidence;}
 public static EncounterSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Encounters are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new EncounterSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect encounter data",failed);}throw new IllegalStateException("Existing encounter data requires matching recovery; never reset");},EncounterSavedData::load),NAME);}
 public static EncounterSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new EncounterSavedData(EncounterEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
