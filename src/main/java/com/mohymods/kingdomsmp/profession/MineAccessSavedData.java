package com.mohymods.kingdomsmp.profession;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
public final class MineAccessSavedData extends SavedData {
 private static final java.util.Map<MinecraftServer,Boolean> PRESENT=new java.util.WeakHashMap<>();
 public static boolean present(MinecraftServer server){return PRESENT.computeIfAbsent(server,s->Files.exists(s.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_mine_access_v1.dat")));}
 private static final String NAME="kingdomsmp_mine_access_v1";private final MineAccessEvidence evidence;
 public MineAccessSavedData(){evidence=new MineAccessEvidence();}private MineAccessSavedData(MineAccessEvidence evidence){this.evidence=evidence;}public MineAccessEvidence evidence(){return evidence;}
 public static MineAccessSavedData get(MinecraftServer server){PRESENT.put(server,true);if(!server.isSameThread())throw new IllegalStateException("Mine access are server-thread only");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new MineAccessSavedData();data.setDirty();return data;}catch(IOException failed){throw new IllegalStateException("Cannot inspect mine access data",failed);}throw new IllegalStateException("Existing mine access data requires matching recovery; never reset");},MineAccessSavedData::load),NAME);}
 public static MineAccessSavedData load(CompoundTag tag,HolderLookup.Provider registries){return new MineAccessSavedData(MineAccessEvidence.load(tag));}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){return evidence.save();}
}
