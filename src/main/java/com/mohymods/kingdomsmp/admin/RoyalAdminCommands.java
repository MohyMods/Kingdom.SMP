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
                .then(Commands.literal("aircraft").then(Commands.literal("inspect").executes(c->{
                    var store=com.mohymods.kingdomsmp.economy.CommerceService.store(c.getSource().getServer());
                    line(c.getSource(),"Registered aircraft: "+store.aircraft().size()+"; buyer-owned/shared access. Inspection never grants frames or alters legacy aircraft.");
                    var flights=com.mohymods.kingdomsmp.integration.aircraft.FlightService.savedMissions(c.getSource().getServer());long day=com.mohymods.kingdomsmp.integration.aircraft.FlightService.day();line(c.getSource(),"Air dispatch: UTC day "+day+" | paid "+store.flightSpent(day)+" | saved pilots "+flights.size()+" | "+com.mohymods.kingdomsmp.integration.aircraft.FlightService.status(c.getSource().getServer()));for(var flight:flights.stream().limit(64).toList())line(c.getSource(),"Dispatch "+flight.id()+" | pilot "+flight.pilot()+" | day "+flight.day()+" | distance "+(long)flight.meters()+"/"+flight.minimum()+" | landed "+flight.arrived()+" | verified "+flight.verified()+" | rewarded "+flight.rewarded());
                    line(c.getSource(),"Workshop repair reservations: "+store.aircraftRepairs().size()+" | held coins "+store.aircraftRepairEscrow());for(var repair:store.aircraftRepairs().stream().limit(64).toList())line(c.getSource(),"Repair "+repair.id()+" | registration "+repair.registration()+" | actor "+repair.actor()+" | coins "+repair.price()+" | pending native receipt");
                    for(var aircraft:store.aircraft().stream().limit(64).toList())line(c.getSource(),aircraft.id()+" | "+aircraft.type()+" | buyer "+aircraft.owner()+" | "+aircraft.phase()+" | generation "+aircraft.generation()+" | crew "+aircraft.crew());return 1;
                })))
                .then(ownerCommands())
                .then(com.mohymods.kingdomsmp.profession.FarmerInspectionService.commands())
                .then(com.mohymods.kingdomsmp.progression.TierTwoService.commands())
                .then(com.mohymods.kingdomsmp.economy.CommerceService.adminCommands())
                .then(com.mohymods.kingdomsmp.economy.AuctionService.adminCommands())
                .then(com.mohymods.kingdomsmp.economy.BuyOrderService.adminCommands())
                .then(com.mohymods.kingdomsmp.economy.MarketService.adminCommands())
                .then(Commands.literal("fishing").then(Commands.literal("inspect").then(Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->{
                    var p=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player");line(c.getSource(),"Aquaculture: "+(com.mohymods.kingdomsmp.fishing.AquacultureAccess.available()?"verified2.7.21":com.mohymods.kingdomsmp.fishing.AquacultureAccess.unavailable()));
                    var store=com.mohymods.kingdomsmp.economy.CommerceService.store(p.server);long week=com.mohymods.kingdomsmp.profession.ContractRules.period("weekly");line(c.getSource(),"UTC week "+week+" | funded fishing rewards "+store.fishingContractSpent(week));
                    if(com.mohymods.kingdomsmp.fishing.AquacultureAccess.available())for(var row:com.mohymods.kingdomsmp.fishing.FishingContractService.entries(p))line(c.getSource(),row.id()+" | "+row.state()+" | "+row.detail());return 1;
                }))))
                .then(Commands.literal("professions").then(Commands.literal("inspect").then(Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->{var p=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player");var r=com.mohymods.kingdomsmp.economy.CommerceService.store(p.server).professions(p.getUUID());c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(p.getGameProfile().getName()+" | active "+r.active()+" | retained tracks "+r.tracks()),false);var s=com.mohymods.kingdomsmp.economy.CommerceService.store(p.server);for(var gear:com.mohymods.kingdomsmp.equipment.FarmerGear.values())line(c.getSource(),gear.id()+" | successful purchases "+s.equipmentPurchases(p.getUUID(),gear.id()));line(c.getSource(),"All-player equipment coin sink: "+s.equipmentCoinsSpent());return 1;}))))
                .then(Commands.literal("contracts").then(Commands.literal("inspect").then(Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player()).executes(c->{var p=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player");var rows=new java.util.ArrayList<>(com.mohymods.kingdomsmp.profession.ContractService.entries(p));rows.addAll(com.mohymods.kingdomsmp.bounty.CommunityContractService.entries(p));rows.addAll(com.mohymods.kingdomsmp.bounty.HuntService.entries(p));for(var row:rows)c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(row.id()+" | "+row.detail()+" | reward "+row.price()+" | "+row.state()),false);line(c.getSource(),"Community Master proofs: "+com.mohymods.kingdomsmp.economy.CommerceService.store(p.server).communityMasterProofs(p.getUUID()));com.mohymods.kingdomsmp.economy.CommerceService.store(p.server).hunt(p.getUUID(),com.mohymods.kingdomsmp.bounty.HuntService.today()).ifPresent(h->line(c.getSource(),"Accepted hunt "+h.id()+" | UTC day "+h.day()+" | target "+h.target()+" | closed "+h.closed()+" | paid "+h.paid()));return 1;}))))
                .then(Commands.literal("travel").then(Commands.literal("inspect").executes(c->{
                    var server=c.getSource().getServer();int tier=ContributionService.kingdom(server).attainedTier();
                    line(c.getSource(),"Current kingdom Tier "+tier+". Destination entry checks apply server-side to players and transferred entities; no travel or tier is granted.");
                    for(var realm:java.util.stream.Stream.concat(com.mohymods.kingdomsmp.kingdom.KingdomTravelRules.realms().stream(),com.mohymods.kingdomsmp.integration.space.SpaceTravelRules.realms().stream().map(r->new com.mohymods.kingdomsmp.kingdom.KingdomTravelRules.Realm(r.dimension(),r.name(),r.tier()))).toList()){
                        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(realm.dimension()));
                        line(c.getSource(),realm.name()+" | "+realm.dimension()+" | Tier "+realm.tier()+" | "+(tier>=realm.tier()?"UNLOCKED":"LOCKED")+" | dimension registered: "+(server.getLevel(key)!=null));
                    }
                    line(c.getSource(),com.mohymods.kingdomsmp.integration.waystones.WaystoneAccess.status());
                    line(c.getSource(),com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess.status());
                    line(c.getSource(),com.mohymods.kingdomsmp.integration.aircraft.AircraftPacketGuard.status());
                    if(com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess.available())line(c.getSource(),"Aircraft current access: "+(com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess.denial(c.getSource().getServer()).isEmpty()?"UNLOCKED":com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess.denial(c.getSource().getServer())));
                    line(c.getSource(),com.mohymods.kingdomsmp.integration.space.SpaceAccess.status());return 1;
                })))
                .then(Commands.literal("integrations").then(Commands.literal("ftbquests").executes(c->{for(var line:com.mohymods.kingdomsmp.integration.ftbquests.FtbQuestsIntegration.inspect(c.getSource().getServer()))c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(line),false);return 1;})))
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
                .forEach(p -> line(source,p.id() + " | " + p.phase() + " | " + p.placed() + " / " + com.mohymods.kingdomsmp.building.ConstructionFoundations.blocks(p).size() + " blocks | " + p.foundations().size() + " supports, depth " + com.mohymods.kingdomsmp.building.ConstructionFoundations.depth(p) + " | " + p.blueprint()));
        if(store.projects().size()>10)line(source,"Showing 10 projects; inspect a known project UUID for details.");
        for(String id:java.util.List.of("ftbchunks","openpartiesandclaims","ftbquests"))
            line(source,id + ": " + ModList.get().getModContainerById(id).map(c -> c.getModInfo().getVersion().toString() + " installed").orElse("absent"));
        line(source,"Installed version is not proof of integration. Full-pack acceptance, economy and professions remain incomplete.");
        return 1;
    }
    private static String status(String failure) { return failure==null ? "matches saved progress" : failure; }
    private static void line(CommandSourceStack source,String text) { source.sendSuccess(() -> Component.literal(text),false); }
}
