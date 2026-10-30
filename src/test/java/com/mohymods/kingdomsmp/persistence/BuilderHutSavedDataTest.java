package com.mohymods.kingdomsmp.persistence;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BuilderHutSavedDataTest {
    @TempDir Path temporaryDirectory;

    private BuilderHutSavedData fresh() { return new BuilderHutSavedData(0, null); }

    @Test
    void absentSaveCreatesDirtyEmptyStateButExistingCorruptionNeverResets() throws IOException {
        Path file = temporaryDirectory.resolve("hut.dat");
        BuilderHutSavedData data = BuilderHutSavedData.createUnlessExisting(file);
        assertTrue(data.hut().isEmpty());
        assertTrue(data.isDirty());
        Files.writeString(file, "broken NBT");
        assertThrows(IllegalStateException.class, () -> BuilderHutSavedData.createUnlessExisting(file));
        assertEquals("broken NBT", Files.readString(file));
    }

    @Test
    void reservationIsUniqueAndRetainsExactIdentitySiteAndRotation() {
        BuilderHutSavedData data = fresh();
        UUID npc = UUID.randomUUID();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(7, 65, -30);
        BuilderHutSavedData.Hut reserved = data.begin(mutable, Rotation.CLOCKWISE_90, npc);
        mutable.set(0, 0, 0);
        assertEquals(new BlockPos(7, 65, -30), reserved.origin());
        assertEquals(npc, reserved.npcId());
        assertEquals(Rotation.CLOCKWISE_90, reserved.rotation());
        assertThrows(IllegalStateException.class, () -> data.begin(BlockPos.ZERO, Rotation.NONE, UUID.randomUUID()));
        assertEquals(reserved, data.hut().orElseThrow());
    }

    @Test
    void interruptedInstallationRoundTripRetainsCursorWithoutReissuingBuilder() {
        BuilderHutSavedData data = fresh();
        data.begin(new BlockPos(30, 65, 90), Rotation.COUNTERCLOCKWISE_90, UUID.randomUUID());
        data.advance(100);
        data.markNpcSpawnCommitted();
        BuilderHutSavedData restored = BuilderHutSavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals(data.hut(), restored.hut());
        assertEquals(data.revision(), restored.revision());
        assertTrue(restored.hut().orElseThrow().npcSpawnCommitted());
        assertFalse(restored.isDirty());
    }

    @Test
    void missingEntityAndConfirmedDeathCannotResetCommittedIssuance() {
        BuilderHutSavedData data = fresh();
        data.begin(BlockPos.ZERO, Rotation.NONE, UUID.randomUUID());
        assertThrows(IllegalStateException.class, data::activate);
        data.markNpcSpawnCommitted();
        data.activate();
        data.markNpcDead();
        long revision = data.revision();
        data.markNpcSpawnCommitted();
        data.markNpcDead();
        data.activate();
        assertEquals(revision, data.revision());
        BuilderHutSavedData restored = BuilderHutSavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals(BuilderHutSavedData.Phase.ACTIVE, restored.hut().orElseThrow().phase());
        assertTrue(restored.hut().orElseThrow().npcDead());
        assertTrue(restored.hut().orElseThrow().npcSpawnCommitted());
    }

    @Test
    void renameChangesOnlyDisplayNameAndSurvivesRestart() {
        BuilderHutSavedData data = fresh();
        BuilderHutSavedData.Hut before = data.begin(new BlockPos(1, 64, 1), Rotation.NONE, UUID.randomUUID());
        data.rename("Aldric of Stonebridge");
        BuilderHutSavedData restored = BuilderHutSavedData.load(data.save(new CompoundTag(), null), null);
        BuilderHutSavedData.Hut after = restored.hut().orElseThrow();
        assertEquals(before.id(), after.id());
        assertEquals(before.npcId(), after.npcId());
        assertEquals(before.origin(), after.origin());
        assertEquals("Aldric of Stonebridge", after.displayName());
        assertThrows(IllegalArgumentException.class, () -> data.rename("\u00a76King"));
        assertThrows(IllegalArgumentException.class, () -> data.rename("\nAldric"));
        assertThrows(IllegalArgumentException.class, () -> data.rename(" "));
    }

    @Test
    void cursorCannotRegressOrExceedBoundAndCompletedInstallationCannotResume() {
        BuilderHutSavedData data = fresh();
        data.begin(BlockPos.ZERO, Rotation.NONE, UUID.randomUUID());
        data.advance(20);
        assertThrows(IllegalArgumentException.class, () -> data.advance(19));
        assertThrows(IllegalArgumentException.class, () -> data.advance(1001));
        assertEquals(20, data.hut().orElseThrow().placedBlocks());
        data.markNpcSpawnCommitted();
        data.activate();
        assertThrows(IllegalStateException.class, () -> data.advance(21));
    }

    @Test
    void futureMissingAndWrongTypedFieldsCannotBecomeDefaults() {
        BuilderHutSavedData data = fresh();
        data.begin(BlockPos.ZERO, Rotation.NONE, UUID.randomUUID());
        CompoundTag valid = data.save(new CompoundTag(), null);
        CompoundTag future = valid.copy();
        future.putInt("schema", 2);
        assertThrows(IllegalArgumentException.class, () -> BuilderHutSavedData.load(future, null));
        CompoundTag missing = valid.copy();
        missing.getCompound("hut").remove("npcId");
        assertThrows(IllegalArgumentException.class, () -> BuilderHutSavedData.load(missing, null));
        CompoundTag wrong = valid.copy();
        wrong.getCompound("hut").putString("placedBlocks", "0");
        assertThrows(IllegalArgumentException.class, () -> BuilderHutSavedData.load(wrong, null));
        CompoundTag invalidBoolean = valid.copy();
        invalidBoolean.getCompound("hut").putByte("npcDead", (byte) 2);
        assertThrows(IllegalArgumentException.class, () -> BuilderHutSavedData.load(invalidBoolean, null));
    }

    @Test
    void invalidLifecycleAndCoordinatesFailInsteadOfRepairingSilently() {
        UUID id = UUID.randomUUID();
        UUID npc = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new BuilderHutSavedData.Hut(
                id, BlockPos.ZERO, Rotation.NONE, npc, "Aldric", BuilderHutSavedData.Phase.ACTIVE, 0, false, false));
        assertThrows(IllegalArgumentException.class, () -> new BuilderHutSavedData.Hut(
                id, BlockPos.ZERO, Rotation.NONE, npc, "Aldric", BuilderHutSavedData.Phase.INSTALLING, 0, false, true));
        assertThrows(IllegalArgumentException.class, () -> new BuilderHutSavedData.Hut(
                id, new BlockPos(Integer.MIN_VALUE, 64, 0), Rotation.NONE, npc, "Aldric", BuilderHutSavedData.Phase.INSTALLING, 0, false, false));
    }

    @Test
    void restartAfterEveryMutationBoundaryPreservesTheSingleReservationAndIssuance() {
        BuilderHutSavedData data = fresh();
        UUID npc = UUID.randomUUID();
        UUID hutId = data.begin(new BlockPos(10, 70, 20), Rotation.CLOCKWISE_180, npc).id();
        data = reload(data);
        assertFalse(data.hut().orElseThrow().npcSpawnCommitted());
        data.advance(1);
        data = reload(data);
        assertEquals(1, data.hut().orElseThrow().placedBlocks());
        data.advance(500);
        data = reload(data);
        data.markNpcSpawnCommitted();
        data = reload(data);
        assertEquals(BuilderHutSavedData.Phase.INSTALLING, data.hut().orElseThrow().phase());
        assertTrue(data.hut().orElseThrow().npcSpawnCommitted());
        data.activate();
        data = reload(data);
        data.rename("Master Aldric");
        data = reload(data);
        data.markNpcDead();
        data = reload(data);
        assertEquals(hutId, data.hut().orElseThrow().id());
        assertEquals(npc, data.hut().orElseThrow().npcId());
        assertEquals(500, data.hut().orElseThrow().placedBlocks());
        assertTrue(data.hut().orElseThrow().npcDead());
        assertEquals("Master Aldric", data.hut().orElseThrow().displayName());
    }

    @Test
    void revisionOverflowCannotApplyEvenPartOfAMutation() {
        BuilderHutSavedData data = fresh();
        BuilderHutSavedData.Hut before = data.begin(BlockPos.ZERO, Rotation.NONE, UUID.randomUUID());
        BuilderHutSavedData exhausted = new BuilderHutSavedData(Long.MAX_VALUE, before);
        assertThrows(ArithmeticException.class, () -> exhausted.advance(10));
        assertEquals(before, exhausted.hut().orElseThrow());
        assertEquals(Long.MAX_VALUE, exhausted.revision());
        assertFalse(exhausted.isDirty());
    }

    @Test
    void emptySaveRoundTripIsUnambiguousAndDoesNotAcceptAHiddenReservation() {
        BuilderHutSavedData empty = fresh();
        CompoundTag tag = empty.save(new CompoundTag(), null);
        assertTrue(BuilderHutSavedData.load(tag, null).hut().isEmpty());
        tag.put("hut", new CompoundTag());
        assertThrows(IllegalArgumentException.class, () -> BuilderHutSavedData.load(tag, null));
        tag.remove("hut");
        tag.putLong("revision", 1);
        assertThrows(IllegalArgumentException.class, () -> BuilderHutSavedData.load(tag, null));
    }

    private static BuilderHutSavedData reload(BuilderHutSavedData data) {
        BuilderHutSavedData restored = BuilderHutSavedData.load(data.save(new CompoundTag(), null), null);
        assertEquals(data.hut(), restored.hut());
        assertEquals(data.revision(), restored.revision());
        assertFalse(restored.isDirty());
        return restored;
    }
}
