package com.mohymods.kingdomsmp.fishing;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
/** Additive world evidence; an unreadable existing collection must never be silently replaced. */
public final class FishingSavedData extends SavedData {
 private static final String NAME="kingdomsmp_fishing";
 private FishingCatchBook book=new FishingCatchBook();
 public FishingCatchBook book(){return book;}
 public static FishingSavedData get(MinecraftServer server){var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException absent){var data=new FishingSavedData();data.setDirty();return data;}catch(IOException e){throw new IllegalStateException("Cannot inspect fishing collection",e);}throw new IllegalStateException("Existing fishing collection failed to load; restore matching world evidence");},FishingSavedData::load),NAME);}
 public static FishingSavedData load(CompoundTag tag,HolderLookup.Provider ignored){var data=new FishingSavedData();data.book=FishingCatchBook.load(tag);return data;}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider ignored){return book.save();}
}
