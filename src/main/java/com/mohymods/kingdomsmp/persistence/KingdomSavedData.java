package com.mohymods.kingdomsmp.persistence;

import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * World-scoped native persistence, stored only in the Overworld.
 *
 * <p>This is not an economy transaction store. Call from the server thread. The
 * state is immutable and this slice exposes no mutation or tier-grant operation.</p>
 */
public final class KingdomSavedData extends SavedData {
    public static final String DATA_NAME = "kingdomsmp_kingdom";
    private static final int MAX_CONTRIBUTORS = 3712;
    private final KingdomState state;
    private final TierOneProgress tierOneProgress;

    KingdomSavedData(KingdomState state, TierOneProgress tierOneProgress) {
        this.state = Objects.requireNonNull(state, "state");
        this.tierOneProgress = Objects.requireNonNull(tierOneProgress, "tierOneProgress");
    }

    public static KingdomSavedData get(MinecraftServer server) {
        Path dataFile = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve(DATA_NAME + ".dat");
        Factory<KingdomSavedData> factory = new Factory<>(
                () -> createUnlessExisting(dataFile), KingdomSavedData::load);
        return server.overworld().getDataStorage().computeIfAbsent(factory, DATA_NAME);
    }

    /** Returns the immutable server snapshot; it cannot be used to mutate storage. */
    public KingdomState snapshot() {
        return state;
    }

    /** Returns immutable contribution accounting; no inventory mutation is exposed. */
    public TierOneProgress tierOneProgress() {
        return tierOneProgress;
    }

    static KingdomSavedData createUnlessExisting(Path dataFile) {
        // DimensionDataStorage may log a failed deserialization and then invoke the
        // supplier. Never let a corrupt/future-schema existing save become a fresh
        // kingdom and overwrite it on the next world save.
        try {
            Files.readAttributes(dataFile, BasicFileAttributes.class);
        } catch (NoSuchFileException absent) {
            KingdomSavedData fresh = new KingdomSavedData(KingdomState.initial(), TierOneProgress.initial());
            fresh.setDirty();
            return fresh;
        } catch (IOException inaccessible) {
            throw new IllegalStateException("Cannot inspect existing kingdom data: " + dataFile,
                    inaccessible);
        }
        throw new IllegalStateException("Existing kingdom data could not be loaded. "
                + "Restore or migrate it before continuing: " + dataFile);
    }

    static KingdomSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        requireTag(tag, "schema", Tag.TAG_INT);
        requireTag(tag, "revision", Tag.TAG_LONG);
        requireTag(tag, "tier", Tag.TAG_INT);
        requireTag(tag, "tierOne", Tag.TAG_COMPOUND);
        return new KingdomSavedData(new KingdomState(
                tag.getInt("schema"), tag.getLong("revision"), tag.getInt("tier")),
                loadTierOne(tag.getCompound("tierOne")));
    }

    static TierOneProgress loadTierOne(CompoundTag tag) {
        requireTag(tag, "revision", Tag.TAG_LONG);
        if (!(tag.get("contributors") instanceof ListTag list)
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Missing or invalid kingdom contributor list");
        }
        // Every recorded contributor has at least one accepted item, so more
        // contributors than the total resource requirements cannot be legitimate.
        if (list.size() > MAX_CONTRIBUTORS) {
            throw new IllegalArgumentException("Kingdom contributor list exceeds resource bound");
        }
        Map<UUID, TierOneProgress.Contributor> contributors = new HashMap<>();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            requireUuid(entry, "player");
            requireTag(entry, "logs", Tag.TAG_INT);
            requireTag(entry, "cobblestone", Tag.TAG_INT);
            requireTag(entry, "wheat", Tag.TAG_INT);
            requireTag(entry, "ironIngots", Tag.TAG_INT);
            requireTag(entry, "lastPositiveSequence", Tag.TAG_LONG);
            UUID player = entry.getUUID("player");
            TierOneProgress.Contributor contributor = new TierOneProgress.Contributor(
                    entry.getInt("logs"), entry.getInt("cobblestone"), entry.getInt("wheat"),
                    entry.getInt("ironIngots"), entry.getLong("lastPositiveSequence"));
            if (contributors.putIfAbsent(player, contributor) != null) {
                throw new IllegalArgumentException("Duplicate kingdom contributor UUID");
            }
        }
        Optional<UUID> winner = Optional.empty();
        if (tag.contains("winner")) {
            requireUuid(tag, "winner");
            winner = Optional.of(tag.getUUID("winner"));
        }
        return new TierOneProgress(tag.getLong("revision"), contributors, winner);
    }

    static CompoundTag saveTierOne(TierOneProgress progress) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("revision", progress.revision());
        ListTag contributors = new ListTag();
        // Stable ordering simplifies world-save review and backup comparisons.
        progress.contributors().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag player = new CompoundTag();
            TierOneProgress.Contributor contribution = entry.getValue();
            player.putUUID("player", entry.getKey());
            player.putInt("logs", contribution.logs());
            player.putInt("cobblestone", contribution.cobblestone());
            player.putInt("wheat", contribution.wheat());
            player.putInt("ironIngots", contribution.ironIngots());
            player.putLong("lastPositiveSequence", contribution.lastPositiveSequence());
            contributors.add(player);
        });
        tag.put("contributors", contributors);
        progress.winner().ifPresent(winner -> tag.putUUID("winner", winner));
        return tag;
    }

    private static void requireUuid(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) {
            throw new IllegalArgumentException("Missing or invalid kingdom UUID: " + key);
        }
    }

    private static void requireTag(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) {
            throw new IllegalArgumentException("Missing or invalid kingdom field: " + key);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", state.schemaVersion());
        tag.putLong("revision", state.revision());
        tag.putInt("tier", state.attainedTier());
        tag.put("tierOne", saveTierOne(tierOneProgress));
        return tag;
    }
}
