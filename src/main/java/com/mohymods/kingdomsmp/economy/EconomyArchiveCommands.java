package com.mohymods.kingdomsmp.economy;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.util.*;

/** Real audited maintenance, bound to administrator, exact current journal state and an expiring review. */
public final class EconomyArchiveCommands {
    private record Review(UUID nonce,EconomyStore.ArchiveStatus state,String reason,long issued){}
    private static final Map<MinecraftServer,Map<UUID,Review>> REVIEWS=new WeakHashMap<>();
    private EconomyArchiveCommands(){}
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){return Commands.literal("archive").requires(source->source.hasPermission(4))
        .then(Commands.literal("inspect").executes(c->run(c.getSource(),()->{var state=store(c.getSource()).archiveStatus();line(c.getSource(),describe(state));line(c.getSource(),"Closed segments remain authoritative audit history. Back up the complete data/kingdomsmp_economy_archive directory with the journal, identity seal and matching player saves.");return 1;})))
        .then(Commands.literal("review").then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->run(c.getSource(),()->{
            var source=c.getSource();String reason=StringArgumentType.getString(c,"reason");EconomyArchive.validateReason(reason);var state=store(source).archiveStatus();if(state.nativePending())throw new IllegalStateException("Reconcile the matching native inventory checkpoint before archive maintenance");if(state.activeOperations()==0)throw new IllegalStateException("No open journal records need closing");
            var review=new Review(UUID.randomUUID(),state,reason,System.nanoTime());REVIEWS.computeIfAbsent(source.getServer(),s->new HashMap<>()).put(actor(source),review);String command="/kingdom admin economy archive confirm "+review.nonce();line(source,describe(state));line(source,"Review: close the current committed records into verified immutable segments. Balances, entitlement IDs, escrow, global revision and player receipts do not reset. Reason: "+reason);
            source.sendSuccess(()->Component.literal("[Confirm reviewed journal archive]").withStyle(style->style.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command))),false);line(source,"Review expires in 60 seconds; any journal change requires another review. Console command: "+command);return 1;
        }))))
        .then(Commands.literal("confirm").then(Commands.argument("review",UuidArgument.uuid()).executes(c->run(c.getSource(),()->{
            var source=c.getSource();var reviews=REVIEWS.get(source.getServer());var review=reviews==null?null:reviews.remove(actor(source));if(review==null||!review.nonce().equals(UuidArgument.getUuid(c,"review"))||System.nanoTime()-review.issued()>60_000_000_000L)throw new IllegalStateException("Archive review expired or belongs to another administrator");var s=store(source);if(!s.archiveStatus().equals(review.state()))throw new IllegalStateException("Journal changed after review; inspect and review again");var result=s.archive(actor(source),review.reason());line(source,"Archive published and verified. "+describe(result));return 1;
        }))));}
    private static UUID actor(CommandSourceStack source){return source.getEntity()==null?new UUID(0,0):source.getEntity().getUUID();}
    private static EconomyStore store(CommandSourceStack source){return CommerceService.store(source.getServer());}
    private static String describe(EconomyStore.ArchiveStatus s){return "Lifetime audited records "+s.revision()+"/"+EconomyArchive.MAX_TOTAL_OPERATIONS+" | active records "+s.activeOperations()+"/"+EconomyStore.MAX_OPERATIONS+" | closed segments "+s.closedSegments()+"/"+EconomyArchive.MAX_SEGMENTS+" | recovery reservations "+s.reservedOperations()+" | active bytes "+s.activeBytes()+" | native pending "+s.nativePending();}
    private static void line(CommandSourceStack source,String text){source.sendSuccess(()->Component.literal(text),false);}
    @FunctionalInterface private interface Action{int run()throws IOException;}
    private static int run(CommandSourceStack source,Action action){try{return action.run();}catch(IOException|IllegalArgumentException|IllegalStateException failure){source.sendFailure(Component.literal("Archive maintenance refused: "+failure.getMessage()));return 0;}}
}
