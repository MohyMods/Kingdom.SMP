package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
/** Actual native dimension travel, readable guide and confirmed claim-aware inspection on private saved Library. */
final class LibraryFieldGuideRuntime {
 static void run(GameTestHelper h,Runnable done)throws Exception{
  var level=h.getLevel();var server=level.getServer();var id=UUID.nameUUIDFromBytes("PremisesThirdStudent".getBytes(java.nio.charset.StandardCharsets.UTF_8));var p=server.getPlayerList().getPlayer(id);if(p==null)p=ClaimRuntimeTests.vanillaPlayer(level,id,"ThirdStudent");final var player=p;
  var project=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().toString().equals(Profession.LIBRARIAN.buildingId())).findFirst().orElseThrow();var b=BlueprintRegistry.get(project.blueprint(),1);var center=project.origin().offset(b.rotate(new BlockPos(7,1,6),project.rotation()));
  var store=CommerceService.store(server);var track=store.professions(id).track(Profession.LIBRARIAN);h.assertTrue(track.completedQuestIds().containsAll(Set.of(NativeOutputQuests.LIBRARY_FAMILIES,NativeOutputQuests.LIBRARY_MAXIMUM)),"Actual earned previous native Apprentice/Journeyman Library source required");
  if(track.completedQuestIds().contains(LibraryFieldGuide.QUEST)){
   h.assertTrue(track.xp()==1400&&track.completedQuestIds().size()==11&&ConstructionService.structureIntact(level,project),"Second JVM restores all11earned1400XP and original Library/Orin");boolean guide=false;for(var pos:BlockPos.betweenClosed(center.offset(-8,-3,-8),center.offset(8,3,8)))if(level.getBlockEntity(pos) instanceof LecternBlockEntity lectern)try{guide|=LibraryExplorationService.validate(p,lectern.getBook()).length()==64;}catch(IllegalStateException noGuide){}
   h.assertTrue(guide,"Actual saved native visited-biome evidence and physical written field guide restore together");CommerceService.verifyReceipt(p);com.mojang.logging.LogUtils.getLogger().info("LIBRARY_FIELD_GUIDE_RESTART_PASSED: native saved own Journeyman biome/physical guide/11earned1400XP/original Library-Orin/canonical receipt restore without replay");done.run();return;
  }
  h.assertTrue(track.xp()==1200&&track.completedQuestIds().size()==10,"Field guide starts only after actual prior10earned native proofs");DimensionTravelRuntimeTests.seed(server,6);
  // Explicit private claim consent remains on the copied Library. No claims in a player world are modified.
  for(int x=(center.getX()-9)>>4;x<=(center.getX()+9)>>4;x++)for(int z=(center.getZ()-9)>>4;z<=(center.getZ()+9)>>4;z++){var at=new BlockPos(x*16,center.getY(),z*16);level.getChunk(x,z);ClaimRuntimeTests.ftbUnclaim(level,at);ClaimRuntimeTests.ftbClaim(p,at);ClaimRuntimeTests.opacUnclaim(level,at);ClaimRuntimeTests.opacClaim(level,id,at);}
  var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("aether:the_aether"));var target=server.getLevel(key);h.assertTrue(target!=null&&LibraryExplorationService.supported(key.location().toString()),"Actual pinned Aether dimension is available; no fabricated test dimension");var destination=new BlockPos(0,250,0);target.getChunkAt(destination);var biome=target.getBiome(destination).unwrapKey().orElseThrow().location().toString();String text="My Aether field observations\ndimension=aether:the_aether biome="+biome+"\nFloating islands and native vegetation; inspected personal field notes.";
  var fake=book(p,text);boolean denied=false;try{LibraryExplorationService.validate(p,fake);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"An unvisited written assertion cannot create native biome provenance");
  DimensionTravelRuntimeTests.seed(server,3);h.assertTrue(p.changeDimension(new DimensionTransition(target,Vec3.atCenterOf(destination),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==null&&p.serverLevel()==level,"Native ordinary-player entry remains locked below AetherTier4");DimensionTravelRuntimeTests.seed(server,6);
  h.assertTrue(p.changeDimension(new DimensionTransition(target,Vec3.atCenterOf(destination),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==p&&p.serverLevel()==target,"Actual permitted ServerPlayer changeDimension enters native Aether");
  // Embedded test peers do not run the network listener heartbeat. Execute its actual native player tick, never an evidence event/count.
  for(int n=0;n<40;n++)p.doTick();
  h.runAfterDelay(2,()->{try{
   h.assertTrue(LibraryExplorationService.validate(player,fake).length()==64,"Actual ordinary-player ticks in the native registered biome produce saved personal provenance");
   h.assertTrue(player.changeDimension(new DimensionTransition(level,Vec3.atCenterOf(center),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==player,"Native Overworld return remains available");finish(h,player,project,center,text);done.run();
  }catch(Exception failure){h.fail("Native field guide failed: "+failure);}});
 }
 static ItemStack book(ServerPlayer p,String text){var book=new ItemStack(Items.WRITTEN_BOOK);book.set(DataComponents.WRITTEN_BOOK_CONTENT,new net.minecraft.world.item.component.WrittenBookContent(net.minecraft.server.network.Filterable.passThrough("Aether field guide"),p.getGameProfile().getName(),0,List.of(net.minecraft.server.network.Filterable.<net.minecraft.network.chat.Component>passThrough(net.minecraft.network.chat.Component.literal(text))),true));return book;}
 static void finish(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project,BlockPos center,String text)throws Exception{
  var level=p.serverLevel();var b=BlueprintRegistry.get(project.blueprint(),1);BlockPos at=null;for(var pos:BlockPos.betweenClosed(center.offset(-4,0,-4),center.offset(4,0,4)))if(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)&&pos.distSqr(center)>4){at=pos.immutable();break;}h.assertTrue(at!=null,"Add a separate native reading lectern; original enchantment catalogue and royal board stay intact");var held=p.getMainHandItem().copy();BlacksmithRepairRuntime.near(p,at);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LECTERN));p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at.below()).add(0,.5,0),Direction.UP,at.below(),false));h.assertTrue(p.getMainHandItem().isEmpty()&&level.getBlockState(at).is(Blocks.LECTERN),"Real permitted placement consumes physical lectern");p.setItemInHand(InteractionHand.MAIN_HAND,held);place(h,p,at,text);
  var owner=ConstructionService.store(p.server).projects().stream().filter(q->q.blueprint().equals(BlueprintRegistry.HUT)).findFirst().orElseThrow().owner();var admin=p.server.getPlayerList().getPlayer(owner);if(admin==null)admin=ClaimRuntimeTests.vanillaPlayer(level,owner,"HallOwner");p.server.getPlayerList().op(admin.getGameProfile());try{
   admin.moveTo(center.getX()+1.5,center.getY(),center.getZ()+1.5);p.moveTo(center.getX()+2.5,center.getY(),center.getZ()+1.5);var review=ProfessionInspectionService.review(admin,p,LibraryFieldGuide.QUEST,center,"Reviewed personal native field observations readable narrative and current Library consent");
   var lectern=(LecternBlockEntity)level.getBlockEntity(at);lectern.setBook(book(p,text+"\nChanged after review"));boolean refused=false;try{ProfessionInspectionService.confirm(admin,review);}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused,"Actual changed written text after review invalidates confirmation and cannot earn XP");lectern.setBook(book(p,text));
   LibraryEnchantmentRuntime.inspect(admin,p,center,LibraryFieldGuide.QUEST);var roster=CommerceService.store(p.server).professions(p.getUUID());h.assertTrue(roster.track(Profession.LIBRARIAN).xp()==1400&&roster.track(Profession.LIBRARIAN).completedQuestIds().size()==11,"Actual confirmed native guide earns200XPonce with incomplete future objectives still closed");refused=false;try{ProfessionInspectionService.review(admin,p,LibraryFieldGuide.QUEST,center,"Trying to replay the personally inspected guide");}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused,"Repeated inspection cannot duplicate profession XP");CommerceService.verifyReceipt(p);h.assertTrue(EconomyStore.open(p.server.getWorldPath(LevelResource.ROOT).resolve("data")).professions(p.getUUID()).equals(roster)&&ConstructionService.structureIntact(level,project),"Canonical audit/rank/original library reopen and no block reward was replaced");
  }finally{p.server.getPlayerList().deop(admin.getGameProfile());}
  com.mojang.logging.LogUtils.getLogger().info("LIBRARY_FIELD_GUIDE_NATIVE_PASSED: native Aether lockedTier3/actualTier6changeDimension and biome ticks/Overworld return, unvisited written assertion refused, real lectern placement/book use/current OP2inspection/changed-text refusal/replay denial/1400earnedXP11proofs; explicit private tier-supply-claims only");
 }
 static void place(GameTestHelper h,ServerPlayer p,BlockPos at,String text){var held=p.getMainHandItem().copy();BlacksmithRepairRuntime.near(p,at);p.setItemInHand(InteractionHand.MAIN_HAND,book(p,text));p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));h.assertTrue(p.getMainHandItem().isEmpty()&&((LecternBlockEntity)p.serverLevel().getBlockEntity(at)).hasBook(),"Actual native lectern use consumes the supplied ordinary physical written book");p.setItemInHand(InteractionHand.MAIN_HAND,held);}
}
