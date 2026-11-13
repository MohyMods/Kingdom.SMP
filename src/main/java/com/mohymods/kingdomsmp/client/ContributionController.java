package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.client.screen.ContributionScreen;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

public final class ContributionController {
    private static final OverviewRequestTracker REQUESTS = new OverviewRequestTracker(10_000_000_000L);
    private static boolean needsRefresh;
    private static String status = "";
    private ContributionController() { }
    public static void open() { request(0, 0, 0, ContributionRequest.NONE); }
    public static void request(int action, int resource, int amount, UUID offer) {
        tick(); var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || needsRefresh && action != 0) return;
        long id = REQUESTS.begin(System.nanoTime(), client.getConnection(), client.screen);
        if (id == 0) return;
        status = action == 2 ? "Saving donation…" : "Waiting for the kingdom…";
        PacketDistributor.sendToServer(new ContributionRequest(id, action, resource, amount, offer));
    }
    public static void receive(ContributionView view) {
        tick(); var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;
        if (view.requestId() == 0) {
            if (client.screen instanceof ContributionScreen screen) screen.accept(view);
            return;
        }
        if (!REQUESTS.complete(view.requestId(), client.getConnection(), client.screen, System.nanoTime())) return;
        needsRefresh = false; status = "";
        if (client.screen instanceof ContributionScreen screen) screen.accept(view);
        else client.setScreen(new ContributionScreen(view, client.screen));
    }
    public static void tick() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) { REQUESTS.clear(); needsRefresh = false; status = ""; return; }
        var state = REQUESTS.tick(System.nanoTime(), client.getConnection(), client.screen);
        if (state == OverviewRequestTracker.Status.TIMED_OUT) {
            needsRefresh = true; status = "No reply. Refresh to check the saved result.";
            if (client.screen instanceof ContributionScreen screen) screen.invalidateReview();
        } else if (state == OverviewRequestTracker.Status.CANCELLED) { needsRefresh = false; status = ""; }
    }
    public static boolean waiting() { return REQUESTS.waiting(); }
    public static boolean needsRefresh() { return needsRefresh; }
    public static String status() { return status; }
    public static void close() { REQUESTS.clear(); needsRefresh = false; status = ""; }
}
