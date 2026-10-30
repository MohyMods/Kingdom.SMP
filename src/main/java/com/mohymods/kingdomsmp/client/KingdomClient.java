package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.KingdomSmp;
import com.mohymods.kingdomsmp.network.KingdomNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Loaded only on physical clients, never by a dedicated server. */
@Mod(value = KingdomSmp.MOD_ID, dist = Dist.CLIENT)
public final class KingdomClient {
    private static final KeyMapping OVERVIEW = new KeyMapping("key.kingdom_smp.overview",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.kingdom_smp");

    public KingdomClient(IEventBus modBus) {
        KingdomNetworking.installClientReceiver(ClientOverviewController::receive);
        modBus.addListener(this::keys);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void keys(RegisterKeyMappingsEvent event) { event.register(OVERVIEW); }

    private void tick(ClientTickEvent.Post event) {
        ClientOverviewController.tick();
        while (OVERVIEW.consumeClick()) {
            if (net.minecraft.client.Minecraft.getInstance().screen == null) {
                ClientOverviewController.request();
            }
        }
    }
}
