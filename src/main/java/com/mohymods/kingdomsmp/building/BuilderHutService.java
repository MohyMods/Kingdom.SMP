package com.mohymods.kingdomsmp.building;

import com.mohymods.kingdomsmp.npc.AldricEntity;
import com.mohymods.kingdomsmp.npc.KingdomNpcs;
import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData;
import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData.Hut;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/** Op-controlled starter installation. Public player blueprint construction is a separate milestone. */
public final class BuilderHutService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long PREVIEW_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final Map<UUID, Preview> PREVIEWS = new HashMap<>();
    private static BiConsumer<ServerPlayer, AldricEntity> dialogueOpener;
    record Preview(BlockPos origin, Rotation rotation, UUID token, long createdAtNanos, String reason) {
        boolean expired(long nowNanos) {
            return nowNanos - createdAtNanos >= PREVIEW_TIMEOUT_NANOS;
        }
    }
    private BuilderHutService() { }

    public static void installDialogueOpener(BiConsumer<ServerPlayer, AldricEntity> opener) {
        dialogueOpener = Objects.requireNonNull(opener);
    }

    public static void initialize(MinecraftServer server) {
        PREVIEWS.clear();
        verifyReservation(server);
    }

    private static void verifyReservation(MinecraftServer server) {
        BuilderHutSavedData data = BuilderHutSavedData.get(server);
        Path reservation = marker(server, "reserved");
        if (Files.exists(reservation)) {
            Hut hut = data.hut().orElseThrow(() -> new IllegalStateException(
                    "Builder's Hut reservation exists but world data is missing. Restore the matching world backup; setup is locked."));
            HutReservationJournal.verify(reservation, reservationText(hut));
        } else if (data.hut().isPresent()) {
            throw new IllegalStateException("Builder's Hut reservation journal is missing. Restore the matching world backup.");
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("hut").requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> {
                    var data = BuilderHutSavedData.get(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal(data.hut()
                            .map(hut -> "Builder's Hut " + hut.id() + " at " + hut.origin().toShortString()
                                    + " / " + hut.rotation() + " / " + hut.phase() + "; NPC " + hut.npcId()
                                    + (hut.npcDead() ? " (dead; replacement requires a recovery policy)" : ""))
                            .orElse("No Builder's Hut has been installed.")), false);
                    return 1;
                }))
                .then(Commands.literal("preview").then(Commands.argument("origin", BlockPosArgument.blockPos())
                        .then(Commands.argument("rotation", StringArgumentType.word())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        List.of("north", "east", "south", "west"), builder))
                                .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    Rotation rotation = rotation(StringArgumentType.getString(context, "rotation"));
                                    if (rotation == null) return fail(context.getSource(), "Rotation must be north, east, south, or west.");
                                    return preview(player, BlockPosArgument.getBlockPos(context, "origin"), rotation,
                                            StringArgumentType.getString(context, "reason"));
                                })))))
                .then(Commands.literal("confirm").then(Commands.argument("token", StringArgumentType.word())
                        .executes(context -> confirm(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "token")))))
                .then(Commands.literal("resume").then(Commands.argument("hutId", StringArgumentType.word())
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Hut hut = BuilderHutSavedData.get(player.server).hut().orElse(null);
                            if (hut == null || !hut.id().toString().equals(StringArgumentType.getString(context, "hutId"))) {
                                return fail(context.getSource(), "Exact reserved hut ID required; use /kingdom hut status.");
                            }
                            if (hut.phase() == BuilderHutSavedData.Phase.ACTIVE) return fail(context.getSource(), "The hut is already installed.");
                            LOGGER.info("Builder hut resume actor={} hut={} reason={}", player.getUUID(), hut.id(),
                                    auditReason(StringArgumentType.getString(context, "reason")));
                            return install(player, hut);
                        }))))
                .then(Commands.literal("name").then(Commands.argument("displayName", StringArgumentType.greedyString())
                        .executes(context -> {
                            BuilderHutSavedData data = BuilderHutSavedData.get(context.getSource().getServer());
                            Hut hut = data.hut().orElse(null);
                            if (hut == null) return fail(context.getSource(), "Install the hut first.");
                            String name = StringArgumentType.getString(context, "displayName");
                            try { data.rename(name); } catch (IllegalArgumentException invalid) {
                                return fail(context.getSource(), invalid.getMessage());
                            }
                            var entity = context.getSource().getServer().overworld().getEntity(hut.npcId());
                            if (entity instanceof AldricEntity builder) builder.setCustomName(Component.literal(name));
                            LOGGER.info("Builder renamed actor={} hut={} before={} after={}",
                                    context.getSource().getTextName(), hut.id(), hut.displayName(), name);
                            context.getSource().sendSuccess(() -> Component.literal("Builder name updated; identity preserved."), true);
                            return 1;
                        })));
    }

    private static int preview(ServerPlayer player, BlockPos origin, Rotation rotation, String reason) {
        verifyReservation(player.server);
        if (BuilderHutSavedData.get(player.server).hut().isPresent()) return fail(player.createCommandSourceStack(), "The kingdom already has a hut reservation.");
        String denied = validateSite(player.serverLevel(), origin, rotation, false);
        if (denied != null) return fail(player.createCommandSourceStack(), denied);
        if (player.distanceToSqr(origin.getX(), origin.getY(), origin.getZ()) > 64 * 64) {
            return fail(player.createCommandSourceStack(), "Stand within 64 blocks of the site.");
        }
        UUID token = UUID.randomUUID();
        long now = System.nanoTime();
        PREVIEWS.entrySet().removeIf(entry -> entry.getValue().expired(now));
        PREVIEWS.put(player.getUUID(), new Preview(origin.immutable(), rotation, token, now, auditReason(reason)));
        var blueprint = BuilderHutBlueprint.create();
        String command = "/kingdom hut confirm " + token;
        Component confirmation = Component.literal("[Confirm Builder's Hut]").withStyle(style -> style
                .withColor(ChatFormatting.GOLD).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Confirm this reviewed site within 5 minutes. All placement checks run again.\n" + command))));
        player.sendSystemMessage(Component.literal("Review Builder's Hut: " + origin.toShortString() + " / " + rotation
                + ", " + blueprint.width(rotation) + "×" + blueprint.depth(rotation) + "×" + BuilderHutBlueprint.HEIGHT
                + ", " + blueprint.placements().size() + " blocks. Empty furnished containers; one builder. Reason: " + auditReason(reason)
                + ". Confirm within 5 minutes: ").append(confirmation));
        return 1;
    }

    private static int confirm(ServerPlayer player, String token) {
        Preview preview = PREVIEWS.remove(player.getUUID());
        if (preview == null || preview.expired(System.nanoTime()) || !preview.token().toString().equals(token)) {
            return fail(player.createCommandSourceStack(), "Preview expired or token mismatched. Preview the exact site again.");
        }
        if (player.distanceToSqr(preview.origin().getX(), preview.origin().getY(), preview.origin().getZ()) > 64 * 64) {
            return fail(player.createCommandSourceStack(), "Stand within 64 blocks of the site.");
        }
        String denied = validateSite(player.serverLevel(), preview.origin(), preview.rotation(), false);
        if (denied != null) return fail(player.createCommandSourceStack(), denied);
        BuilderHutSavedData data = BuilderHutSavedData.get(player.server);
        if (data.hut().isPresent() || Files.exists(marker(player.server, "reserved"))) {
            return fail(player.createCommandSourceStack(), "A hut has already been reserved.");
        }
        Hut hut = data.begin(preview.origin(), preview.rotation(), UUID.randomUUID());
        // The immutable reservation survives even if the world SavedData save lags a process crash.
        HutReservationJournal.create(marker(player.server, "reserved"), reservationText(hut));
        LOGGER.info("Builder hut confirmed actor={} hut={} origin={} rotation={} reason={}",
                player.getUUID(), hut.id(), hut.origin(), hut.rotation(), preview.reason());
        return install(player, hut);
    }

    /** Returns a bounded actionable denial. Never causes chunk loading. Null means valid. */
    public static String validateSite(ServerLevel level, BlockPos origin, Rotation rotation, boolean resuming) {
        String protection=com.mohymods.kingdomsmp.placement.PlacementProtection.denial();if(protection!=null)return protection;
        if (level.dimension() != Level.OVERWORLD) return "The starter hut must be in the Overworld.";
        BuilderHutBlueprint blueprint = BuilderHutBlueprint.create();
        BlockPos last = origin.offset(blueprint.width(rotation) - 1, BuilderHutBlueprint.HEIGHT - 1, blueprint.depth(rotation) - 1);
        if (level.isOutsideBuildHeight(origin.below()) || level.isOutsideBuildHeight(last)
                || !level.getWorldBorder().isWithinBounds(origin) || !level.getWorldBorder().isWithinBounds(last)) {
            return "The complete footprint must be inside the world border and build height.";
        }
        for (BlockPos position : BlockPos.betweenClosed(origin.below(), last)) {
            if (!level.hasChunkAt(position)) return "Every footprint chunk must already be loaded.";
        }
        Map<BlockPos, BlockState> expected = new HashMap<>();
        if (resuming) for (var block : blueprint.placements(rotation)) expected.put(origin.offset(block.offset()), block.state());
        for (int x = 0; x < blueprint.width(rotation); x++) for (int z = 0; z < blueprint.depth(rotation); z++) {
            BlockPos support = origin.offset(x, -1, z);
            if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return "Prepare solid level ground below the entire footprint.";
        }
        for (BlockPos position : BlockPos.betweenClosed(origin, last)) {
            BlockState current = level.getBlockState(position);
            if (!current.isAir() && !(resuming && expected.containsKey(position) && matchesRequired(current, expected.get(position)))) {
                return "Site obstructed at " + position.toShortString() + "; terrain and containers are never replaced.";
            }
        }
        AABB bounds = new AABB(origin.getX(), origin.getY(), origin.getZ(), last.getX() + 1, last.getY() + 1, last.getZ() + 1);
        if (!level.getEntities((net.minecraft.world.entity.Entity) null, bounds,
                entity -> !(resuming && entity instanceof AldricEntity builder && isRegistered(builder))).isEmpty()) {
            return "Move players, mobs, and dropped items outside the footprint.";
        }
        return null;
    }

    private static int install(ServerPlayer player, Hut hut) {
        ServerLevel level = player.serverLevel();
        if (player.distanceToSqr(hut.origin().getX(), hut.origin().getY(), hut.origin().getZ()) > 64 * 64) {
            return fail(player.createCommandSourceStack(), "Stand within 64 blocks of the reserved site.");
        }
        String denied = validateSite(level, hut.origin(), hut.rotation(), true);
        if (denied != null) return fail(player.createCommandSourceStack(), denied);
        BuilderHutSavedData data = BuilderHutSavedData.get(player.server);
        HutReservationJournal.verify(marker(player.server, "reserved"), reservationText(hut));
        List<BuilderHutBlueprint.Placement> blocks = BuilderHutBlueprint.create().placements(hut.rotation());
        List<BlockSnapshot> replaced = new ArrayList<>();
        try {
            for (int index = 0; index < blocks.size(); index++) {
                var block = blocks.get(index);
                BlockPos position = hut.origin().offset(block.offset());
                BlockState current = level.getBlockState(position);
                if (!matchesRequired(current, block.state())) {
                    if (!current.isAir()) return fail(player.createCommandSourceStack(), "Site changed; installation paused without overwriting blocks.");
                    replaced.add(BlockSnapshot.create(level.dimension(), level, position));
                    if (!level.setBlock(position, block.state(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                        return fail(player.createCommandSourceStack(), "A block placement failed; installation remains reserved.");
                    }
                }
                data.advance(Math.max(data.hut().orElseThrow().placedBlocks(), index + 1));
            }
            if (!replaced.isEmpty() && NeoForge.EVENT_BUS.post(new BlockEvent.EntityMultiPlaceEvent(replaced,
                    level.getBlockState(hut.origin().below()), player)).isCanceled()) {
                for (int index = replaced.size() - 1; index >= 0; index--) {
                    var snapshot = replaced.get(index);
                    BlockState expected = blocks.stream().filter(block -> hut.origin().offset(block.offset()).equals(snapshot.getPos()))
                            .findFirst().orElseThrow().state();
                    if (level.getBlockState(snapshot.getPos()).equals(expected)
                            && (!(level.getBlockEntity(snapshot.getPos()) instanceof Container container) || container.isEmpty())) {
                        level.setBlock(snapshot.getPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
                return fail(player.createCommandSourceStack(), "A protection/place listener denied installation. Reservation retained; resolve permission before resuming.");
            }
            for (var block : blocks) level.updateNeighborsAt(hut.origin().offset(block.offset()), block.state().getBlock());
            if (!structureIntact(level, hut)) return fail(player.createCommandSourceStack(), "Structure verification failed; installation remains reserved.");
            AldricEntity builder;
            var existing = level.getEntity(hut.npcId());
            if (existing instanceof AldricEntity found && hut.id().equals(found.hutId())) {
                builder = found;
            } else {
                if (hut.npcSpawnCommitted() || Files.exists(marker(player.server, "issued"))) {
                    return fail(player.createCommandSourceStack(), "Builder issuance was already committed. Missing entities are never recreated automatically; restore a matching backup.");
                }
                data.markNpcSpawnCommitted();
                HutReservationJournal.create(marker(player.server, "issued"), hut.npcId().toString());
                builder = KingdomNpcs.ALDRIC.get().create(level);
                if (builder == null) return fail(player.createCommandSourceStack(), "Builder creation failed; issuance remains locked.");
                builder.setUUID(hut.npcId());
                BlockPos home = home(hut);
                builder.configure(hut.id(), home, hut.displayName());
                builder.moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 180, 0);
                if (!level.addFreshEntity(builder)) return fail(player.createCommandSourceStack(), "Builder spawn rejected; issuance remains locked.");
            }
            data.activate();
            LOGGER.info("Builder hut installed actor={} hut={} npc={} blocks={}", player.getUUID(), hut.id(), builder.getUUID(), blocks.size());
            player.sendSystemMessage(Component.literal("Builder's Hut ready. Speak with " + hut.displayName() + " inside."));
            return 1;
        } catch (RuntimeException failure) {
            LOGGER.error("Builder hut installation paused: {}", hut.id(), failure);
            return fail(player.createCommandSourceStack(), "Installation paused; reservation retained. Check server log before using resume.");
        }
    }

    public static BlockPos home(Hut hut) {
        var blueprint = BuilderHutBlueprint.create();
        return hut.origin().offset(blueprint.rotateOffset(blueprint.homeOffset(), hut.rotation()));
    }

    public static boolean structureIntact(ServerLevel level, Hut hut) {
        for (var block : BuilderHutBlueprint.create().placements(hut.rotation())) {
            BlockPos position = hut.origin().offset(block.offset());
            if (!level.hasChunkAt(position) || !matchesRequired(level.getBlockState(position), block.state())) return false;
        }
        return true;
    }

    static boolean matchesRequired(BlockState current, BlockState required) {
        if (current.getBlock() != required.getBlock()) return false;
        // Normal use of furniture must not count as structural damage.
        for (var property : List.of(BlockStateProperties.OPEN, BlockStateProperties.POWERED, BlockStateProperties.OCCUPIED)) {
            if (current.hasProperty(property)) current = current.setValue(property, required.getValue(property));
        }
        return current.equals(required);
    }

    public static boolean isRegistered(AldricEntity builder) {
        if (!(builder.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return false;
        Hut hut = BuilderHutSavedData.get(level.getServer()).hut().orElse(null);
        return hut != null && hut.npcSpawnCommitted() && !hut.npcDead() && hut.npcId().equals(builder.getUUID())
                && hut.id().equals(builder.hutId()) && home(hut).equals(builder.home());
    }

    public static boolean canInteract(ServerPlayer player, AldricEntity builder) {
        if (player.level() != builder.level() || !player.isAlive() || player.isSpectator() || !builder.isAlive()
                || player.distanceToSqr(builder) > 64 || !player.hasLineOfSight(builder) || !isRegistered(builder)) return false;
        Hut hut = BuilderHutSavedData.get(player.server).hut().orElseThrow();
        return hut.phase() == BuilderHutSavedData.Phase.ACTIVE && structureIntact(player.serverLevel(), hut);
    }

    public static void openDialogue(ServerPlayer player, AldricEntity builder) {
        if (canInteract(player, builder) && dialogueOpener != null) dialogueOpener.accept(player, builder);
        else player.displayClientMessage(Component.literal("The builder is unavailable. Check the hut, proximity, and registered identity."), true);
    }

    public static String statusText(ServerPlayer player) {
        Hut hut = BuilderHutSavedData.get(player.server).hut().orElse(null);
        if (hut == null) return "The kingdom has no Builder's Hut.";
        if (hut.npcDead()) return "The builder has died. Replacement requires an approved recovery policy.";
        if (hut.phase() != BuilderHutSavedData.Phase.ACTIVE) return "Hut installation is paused; an administrator must inspect the reservation.";
        if (!structureIntact(player.server.overworld(), hut)) return "The hut is damaged or unloaded; interaction and routine are paused.";
        var entity = player.server.overworld().getEntity(hut.npcId());
        return entity instanceof AldricEntity builder ? builder.routineStatus() : "The registered builder is unloaded or missing; no replacement is spawned.";
    }

    public static void npcDied(AldricEntity builder) {
        if (isRegistered(builder)) {
            BuilderHutSavedData.get(((ServerLevel) builder.level()).getServer()).markNpcDead();
            LOGGER.warn("Registered builder died: hut={} npc={}; automatic replacement disabled", builder.hutId(), builder.getUUID());
        }
    }

    private static Rotation rotation(String value) {
        return switch (value) { case "north" -> Rotation.NONE; case "east" -> Rotation.CLOCKWISE_90;
            case "south" -> Rotation.CLOCKWISE_180; case "west" -> Rotation.COUNTERCLOCKWISE_90; default -> null; };
    }
    private static int fail(CommandSourceStack source, String message) { source.sendFailure(Component.literal(message)); return 0; }
    private static String auditReason(String reason) {
        String clean = reason.replaceAll("[\\p{Cntrl}§]", "").strip();
        return clean.substring(0, Math.min(160, clean.length()));
    }
    private static Path marker(MinecraftServer server, String kind) { return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("kingdomsmp_builder_hut." + kind); }
    private static String reservationText(Hut hut) { return "1\n" + hut.id() + "\n" + hut.npcId() + "\n" + hut.origin().toShortString() + "\n" + hut.rotation(); }
}
