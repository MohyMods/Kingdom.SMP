package com.mohymods.kingdomsmp.profession;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
/** Separate additive schema; broken existing provenance never resets into countable natural timber. */
public final class TreeSavedData extends SavedData {
    private static final String NAME="kingdomsmp_tree_evidence_v1";
    private TreeEvidence evidence=new TreeEvidence();public TreeEvidence evidence(){return evidence;}
    public static TreeSavedData get(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("Tree saved data requires server thread");var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(()->{try{Files.readAttributes(path,BasicFileAttributes.class);}catch(NoSuchFileException missing){var data=new TreeSavedData();data.setDirty();return data;}catch(IOException failure){throw new IllegalStateException("Cannot inspect tree evidence",failure);}throw new IllegalStateException("Existing tree evidence failed to load; restore matching checkpoint");},TreeSavedData::load),NAME);
    }
    public static TreeSavedData load(CompoundTag tag,HolderLookup.Provider provider){var data=new TreeSavedData();data.evidence=TreeEvidence.load(tag);return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){return evidence.save();}
}
