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

/**
 * Original single-floor civic bank: accessible public lobby, teller counter,
 * banker desk and rear strongroom. Service authorization belongs to the server;
 * neither the decorations nor the bank's completion grant financial privileges.
 */
public final class RoyalBankBlueprint {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("kingdom_smp", "royal_bank");
    private RoyalBankBlueprint() { }

    public static ConstructionBlueprint create() {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        // Solid stone floor; corner piers frame the dark slate exterior.
        for (int y = 0; y <= 5; y++) for (int x = 0; x < 15; x++) for (int z = 0; z < 13; z++) {
            BlockState state = null;
            if (y == 0) state = Blocks.STONE_BRICKS.defaultBlockState();
            else if (x == 0 || x == 14 || z == 0 || z == 12) {
                boolean pier = (x == 0 || x == 14) && (z == 0 || z == 12)
                        || (z == 0 || z == 12) && (x == 3 || x == 11);
                state = pier || y == 1 ? Blocks.STONE_BRICKS.defaultBlockState()
                        : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                if (!pier && (y == 2 || y == 3) && (x == 0 || x == 14 ? z % 4 != 0 : x % 4 != 0))
                    state = Blocks.GLASS.defaultBlockState();
                if (y == 5) state = Blocks.DARK_OAK_PLANKS.defaultBlockState();
            }
            if (state != null) put(blocks, x, y, z, state);
        }
        // Full ceiling supports the lanterns; a closed gable rises above it.
        for (int x = 0; x < 15; x++) for (int z = 0; z < 13; z++)
            put(blocks, x, 6, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        for (int rise = 0; rise <= 6; rise++) for (int x = 0; x < 15; x++) {
            put(blocks, x, 6 + rise, rise, Blocks.DARK_OAK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            put(blocks, x, 6 + rise, 12 - rise, Blocks.DARK_OAK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            if (x == 0 || x == 14) for (int z = rise + 1; z < 12 - rise; z++)
                put(blocks, x, 6 + rise, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        // An exterior gold crest and pale lintel identify the bank from the settlement street.
        put(blocks, 7, 4, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        put(blocks, 6, 4, 0, Blocks.POLISHED_ANDESITE.defaultBlockState());
        put(blocks, 8, 4, 0, Blocks.POLISHED_ANDESITE.defaultBlockState());
        // The one-block-high teller counter leaves a clear line of sight to the banker.
        for (int x = 4; x <= 10; x++) put(blocks, x, 1, 7, Blocks.POLISHED_ANDESITE.defaultBlockState());
        for (int x : new int[]{4, 10}) {
            put(blocks, x, 1, 7, Blocks.GOLD_BLOCK.defaultBlockState());
            put(blocks, x, 2, 7, Blocks.POTTED_FERN.defaultBlockState());
        }
        // Public seats and a real, unobstructed milestone/service lectern.
        for (int z : new int[]{3, 5}) {
            put(blocks, 2, 1, z, Blocks.DARK_OAK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
            put(blocks, 12, 1, z, Blocks.DARK_OAK_STAIRS.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        }
        put(blocks, 3, 1, 4, Blocks.LECTERN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        for (int z = 1; z <= 6; z++) put(blocks, 7, 1, z, Blocks.RED_CARPET.defaultBlockState());
        // Teller paperwork and storage. The routes around both counter ends stay clear.
        put(blocks, 7, 1, 10, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        put(blocks, 7, 2, 10, Blocks.POTTED_FERN.defaultBlockState());
        for (int x = 2; x <= 4; x++) put(blocks, x, 1, 11, Blocks.BOOKSHELF.defaultBlockState());
        put(blocks, 2, 1, 9, Blocks.BARREL.defaultBlockState());
        put(blocks, 2, 1, 10, Blocks.BARREL.defaultBlockState());
        // Rear strongroom decoration; these are ordinary blocks, never an escrow store.
        for (int x = 10; x <= 13; x++) for (int y = 1; y <= 3; y++)
            put(blocks, x, y, 8, Blocks.IRON_BARS.defaultBlockState());
        for (int z = 9; z <= 11; z++) for (int y = 1; y <= 3; y++)
            put(blocks, 10, y, z, Blocks.IRON_BARS.defaultBlockState());
        var vaultDoor = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        put(blocks, 12, 1, 8, vaultDoor.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        put(blocks, 12, 2, 8, vaultDoor.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        put(blocks, 12, 1, 10, Blocks.BARREL.defaultBlockState());
        put(blocks, 13, 1, 11, Blocks.GOLD_BLOCK.defaultBlockState());
        // Hanging lanterns attach directly to the continuous ceiling at y6.
        for (int x : new int[]{3, 7, 11}) for (int z : new int[]{3, 9})
            put(blocks, x, 5, z, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        var entrance = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        put(blocks, 7, 1, 0, entrance.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        put(blocks, 7, 2, 0, entrance.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        var placements = blocks.entrySet().stream()
                .map(e -> new BuilderHutBlueprint.Placement(e.getKey(), e.getValue())).toList();
        return new ConstructionBlueprint(ID, 1, 15, 13, 13, placements,
                new BlockPos(7, 1, 9), new BlockPos(7, 1, 9), new BlockPos(3, 1, 4));
    }

    private static void put(Map<BlockPos, BlockState> blocks, int x, int y, int z, BlockState state) {
        blocks.put(new BlockPos(x, y, z), state);
    }
}
