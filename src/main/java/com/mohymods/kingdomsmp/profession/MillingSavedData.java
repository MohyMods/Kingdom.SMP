package com.mohymods.kingdomsmp.profession;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
public final class MillingSavedData extends SavedData {
 private static final String NAME="kingdomsmp_milling_v1";private final MillingEvidence proof;
 private MillingSavedData(MillingEvidence proof){this.proof=proof;}
 public MillingEvidence proof(){return proof;}
 public static MillingSavedData get(MinecraftServer server){if(!server.isSameThread())throw new IllegalStateException("Milling evidence is server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var d=new MillingSavedData(new MillingEvidence());d.setDirty();return d;}catch(java.io.IOException e){throw new IllegalStateException("Cannot inspect milling evidence",e);}throw new IllegalStateException("Existing milling evidence cannot load; matching recovery required");},(tag,registry)->new MillingSavedData(MillingEvidence.load(tag))),NAME);}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registry){return proof.save();}
}
