package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import com.mohymods.kingdomsmp.persistence.KingdomSavedData;
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

    private KingdomNetworking() {}

    public static void installClientReceiver(Consumer<KingdomSnapshotPayload> receiver) {
        clientReceiver = Objects.requireNonNull(receiver);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
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

    private static void request(RequestKingdomPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        // The registrar runs on the main thread by default; mutable world data is read here only.
        long now = System.nanoTime();
        Long last = LAST_REQUEST.get(player);
        if (last != null && now - last < REQUEST_INTERVAL_NANOS) return;
        LAST_REQUEST.put(player, now);
        KingdomSavedData data = KingdomSavedData.get(player.server);
        KingdomState state = data.snapshot();
        TierOneProgress progress = data.tierOneProgress();
        TierOneProgress.Contributor personal = progress.contributors().get(player.getUUID());
        context.reply(new KingdomSnapshotPayload(state.attainedTier(), state.revision(), payload.requestId(),
                progress.total(Resource.LOGS), progress.total(Resource.COBBLESTONE),
                progress.total(Resource.WHEAT), progress.total(Resource.IRON_INGOTS),
                personal == null ? 0 : personal.scoreUnits()));
    }
}
