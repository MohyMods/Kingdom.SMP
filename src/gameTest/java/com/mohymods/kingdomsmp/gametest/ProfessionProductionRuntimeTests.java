package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
/** Focused native producers on a copied, stopped, earned all-premises fixture; original saves stay untouched. */
public final class ProfessionProductionRuntimeTests {
 public static void commissionedMaterials(GameTestHelper h)throws Exception{
  var level=h.getLevel();var server=level.getServer();server.tickRateManager().setTickRate(200);var projects=ConstructionService.store(server).projects().stream().filter(p->ProfessionPremises.contains(p.blueprint())).toList();h.assertTrue(projects.size()==9,"Focused test requires the nine original paid premises");var buyer=ClaimRuntimeTests.vanillaPlayer(level,projects.getFirst().owner(),projects.getFirst().ownerName());var hub=ConstructionService.lodge(server).origin().offset(119,0,79);buyer.moveTo(hub.getX()+.5,hub.getY()+1,hub.getZ()+.5);level.getChunkSource().move(buyer);for(var p:projects){var bounds=ConstructionService.bounds(p);for(int x=((int)bounds.minX)>>4;x<=((int)bounds.maxX)>>4;x++)for(int z=((int)bounds.minZ)>>4;z<=((int)bounds.maxZ)>>4;z++)level.getChunk(x,z);}
  ProfessionPremisesRuntimeTests.whenResidents(h,projects,()->{try{for(var project:projects)ProfessionPremisesRuntimeTests.verify(h,project);for(var role:List.of(Profession.MINER,Profession.LIBRARIAN))produce(h,role);if(Boolean.getBoolean("kingdom.testLibraryFieldGuide")){LibraryFieldGuideRuntime.run(h,()->h.succeed());return;}if(Boolean.getBoolean("kingdom.testCreateDrive")){CreateDriveRuntime.run(h,()->h.succeed());return;}if(Boolean.getBoolean("kingdom.testCreateCraft")){CreateCraftRuntime.run(h);h.succeed();return;}LibraryEnchantmentRuntime.run(h);if(Boolean.getBoolean("kingdom.testMerchantProduction")){MerchantTransactionRuntime.run(h,()->{com.mojang.logging.LogUtils.getLogger().info("PROFESSION_PRODUCTION_PASSED: native Merchant transactions and original profession regression");h.succeed();});return;}var proof=new CompoundTag();proof.putInt("schema",1);proof.putUUID("store",CommerceService.store(server).id());AtomicNbt.write(server.getWorldPath(LevelResource.ROOT).resolve("profession-production-proof.nbt"),proof);com.mojang.logging.LogUtils.getLogger().info("PROFESSION_PRODUCTION_PASSED: copied stopped original9premises/identities/enrollments/claims, retained or earned128native stonebrick/32book commissions and their original role XP/native receipts, actual Library native progression; only explicit private supply/consent/vanilla-XP fixtures, no XP/quest/native output injection");h.succeed();}catch(Exception failure){h.fail(failure.toString());}});
 }
 static void produce(GameTestHelper h,Profession role)throws Exception{
  var server=h.getLevel().getServer();var level=h.getLevel();var store=CommerceService.store(server);var project=ConstructionService.store(server).projects().stream().filter(p->p.blueprint().toString().equals(role.buildingId())).findFirst().orElseThrow();var b=BlueprintRegistry.get(project.blueprint(),1);var id=role==Profession.MINER?project.owner():UUID.nameUUIDFromBytes("PremisesThirdStudent".getBytes(java.nio.charset.StandardCharsets.UTF_8));var p=server.getPlayerList().getPlayer(id);if(p==null)p=ClaimRuntimeTests.vanillaPlayer(level,id,role==Profession.MINER?project.ownerName():"ThirdStudent");String quest=role==Profession.MINER?CraftDeliveryQuests.MINER_MASONRY:CraftDeliveryQuests.LIBRARY_BOOKS;long xp=store.professions(id).track(role).xp();h.assertTrue(store.professions(id).isActive(role),"Existing correct profession remains active");if(store.professions(id).track(role).completedQuestIds().contains(quest)){h.assertTrue(CraftQuestEvidence.count(p,role,ProfessionRank.APPRENTICE,role==Profession.MINER?"minecraft:stone_bricks":"minecraft:book")>0,"Existing completed material objective retains its original native proof");CommerceService.verifyReceipt(p);return;}
  p.closeContainer();var menu=p.inventoryMenu;p.containerMenu=menu;
  // Both approved primary recipes fit ordinary personal2x2crafting; no claimed workstation is bypassed.
  for(int n=0;n<32;n++){for(int slot=1;slot<=4;slot++)menu.getSlot(slot).set(ItemStack.EMPTY);for(int slot=1;slot<=4;slot++)menu.getSlot(slot).set(new ItemStack(role==Profession.MINER?Items.STONE:slot==4?Items.LEATHER:Items.PAPER));h.assertTrue(menu.getSlot(0).getItem().is(role==Profession.MINER?Items.STONE_BRICKS:Items.BOOK),"Actual native personal2x2primary commission recipe");menu.quickMoveStack(p,0);for(int slot=1;slot<=4;slot++)h.assertTrue(menu.getSlot(slot).getItem().isEmpty(),"Native crafting consumes every primary ingredient cell");}
  p.closeContainer();h.assertTrue(CraftQuestEvidence.count(p,role,ProfessionRank.APPRENTICE,role==Profession.MINER?"minecraft:stone_bricks":"minecraft:book")== (role==Profession.MINER?128:32),"Exact primary native recipes supply only this active role's production evidence");var home=project.origin().offset(b.rotate(b.home(),project.rotation()));BlacksmithRepairRuntime.near(p,home);CommerceService.openResident(p,(ReeveEntity)level.getEntity(project.npc()));var quote=ProfessionService.review(p,"materials:"+quest);ProfessionService.confirm(p,quote.nonce());h.assertTrue(p.getInventory().countItem(role==Profession.MINER?Items.STONE_BRICKS:Items.BOOK)==0&&store.professions(id).track(role).xp()==xp+100,"Matching native physical outputs are permanently consumed for exactly100professionXP");CommerceService.verifyReceipt(p);h.assertTrue(EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data")).professions(id).equals(store.professions(id)),"Native receipt and prior saved records reopen exactly");
 }
}
