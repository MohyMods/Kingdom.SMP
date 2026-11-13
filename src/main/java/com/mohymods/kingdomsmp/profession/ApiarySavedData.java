package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class ApiarySavedData extends SavedData {
 private static final String NAME="kingdomsmp_apiaries_v1";private final ApiaryEvidence evidence;
 public ApiarySavedData(){evidence=new ApiaryEvidence();}private ApiarySavedData(ApiaryEvidence evidence){this.evidence=evidence;}public ApiaryEvidence evidence(){return evidence;}
 public static ApiarySavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Apiaries are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new ApiarySavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect apiary data",failed);}throw new IllegalStateException("Existing apiary data requires matching recovery; never reset");},ApiarySavedData::load),NAME);}
 public static ApiarySavedData load(CompoundTag tag,HolderLookup.Provider registries){return new ApiarySavedData(ApiaryEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
