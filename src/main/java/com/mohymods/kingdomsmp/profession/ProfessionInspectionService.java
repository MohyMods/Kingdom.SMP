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
    private record Review(UUID nonce,UUID admin,UUID player,String quest,BlockPos center,String dimension,String reason,long revision,long issued,String snapshot,CreateTransmissionInspection.Running controls) { }
    private record Scan(String snapshot,CreateTransmissionInspection.Running controls) { }
    private static final Map<ServerPlayer,Review> REVIEWS=new WeakHashMap<>();
    private ProfessionInspectionService() { }
    public static Optional<UUID> currentReview(ServerPlayer admin){var r=REVIEWS.get(admin);return admin.hasPermissions(2)&&r!=null?Optional.of(r.nonce()):Optional.empty();}
    private static Scan scan(ServerPlayer admin,ServerPlayer player,String id,BlockPos center,CreateTransmissionInspection.Running expected){
        if(!admin.hasPermissions(2)||admin instanceof FakePlayer||player instanceof FakePlayer||!admin.isAlive()||!player.isAlive()||player.isSpectator()||player.isCreative()
            ||admin.server.getPlayerList().getPlayer(admin.getUUID())!=admin||admin.server.getPlayerList().getPlayer(player.getUUID())!=player)throw new IllegalStateException("A living administrator and enrolled survival player must attend the inspection");
        var d=InspectionQuests.get(id);var roster=CommerceService.store(admin.server).professions(player.getUUID());
        if(!roster.isActive(d.profession())||roster.track(d.profession()).completedQuestIds().contains(id)||!InspectionQuests.catalog().mayAttempt(d.profession(),id,roster.track(d.profession()).completedQuestIds())
            ||ContributionService.kingdom(admin.server).attainedTier()<d.minimumTier()||!ProfessionService.available(player,d.profession()))throw new IllegalStateException("Profession, premises, tier or objective prerequisites are unavailable");
        if(player.serverLevel()!=admin.serverLevel()||admin.distanceToSqr(center.getX()+.5,center.getY()+.5,center.getZ()+.5)>256||player.distanceToSqr(center.getX()+.5,center.getY()+.5,center.getZ()+.5)>1024)throw new IllegalStateException("Administrator must be within16blocks and player within32blocks in the inspected dimension");
        var level=admin.serverLevel();int radius=id.equals("kingdom_smp:builder/j05")||id.equals("kingdom_smp:builder/j08")?25:8;var from=center.offset(-radius,-3,-radius);var to=center.offset(radius,(id.equals(LayeredInteriorInspection.QUEST)||BuilderCivicInspection.building(id))?10:3,radius);
        for(var pos:BlockPos.betweenClosed(center.offset(-radius-1,-4,-radius-1),center.offset(radius+1,(id.equals(LayeredInteriorInspection.QUEST)||BuilderCivicInspection.building(id))?12:5,radius+1)))if(!level.hasChunkAt(pos))throw new IllegalStateException("Inspection never loads chunks; visit the full site first at "+pos.toShortString());
        String denial=PlacementProtection.check(level,player,center.offset(-radius-1,-4,-radius-1),center.offset(radius+1,(id.equals(LayeredInteriorInspection.QUEST)||BuilderCivicInspection.building(id))?12:5,radius+1));
        if(denial!=null)throw new IllegalStateException("Inspected player's site permission: "+denial);
        var controls=id.equals(CreateTransmissionInspection.QUEST)?expected==null?CreateTransmissionInspection.capture(player,center):expected:null;
        int doors=0,stairs=0,markers=0,saplings=0,logs=0;Set<BlockPos> routes=new HashSet<>();
        long snapshot=0xcbf29ce484222325L;
        for(var cursor:BlockPos.betweenClosed(from,to)){
            var pos=cursor.immutable();var state=level.getBlockState(pos);
            snapshot=(snapshot^pos.asLong())*0x100000001b3L;snapshot=(snapshot^Block.getId(CreateTransmissionInspection.snapshot(pos,state,controls)))*0x100000001b3L;
            if(state.getBlock() instanceof DoorBlock&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER){
                var facing=state.getValue(DoorBlock.FACING);boolean clear=true;
                for(var side:List.of(facing,facing.getOpposite())){
                    var step=pos.relative(side);
                    boolean support=level.getBlockState(step.below()).isFaceSturdy(level,step.below(),Direction.UP)||(d.profession()==Profession.BLACKSMITH||d.profession()==Profession.MERCHANT)&&level.getBlockState(step.below()).isAir()&&level.getBlockState(step.below(2)).isFaceSturdy(level,step.below(2),Direction.UP);
                    if(!level.getBlockState(step).getCollisionShape(level,step).isEmpty()||!level.getBlockState(step.above()).getCollisionShape(level,step.above()).isEmpty()||!support)clear=false;
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
        if(d.profession()==Profession.BUILDER&&d.rank()==ProfessionRank.JOURNEYMAN)proof=BuilderInspectionGeometry.validate(player,id,center)+",snapshot="+Long.toUnsignedString(snapshot,16);
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
        if(id.equals("kingdom_smp:lumberjack/j08")){
            var data=TreeSavedData.get(admin.server).evidence();for(var metric:List.of("acacia","dark_oak","jungle","mature_cherry","mature_mangrove"))if(data.evidence().count(player.getUUID(),ProfessionRank.JOURNEYMAN,metric)<1)throw new IllegalStateException("Present actual current-band timber and owned mature-tree history");
            boolean nativeTree=data.plantedNear(level.dimension().location().toString(),center).entrySet().stream().anyMatch(e->{var pos=BlockPos.of(e.getKey().position());return level.hasChunkAt(pos)&&level.getBlockState(pos).toString().equals(e.getValue().state());});if(!nativeTree)throw new IllegalStateException("Maintain a genuine native-grown tree in the inspected managed woodland");
            Set<net.minecraft.world.level.block.Block> varieties=new HashSet<>();for(var pos:BlockPos.betweenClosed(from,to))if(level.getBlockState(pos).is(BlockTags.SAPLINGS))varieties.add(level.getBlockState(pos).getBlock());if(varieties.size()<3)throw new IllegalStateException("Maintain at least3actual nursery sapling species and accessible marked replanting areas; administrator must inspect the forestry plan");
        }
        if(id.equals(BuilderCivicInspection.ROUTES))proof=BuilderCivicInspection.routes(player,center)+",snapshot="+Long.toUnsignedString(snapshot,16);
        if(BuilderCivicInspection.building(id)){if(doors<1)throw new IllegalStateException("Maintain a usable public entrance to the supplied civic project");proof=BuilderCivicInspection.project(player,center)+",snapshot="+Long.toUnsignedString(snapshot,16);}
        if(id.equals(SupplementariesInspection.QUEST))proof=SupplementariesInspection.validate(player,center)+",snapshot="+Long.toUnsignedString(snapshot,16);
        if(id.equals(LayeredInteriorInspection.QUEST)){if(doors<1)throw new IllegalStateException("Maintain a supported clear usable entrance to the two-floor project");proof=LayeredInteriorInspection.validate(player,center)+",snapshot="+Long.toUnsignedString(snapshot,16);}
        if(id.equals(CreateWorkshopInspection.QUEST))proof=CreateWorkshopInspection.validate(player,center)+",snapshot="+Long.toUnsignedString(snapshot,16);
        if(id.equals(CreateDriveInspection.QUEST))proof=CreateDriveInspection.validate(player,center)+",snapshot="+Long.toUnsignedString(snapshot,16);
        if(d.profession()==Profession.ALCHEMIST){
            int stands=0,water=0,storage=0,fuel=0;for(var pos:BlockPos.betweenClosed(from,to)){boolean accessible=false;for(var face:Direction.Plane.HORIZONTAL){var step=pos.relative(face);if(level.getBlockState(step).getCollisionShape(level,step).isEmpty()&&level.getBlockState(step.above()).getCollisionShape(level,step.above()).isEmpty()&&level.getBlockState(step.below()).isFaceSturdy(level,step.below(),Direction.UP))accessible=true;}if(!accessible)continue;var state=level.getBlockState(pos);if(state.is(Blocks.WATER_CAULDRON))water++;if(state.is(Blocks.BARREL)||state.is(Blocks.CHEST))storage++;if(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity stand){int charged=((com.mohymods.kingdomsmp.mixin.NativeBrewingAccess)stand).kingdom$fuel();if(charged>0){stands++;fuel+=charged;}}}
            if(stands<1||water<1||storage<1||markers<1)throw new IllegalStateException("Maintain accessible native charged brewing stand, actual water cauldron, lighting and storage; placing furniture alone cannot charge blaze fuel");proof="laboratory=working,water="+water+",fuel="+fuel+",snapshot="+Long.toUnsignedString(snapshot,16);
        }
        if(d.profession()==Profession.MERCHANT){
            int tables=0,lecterns=0,storage=0;for(var pos:BlockPos.betweenClosed(from,to)){var state=level.getBlockState(pos);boolean accessible=false;for(var face:Direction.Plane.HORIZONTAL){var step=pos.relative(face);if(level.getBlockState(step).getCollisionShape(level,step).isEmpty()&&level.getBlockState(step.above()).getCollisionShape(level,step.above()).isEmpty()&&level.getBlockState(step.below()).isFaceSturdy(level,step.below(),Direction.UP))accessible=true;}if(!accessible)continue;if(state.is(Blocks.CARTOGRAPHY_TABLE))tables++;if(state.is(Blocks.LECTERN))lecterns++;if(state.is(Blocks.BARREL)||state.is(Blocks.CHEST))storage++;}
            if(doors<1||markers<1||tables<1||lecterns<1||storage<1)throw new IllegalStateException("Maintain a lit accessible trading desk, accounts lectern and delivery storage: doors="+doors+",lights="+markers+",tables="+tables+",lecterns="+lecterns+",storage="+storage);
            var store=CommerceService.store(admin.server);try{CommerceService.verifyReceipt(player,store);}catch(java.io.IOException failure){throw new IllegalStateException("Reconcile the original native inventory receipt before account inspection",failure);}
            var accounts=store.merchantProof(player.getUUID());if(!accounts.exchanges().containsAll(Set.of("copper:silver","silver:gold"))||!accounts.orderFilled())throw new IllegalStateException("Inspect genuine coin conservation and settled order records");
            proof="accounts=canonical,exchanges=2,mail="+store.itemMailbox(player.getUUID()).size()+",coins="+store.balance(player.getUUID())+",snapshot="+Long.toUnsignedString(snapshot,16);
        }
        if(d.profession()==Profession.LIBRARIAN){
            int shelves=0,lecterns=0,enchanting=0;boolean witnessed=false;String catalogue=null,catalogueError=null,guide=null;var data=ProfessionEvidenceSavedData.get(admin.server).evidence(Profession.LIBRARIAN);
            for(var pos:BlockPos.betweenClosed(from,to)){var state=level.getBlockState(pos);if(state.is(Blocks.BOOKSHELF))shelves++;if(!(state.is(Blocks.LECTERN)||state.is(Blocks.ENCHANTING_TABLE)))continue;boolean access=false;for(var face:Direction.Plane.HORIZONTAL){var at=pos.relative(face);if(level.getBlockState(at).getCollisionShape(level,at).isEmpty()&&level.getBlockState(at.above()).getCollisionShape(level,at.above()).isEmpty()&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))access=true;}if(!access)continue;if(state.is(Blocks.LECTERN)){lecterns++;if(id.equals(LibraryFieldGuide.QUEST)&&level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern&&lectern.hasBook()){try{var candidate=LibraryExplorationService.validate(player,lectern.getBook());if(guide==null||candidate.compareTo(guide)<0)guide=candidate;}catch(IllegalStateException invalid){catalogueError=invalid.getMessage();}}if((id.equals("kingdom_smp:librarian/a06")||id.equals("kingdom_smp:librarian/a08"))&&level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern&&lectern.getBook().has(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT)){try{var candidate=LibraryCatalogue.validate(player,lectern.getBook());if(catalogue==null||candidate.compareTo(catalogue)<0)catalogue=candidate;}catch(IllegalStateException invalid){catalogueError=invalid.getMessage();}}}else{enchanting++;if(data.seen(player.getUUID(),LibraryEnchantService.METRIC,LibraryEnchantService.siteIdentity(level.dimension().location().toString(),pos)))witnessed=true;}}
            if(shelves<LibraryQuestRules.get(admin.server).bookshelves()||lecterns<1||markers<1)throw new IllegalStateException("Maintain the approved reading area: required actual bookshelves, light and a safely accessible lectern; administrator must attest readability, authorship and consent");
            if((id.equals("kingdom_smp:librarian/a03")||id.equals("kingdom_smp:librarian/a08"))&&(enchanting<1||!witnessed))throw new IllegalStateException("Use this actual permitted enchanting table with genuine native lapis and XP after the reading-area inspection; then review its setup");
            if((id.equals("kingdom_smp:librarian/a06")||id.equals("kingdom_smp:librarian/a08"))&&catalogue==null)throw new IllegalStateException(catalogueError==null?"Maintain the actual inspected written enchantment catalogue on an accessible reading lectern":catalogueError);
            if(id.equals(LibraryFieldGuide.QUEST)&&guide==null)throw new IllegalStateException(catalogueError==null?"Present a written field guide referencing your actual saved visits on an accessible lectern":catalogueError);
            proof="library="+shelves+",book="+(guide!=null?guide:catalogue==null?"none":catalogue)+",site="+witnessed+",snapshot="+Long.toUnsignedString(snapshot,16);
        }
        if(id.equals("kingdom_smp:blacksmith/a08")||id.equals("kingdom_smp:blacksmith/j08")){
            int anvils=0,furnaces=0,storage=0,fuel=0;
            for(var pos:BlockPos.betweenClosed(from,to)){var state=level.getBlockState(pos);boolean accessible=false;for(var face:Direction.Plane.HORIZONTAL){var step=pos.relative(face);if(level.getBlockState(step).getCollisionShape(level,step).isEmpty()&&level.getBlockState(step.above()).getCollisionShape(level,step.above()).isEmpty()&&level.getBlockState(step.below()).isFaceSturdy(level,step.below(),Direction.UP))accessible=true;}
                if(!accessible)continue;if(state.getBlock() instanceof AnvilBlock)anvils++;if(state.getBlock() instanceof AbstractFurnaceBlock)furnaces++;if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container){storage++;for(int slot=0;slot<Math.min(128,container.getContainerSize());slot++){var stack=container.getItem(slot);if(stack.is(net.minecraft.world.item.Items.COAL)||stack.is(net.minecraft.world.item.Items.CHARCOAL))fuel=Math.addExact(fuel,stack.getCount());}}
            }
            if(doors<1||anvils<1||furnaces<1||storage<1||fuel<1)throw new IllegalStateException("Working smithy needs safe access/anvil/furnace/storage/fuel: doors="+doors+",anvils="+anvils+",furnaces="+furnaces+",storage="+storage+",fuel="+fuel+". Administrator must attest workflow and consent");
            proof="forge=working,fuel="+fuel+",snapshot="+Long.toUnsignedString(snapshot,16);
        }
        if(controls!=null){if(expected!=null)CreateTransmissionInspection.stopped(player,controls);proof="transmission="+controls.digest()+",snapshot="+Long.toUnsignedString(snapshot,16);}
        return new Scan(proof,controls);
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
        var scan=scan(admin,player,id,center,null);
        var r=new Review(UUID.randomUUID(),admin.getUUID(),player.getUUID(),id,center.immutable(),admin.serverLevel().dimension().location().toString(),reason,CommerceService.store(admin.server).revision(),System.nanoTime(),scan.snapshot(),scan.controls());
        REVIEWS.put(admin,r);return r.nonce();
    }
    public static void confirm(ServerPlayer admin,UUID nonce)throws IOException{
        var r=REVIEWS.remove(admin);var store=CommerceService.store(admin.server);
        if(r==null||!r.admin().equals(admin.getUUID())||!r.nonce().equals(nonce)||r.revision()!=store.revision()||System.nanoTime()-r.issued()>30_000_000_000L||!r.dimension().equals(admin.serverLevel().dimension().location().toString()))throw new IllegalStateException("Inspection expired, dimension or authoritative state changed; review again");
        var player=admin.server.getPlayerList().getPlayer(r.player());if(player==null)throw new IllegalStateException("Inspected player must remain online");
        String snapshot=scan(admin,player,r.quest(),r.center(),r.controls()).snapshot();if(!snapshot.equals(r.snapshot()))throw new IllegalStateException("Inspected geometry changed; review the current site again");
        String evidence=r.dimension()+"@"+r.center().toShortString()+";"+snapshot+";"+r.reason();
        if(evidence.length()>256)throw new IllegalStateException("Inspection audit exceeds256characters; shorten the attestation");
        store.approveProfessionInspection(nonce,admin.getUUID(),player.getUUID(),r.quest(),evidence);
        com.mojang.logging.LogUtils.getLogger().info("PROFESSION_INSPECTION actor={} player={} quest={} evidence={}",admin.getUUID(),player.getUUID(),r.quest(),evidence);
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> commands(){
        return Commands.literal("profession").requires(s->s.hasPermission(2))
            .then(Commands.literal("ores").then(Commands.argument("player",EntityArgument.player()).executes(c->{try{var player=EntityArgument.getPlayer(c,"player");var data=NativeOreSavedData.get(c.getSource().getServer()).evidence();c.getSource().sendSuccess(()->Component.literal("Native mining for "+player.getGameProfile().getName()+": "+data.count(player.getUUID(),ProfessionRank.APPRENTICE,"coal")+" coal and "+data.count(player.getUUID(),ProfessionRank.APPRENTICE,"iron")+" iron; "+data.count(player.getUUID(),ProfessionRank.APPRENTICE,"copper")+" raw native copper, "+data.copper(player.getUUID(),ProfessionRank.APPRENTICE,NativeOreRules.get(player.server).separation())+" in separate working areas. Bounded area history: "+data.areas(player.getUUID(),16)+". Read-only. Unknown legacy locations are not assigned; restore matching evidence rather than resetting it."),false);return 1;}catch(RuntimeException failure){c.getSource().sendFailure(Component.literal(Objects.toString(failure.getMessage(),"Matching native mining evidence recovery required")));return 0;}})))
            .then(Commands.literal("milling").then(Commands.argument("player",EntityArgument.player()).executes(c->{try{var player=EntityArgument.getPlayer(c,"player");var proof=MillingSavedData.get(c.getSource().getServer()).proof();c.getSource().sendSuccess(()->Component.literal("Native milling for "+player.getGameProfile().getName()+": "+proof.count(player.getUUID())+" personally verified flour outputs. Pending native batches (up to16):"),false);for(var entry:proof.jobsFor(player.getUUID(),16)){var cell=entry.getKey();var job=entry.getValue();c.getSource().sendSuccess(()->Component.literal(cell.dimension()+"@"+BlockPos.of(cell.position()).toShortString()+" source="+job.source()+" remaining="+job.remaining()+"/"+job.initial()+". Read-only; inspect matching native input before any recovery."),false);}return 1;}catch(Exception failure){c.getSource().sendFailure(Component.literal(Objects.toString(failure.getMessage(),"Matching milling evidence recovery required")));return 0;}})))
            .then(Commands.literal("repairs").then(Commands.argument("player",EntityArgument.player()).executes(c->{try{var player=EntityArgument.getPlayer(c,"player");var data=RepairSavedData.get(c.getSource().getServer()).evidence();c.getSource().sendSuccess(()->Component.literal("Native repair evidence for "+player.getGameProfile().getName()+": "+data.count(player.getUUID(),ProfessionRank.APPRENTICE)+" distinct Apprentice and "+data.count(player.getUUID(),ProfessionRank.JOURNEYMAN)+" enchanted Journeyman jobs. "+CommerceService.store(c.getSource().getServer()).enchantedOutputAudit(player.getUUID())+". Bounded native lineages: "+data.history(player.getUUID(),16)),false);return 1;}catch(Exception failure){c.getSource().sendFailure(Component.literal(Objects.toString(failure.getMessage(),"Matching repair evidence recovery required")));return 0;}})))
            .then(Commands.literal("inspect").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("quest",ResourceLocationArgument.id())
                .suggests((c,b)->{InspectionQuests.ids().stream().sorted().forEach(b::suggest);return b.buildFuture();})
                .then(Commands.argument("center",BlockPosArgument.blockPos()).then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{try{
                    var admin=c.getSource().getPlayerOrException();String id=ResourceLocationArgument.getId(c,"quest").toString();var nonce=review(admin,EntityArgument.getPlayer(c,"player"),id,BlockPosArgument.getLoadedBlockPos(c,"center"),StringArgumentType.getString(c,"reason"));
                    var r=REVIEWS.get(admin);c.getSource().sendSuccess(()->Component.literal("Review "+InspectionQuests.get(id).name()+"; "+r.snapshot()+"; "+r.reason()+". "+(r.controls()==null?"":"Switch lever at "+r.controls().lever().toShortString()+" on and wait for the reviewed output to stop. ")).append(Component.literal("[Confirm inspection]").withStyle(s->s.withColor(0xE8C66A).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/kingdom admin profession confirm "+nonce)))),false);return 1;
                }catch(Exception e){c.getSource().sendFailure(Component.literal(Objects.toString(e.getMessage(),e.toString())));return 0;}}))))))
            .then(Commands.literal("confirm").then(Commands.argument("review",UuidArgument.uuid()).executes(c->{try{confirm(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"review"));c.getSource().sendSuccess(()->Component.literal("Inspected objective and earned XP saved once; no blocks or inventory changed"),false);return 1;}catch(Exception e){c.getSource().sendFailure(Component.literal(Objects.toString(e.getMessage(),e.toString())));return 0;}})));
    }
}
