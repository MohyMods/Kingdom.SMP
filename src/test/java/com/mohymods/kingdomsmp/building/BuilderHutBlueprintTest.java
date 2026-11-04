package com.mohymods.kingdomsmp.building;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuilderHutBlueprintTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void structureHasUniqueBoundedPlacementsAndFullSolidFoundationInEveryRotation() {
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        for (Rotation rotation : Rotation.values()) {
            Map<BlockPos, BlockState> blocks = index(blueprint.placements(rotation));
            assertEquals(blueprint.placements().size(), blocks.size());
            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                BlockPos position = entry.getKey();
                assertTrue(position.getX() >= 0 && position.getX() < blueprint.width(rotation));
                assertTrue(position.getZ() >= 0 && position.getZ() < blueprint.depth(rotation));
                assertTrue(position.getY() >= 0 && position.getY() < BuilderHutBlueprint.HEIGHT);
                assertFalse(entry.getValue().isAir());
            }
            for (int x = 0; x < blueprint.width(rotation); x++) {
                for (int z = 0; z < blueprint.depth(rotation); z++) {
                    BlockPos position = new BlockPos(x, 0, z);
                    BlockState foundation = blocks.get(position);
                    assertNotNull(foundation, "Foundation must cover the entire reserved footprint");
                    assertTrue(foundation.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, position));
                }
            }
        }
    }

    @Test
    void entranceHasWorkingPairedDoorAndUnobstructedHeadroomToAldricsHome() {
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        for (Rotation rotation : Rotation.values()) {
            Map<BlockPos, BlockState> blocks = index(blueprint.placements(rotation));
            BlockPos doorPosition = blueprint.rotateOffset(blueprint.doorOffset(), rotation);
            BlockState lower = blocks.get(doorPosition);
            BlockState upper = blocks.get(doorPosition.above());
            assertTrue(lower.is(Blocks.SPRUCE_DOOR));
            assertTrue(upper.is(Blocks.SPRUCE_DOOR));
            assertEquals(DoubleBlockHalf.LOWER, lower.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF));
            assertEquals(DoubleBlockHalf.UPPER, upper.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF));
            assertEquals(rotation.rotate(Direction.NORTH), lower.getValue(BlockStateProperties.HORIZONTAL_FACING));
            assertFalse(lower.getValue(BlockStateProperties.OPEN));
            for (int z = 0; z <= 6; z++) {
                if (z == 1) continue; // The door opens onto this otherwise clear center aisle.
                BlockPos feet = blueprint.rotateOffset(new BlockPos(5, 1, z), rotation);
                assertFalse(blocks.containsKey(feet), "Furniture must not block the center aisle");
                assertFalse(blocks.containsKey(feet.above()), "The center aisle needs two-block headroom");
            }
            BlockPos home = blueprint.rotateOffset(blueprint.homeOffset(), rotation);
            assertFalse(blocks.containsKey(home));
            assertFalse(blocks.containsKey(home.above()));
            assertNotNull(blocks.get(home.below()));
        }
    }

    @Test
    void furnishedInteriorContainsUsableBedStorageAndBuildersWorkstations() {
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        Map<BlockPos, BlockState> blocks = index(blueprint.placements());
        assertEquals(2, blocks.values().stream().filter(state -> state.is(Blocks.BARREL)).count());
        assertEquals(3, blocks.values().stream().filter(state -> state.is(Blocks.LANTERN)).count());
        for (var block : List.of(Blocks.CRAFTING_TABLE, Blocks.STONECUTTER, Blocks.CARTOGRAPHY_TABLE,
                Blocks.BOOKSHELF, Blocks.POTTED_FERN, Blocks.GLASS)) {
            assertTrue(blocks.values().stream().anyMatch(state -> state.is(block)), block.toString());
        }
        BlockState foot = blocks.get(new BlockPos(2, 1, 5));
        BlockState head = blocks.get(new BlockPos(2, 1, 6));
        assertTrue(foot.is(Blocks.RED_BED));
        assertTrue(head.is(Blocks.RED_BED));
        assertEquals(BedPart.FOOT, foot.getValue(BlockStateProperties.BED_PART));
        assertEquals(BedPart.HEAD, head.getValue(BlockStateProperties.BED_PART));
        assertEquals(Direction.SOUTH, foot.getValue(BlockStateProperties.HORIZONTAL_FACING));
        assertEquals(Direction.SOUTH, head.getValue(BlockStateProperties.HORIZONTAL_FACING));
        assertFalse(foot.getValue(BlockStateProperties.OCCUPIED));
    }

    @Test
    void rotationPreservesPairedFurnitureAndChangesAllDirectionalStates() {
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        for (Rotation rotation : Rotation.values()) {
            Map<BlockPos, BlockState> rotated = index(blueprint.placements(rotation));
            BlockPos footPosition = blueprint.rotateOffset(new BlockPos(2, 1, 5), rotation);
            Direction bedFacing = rotation.rotate(Direction.SOUTH);
            BlockState foot = rotated.get(footPosition);
            BlockState head = rotated.get(footPosition.relative(bedFacing));
            assertEquals(bedFacing, foot.getValue(BlockStateProperties.HORIZONTAL_FACING));
            assertEquals(BedPart.HEAD, head.getValue(BlockStateProperties.BED_PART));
            BlockState cutter = rotated.get(blueprint.rotateOffset(new BlockPos(8, 1, 3), rotation));
            assertEquals(rotation.rotate(Direction.WEST), cutter.getValue(BlockStateProperties.HORIZONTAL_FACING));
            BlockState barrel = rotated.get(blueprint.rotateOffset(new BlockPos(8, 1, 5), rotation));
            assertEquals(rotation.rotate(Direction.WEST), barrel.getValue(BlockStateProperties.FACING));
        }
        assertEquals(BuilderHutBlueprint.DEPTH, blueprint.width(Rotation.CLOCKWISE_90));
        assertEquals(BuilderHutBlueprint.WIDTH, blueprint.depth(Rotation.CLOCKWISE_90));
    }

    @Test
    void placementsCannotBeMutatedAndSnapshotMutablePositionInputs() {
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        assertThrows(UnsupportedOperationException.class, () -> blueprint.placements().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> blueprint.placements(Rotation.CLOCKWISE_90).clear());
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        BuilderHutBlueprint.Placement placement = new BuilderHutBlueprint.Placement(mutable,
                Blocks.STONE_BRICKS.defaultBlockState());
        mutable.set(9, 9, 9);
        assertEquals(new BlockPos(1, 2, 3), placement.offset());
    }

    @Test
    void structuralSupportPrecedesDependentDoorBedAndLightingBlocks() {
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        Map<BlockPos, Integer> order = new HashMap<>();
        List<BuilderHutBlueprint.Placement> placements = blueprint.placements();
        for (int index = 0; index < placements.size(); index++) order.put(placements.get(index).offset(), index);
        for (BuilderHutBlueprint.Placement placement : placements) {
            BlockState state = placement.state();
            if (state.is(Blocks.RED_BED) || (state.is(Blocks.SPRUCE_DOOR)
                    && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER)) {
                assertTrue(order.get(placement.offset().below()) < order.get(placement.offset()));
            } else if (state.is(Blocks.LANTERN)) {
                assertTrue(state.getValue(BlockStateProperties.HANGING));
                assertTrue(order.get(placement.offset().above()) < order.get(placement.offset()));
            }
        }
    }

    private static Map<BlockPos, BlockState> index(List<BuilderHutBlueprint.Placement> placements) {
        return placements.stream().collect(Collectors.toUnmodifiableMap(
                BuilderHutBlueprint.Placement::offset, BuilderHutBlueprint.Placement::state));
    }
}
