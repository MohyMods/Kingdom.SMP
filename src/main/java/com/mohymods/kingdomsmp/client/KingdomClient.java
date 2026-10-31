package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.KingdomSmp;
import com.mohymods.kingdomsmp.network.KingdomNetworking;
import com.mohymods.kingdomsmp.npc.KingdomNpcs;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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

    public KingdomClient(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,BlueprintPlacement.CONFIG);
        com.mohymods.kingdomsmp.placement.PlacementNetworking.install(BlueprintPlacement::receive);
        NeoForge.EVENT_BUS.addListener(BlueprintPlacement::render);
        NeoForge.EVENT_BUS.addListener(BlueprintPlacement::hud);
        NeoForge.EVENT_BUS.addListener(BlueprintPlacement::use);
        KingdomNetworking.installClientReceiver(ClientOverviewController::receive);
        com.mohymods.kingdomsmp.network.ContributionNetworking.install(ContributionController::receive);
        KingdomNetworking.installBuilderReceiver(BuilderDialogueController::receive);
        com.mohymods.kingdomsmp.network.ConstructionNetworking.install(ConstructionController::receive);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
                event.registerEntityRenderer(KingdomNpcs.ALDRIC.get(), AldricRenderer::new);
                event.registerEntityRenderer(KingdomNpcs.REEVE.get(), VillagerRenderer::new);
        });
        NeoForge.EVENT_BUS.addListener(ConstructionPreview::render);
        modBus.addListener(this::keys);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void keys(RegisterKeyMappingsEvent event) { event.register(OVERVIEW);event.register(BlueprintPlacement.ROTATE);event.register(BlueprintPlacement.UP);event.register(BlueprintPlacement.DOWN); }

    private void tick(ClientTickEvent.Post event) {
        ClientOverviewController.tick();
        ContributionController.tick();
        BuilderDialogueController.tick();
        ConstructionController.tick();
        BlueprintPlacement.tick();
        while (OVERVIEW.consumeClick()) {
            if (net.minecraft.client.Minecraft.getInstance().screen == null) {
                ClientOverviewController.request();
            }
        }
    }
}
