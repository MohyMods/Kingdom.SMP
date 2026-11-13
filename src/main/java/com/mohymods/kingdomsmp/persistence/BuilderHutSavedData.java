package com.mohymods.kingdomsmp.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** One kingdom-owned starter hut. Mutations belong exclusively to the server thread. */
public final class BuilderHutSavedData extends SavedData {
    public static final String DATA_NAME = "kingdomsmp_builder_hut";
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_PLACED_BLOCKS = 1000;
    public static final String DEFAULT_NAME = "Aldric Stonehand";

    public enum Phase { INSTALLING, ACTIVE }

    /** NPC issuance and death are one-way: loading a missing entity never grants a replacement. */
    public record Hut(UUID id, BlockPos origin, Rotation rotation, UUID npcId,
                      String displayName, Phase phase, int placedBlocks,
                      boolean npcSpawnCommitted, boolean npcDead) {
        public Hut {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(rotation, "rotation");
            Objects.requireNonNull(npcId, "npcId");
            Objects.requireNonNull(phase, "phase");
            origin = origin.immutable();
            validateName(displayName);
            if (Math.abs((long) origin.getX()) >= 30_000_000L
                    || Math.abs((long) origin.getZ()) >= 30_000_000L
                    || origin.getY() < -2048 || origin.getY() > 2047) {
                throw new IllegalArgumentException("Builder's Hut origin exceeds world bounds");
            }
            if (placedBlocks < 0 || placedBlocks > MAX_PLACED_BLOCKS) {
                throw new IllegalArgumentException("Invalid Builder's Hut placement cursor");
            }
            if (phase == Phase.ACTIVE && !npcSpawnCommitted) {
                throw new IllegalArgumentException("Active hut has no committed builder identity");
            }
            if (npcDead && !npcSpawnCommitted) {
                throw new IllegalArgumentException("Unissued builder cannot be recorded dead");
            }
        }

        private Hut change(String name, Phase nextPhase, int cursor, boolean issued, boolean dead) {
            return new Hut(id, origin, rotation, npcId, name, nextPhase, cursor, issued, dead);
        }
    }

    private long revision;
    private Hut hut;

    BuilderHutSavedData(long revision, Hut hut) {
        if (revision < 0) throw new IllegalArgumentException("Negative Builder's Hut revision");
        if (hut == null && revision != 0) throw new IllegalArgumentException("Empty hut has nonzero revision");
        if (hut != null && revision == 0) throw new IllegalArgumentException("Installed hut has no revision");
        this.revision = revision;
        this.hut = hut;
    }

    public static BuilderHutSavedData get(MinecraftServer server) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(DATA_NAME + ".dat");
        Factory<BuilderHutSavedData> factory = new Factory<>(
                () -> createUnlessExisting(file), BuilderHutSavedData::load);
        return server.overworld().getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    public Optional<Hut> hut() { return Optional.ofNullable(hut); }
    public long revision() { return revision; }

    public Hut begin(BlockPos origin, Rotation rotation, UUID npcId) {
        if (hut != null) throw new IllegalStateException("The kingdom already has a Builder's Hut reservation");
        replace(new Hut(UUID.randomUUID(), origin, rotation, npcId, DEFAULT_NAME,
                Phase.INSTALLING, 0, false, false));
        return hut;
    }

    public void advance(int cursor) {
        Hut current = requireHut();
        if (current.phase() != Phase.INSTALLING) throw new IllegalStateException("Hut installation is already complete");
        if (cursor < current.placedBlocks()) throw new IllegalArgumentException("Placement cursor cannot move backwards");
        if (cursor != current.placedBlocks()) {
            replace(current.change(current.displayName(), current.phase(), cursor,
                    current.npcSpawnCommitted(), current.npcDead()));
        }
    }

    public void markNpcSpawnCommitted() {
        Hut current = requireHut();
        if (!current.npcSpawnCommitted()) {
            replace(current.change(current.displayName(), current.phase(), current.placedBlocks(), true, false));
        }
    }

    public void activate() {
        Hut current = requireHut();
        if (!current.npcSpawnCommitted()) throw new IllegalStateException("Commit the builder identity before activation");
        if (current.phase() != Phase.ACTIVE) {
            replace(current.change(current.displayName(), Phase.ACTIVE, current.placedBlocks(), true, current.npcDead()));
        }
    }

    public void rename(String name) {
        validateName(name);
        Hut current = requireHut();
        if (!current.displayName().equals(name)) {
            replace(current.change(name, current.phase(), current.placedBlocks(), current.npcSpawnCommitted(), current.npcDead()));
        }
    }

    public void markNpcDead() {
        Hut current = requireHut();
        if (!current.npcSpawnCommitted()) throw new IllegalStateException("Unissued builder cannot die");
        if (!current.npcDead()) {
            replace(current.change(current.displayName(), current.phase(), current.placedBlocks(), true, true));
        }
    }

    private Hut requireHut() {
        if (hut == null) throw new IllegalStateException("The kingdom has no Builder's Hut");
        return hut;
    }

    private void replace(Hut next) {
        long nextRevision = Math.incrementExact(revision);
        hut = next;
        revision = nextRevision;
        setDirty();
    }

    public static void validateName(String name) {
        Objects.requireNonNull(name, "displayName");
        if (name.isBlank() || name.length() > 48 || !name.equals(name.strip())
                || name.codePoints().anyMatch(c -> Character.isISOControl(c) || c == '\u00a7')) {
            throw new IllegalArgumentException("Builder name must be 1–48 plain characters without surrounding whitespace");
        }
    }

    static BuilderHutSavedData createUnlessExisting(Path file) {
        try {
            Files.readAttributes(file, BasicFileAttributes.class);
        } catch (NoSuchFileException absent) {
            BuilderHutSavedData data = new BuilderHutSavedData(0, null);
            data.setDirty();
            return data;
        } catch (IOException inaccessible) {
            throw new IllegalStateException("Cannot inspect existing Builder's Hut data: " + file, inaccessible);
        }
        throw new IllegalStateException("Existing Builder's Hut data could not be loaded. Restore or migrate it: " + file);
    }

    static BuilderHutSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        require(tag, "schema", Tag.TAG_INT);
        require(tag, "revision", Tag.TAG_LONG);
        require(tag, "hasHut", Tag.TAG_BYTE);
        if (tag.getInt("schema") != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported Builder's Hut schema");
        boolean hasHut = strictBoolean(tag, "hasHut");
        if (!hasHut) {
            if (tag.contains("hut")) throw new IllegalArgumentException("Unexpected Builder's Hut record");
            return new BuilderHutSavedData(tag.getLong("revision"), null);
        }
        require(tag, "hut", Tag.TAG_COMPOUND);
        CompoundTag record = tag.getCompound("hut");
        for (String key : new String[]{"id", "npcId"}) {
            if (!record.hasUUID(key)) throw new IllegalArgumentException("Invalid Builder's Hut UUID: " + key);
        }
        for (String key : new String[]{"x", "y", "z", "placedBlocks"}) require(record, key, Tag.TAG_INT);
        for (String key : new String[]{"rotation", "displayName", "phase"}) require(record, key, Tag.TAG_STRING);
        require(record, "npcSpawnCommitted", Tag.TAG_BYTE);
        require(record, "npcDead", Tag.TAG_BYTE);
        Hut hut = new Hut(record.getUUID("id"), new BlockPos(record.getInt("x"), record.getInt("y"), record.getInt("z")),
                Rotation.valueOf(record.getString("rotation")), record.getUUID("npcId"), record.getString("displayName"),
                Phase.valueOf(record.getString("phase")), record.getInt("placedBlocks"),
                strictBoolean(record, "npcSpawnCommitted"), strictBoolean(record, "npcDead"));
        return new BuilderHutSavedData(tag.getLong("revision"), hut);
    }

    private static boolean strictBoolean(CompoundTag tag, String key) {
        byte value = tag.getByte(key);
        if (value != 0 && value != 1) throw new IllegalArgumentException("Invalid Builder's Hut boolean: " + key);
        return value == 1;
    }

    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or invalid Builder's Hut field: " + key);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", SCHEMA_VERSION);
        tag.putLong("revision", revision);
        tag.putBoolean("hasHut", hut != null);
        tag.remove("hut");
        if (hut != null) {
            CompoundTag record = new CompoundTag();
            record.putUUID("id", hut.id());
            record.putUUID("npcId", hut.npcId());
            record.putInt("x", hut.origin().getX());
            record.putInt("y", hut.origin().getY());
            record.putInt("z", hut.origin().getZ());
            record.putString("rotation", hut.rotation().name());
            record.putString("displayName", hut.displayName());
            record.putString("phase", hut.phase().name());
            record.putInt("placedBlocks", hut.placedBlocks());
            record.putBoolean("npcSpawnCommitted", hut.npcSpawnCommitted());
            record.putBoolean("npcDead", hut.npcDead());
            tag.put("hut", record);
        }
        return tag;
    }
}
