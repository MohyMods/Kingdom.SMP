package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class RepairSavedData extends SavedData {
 private static final String NAME="kingdomsmp_native_repairs_v1";private final RepairEvidence evidence;
 public RepairSavedData(){evidence=new RepairEvidence();}private RepairSavedData(RepairEvidence evidence){this.evidence=evidence;}public RepairEvidence evidence(){return evidence;}
 public static RepairSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Native repairs are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new RepairSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect native repair data",failed);}throw new IllegalStateException("Existing native repair data requires matching recovery; never reset");},RepairSavedData::load),NAME);}
 public static RepairSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new RepairSavedData(RepairEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
