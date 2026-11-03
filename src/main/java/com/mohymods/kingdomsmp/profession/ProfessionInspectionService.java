package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Human attestation plus current loaded geometry/consent, revalidated at explicit confirmation. */
public final class ProfessionInspectionService {
    private record Review(UUID nonce,UUID admin,UUID player,String quest,BlockPos center,String dimension,String reason,long revision,long issued,String snapshot) { }
    private static final Map<ServerPlayer,Review> REVIEWS=new WeakHashMap<>();
    private ProfessionInspectionService() { }
    public static Optional<UUID> currentReview(ServerPlayer admin){var r=REVIEWS.get(admin);return admin.hasPermissions(2)&&r!=null?Optional.of(r.nonce()):Optional.empty();}
    private static String scan(ServerPlayer admin,ServerPlayer player,String id,BlockPos center){
        if(!admin.hasPermissions(2)||admin instanceof FakePlayer||player instanceof FakePlayer||!admin.isAlive()||!player.isAlive()||player.isSpectator()||player.isCreative()
            ||admin.server.getPlayerList().getPlayer(admin.getUUID())!=admin||admin.server.getPlayerList().getPlayer(player.getUUID())!=player)throw new IllegalStateException("A living administrator and enrolled survival player must attend the inspection");
        var d=InspectionQuests.get(id);var roster=CommerceService.store(admin.server).professions(player.getUUID());
        if(!roster.isActive(d.profession())||roster.track(d.profession()).completedQuestIds().contains(id)||!InspectionQuests.catalog().mayAttempt(d.profession(),id,roster.track(d.profession()).completedQuestIds())
            ||ContributionService.kingdom(admin.server).attainedTier()<d.minimumTier()||!ProfessionService.available(player,d.profession()))throw new IllegalStateException("Profession, premises, tier or objective prerequisites are unavailable");
        if(player.serverLevel()!=admin.serverLevel()||admin.distanceToSqr(center.getX()+.5,center.getY()+.5,center.getZ()+.5)>256||player.distanceToSqr(center.getX()+.5,center.getY()+.5,center.getZ()+.5)>1024)throw new IllegalStateException("Administrator must be within16blocks and player within32blocks in the inspected dimension");
        var level=admin.serverLevel();var from=center.offset(-8,-3,-8);var to=center.offset(8,3,8);
        for(var pos:BlockPos.betweenClosed(center.offset(-9,-4,-9),center.offset(9,5,9)))if(!level.hasChunkAt(pos))throw new IllegalStateException("Inspection never loads chunks; visit the full site first");
        String denial=PlacementProtection.check(level,player,center.offset(-9,-4,-9),center.offset(9,5,9));
        if(denial!=null)throw new IllegalStateException("Inspected player's site permission: "+denial);
        int doors=0,stairs=0,markers=0,saplings=0,logs=0;Set<BlockPos> routes=new HashSet<>();
        long snapshot=0xcbf29ce484222325L;
        for(var cursor:BlockPos.betweenClosed(from,to)){
            var pos=cursor.immutable();var state=level.getBlockState(pos);
            snapshot=(snapshot^pos.asLong())*0x100000001b3L;snapshot=(snapshot^Block.getId(state))*0x100000001b3L;
            if(state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER){
                var facing=state.getValue(DoorBlock.FACING);boolean clear=true;
                for(var side:List.of(facing,facing.getOpposite())){
                    var step=pos.relative(side);
                    if(!level.getBlockState(step).getCollisionShape(level,step).isEmpty()||!level.getBlockState(step.above()).getCollisionShape(level,step.above()).isEmpty()||!level.getBlockState(step.below()).isFaceSturdy(level,step.below(),Direction.UP))clear=false;
                }
                if(clear)doors++;
            }
            if(state.getBlock() instanceof StairBlock)stairs++;
            if(state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH)||state.is(Blocks.LANTERN))markers++;
            if(state.is(BlockTags.SAPLINGS))saplings++;
            if(state.is(BlockTags.LOGS))logs++;
            if(state.is(Blocks.DIRT_PATH)&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()&&level.getBlockState(pos.above(2)).getCollisionShape(level,pos.above(2)).isEmpty())routes.add(pos);
        }
        int route=largestConnected(routes);
        if(d.profession()==Profession.BUILDER&&d.rank()==ProfessionRank.APPRENTICE){if(doors<1||stairs<1)throw new IllegalStateException("Working entrance needs a supported clear two-sided doorway, stairs and usable navigation; administrator must attest actual project authorship and consent");}
        else if(d.profession()==Profession.LUMBERJACK&&(route<8||markers<2||saplings<1||logs<1))throw new IllegalStateException("Forest route needs8connected clear path cells,2light markers, replacement saplings and a timber work area; administrator must inspect safe felling and consent");
        String proof="doors="+doors+",stairs="+stairs+",route="+route+",lights="+markers+",saplings="+saplings+",logs="+logs+",snapshot="+Long.toUnsignedString(snapshot,16);
        if(d.profession()==Profession.BUILDER&&d.rank()==ProfessionRank.JOURNEYMAN)proof=BuilderInspectionGeometry.validate(level,id,center)+",snapshot="+Long.toUnsignedString(snapshot,16);
        if(id.equals("kingdom_smp:builder/a08")){
            var retained=new StringBuilder(proof);
            for(var prior:RetainedBuildingQuests.ids().stream().filter(q->RetainedBuildingQuests.get(q).rank()==ProfessionRank.APPRENTICE).sorted().toList()){
                var actual=RetainedBuildingService.inspectCompletedProject(player,prior);int goal=RetainedBuildingRules.get(admin.server).goal(prior);
                if(actual.count()<goal)throw new IllegalStateException("Retain and maintain the approved earlier project: "+prior+" "+actual.count()+"/"+goal);
                retained.append('|').append(prior).append('=').append(actual.count()).append(':').append(actual.hash());
            }
            try{proof="doors="+doors+",stairs="+stairs+",retained="+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(retained.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
            catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        }
        if(id.equals("kingdom_smp:lumberjack/a08")){
            var data=TreeSavedData.get(admin.server).evidence();var history=new StringBuilder(proof);
            for(var metric:List.of("oak","birch","spruce","grown")){int count=data.evidence().count(player.getUUID(),ProfessionRank.APPRENTICE,metric);if(count<1)throw new IllegalStateException("Present actual recorded species and replacement history: "+metric);history.append('|').append(metric).append('=').append(count);}
            boolean mature=data.plantedNear(level.dimension().location().toString(),center).entrySet().stream().anyMatch(e->{var pos=BlockPos.of(e.getKey().position());return level.hasChunkAt(pos)&&level.getBlockState(pos).toString().equals(e.getValue().state());});
            if(!mature)throw new IllegalStateException("Present a real native-grown replacement tree near the inspected woodland");
            try{proof="forest=maintained,history="+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(history.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        }
        return proof;
    }
    static int largestConnected(Set<BlockPos> positions){
        Set<BlockPos> remaining=new HashSet<>(positions);int largest=0;
        while(!remaining.isEmpty()){
            var queue=new ArrayDeque<BlockPos>();var start=remaining.iterator().next();remaining.remove(start);queue.add(start);int size=0;
            while(!queue.isEmpty()){
                var pos=queue.removeFirst();size++;
                for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){
                    var next=pos.relative(direction).offset(0,dy,0);if(remaining.remove(next))queue.addLast(next);
                }
            }
            largest=Math.max(largest,size);
        }
        return largest;
    }
    public static UUID review(ServerPlayer admin,ServerPlayer player,String id,BlockPos center,String reason){
        if(reason==null||reason.length()<12||reason.length()>96||reason.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Record12–96characters attesting project authorship, function, safety and consent");
        String snapshot=scan(admin,player,id,center);
        var r=new Review(UUID.randomUUID(),admin.getUUID(),player.getUUID(),id,center.immutable(),admin.serverLevel().dimension().location().toString(),reason,CommerceService.store(admin.server).revision(),System.nanoTime(),snapshot);
        REVIEWS.put(admin,r);return r.nonce();
    }
    public static void confirm(ServerPlayer admin,UUID nonce)throws IOException{
        var r=REVIEWS.remove(admin);var store=CommerceService.store(admin.server);
        if(r==null||!r.admin().equals(admin.getUUID())||!r.nonce().equals(nonce)||r.revision()!=store.revision()||System.nanoTime()-r.issued()>30_000_000_000L||!r.dimension().equals(admin.serverLevel().dimension().location().toString()))throw new IllegalStateException("Inspection expired, dimension or authoritative state changed; review again");
        var player=admin.server.getPlayerList().getPlayer(r.player());if(player==null)throw new IllegalStateException("Inspected player must remain online");
        String snapshot=scan(admin,player,r.quest(),r.center());if(!snapshot.equals(r.snapshot()))throw new IllegalStateException("Inspected geometry changed; review the current site again");
        String evidence=r.dimension()+"@"+r.center().toShortString()+";"+snapshot+";"+r.reason();
        if(evidence.length()>256)throw new IllegalStateException("Inspection audit exceeds256characters; shorten the attestation");
        store.approveProfessionInspection(nonce,admin.getUUID(),player.getUUID(),r.quest(),evidence);
        com.mojang.logging.LogUtils.getLogger().info("PROFESSION_INSPECTION actor={} player={} quest={} evidence={}",admin.getUUID(),player.getUUID(),r.quest(),evidence);
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> commands(){
        return Commands.literal("profession").requires(s->s.hasPermission(2))
            .then(Commands.literal("inspect").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("quest",ResourceLocationArgument.id())
                .suggests((c,b)->{InspectionQuests.ids().stream().sorted().forEach(b::suggest);return b.buildFuture();})
                .then(Commands.argument("center",BlockPosArgument.blockPos()).then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{try{
                    var admin=c.getSource().getPlayerOrException();String id=ResourceLocationArgument.getId(c,"quest").toString();var nonce=review(admin,EntityArgument.getPlayer(c,"player"),id,BlockPosArgument.getLoadedBlockPos(c,"center"),StringArgumentType.getString(c,"reason"));
                    var r=REVIEWS.get(admin);c.getSource().sendSuccess(()->Component.literal("Review "+InspectionQuests.get(id).name()+"; "+r.snapshot()+"; "+r.reason()+". ").append(Component.literal("[Confirm inspection]").withStyle(s->s.withColor(0xE8C66A).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/kingdom admin profession confirm "+nonce)))),false);return 1;
                }catch(Exception e){c.getSource().sendFailure(Component.literal(Objects.toString(e.getMessage(),e.toString())));return 0;}}))))))
            .then(Commands.literal("confirm").then(Commands.argument("review",UuidArgument.uuid()).executes(c->{try{confirm(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"review"));c.getSource().sendSuccess(()->Component.literal("Inspected objective and earned XP saved once; no blocks or inventory changed"),false);return 1;}catch(Exception e){c.getSource().sendFailure(Component.literal(Objects.toString(e.getMessage(),e.toString())));return 0;}})));
    }
}
