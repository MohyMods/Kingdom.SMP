package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.client.screen.BuilderDialogueScreen;
import com.mohymods.kingdomsmp.network.BuilderDialoguePayload;
import com.mohymods.kingdomsmp.network.RequestBuilderDialoguePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/** Client-only dialogue lifecycle. Inspect replies never create a second conversation. */
public final class BuilderDialogueController {
    private static final OverviewRequestTracker REQUESTS = new OverviewRequestTracker(5_000_000_000L);
    private static UUID pendingNpc;
    private static int pendingTopic;
    private static Component status = Component.empty();

    private BuilderDialogueController() { }

    public static void receive(BuilderDialoguePayload payload) {
        tick();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null || localNpc(payload.npcId()) == null) return;
        if (payload.requestId() == 0) {
            // The initial interaction is server-initiated; never replace a different screen.
            if (payload.topic() != 0 || client.screen != null) return;
            status = Component.empty();
            client.setScreen(new BuilderDialogueScreen(payload));
            return;
        }
        if (!(client.screen instanceof BuilderDialogueScreen dialogue)
                || !payload.npcId().equals(dialogue.npcId())
                || !payload.npcId().equals(pendingNpc) || payload.topic() != pendingTopic
                || !REQUESTS.complete(payload.requestId(), client.getConnection(), client.screen, System.nanoTime())) return;
        pendingNpc = null;
        status = Component.empty();
        dialogue.acceptSnapshot(payload);
    }

    public static void request(int topic) {
        tick();
        Minecraft client = Minecraft.getInstance();
        if (topic < 0 || topic > 4 || client.player == null || client.getConnection() == null
                || !(client.screen instanceof BuilderDialogueScreen dialogue)
                || localNpc(dialogue.npcId()) == null) return;
        long requestId = REQUESTS.begin(System.nanoTime(), client.getConnection(), client.screen);
        if (requestId == 0) return;
        pendingNpc = dialogue.npcId();
        pendingTopic = topic;
        status = text("Asking the builder…");
        PacketDistributor.sendToServer(new RequestBuilderDialoguePayload(pendingNpc, topic, requestId));
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            clear();
            return;
        }
        if (client.screen instanceof BuilderDialogueScreen dialogue && localNpc(dialogue.npcId()) == null) {
            clear();
            client.setScreen(null);
            client.player.displayClientMessage(text("Conversation ended: the builder is out of reach."), true);
            return;
        }
        switch (REQUESTS.tick(System.nanoTime(), client.getConnection(), client.screen)) {
            case CANCELLED -> clear();
            case TIMED_OUT -> {
                pendingNpc = null;
                status = text("No reply. Move closer and try again.");
            }
            default -> { }
        }
    }

    public static void clear() {
        REQUESTS.clear();
        pendingNpc = null;
        status = Component.empty();
    }

    /** The server performs the authoritative registered-NPC/access check as well. */
    public static LivingEntity localNpc(UUID id) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return null;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(id) && entity instanceof LivingEntity living
                    && living.isAlive() && client.player.distanceToSqr(living) <= 64.0) return living;
        }
        return null;
    }

    public static boolean waiting() { return REQUESTS.waiting(); }
    public static Component status() { return status; }
    private static Component text(String value) { return Component.literal(value); }
}
