package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData;
import com.mohymods.kingdomsmp.persistence.KingdomSavedData;

import com.mohymods.kingdomsmp.building.BuilderHutBlueprint;
import com.mohymods.kingdomsmp.building.BuilderHutService;
import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.npc.AldricEntity;
import com.mohymods.kingdomsmp.npc.KingdomNpcs;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Loaded by the isolated GameTest run; never packaged in the playable mod. */
@Mod("kingdom_smp_tests")
@GameTestHolder("kingdom_smp_tests")
@PrefixGameTestTemplate(false)
public final class KingdomRuntimeTests {
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void hutLifecycle(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        helper.assertTrue(KingdomSavedData.get(server).snapshot().equals(KingdomState.initial()), "Real server starts at Tier 1");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        for (BlockPos support : BlockPos.betweenClosed(origin.below(), origin.offset(10, -1, 10))) {
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 18);
        }
        var operator = new RecordingPlayer(level);
        operator.moveTo(origin.getX() - 2, origin.getY() + 1, origin.getZ() + 4);
        for (Rotation rotation : Rotation.values()) {
            helper.assertTrue(BuilderHutService.validateSite(level, origin, rotation, false) == null, "Clear rotated footprint accepted: " + rotation);
        }
        level.setBlock(origin.above(), Blocks.CHEST.defaultBlockState(), 18);
        helper.assertTrue(BuilderHutService.validateSite(level, origin, Rotation.NONE, false) != null, "Container collision denied");
        level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 18);
        helper.assertTrue(BuilderHutService.validateSite(level, new BlockPos(25_000_000, 80, 25_000_000), Rotation.NONE, false)
                .contains("loaded"), "Unloaded site denied without loading it");
        var dispatcher = server.getCommands().getDispatcher();
        String preview = "kingdom hut preview " + origin.getX() + " " + origin.getY() + " " + origin.getZ() + " north runtime-test";
        try {
            dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(0));
            helper.fail("Unprivileged setup command accepted");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
        helper.assertTrue(dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4)) == 1, "Admin preview accepted");
        String token = operator.token();
        level.setBlock(origin.above(), Blocks.STONE.defaultBlockState(), 18);
        helper.assertTrue(dispatcher.execute("kingdom hut confirm " + token, operator.createCommandSourceStack().withPermission(4)) == 0,
                "Confirmation revalidates stale site");
        helper.assertTrue(BuilderHutSavedData.get(server).hut().isEmpty(), "Stale preview does not reserve a hut");
        level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 18);
        dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4));
        Consumer<BlockEvent.EntityMultiPlaceEvent> deny = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            helper.assertTrue(dispatcher.execute("kingdom hut confirm " + operator.token(), operator.createCommandSourceStack().withPermission(4)) == 0,
                    "Protection listener cancellation honored");
        } finally { NeoForge.EVENT_BUS.unregister(deny); }
        var data = BuilderHutSavedData.get(server);
        var reserved = data.hut().orElseThrow();
        helper.assertTrue(!reserved.npcSpawnCommitted(), "Denied placement does not issue an NPC");
        helper.assertTrue(level.getBlockState(origin).isAir(), "Cancelled placement rolls back newly installed blocks");
        helper.assertTrue(dispatcher.execute("kingdom hut resume " + reserved.id() + " test-resume", operator.createCommandSourceStack().withPermission(4)) == 1,
                "Exact reservation resumes successfully");
        var hut = data.hut().orElseThrow();
        helper.assertTrue(BuilderHutService.structureIntact(level, hut), "Furnished building passes actual-world block verification");
        helper.assertTrue(hut.phase() == BuilderHutSavedData.Phase.ACTIVE, "Hut active only after verification and builder issuance");
        AldricEntity builder = (AldricEntity) level.getEntity(hut.npcId());
        helper.assertTrue(builder != null && BuilderHutService.isRegistered(builder), "One registered builder with stable home and UUID");
        helper.assertTrue(dispatcher.execute("kingdom hut resume " + hut.id() + " duplicate", operator.createCommandSourceStack().withPermission(4)) == 0,
                "Completed installation cannot be replayed");
        helper.assertTrue(dispatcher.execute("kingdom hut confirm " + token, operator.createCommandSourceStack().withPermission(4)) == 0,
                "Old confirmation cannot be replayed");
        dispatcher.execute("kingdom hut name Master Aldric", operator.createCommandSourceStack().withPermission(4));
        helper.assertTrue(builder.getName().getString().equals("Master Aldric") && builder.getUUID().equals(hut.npcId()), "Rename preserves stable identity");
        operator.moveTo(builder.getX(), builder.getY(), builder.getZ() + 2);
        helper.assertTrue(BuilderHutService.canInteract(operator, builder), "Nearby registered builder permits dialogue");
        operator.moveTo(builder.getX() + 20, builder.getY(), builder.getZ());
        helper.assertTrue(!BuilderHutService.canInteract(operator, builder), "Remote dialogue denied");
        operator.moveTo(builder.getX(), builder.getY(), builder.getZ() + 2);
        level.setBlock(origin, Blocks.AIR.defaultBlockState(), 18);
        helper.assertTrue(!BuilderHutService.canInteract(operator, builder), "Damage suspends dialogue");
        level.setBlock(origin, BuilderHutBlueprint.create().placements().getFirst().state(), 18);
        // Native SavedData disk write and a fresh storage instance, not just an in-memory NBT copy.
        var folder = Files.createTempDirectory("kingdom-native-save-test");
        var storage = new DimensionDataStorage(folder.toFile(), DataFixers.getDataFixer(), server.registryAccess());
        data.setDirty();
        storage.set(BuilderHutSavedData.DATA_NAME, data);
        storage.set(KingdomSavedData.DATA_NAME, KingdomSavedData.get(server));
        storage.save();
        IOUtilities.waitUntilIOWorkerComplete();
        var reloadedStorage = new DimensionDataStorage(folder.toFile(), DataFixers.getDataFixer(), server.registryAccess());
        var restored = reloadedStorage.readTagFromDisk(BuilderHutSavedData.DATA_NAME, null, 3955).getCompound("data");
        helper.assertTrue(restored.equals(data.save(new CompoundTag(), server.registryAccess())), "Native disk reload preserves site, rotation, name and issuance");
        var restoredKingdom = reloadedStorage.readTagFromDisk(KingdomSavedData.DATA_NAME, null, 3955).getCompound("data");
        helper.assertTrue(restoredKingdom.getInt("tier") == 1, "Native kingdom disk reload retains Tier 1");
        var entityTag = builder.saveWithoutId(new CompoundTag());
        AldricEntity restoredEntity = KingdomNpcs.ALDRIC.get().create(level);
        restoredEntity.load(entityTag);
        helper.assertTrue(restoredEntity.getUUID().equals(builder.getUUID()) && restoredEntity.hutId().equals(hut.id())
                && restoredEntity.home().equals(builder.home()), "Entity serialization preserves registered identity/home");
        builder.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(data.hut().orElseThrow().npcDead(), "Death is persisted and cannot reissue a builder");
        helper.assertTrue(!BuilderHutService.canInteract(operator, restoredEntity), "Restored entity cannot bypass persisted death");
        helper.succeed();
    }

    private static final class RecordingPlayer extends FakePlayer {
        private final List<String> messages = new ArrayList<>();
        RecordingPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "RuntimeOperator")); }
        @Override public void sendSystemMessage(Component message) { messages.add(message.getString()); }
        String token() {
            var pattern = Pattern.compile("/kingdom hut confirm ([a-f0-9-]{36})");
            for (int index = messages.size() - 1; index >= 0; index--) {
                var match = pattern.matcher(messages.get(index));
                if (match.find()) return match.group(1);
            }
            throw new IllegalStateException("No preview token delivered to operator");
        }
    }
}
