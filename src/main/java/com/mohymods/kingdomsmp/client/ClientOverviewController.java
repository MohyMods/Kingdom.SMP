package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.client.screen.KingdomOverviewScreen;
import com.mohymods.kingdomsmp.network.KingdomSnapshotPayload;
import com.mohymods.kingdomsmp.network.RequestKingdomPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only request lifecycle. Correlated replies cannot reopen a closed screen. */
public final class ClientOverviewController {
    private static final OverviewRequestTracker REQUESTS = new OverviewRequestTracker(5_000_000_000L);
    private static Component status = Component.empty();
    private ClientOverviewController() {}

    public static void request() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            status = Component.translatable("kingdom_smp.connection.required");
            return;
        }
        long requestId = REQUESTS.begin(System.nanoTime(), client.getConnection(), client.screen);
        if (requestId == 0) return;
        status = Component.translatable("kingdom_smp.connection.waiting");
        if (client.screen == null) client.player.displayClientMessage(status, true);
        PacketDistributor.sendToServer(new RequestKingdomPayload(requestId));
    }

    public static void receive(KingdomSnapshotPayload snapshot) {
        // Resolve expiry/navigation before complete() can clear an invalid pending
        // request, so its failure status is not stranded on "Waiting".
        tick();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !REQUESTS.complete(snapshot.requestId(),
                client.getConnection(), client.screen, System.nanoTime())) return;
        status = Component.empty();
        if (client.screen instanceof KingdomOverviewScreen overview) overview.acceptSnapshot(snapshot);
        else client.setScreen(new KingdomOverviewScreen(snapshot));
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            REQUESTS.clear();
            status = Component.empty();
            return;
        }
        switch (REQUESTS.tick(System.nanoTime(), client.getConnection(), client.screen)) {
            case CANCELLED -> status = Component.empty();
            case TIMED_OUT -> {
                status = Component.translatable("kingdom_smp.connection.timeout");
                if (client.screen == null) client.player.displayClientMessage(status, false);
            }
            default -> { /* IDLE and WAITING retain the current presentation status. */ }
        }
    }
    public static boolean waiting() { return REQUESTS.waiting(); }
    public static Component status() { return status; }
}
