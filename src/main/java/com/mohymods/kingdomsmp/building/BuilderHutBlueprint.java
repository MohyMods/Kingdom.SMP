package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Original furnished starter hut. Coordinates are relative to its minimum
 * foundation corner; no method accesses a level, clears terrain or places blocks.
 */
public final class BuilderHutBlueprint {
    public static final String ID = "kingdom_smp:builders_hut";
    public static final int WIDTH = 11;
    public static final int DEPTH = 9;
    public static final int HEIGHT = 9;

    /** BlockPos is immutable here, even if a caller supplies a mutable subclass. */
    public record Placement(BlockPos offset, BlockState state) {
        public Placement {
            offset = Objects.requireNonNull(offset, "offset").immutable();
            Objects.requireNonNull(state, "state");
        }
    }

    private final List<Placement> placements;

    private BuilderHutBlueprint(List<Placement> placements) {
        this.placements = List.copyOf(placements);
    }

    /** Construct after Minecraft's block registry has initialized. */
    public static BuilderHutBlueprint create() {
        List<Placement> blocks = new ArrayList<>();

        // A complete supported platform, with a timber interior and stone apron.
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                BlockState floor = x >= 2 && x <= 8 && z >= 2 && z <= 6
                        ? Blocks.SPRUCE_PLANKS.defaultBlockState() : foundation(x, z);
                add(blocks, x, 0, z, floor);
            }
        }

        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 9; x++) {
                for (int z = 1; z <= 7; z++) {
                    if (x != 1 && x != 9 && z != 1 && z != 7) continue;
                    if (x == 5 && z == 1 && y <= 2) continue; // Door pair added last.
                    BlockState state;
                    if ((x == 1 || x == 9) && (z == 1 || z == 4 || z == 7)) {
                        state = log(Direction.Axis.Y);
                    } else if (y == 1) {
                        state = foundation(x, z);
                    } else if (y == 2 && window(x, z)) {
                        state = Blocks.GLASS.defaultBlockState();
                    } else {
                        state = Blocks.BIRCH_PLANKS.defaultBlockState();
                    }
                    add(blocks, x, y, z, state);
                }
            }
        }

        // Exposed top ring and a central crossbeam supporting the interior lamps.
        for (int x = 1; x <= 9; x++) {
            add(blocks, x, 4, 1, log(Direction.Axis.X));
            add(blocks, x, 4, 7, log(Direction.Axis.X));
            add(blocks, x, 4, 4, log(Direction.Axis.X));
        }
        for (int z = 2; z <= 6; z++) {
            if (z == 4) continue;
            add(blocks, 1, 4, z, log(Direction.Axis.Z));
            add(blocks, 9, 4, z, log(Direction.Axis.Z));
        }

        // Side gables close the attic while leaving the central room open.
        for (int z = 1; z <= 7; z++) {
            int roofY = 4 + Math.min(z, 8 - z);
            for (int y = 5; y < roofY; y++) {
                add(blocks, 1, y, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                add(blocks, 9, y, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
        }
        for (int z = 0; z < DEPTH; z++) {
            int roofY = 4 + Math.min(z, 8 - z);
            BlockState roof = z == 4 ? Blocks.SPRUCE_SLAB.defaultBlockState()
                    : Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                            z < 4 ? Direction.SOUTH : Direction.NORTH);
            for (int x = 0; x < WIDTH; x++) add(blocks, x, roofY, z, roof);
        }

        // Real usable work/storage blocks. Every container starts empty.
        add(blocks, 8, 1, 2, Blocks.CRAFTING_TABLE.defaultBlockState());
        add(blocks, 8, 1, 3, Blocks.STONECUTTER.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        add(blocks, 8, 1, 5, Blocks.BARREL.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.WEST));
        add(blocks, 8, 1, 6, Blocks.BARREL.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.WEST));
        add(blocks, 2, 1, 2, Blocks.BOOKSHELF.defaultBlockState());
        add(blocks, 3, 1, 2, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        add(blocks, 3, 1, 3, Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        add(blocks, 2, 2, 2, Blocks.POTTED_FERN.defaultBlockState());

        BlockState bed = Blocks.RED_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        add(blocks, 2, 1, 5, bed.setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
        add(blocks, 2, 1, 6, bed.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));

        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        add(blocks, 5, 1, 1, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        add(blocks, 5, 2, 1, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
        add(blocks, 3, 3, 4, lantern);
        add(blocks, 7, 3, 4, lantern);
        add(blocks, 5, 3, 0, lantern);

        validateDefinition(blocks);
        return new BuilderHutBlueprint(blocks);
    }

    public List<Placement> placements() { return placements; }

    /** Rotation includes state orientation as well as normalized local coordinates. */
    public List<Placement> placements(Rotation rotation) {
        Objects.requireNonNull(rotation, "rotation");
        if (rotation == Rotation.NONE) return placements;
        return placements.stream().map(block -> new Placement(rotateOffset(block.offset(), rotation),
                block.state().rotate(rotation))).toList();
    }

    public int width(Rotation rotation) {
        return quarterTurn(rotation) ? DEPTH : WIDTH;
    }

    public int depth(Rotation rotation) {
        return quarterTurn(rotation) ? WIDTH : DEPTH;
    }

    public BlockPos homeOffset() { return new BlockPos(5, 1, 4); }
    public BlockPos entryOffset() { return new BlockPos(5, 1, 0); }
    public BlockPos doorOffset() { return new BlockPos(5, 1, 1); }

    public BlockPos rotateOffset(BlockPos offset, Rotation rotation) {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(rotation, "rotation");
        return switch (rotation) {
            case NONE -> offset.immutable();
            case CLOCKWISE_90 -> new BlockPos(DEPTH - 1 - offset.getZ(), offset.getY(), offset.getX());
            case CLOCKWISE_180 -> new BlockPos(WIDTH - 1 - offset.getX(), offset.getY(), DEPTH - 1 - offset.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(offset.getZ(), offset.getY(), WIDTH - 1 - offset.getX());
        };
    }

    private static boolean quarterTurn(Rotation rotation) {
        Objects.requireNonNull(rotation, "rotation");
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
    }

    private static BlockState log(Direction.Axis axis) {
        return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
    }

    private static BlockState foundation(int x, int z) {
        return switch ((x * 31 + z * 17) % 7) {
            case 0 -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case 1 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case 2 -> Blocks.ANDESITE.defaultBlockState();
            case 3 -> Blocks.COBBLESTONE.defaultBlockState();
            default -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    private static boolean window(int x, int z) {
        return ((z == 1 || z == 7) && (x == 3 || x == 7))
                || ((x == 1 || x == 9) && (z == 3 || z == 5));
    }

    private static void add(List<Placement> blocks, int x, int y, int z, BlockState state) {
        blocks.add(new Placement(new BlockPos(x, y, z), state));
    }

    private static void validateDefinition(List<Placement> blocks) {
        Set<BlockPos> occupied = new HashSet<>();
        for (Placement block : blocks) {
            BlockPos position = block.offset();
            if (position.getX() < 0 || position.getX() >= WIDTH || position.getY() < 0
                    || position.getY() >= HEIGHT || position.getZ() < 0 || position.getZ() >= DEPTH
                    || block.state().isAir() || !occupied.add(position)) {
                throw new IllegalStateException("Invalid or duplicate Builder's Hut placement: " + position);
            }
        }
    }
}
