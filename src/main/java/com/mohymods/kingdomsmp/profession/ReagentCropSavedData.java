package com.mohymods.kingdomsmp.profession;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
public final class ReagentCropSavedData extends SavedData {
 private static final String NAME="kingdomsmp_reagent_crops_v1";private final ReagentCropProof proof;
 private ReagentCropSavedData(ReagentCropProof proof){this.proof=proof;}
 public ReagentCropProof proof(){return proof;}
 public static ReagentCropSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Reagent evidence is server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var d=new ReagentCropSavedData(new ReagentCropProof());d.setDirty();return d;}catch(java.io.IOException e){throw new IllegalStateException("Cannot inspect reagent evidence",e);}throw new IllegalStateException("Existing reagent evidence cannot load; matching recovery required");},(tag,registry)->new ReagentCropSavedData(ReagentCropProof.load(tag))),NAME);}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registry){return proof.save();}
}
