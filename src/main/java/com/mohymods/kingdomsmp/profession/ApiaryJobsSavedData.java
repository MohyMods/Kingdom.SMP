package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class ApiaryJobsSavedData extends SavedData {
 private static final String NAME="kingdomsmp_apiary_jobs_v1";private final ApiaryJobsEvidence evidence;
 public ApiaryJobsSavedData(){evidence=new ApiaryJobsEvidence();}private ApiaryJobsSavedData(ApiaryJobsEvidence evidence){this.evidence=evidence;}public ApiaryJobsEvidence evidence(){return evidence;}
 public static ApiaryJobsSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Apiary jobs are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new ApiaryJobsSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect apiary job data",failed);}throw new IllegalStateException("Existing apiary job data requires matching recovery; never reset");},ApiaryJobsSavedData::load),NAME);}
 public static ApiaryJobsSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new ApiaryJobsSavedData(ApiaryJobsEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
