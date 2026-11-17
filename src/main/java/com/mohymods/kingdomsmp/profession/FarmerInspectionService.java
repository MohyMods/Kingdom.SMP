package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** A real local human review, structural minimums, current claim checks, exact confirmation and canonical audit. */
public final class FarmerInspectionService {
 private record Inspection(UUID nonce,UUID admin,UUID player,String quest,BlockPos center,String reason,long revision,long issued,String proof){ }
 private static final Map<ServerPlayer,Inspection> REVIEWS=new WeakHashMap<>();
 private FarmerInspectionService(){ }
 public static Optional<UUID> currentReview(ServerPlayer admin){var review=REVIEWS.get(admin);return admin.hasPermissions(2)&&review!=null?Optional.of(review.nonce()):Optional.empty();}
 private static String scan(ServerPlayer admin,ServerPlayer player,String quest,BlockPos center){
  if(!admin.hasPermissions(2)||admin instanceof net.neoforged.neoforge.common.util.FakePlayer||!admin.isAlive())throw new IllegalStateException("A living administrator must inspect the actual farm.");
  if(!FarmerQuests.inspection(quest))throw new IllegalArgumentException("Choose Farmer a08, j08, m07 or m08.");
  var roster=CommerceService.store(admin.server).professions(player.getUUID());if(!roster.isActive(Profession.FARMER)||roster.track(Profession.FARMER).completedQuestIds().contains(quest)||!FarmerQuests.catalog().mayAttempt(Profession.FARMER,quest,roster.track(Profession.FARMER).completedQuestIds()))throw new IllegalStateException("Farmer inspection is inactive, already completed or missing required earlier proofs.");
  if(player.serverLevel()!=admin.serverLevel()||admin.distanceToSqr(center.getX()+.5,center.getY()+.5,center.getZ()+.5)>256)throw new IllegalStateException("Inspect within16blocks in the player's current dimension.");
  var level=admin.serverLevel();var from=center.offset(-8,-3,-8);var to=center.offset(8,3,8);String denial=PlacementProtection.check(level,player,from,to);if(denial!=null)throw new IllegalStateException("Target player's farm permissions: "+denial);
  int soil=0,crops=0,water=0,paths=0,storage=0;Set<Block> categories=new HashSet<>();
  for(var pos:BlockPos.betweenClosed(from,to)){
   if(!level.hasChunkAt(pos))throw new IllegalStateException("Inspection never loads chunks; visit the complete farm area first.");var state=level.getBlockState(pos);if(state.is(Blocks.FARMLAND))soil++;if(state.getBlock() instanceof CropBlock){crops++;categories.add(state.getBlock());}if(state.getFluidState().is(Fluids.WATER))water++;if(state.is(Blocks.DIRT_PATH))paths++;if(state.is(Blocks.CHEST)||state.is(Blocks.BARREL))storage++;
  }
  String slot=quest.substring(quest.lastIndexOf('/')+1);boolean first=slot.equals("a08"),middle=slot.equals("j08");int minSoil=first?8:middle?24:48,minCrops=first?4:middle?12:24,minTypes=first?1:middle?3:4,minWater=first?1:middle?2:4,minPaths=first?1:middle?4:8,minStorage=first?0:middle?1:2;
  long animals=level.getEntitiesOfClass(Animal.class,new AABB(from.getX(),from.getY(),from.getZ(),to.getX()+1,to.getY()+1,to.getZ()+1),a->a.isAlive()&&!a.isBaby()).size();
  String proof="soil="+soil+",crops="+crops+",types="+categories.size()+",water="+water+",paths="+paths+",storage="+storage+",animals="+animals;
  if(soil<minSoil||crops<minCrops||categories.size()<minTypes||water<minWater||paths<minPaths||storage<minStorage||!first&&!middle&&animals<4)throw new IllegalStateException("Farm minimums not met: "+proof+"; need soil/crops/types/water/paths/storage "+minSoil+"/"+minCrops+"/"+minTypes+"/"+minWater+"/"+minPaths+"/"+minStorage+(!first&&!middle?" and4adult farm animals":""));
  return proof;
 }
 public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> commands(){
  return Commands.literal("farmer").requires(s->s.hasPermission(2))
   .then(Commands.literal("inspect").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("quest",ResourceLocationArgument.id()).suggests((c,b)->{for(String id:List.of("kingdom_smp:farmer/a08","kingdom_smp:farmer/j08","kingdom_smp:farmer/m07","kingdom_smp:farmer/m08"))b.suggest(id);return b.buildFuture();}).then(Commands.argument("center",BlockPosArgument.blockPos()).then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{try{
    var admin=c.getSource().getPlayerOrException();var player=EntityArgument.getPlayer(c,"player");String id=ResourceLocationArgument.getId(c,"quest").toString(),reason=StringArgumentType.getString(c,"reason");if(reason.length()<12||reason.length()>96)throw new IllegalArgumentException("Record a12–96character human inspection: ownership, functionality and shared-access criteria.");var center=BlockPosArgument.getLoadedBlockPos(c,"center");String proof=scan(admin,player,id,center);var review=new Inspection(UUID.randomUUID(),admin.getUUID(),player.getUUID(),id,center.immutable(),reason,CommerceService.store(admin.server).revision(),System.nanoTime(),proof);REVIEWS.put(admin,review);String command="/kingdom admin farmer confirm "+review.nonce();c.getSource().sendSuccess(()->Component.literal("Review "+FarmerQuests.title(id)+" for "+player.getGameProfile().getName()+" at "+center.toShortString()+". "+proof+". Attestation: "+reason+". ").append(Component.literal("[Confirm inspected site]").withStyle(s->s.withColor(0xE8C66A).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,command)))),false);return 1;
   }catch(Exception e){c.getSource().sendFailure(Component.literal(e.getMessage()==null?e.toString():e.getMessage()));return 0;}}))))))
   .then(Commands.literal("confirm").then(Commands.argument("review",UuidArgument.uuid()).executes(c->{try{
    var admin=c.getSource().getPlayerOrException();var review=REVIEWS.remove(admin);var store=CommerceService.store(admin.server);if(review==null||!review.admin().equals(admin.getUUID())||!review.nonce().equals(UuidArgument.getUuid(c,"review"))||review.revision()!=store.revision()||System.nanoTime()-review.issued()>30_000_000_000L)throw new IllegalStateException("Inspection review expired or authoritative state changed. Inspect again.");var player=admin.server.getPlayerList().getPlayer(review.player());if(player==null)throw new IllegalStateException("The inspected player must remain online.");String proof=scan(admin,player,review.quest(),review.center());String evidence=admin.serverLevel().dimension().location()+"@"+review.center().toShortString()+";"+proof+";"+review.reason();if(evidence.length()>256)throw new IllegalStateException("Inspection evidence exceeds audit bounds; shorten its reason.");store.approveFarmerInspection(review.nonce(),admin.getUUID(),player.getUUID(),review.quest(),evidence);com.mojang.logging.LogUtils.getLogger().info("FARMER_INSPECTION actor={} player={} quest={} evidence={}",admin.getUUID(),player.getUUID(),review.quest(),evidence);c.getSource().sendSuccess(()->Component.literal("Inspection and personal quest XP saved together once. No property or claim was changed."),false);return 1;
   }catch(Exception e){c.getSource().sendFailure(Component.literal(e.getMessage()==null?e.toString():e.getMessage()));return 0;}})));
 }
}
