package com.mohymods.kingdomsmp.bounty;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** World save evidence only. Corruption must not silently regenerate the authentication key. */
public final class HuntSavedData extends SavedData {
    private static final String NAME="kingdomsmp_hunts";
    private HuntEvidence evidence=new HuntEvidence();
    public HuntEvidence evidence() { return evidence; }
    public static HuntSavedData get(MinecraftServer server) {
        var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{
            try { Files.readAttributes(path,BasicFileAttributes.class); }
            catch (NoSuchFileException absent) { var fresh=new HuntSavedData();fresh.setDirty();return fresh; }
            catch (IOException e) { throw new IllegalStateException("Cannot inspect hunt evidence",e); }
            throw new IllegalStateException("Existing hunt evidence failed to load; restore its matching world save: "+path);
        },HuntSavedData::load),NAME);
    }
    public static HuntSavedData load(CompoundTag tag,HolderLookup.Provider ignored) { var data=new HuntSavedData();data.evidence=HuntEvidence.load(tag);return data; }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider ignored) { return evidence.save(); }
}
