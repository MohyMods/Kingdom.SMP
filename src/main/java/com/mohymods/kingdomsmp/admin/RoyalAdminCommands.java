package com.mohymods.kingdomsmp.admin;

import com.mohymods.kingdomsmp.building.ConstructionService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Real operator diagnostics and an explicit one-time starting-owner assignment. */
public final class RoyalAdminCommands {
    private RoyalAdminCommands() { }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("admin").requires(source -> source.hasPermission(2))
                .executes(context -> overview(context.getSource()))
                .then(ownerCommands())
                .then(com.mohymods.kingdomsmp.progression.TierTwoService.commands())
                .then(com.mohymods.kingdomsmp.economy.CommerceService.adminCommands())
                .then(Commands.literal("inspect").executes(context -> overview(context.getSource())))
                .then(Commands.literal("construction")
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("project", UuidArgument.uuid()).executes(context -> {
                                    var source=context.getSource();
                                    var id=UuidArgument.getUuid(context,"project");
                                    var project=ConstructionService.store(source.getServer()).projects().stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
                                    if(project==null){source.sendFailure(Component.literal("No construction project exists with that UUID."));return 0;}
                                    line(source,"Project " + project.id() + " | " + project.phase() + " | blueprint " + project.blueprint() + " v" + project.blueprintVersion());
                                    line(source,"Owner " + project.owner() + " | placed " + project.placed() + " | origin " + project.origin() + " | rotation " + project.rotation());
                                    line(source,"Resident " + project.npc() + " | issued " + project.npcIssued() + " | dead " + project.npcDead());
                                    if(project.origin()!=null) {
                                        boolean loaded=ConstructionService.loaded(source.getServer().overworld(),project);
                                        line(source,"Chunks loaded: " + loaded + " | prefix: " + (loaded ? status(ConstructionService.verifyPrefix(source.getServer().overworld(),project)) : "not inspected; chunks unloaded"));
                                    }
                                    return 1;
                                }))))
                .then(Commands.literal("validate").executes(context -> context.getSource().getServer().getCommands().getDispatcher()
                        .execute("kingdom milestones validate",context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ownerCommands(){
        return Commands.literal("owner").requires(source->source.hasPermission(4))
            .then(Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player())
                .executes(c->{var p=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player");line(c.getSource(),"Review: pin the free starting Hut to "+p.getGameProfile().getName()+" ("+p.getUUID()+"). Run the same command with confirm. This cannot reassign an existing owner.");return 1;})
                .then(Commands.literal("confirm").executes(c->{
                    var player=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player");
                    try{
                        var profile=com.mohymods.kingdomsmp.progression.WorldProgression.get(c.getSource().getServer());profile.assignOwner(player.getUUID());
                        ConstructionService.store(player.server);
                        com.mojang.logging.LogUtils.getLogger().info("STARTING_OWNER_CONFIRMED actor={} owner={}",c.getSource().getTextName(),player.getUUID());
                        line(c.getSource(),"Owner pinned. The player can open K → Construction to claim the free Hut plan.");return 1;
                    }catch(java.io.IOException|IllegalStateException e){c.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}
                })));
    }
    private static int overview(CommandSourceStack source) {
        var server=source.getServer();
        var profile=com.mohymods.kingdomsmp.progression.WorldProgression.get(server);
        line(source,"Progression profile v1: "+profile.mode()+" | starting owner "+profile.owner());
        var state=ContributionService.kingdom(server);
        line(source,"Kingdom " + state.attainedTier() + "/12 | revision " + state.revision());
        var hut=BuilderHutSavedData.get(server).hut();
        line(source,hut.map(h -> "Hut " + h.id() + " | " + h.phase() + " | Aldric " + h.npcId() + " | dead " + h.npcDead()).orElse("No Hut reservation"));
        var store=ConstructionService.store(server);
        line(source,"Construction journal revision " + store.revision() + " | quarantined " + store.quarantined() + " | projects " + store.projects().size());
        store.projects().stream().sorted(java.util.Comparator.comparing(p -> p.id().toString())).limit(10)
                .forEach(p -> line(source,p.id() + " | " + p.phase() + " | " + p.placed() + " blocks | " + p.blueprint()));
        if(store.projects().size()>10)line(source,"Showing 10 projects; inspect a known project UUID for details.");
        for(String id:java.util.List.of("ftbchunks","openpartiesandclaims","ftbquests"))
            line(source,id + ": " + ModList.get().getModContainerById(id).map(c -> c.getModInfo().getVersion().toString() + " installed").orElse("absent"));
        line(source,"Installed version is not proof of integration. Full-pack acceptance, economy and professions remain incomplete.");
        return 1;
    }
    private static String status(String failure) { return failure==null ? "matches saved progress" : failure; }
    private static void line(CommandSourceStack source,String text) { source.sendSuccess(() -> Component.literal(text),false); }
}
