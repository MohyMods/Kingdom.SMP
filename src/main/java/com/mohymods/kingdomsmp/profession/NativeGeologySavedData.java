package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class NativeGeologySavedData extends SavedData {
 private static final java.util.Map<MinecraftServer,Boolean> PRESENT=new java.util.WeakHashMap<>();
 public static boolean present(MinecraftServer server){return PRESENT.computeIfAbsent(server,s->Files.exists(s.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_native_geology_v1.dat")));}
 private static final String NAME="kingdomsmp_native_geology_v1";private final NativeGeologyEvidence evidence;
 public NativeGeologySavedData(){evidence=new NativeGeologyEvidence();}private NativeGeologySavedData(NativeGeologyEvidence evidence){this.evidence=evidence;}public NativeGeologyEvidence evidence(){return evidence;}
 public static NativeGeologySavedData get(MinecraftServer server){PRESENT.put(server,true);if(!server.isSameThread())throw new IllegalStateException("Geological observations are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new NativeGeologySavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect geological data",failed);}throw new IllegalStateException("Existing geological data requires matching recovery; never reset");},NativeGeologySavedData::load),NAME);}
 public static NativeGeologySavedData load(CompoundTag tag,HolderLookup.Provider registries){return new NativeGeologySavedData(NativeGeologyEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
