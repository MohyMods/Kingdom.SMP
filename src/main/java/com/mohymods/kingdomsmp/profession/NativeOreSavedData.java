package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class NativeOreSavedData extends SavedData {
 private static final String NAME="kingdomsmp_native_ores_v1";private final NativeOreEvidence evidence;
 public NativeOreSavedData(){evidence=new NativeOreEvidence();}private NativeOreSavedData(NativeOreEvidence evidence){this.evidence=evidence;}public NativeOreEvidence evidence(){return evidence;}
 public static NativeOreSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Native ores are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new NativeOreSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect native ore data",failed);}throw new IllegalStateException("Existing native ore data requires matching recovery; never reset");},NativeOreSavedData::load),NAME);}
 public static NativeOreSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new NativeOreSavedData(NativeOreEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
