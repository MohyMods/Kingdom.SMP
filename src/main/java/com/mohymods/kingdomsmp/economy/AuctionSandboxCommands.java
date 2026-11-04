package com.mohymods.kingdomsmp.economy;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.UUID;

/** Explicitly enabled isolated-world test only; never changes the real server clock or unrelated listings. */
public final class AuctionSandboxCommands {
    private AuctionSandboxCommands(){ }
    public static LiteralArgumentBuilder<CommandSourceStack> command(){
        return Commands.literal("settle-test").requires(s->s.hasPermission(4))
                .then(Commands.argument("listing",UuidArgument.uuid())
                        .executes(c->{var source=c.getSource();if(!enabled(source)){source.sendFailure(Component.literal("Disabled. This test requires an isolated world and its serverconfig/kingdomsmp-sandbox.enabled marker."));return 0;}
                            source.sendSuccess(()->Component.literal("Review exact funded timed listing "+UuidArgument.getUuid(c,"listing")+". Append confirm <reason> to settle only this listing at its saved deadline. Real time and other listings remain unchanged."),false);return 1;})
                        .then(Commands.literal("confirm").then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{
                            var source=c.getSource();if(!enabled(source)){source.sendFailure(Component.literal("Sandbox marker is absent; no settlement performed."));return 0;}
                            try{
                                var s=CommerceService.store(source.getServer());var l=s.listing(UuidArgument.getUuid(c,"listing")).orElseThrow(()->new IllegalStateException("Unknown listing."));
                                if(!l.active()||!l.timed()||l.bidder()==null)throw new IllegalStateException("Select an active funded timed auction.");
                                UUID op=UUID.randomUUID(),actor=source.getEntity()==null?com.mohymods.kingdomsmp.network.CommerceRequest.NONE:source.getEntity().getUUID();
                                s.settleTimed(op,actor,l.id(),Math.max(System.currentTimeMillis(),l.expires()),StringArgumentType.getString(c,"reason"));
                                source.sendSuccess(()->Component.literal("Sandbox settlement saved and audited: "+op+". Winner items and seller proceeds are in private mail; tax left circulation."),true);return 1;
                            }catch(java.io.IOException|IllegalArgumentException|IllegalStateException e){source.sendFailure(Component.literal(e.getMessage()==null?"Auction settlement requires checkpoint recovery.":e.getMessage()));return 0;}
                        }))));
    }
    static boolean enabled(CommandSourceStack source){return Files.isRegularFile(source.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-sandbox.enabled"),LinkOption.NOFOLLOW_LINKS);}
}
