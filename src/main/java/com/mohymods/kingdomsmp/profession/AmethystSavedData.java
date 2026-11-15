package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class AmethystSavedData extends SavedData {
 private static final java.util.Map<MinecraftServer,Boolean> PRESENT=new java.util.WeakHashMap<>();
 public static boolean present(MinecraftServer server){return PRESENT.computeIfAbsent(server,s->Files.exists(s.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_amethyst_v1.dat")));}
 private static final String NAME="kingdomsmp_amethyst_v1";private final AmethystEvidence evidence;
 public AmethystSavedData(){evidence=new AmethystEvidence();}private AmethystSavedData(AmethystEvidence evidence){this.evidence=evidence;}public AmethystEvidence evidence(){return evidence;}
 public static AmethystSavedData get(MinecraftServer server){PRESENT.put(server,true);if(!server.isSameThread())throw new IllegalStateException("Amethyst observations are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new AmethystSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect amethyst data",failed);}throw new IllegalStateException("Existing amethyst data requires matching recovery; never reset");},AmethystSavedData::load),NAME);}
 public static AmethystSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new AmethystSavedData(AmethystEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
