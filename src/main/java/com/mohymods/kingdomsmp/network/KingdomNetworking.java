package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import com.mohymods.kingdomsmp.persistence.KingdomSavedData;
import com.mohymods.kingdomsmp.building.BuilderHutService;
import com.mohymods.kingdomsmp.npc.AldricEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Objects;
import java.util.function.Consumer;

/** Protocol v1, main-thread handlers, and no client class links on a server. */
public final class KingdomNetworking {
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();
    private static final long REQUEST_INTERVAL_NANOS = 250_000_000L;
    private static volatile Consumer<KingdomSnapshotPayload> clientReceiver;
    private static volatile Consumer<BuilderDialoguePayload> builderReceiver;
    private static final Map<ServerPlayer, Long> LAST_DIALOGUE = new WeakHashMap<>();

    private KingdomNetworking() {}

    public static void installClientReceiver(Consumer<KingdomSnapshotPayload> receiver) {
        clientReceiver = Objects.requireNonNull(receiver);
    }

    public static void installBuilderReceiver(Consumer<BuilderDialoguePayload> receiver) {
        builderReceiver = Objects.requireNonNull(receiver);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("6");
        com.mohymods.kingdomsmp.placement.PlacementNetworking.register(registrar);
        ConstructionNetworking.register(registrar);
        ContributionNetworking.register(registrar);
        registrar.playToServer(RequestBuilderDialoguePayload.TYPE, RequestBuilderDialoguePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.serverLevel().getEntity(payload.npcId()) instanceof AldricEntity builder) {
                        sendBuilderDialogue(player, builder, payload.topic(), payload.requestId());
                    }
                });
        registrar.playToClient(BuilderDialoguePayload.TYPE, BuilderDialoguePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (builderReceiver == null) throw new IllegalStateException("Builder client receiver missing");
                    builderReceiver.accept(payload);
                });
        registrar.playToServer(RequestKingdomPayload.TYPE, RequestKingdomPayload.STREAM_CODEC,
                KingdomNetworking::request);
        registrar.playToClient(KingdomSnapshotPayload.TYPE, KingdomSnapshotPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (clientReceiver == null) {
                        throw new IllegalStateException("Kingdom client receiver was not installed");
                    }
                    clientReceiver.accept(payload);
                });
    }

    public static void openBuilderDialogue(ServerPlayer player, AldricEntity builder) {
        sendBuilderDialogue(player, builder, 0, 0);
    }

    private static void sendBuilderDialogue(ServerPlayer player, AldricEntity builder, int topic, long requestId) {
        if (!BuilderHutService.canInteract(player, builder)) return;
        long now = System.nanoTime();
        Long last = LAST_DIALOGUE.get(player);
        if (last != null && now - last < REQUEST_INTERVAL_NANOS) return;
        LAST_DIALOGUE.put(player, now);
        PacketDistributor.sendToPlayer(player, new BuilderDialoguePayload(builder.getUUID(), builder.getName().getString(),
                BuilderHutService.statusText(player), topic,
                ContributionService.ledger(player.server).progress().winner().isPresent(), requestId));
    }

    private static void request(RequestKingdomPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        // The registrar runs on the main thread by default; mutable world data is read here only.
        long now = System.nanoTime();
        Long last = LAST_REQUEST.get(player);
        if (last != null && now - last < REQUEST_INTERVAL_NANOS) return;
        LAST_REQUEST.put(player, now);
        KingdomSavedData data = KingdomSavedData.get(player.server);
        KingdomState state = ContributionService.kingdom(player.server);
        TierOneProgress progress = ContributionService.ledger(player.server).progress();
        TierOneProgress.Contributor personal = progress.contributors().get(player.getUUID());
        context.reply(new KingdomSnapshotPayload(state.attainedTier(), state.revision(), payload.requestId(),
                progress.total(Resource.LOGS), progress.total(Resource.COBBLESTONE),
                progress.total(Resource.WHEAT), progress.total(Resource.IRON_INGOTS),
                personal == null ? 0 : personal.scoreUnits(),
                com.mohymods.kingdomsmp.milestone.MilestoneService.descriptions(player.server)));
    }
}
