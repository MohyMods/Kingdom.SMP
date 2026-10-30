package com.mohymods.kingdomsmp.persistence;

import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KingdomSavedDataTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsDirtyInitialStateOnlyWhenThereIsNoExistingSave() {
        KingdomSavedData fresh = KingdomSavedData.createUnlessExisting(
                temporaryDirectory.resolve("kingdom.dat"));
        assertEquals(KingdomState.initial(), fresh.snapshot());
        assertTrue(fresh.isDirty(), "New state must be written during the world save");
    }

    @Test
    void existingUnreadableOrCorruptSaveCannotFallBackToFreshState() throws IOException {
        Path save = temporaryDirectory.resolve("kingdom.dat");
        Files.writeString(save, "not valid NBT");
        assertThrows(IllegalStateException.class,
                () -> KingdomSavedData.createUnlessExisting(save));
        assertEquals("not valid NBT", Files.readString(save));
    }

    @Test
    void actualNbtRoundTripRetainsTheEntireAuthoritativeSnapshotWithoutDirtyingIt() {
        CompoundTag input = validStateTag();
        KingdomSavedData restored = KingdomSavedData.load(input, null);
        assertEquals(new KingdomState(1, 73, 9), restored.snapshot());
        assertFalse(restored.isDirty());

        CompoundTag output = restored.save(new CompoundTag(), null);
        KingdomSavedData reloaded = KingdomSavedData.load(output, null);
        assertEquals(restored.snapshot(), reloaded.snapshot());
        assertFalse(reloaded.isDirty());
    }

    @Test
    void futureSchemaFailsInsteadOfResettingProgress() {
        CompoundTag input = validStateTag();
        input.putInt("schema", 2);
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void omittedRevisionCannotBecomeAnApparentlyValidDefaultZero() {
        CompoundTag input = validStateTag();
        input.remove("revision");
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void numericTagTypesCannotBeSilentlyCoerced() {
        CompoundTag input = validStateTag();
        input.putString("tier", "9");
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void invalidStoredTierFailsInsteadOfBeingClamped() {
        CompoundTag input = validStateTag();
        input.putInt("tier", 13);
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void partialProgressRoundTripRetainsPlayerCountsSequenceAndLeaderboard() {
        UUID alice = new UUID(0, 1);
        UUID bob = new UUID(0, 2);
        TierOneProgress progress = TierOneProgress.initial()
                .contribute(alice, TierOneProgress.Resource.LOGS, 100).progress()
                .contribute(bob, TierOneProgress.Resource.IRON_INGOTS, 20).progress();
        KingdomSavedData saved = new KingdomSavedData(KingdomState.initial(), progress);
        KingdomSavedData loaded = KingdomSavedData.load(saved.save(new CompoundTag(), null), null);
        assertEquals(progress, loaded.tierOneProgress());
        assertEquals(bob, loaded.tierOneProgress().leader().orElseThrow());
        assertTrue(loaded.tierOneProgress().winner().isEmpty());
        assertFalse(loaded.isDirty());
    }

    @Test
    void completedProgressRoundTripRetainsExactlyTheSameWinnerWithoutGrantingTier() {
        UUID alice = new UUID(0, 1);
        TierOneProgress progress = TierOneProgress.initial();
        for (TierOneProgress.Resource resource : TierOneProgress.Resource.values()) {
            progress = progress.contribute(alice, resource, resource.required()).progress();
        }
        KingdomSavedData saved = new KingdomSavedData(KingdomState.initial(), progress);
        KingdomSavedData loaded = KingdomSavedData.load(saved.save(new CompoundTag(), null), null);
        assertEquals(progress, loaded.tierOneProgress());
        assertEquals(alice, loaded.tierOneProgress().winner().orElseThrow());
        assertEquals(1, loaded.snapshot().attainedTier());
        assertFalse(loaded.tierOneProgress().contribute(alice, TierOneProgress.Resource.LOGS, 1).completedNow());
    }

    @Test
    void missingProgressCannotFallBackToEmptyContributionTotals() {
        CompoundTag input = validStateTag();
        input.remove("tierOne");
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void duplicateContributorIdsCannotBeMergedOrOverwrittenDuringLoad() {
        CompoundTag input = validStateTag();
        CompoundTag tierOne = input.getCompound("tierOne");
        tierOne.putLong("revision", 1);
        ListTag contributors = new ListTag();
        contributors.add(validContributor());
        contributors.add(validContributor());
        tierOne.put("contributors", contributors);
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void wrongListElementTypeIsRejectedInsteadOfBecomingAnEmptyList() {
        CompoundTag input = validStateTag();
        ListTag contributors = new ListTag();
        contributors.add(IntTag.valueOf(1));
        input.getCompound("tierOne").put("contributors", contributors);
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void oversizedContributorListsAreRejectedBeforeDeserializingEntries() {
        CompoundTag input = validStateTag();
        ListTag contributors = new ListTag();
        for (int index = 0; index < 3713; index++) contributors.add(new CompoundTag());
        input.getCompound("tierOne").put("contributors", contributors);
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void malformedWinnerIsRejectedInsteadOfDroppingEntitlement() {
        CompoundTag input = validStateTag();
        input.getCompound("tierOne").putString("winner", "not a UUID");
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    @Test
    void invalidContributorCountsAreRejected() {
        CompoundTag input = validStateTag();
        CompoundTag player = validContributor();
        player.putInt("logs", -1);
        ListTag contributors = new ListTag();
        contributors.add(player);
        input.getCompound("tierOne").putLong("revision", 1);
        input.getCompound("tierOne").put("contributors", contributors);
        assertThrows(IllegalArgumentException.class, () -> KingdomSavedData.load(input, null));
    }

    private static CompoundTag validContributor() {
        CompoundTag player = new CompoundTag();
        player.putUUID("player", new UUID(0, 1));
        player.putInt("logs", 1);
        player.putInt("cobblestone", 0);
        player.putInt("wheat", 0);
        player.putInt("ironIngots", 0);
        player.putLong("lastPositiveSequence", 1);
        return player;
    }

    private static CompoundTag validStateTag() {
        CompoundTag input = new CompoundTag();
        input.putInt("schema", 1);
        input.putLong("revision", 73);
        input.putInt("tier", 9);
        CompoundTag tierOne = new CompoundTag();
        tierOne.putLong("revision", 0);
        tierOne.put("contributors", new ListTag());
        input.put("tierOne", tierOne);
        return input;
    }
}
