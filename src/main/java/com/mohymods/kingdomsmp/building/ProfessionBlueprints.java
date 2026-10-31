package com.mohymods.kingdomsmp.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.LinkedHashMap;
import java.util.Map;

/** Original Tier-2 profession premises; progression and shop authority lives in the server services. */
public final class ProfessionBlueprints {
    public static final ResourceLocation FARMER = id("farmers_house");
    public static final ResourceLocation BUILDER = id("builders_workshop");
    public static final ResourceLocation LUMBERJACK = id("lumberjacks_lodge");
    private ProfessionBlueprints() { }

    public static ConstructionBlueprint create(ResourceLocation id) {
        if (id.equals(FARMER)) return farmer();
        if (id.equals(BUILDER)) return builder();
        if (id.equals(LUMBERJACK)) return lumberjack();
        throw new IllegalArgumentException("Unknown profession blueprint: " + id);
    }

    private static ConstructionBlueprint farmer() {
        // A compact farmhouse occupies the west side; a fenced garden fills the east wing.
        var blocks = shell(15, 10, 11, Blocks.BIRCH_PLANKS.defaultBlockState(),
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(),
                Blocks.SPRUCE_STAIRS.defaultBlockState());
        entrance(blocks, 7);
        put(blocks, 3, 1, 3, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        put(blocks, 3, 1, 8, Blocks.COMPOSTER.defaultBlockState());
        put(blocks, 4, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
        put(blocks, 6, 1, 8, Blocks.BARREL.defaultBlockState());
        put(blocks, 7, 1, 8, Blocks.BARREL.defaultBlockState());
        put(blocks, 2, 1, 8, Blocks.HAY_BLOCK.defaultBlockState());
        put(blocks, 2, 2, 8, Blocks.HAY_BLOCK.defaultBlockState());
        put(blocks, 2, 1, 7, Blocks.HAY_BLOCK.defaultBlockState());
        put(blocks, 2, 1, 4, Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        put(blocks, 6, 1, 4, Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        put(blocks, 4, 1, 4, Blocks.OAK_PLANKS.defaultBlockState());
        put(blocks, 4, 2, 4, Blocks.POTTED_DANDELION.defaultBlockState());
        // Dry crop beds avoid introducing fluid that could flow into unfinished construction.
        // They are real farmland/crops, not an invented irrigation mechanic.
        for (int x = 11; x <= 13; x++) for (int z = 3; z <= 7; z++) {
            // Move each bed/crop pair together to the construction tail: an exposed
            // unplanted field must not dry into dirt while the entire house is built.
            blocks.remove(new BlockPos(x, 0, z));
            put(blocks, x, 0, z, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
            put(blocks, x, 1, z, Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7));
        }
        for (int x = 10; x <= 14; x++) {
            put(blocks, x, 1, 2, Blocks.OAK_FENCE.defaultBlockState());
            put(blocks, x, 1, 8, Blocks.OAK_FENCE.defaultBlockState());
        }
        for (int z = 3; z <= 7; z++) for (int x : new int[]{10, 14})
            put(blocks, x, 1, z, Blocks.OAK_FENCE.defaultBlockState());
        put(blocks, 12, 1, 2, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        put(blocks, 11, 1, 9, Blocks.COMPOSTER.defaultBlockState());
        put(blocks, 13, 1, 9, Blocks.HAY_BLOCK.defaultBlockState());
        lantern(blocks, 3, 4, 2); lantern(blocks, 6, 4, 7);
        return finish(FARMER, 15, blocks, new BlockPos(4, 1, 7), new BlockPos(3, 1, 3));
    }

    private static ConstructionBlueprint builder() {
        var blocks = shell(13, 13, 11, Blocks.BRICKS.defaultBlockState(),
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(),
                Blocks.SPRUCE_STAIRS.defaultBlockState());
        entrance(blocks, 6);
        put(blocks, 4, 1, 3, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        put(blocks, 3, 1, 7, Blocks.STONECUTTER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        put(blocks, 3, 1, 8, Blocks.SMITHING_TABLE.defaultBlockState());
        put(blocks, 8, 1, 7, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        put(blocks, 8, 2, 7, Blocks.POTTED_FERN.defaultBlockState());
        put(blocks, 8, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
        for (int x = 2; x <= 4; x++) put(blocks, x, 1, 9, Blocks.BARREL.defaultBlockState());
        for (int x = 8; x <= 10; x++) put(blocks, x, 1, 9, Blocks.BOOKSHELF.defaultBlockState());
        // A small masonry sample displays materials without blocking the central aisle.
        put(blocks, 9, 1, 4, Blocks.STONE_BRICKS.defaultBlockState());
        put(blocks, 10, 1, 4, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        put(blocks, 9, 2, 4, Blocks.STONE_BRICK_WALL.defaultBlockState());
        put(blocks, 2, 1, 4, Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        lantern(blocks, 3, 4, 3); lantern(blocks, 9, 4, 7);
        return finish(BUILDER, 13, blocks, new BlockPos(6, 1, 7), new BlockPos(4, 1, 3));
    }

    private static ConstructionBlueprint lumberjack() {
        var blocks = shell(13, 13, 11, Blocks.SPRUCE_PLANKS.defaultBlockState(),
                Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), Blocks.DARK_OAK_PLANKS.defaultBlockState(),
                Blocks.DARK_OAK_STAIRS.defaultBlockState());
        entrance(blocks, 6);
        put(blocks, 4, 1, 3, Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        put(blocks, 3, 1, 7, Blocks.STONECUTTER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        put(blocks, 3, 1, 8, Blocks.CRAFTING_TABLE.defaultBlockState());
        put(blocks, 3, 1, 9, Blocks.BARREL.defaultBlockState());
        put(blocks, 4, 1, 9, Blocks.BARREL.defaultBlockState());
        put(blocks, 8, 1, 8, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        for (int x = 9; x <= 11; x++) for (int z = 7; z <= 9; z++)
            put(blocks, x, 1, z, Blocks.SPRUCE_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        for (int x = 9; x <= 11; x++) put(blocks, x, 2, 8, Blocks.SPRUCE_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        put(blocks, 2, 1, 4, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        put(blocks, 10, 1, 4, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        put(blocks, 7, 1, 4, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        put(blocks, 7, 2, 4, Blocks.POTTED_SPRUCE_SAPLING.defaultBlockState());
        lantern(blocks, 3, 4, 3); lantern(blocks, 9, 4, 6);
        return finish(LUMBERJACK, 13, blocks, new BlockPos(6, 1, 7), new BlockPos(4, 1, 3));
    }

    /** The farmhouse has a narrower house than its garden-inclusive footprint. */
    private static Map<BlockPos, BlockState> shell(int footprintWidth, int houseWidth, int depth,
            BlockState wall, BlockState frame, BlockState roof, BlockState roofStair) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int x = 0; x < footprintWidth; x++) for (int z = 0; z < depth; z++)
            put(blocks, x, 0, z, Blocks.STONE_BRICKS.defaultBlockState());
        for (int y = 1; y <= 4; y++) for (int x = 0; x < houseWidth; x++) for (int z = 0; z < depth; z++)
            if (x == 0 || x == houseWidth - 1 || z == 0 || z == depth - 1) {
                boolean post = (x == 0 || x == houseWidth - 1) && (z == 0 || z == depth - 1)
                        || (z == 0 || z == depth - 1) && x % 4 == 0;
                BlockState state = post ? frame : y == 1 ? Blocks.STONE_BRICKS.defaultBlockState() : wall;
                if (!post && (y == 2 || y == 3) && (x == 0 || x == houseWidth - 1 ? z % 3 != 0 : x % 3 != 0))
                    state = Blocks.GLASS.defaultBlockState();
                put(blocks, x, y, z, state);
            }
        for (int x = 0; x < houseWidth; x++) for (int z = 0; z < depth; z++) put(blocks, x, 5, z, roof);
        for (int rise = 0; rise <= depth / 2; rise++) for (int x = 0; x < houseWidth; x++) {
            put(blocks, x, 5 + rise, rise, roofStair.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            put(blocks, x, 5 + rise, depth - 1 - rise, roofStair.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            if (x == 0 || x == houseWidth - 1) for (int z = rise + 1; z < depth - 1 - rise; z++)
                put(blocks, x, 5 + rise, z, roof);
        }
        return blocks;
    }

    private static void entrance(Map<BlockPos, BlockState> blocks, int x) {
        var door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        put(blocks, x, 1, 0, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        put(blocks, x, 2, 0, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
    }
    private static void lantern(Map<BlockPos, BlockState> blocks, int x, int y, int z) {
        put(blocks, x, y, z, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
    }
    private static ConstructionBlueprint finish(ResourceLocation id, int width, Map<BlockPos, BlockState> blocks,
            BlockPos home, BlockPos board) {
        return new ConstructionBlueprint(id, 1, width, 11, 11, blocks.entrySet().stream()
                .map(e -> new BuilderHutBlueprint.Placement(e.getKey(), e.getValue())).toList(), home, home, board);
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("kingdom_smp", path); }
    private static void put(Map<BlockPos, BlockState> blocks, int x, int y, int z, BlockState state) {
        blocks.put(new BlockPos(x, y, z), state);
    }
}
