package com.mohymods.kingdomsmp.integration.ftbquests;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Optional boundary: this class has no FTB or client types in its fields or signatures. */
public final class FtbQuestsIntegration {
    public static final String VERIFIED_VERSION = "2101.1.37";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;
    private static String status = "Not registered";

    @FunctionalInterface
    public interface SnapshotProvider {
        /** Read cached authoritative records only; empty means startup/recovery is not ready. */
        Optional<KingdomQuestSnapshot> snapshot(MinecraftServer server);
    }

    private FtbQuestsIntegration() { }

    public static boolean register(IEventBus gameBus, SnapshotProvider provider) {
        Objects.requireNonNull(gameBus); Objects.requireNonNull(provider);
        if (registered) throw new IllegalStateException("FTB projection registered twice");
        registered = true;
        var container = ModList.get().getModContainerById("ftbquests");
        if (container.isEmpty()) { status = "FTB Quests absent; optional projection inactive"; return false; }
        String version = container.orElseThrow().getModInfo().getVersion().toString();
        if (!VERIFIED_VERSION.equals(version)) {
            status = "FTB Quests " + version + " is not the verified " + VERIFIED_VERSION + "; projection inactive";
            LOGGER.warn(status); return false;
        }
        // FTB-linked code is reached only after exact dependency presence/version checks.
        FtbQuestsProjection.register(provider);
        gameBus.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,FtbQuestsIntegration::prepareCampaign);
        status = "FTB Quests " + version + "; read-only Kingdom projection registered";
        LOGGER.info(status); return true;
    }

    private static void prepareCampaign(ServerAboutToStartEvent event) {
        try {
            var result = KingdomQuestCampaign.install(FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests"));
            LOGGER.info("Kingdom FTB chapter {}: {}", KingdomQuestCampaign.FILENAME, result);
            if (result == KingdomQuestCampaign.Result.EXISTING_DIFFERENT)
                LOGGER.warn("Existing Kingdom FTB chapter differs; preserved without overwrite. Projection bindings require exact IDs and no rewards.");
        } catch (IOException | RuntimeException error) {
            // Optional campaign failure must not damage world data or reset another quest pack.
            LOGGER.error("Kingdom FTB chapter could not be installed safely; existing quest files were preserved", error);
        }
    }

    public static String status() { return status; }

    public static List<String> inspect(MinecraftServer server) {
        if (!status.contains("projection registered")) return List.of(status);
        return FtbQuestsProjection.inspect(server, status);
    }
}
