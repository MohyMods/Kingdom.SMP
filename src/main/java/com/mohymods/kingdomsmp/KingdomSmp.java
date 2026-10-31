package com.mohymods.kingdomsmp;

import com.mohymods.kingdomsmp.network.KingdomNetworking;
import com.mohymods.kingdomsmp.persistence.KingdomSavedData;
import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.building.BuilderHutService;
import com.mohymods.kingdomsmp.npc.KingdomNpcs;
import com.mojang.brigadier.Command;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/** Common entry point: deliberately contains no client types. */
@Mod(KingdomSmp.MOD_ID)
public final class KingdomSmp {
    public static final String MOD_ID = "kingdom_smp";
    private static final Logger LOGGER = LogUtils.getLogger();

    public KingdomSmp(IEventBus modBus) {
        KingdomNpcs.register(modBus);
        com.mohymods.kingdomsmp.building.KingdomBlueprints.register(modBus);
        com.mohymods.kingdomsmp.building.ConstructionProtection.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(com.mohymods.kingdomsmp.building.ConstructionService::tick);
        NeoForge.EVENT_BUS.addListener(ContributionService::login);
        NeoForge.EVENT_BUS.addListener(ContributionService::logout);
        BuilderHutService.installDialogueOpener(KingdomNetworking::openBuilderDialogue);
        modBus.addListener(KingdomNetworking::register);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void serverStarted(ServerStartedEvent event) {
        BuilderHutService.initialize(event.getServer());
        ContributionService.initialize(event.getServer());
        com.mohymods.kingdomsmp.building.ConstructionService.initialize(event.getServer());
        KingdomState state = ContributionService.kingdom(event.getServer());
        LOGGER.info("Kingdom SMP loaded: schema={}, tier={}, revision={}",
                state.schemaVersion(), state.attainedTier(), state.revision());
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kingdom")
                .then(BuilderHutService.commands())
                .then(com.mohymods.kingdomsmp.building.ConstructionService.commands())
                .then(Commands.literal("status").executes(context -> {
                    KingdomState state = ContributionService.kingdom(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.translatable("kingdom_smp.command.status",
                            state.attainedTier(), KingdomState.MAX_TIER, state.revision()), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
