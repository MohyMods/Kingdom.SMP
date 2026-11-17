package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class NetherOreSavedData extends SavedData {
 private static final String NAME="kingdomsmp_nether_ores_v1";private final NetherOreEvidence evidence;
 public NetherOreSavedData(){evidence=new NetherOreEvidence();}private NetherOreSavedData(NetherOreEvidence evidence){this.evidence=evidence;}public NetherOreEvidence evidence(){return evidence;}
 public static NetherOreSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Native ores are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new NetherOreSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect native ore data",failed);}throw new IllegalStateException("Existing native ore data requires matching recovery; never reset");},NetherOreSavedData::load),NAME);}
 public static NetherOreSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new NetherOreSavedData(NetherOreEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
