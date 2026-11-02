package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.aircraft.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_aircraft_sales_tests")
@PrefixGameTestTemplate(false)
public final class AircraftSalesRuntimeTests {
  @GameTest(template = "empty", timeoutTicks = 1600)
  public static void actualPurchaseNativeDeploymentSharedAccessAndReturn(GameTestHelper h)
      throws Exception {
    var level = h.getLevel();
    var server = level.getServer();
    var path = server.getWorldPath(LevelResource.ROOT).resolve("aircraft-sales-proof.nbt");
    var project =
        ConstructionService.store(server).projects().stream()
            .filter(
                p ->
                    p.blueprint().equals(RoyalAerodromeBlueprint.ID)
                        && p.phase() == ConstructionStore.Phase.COMPLETE)
            .findFirst()
            .orElseThrow();
    var p = ClaimRuntimeTests.vanillaPlayer(level, project.owner(), project.ownerName());
    CommerceService.verifyReceipt(p);
    var blueprint = BlueprintRegistry.get(project.blueprint(), 1);
    var home = project.origin().offset(blueprint.rotate(blueprint.home(), project.rotation()));
    p.teleportTo(level, home.getX() + .5, home.getY(), home.getZ() - 2.5, 0, 0);
    level.getChunkSource().move(p);
    var peer =
        ClaimRuntimeTests.vanillaPlayer(
            level,
            UUID.nameUUIDFromBytes(
                "OfflinePlayer:AircraftCrew".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            "AircraftCrew");
    peer.teleportTo(level, p.getX(), p.getY(), p.getZ() - 1, 0, 0);
    level.getChunkSource().move(peer);
    h.runAfterDelay(
        100,
        () ->
            guard(
                h,
                () -> {
                  var npc = (ReeveEntity) level.getEntity(project.npc());
                  h.assertTrue(
                      npc != null && ConstructionService.registeredReeve(npc),
                      "Original permanent Corin remains at completed Aerodrome");
                  CommerceService.openResident(p, npc);
                  var store = CommerceService.store(server);
                  if (Files.exists(path)) {
                    var proof = AtomicNbt.read(path);
                    var record = store.aircraft(proof.getUUID("registration")).orElseThrow();
                    h.assertTrue(
                        record.owner().equals(p.getUUID())
                            && record.crew().isEmpty()
                            && record.generation() == 1
                            && record.phase()
                                == (proof.hasUUID("active")
                                    ? AircraftState.Phase.ACTIVE
                                    : AircraftState.Phase.PACKED),
                        "Buyer and revoked crew and one repack survive real dedicated restart");
                    h.assertTrue(
                        store.balance(p.getUUID()) == proof.getLong("balance")
                            && store.itemMailbox(p.getUUID()).stream()
                                    .filter(
                                        m ->
                                            m.id().equals(proof.getUUID("return"))
                                                && m.claimed() == proof.hasUUID("active"))
                                    .count()
                                == 1,
                        "Exact purse, saved native receipt and one private returned airframe"
                            + " persist");
                    if (proof.hasUUID("active")) {
                      Entity saved = level.getEntity(proof.getUUID("active"));
                      h.assertTrue(
                          record.phase() == AircraftState.Phase.ACTIVE
                              && saved != null
                              && AircraftSales.denial(p, saved).isEmpty(),
                          "Third dedicated JVM retains the same active native UUID and buyer"
                              + " rights; no absent-frame respawn");
                      h.assertTrue(
                          store.itemMailbox(p.getUUID()).stream().allMatch(m -> m.claimed()),
                          "Both original and returned private deliveries remain claimed exactly"
                              + " once");
                      if (proof.hasUUID("pending")) {
                        var recovered = store.aircraft(proof.getUUID("pending")).orElseThrow();
                        h.assertTrue(
                            recovered.phase() == AircraftState.Phase.ACTIVE
                                && level.getEntity(proof.getUUID("pendingBody")) != null
                                && recovered
                                    .deployment()
                                    .getCompound("entity")
                                    .getUUID("UUID")
                                    .equals(proof.getUUID("pendingBody")),
                            "Fourth JVM resumes consumed-receipt STARTING deployment exactly once"
                                + " when its actor and footprint are loaded");
                        CommerceService.verifyReceipt(p);
                        h.assertTrue(
                            store.pendingItem().isEmpty()
                                && !store.quarantined()
                                && store.aircraft().size() == 2,
                            "Both bought aircraft retain one canonical record with no quarantined"
                                + " or pending native receipt");
                        h.succeed();
                        com.mojang.logging.LogUtils.getLogger()
                            .info(
                                "AIRCRAFT_SALES_ACTIVE_VERIFIED: fourth dedicated JVM keeps"
                                    + " original active UUID and resumes a real saved"
                                    + " consumed-receipt STARTING deployment into exactly one"
                                    + " native body, without recharging or reissuing items");
                        return;
                      }
                      AircraftSales.review(p, "air:buy:immersive_aircraft:biplane");
                      var nextQuote = AircraftSales.quote(p).orElseThrow();
                      AircraftSales.confirm(p, nextQuote.nonce());
                      claim(p, nextQuote.nonce());
                      // Explicit fault fixture: stage the exact native receipt boundary after
                      // consumption,
                      // before world entity creation. No production fault-injection flag or bypass.
                      var next = store.aircraft(nextQuote.nonce()).orElseThrow();
                      var nextItem =
                          p.getInventory().items.stream()
                              .filter(AircraftSales::registered)
                              .findFirst()
                              .orElseThrow();
                      var pendingSite = project.origin().offset(55, 1, 13);
                      for (var floor :
                          BlockPos.betweenClosed(
                              pendingSite.offset(-8, -1, -8), pendingSite.offset(8, -1, 8)))
                        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 18);
                      var nextBody =
                          net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                              .get(net.minecraft.resources.ResourceLocation.parse(next.type()))
                              .create(level);
                      nextBody.moveTo(
                          pendingSite.getX() + .5, pendingSite.getY(), pendingSite.getZ() + .5);
                      nextBody
                          .getPersistentData()
                          .put(
                              AircraftSales.MARKER,
                              nextItem
                                  .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                                  .copyTag()
                                  .getCompound(AircraftSales.MARKER));
                      var serialized = new CompoundTag();
                      h.assertTrue(
                          nextBody.save(serialized), "Native pending aircraft serializes normally");
                      CommerceService.checkpoint(p);
                      var staged =
                          store.prepareAircraftDeployment(
                              UUID.randomUUID(),
                              p.getUUID(),
                              next.id(),
                              serialized,
                              level.dimension().location().toString());
                      nextItem.shrink(1);
                      CommerceService.putReceipt(
                          p, new EconomyStore.Receipt(store.id(), staged.sequence()));
                      CommerceService.checkpoint(p);
                      h.assertTrue(
                          store.recoverItem(
                              CommerceService.receipt(
                                  AtomicNbt.read(CommerceService.playerFile(server, p.getUUID())))),
                          "Exact consumed native receipt stages STARTING authority without a body");
                      proof.putUUID("pending", next.id());
                      proof.putUUID("pendingBody", nextBody.getUUID());
                      proof.putLong("balance", store.balance(p.getUUID()));
                      AtomicNbt.write(path, proof);
                      server.getPlayerList().remove(p);
                      h.assertTrue(
                          store.aircraft(next.id()).orElseThrow().phase()
                                  == AircraftState.Phase.STARTING
                              && level.getEntity(nextBody.getUUID()) == null,
                          "Actor is logged out before recovery can create an entity");
                      h.succeed();
                      com.mojang.logging.LogUtils.getLogger()
                          .info(
                              "AIRCRAFT_SALES_PENDING_PREPARED: third JVM verifies original active"
                                  + " body and stages exact consumed saved native receipt before"
                                  + " world spawn; actor logs out, canonical STARTING awaits real"
                                  + " restart");
                      return;
                    }
                    claim(p, proof.getUUID("return"));
                    var item =
                        p.getInventory().items.stream()
                            .filter(AircraftSales::registered)
                            .findFirst()
                            .orElseThrow();
                    p.setItemInHand(InteractionHand.MAIN_HAND, item);
                    place(h, p, project.origin().offset(40, 1, 13));
                    var latest = store.aircraft(record.id()).orElseThrow();
                    h.assertTrue(
                        latest.phase() == AircraftState.Phase.ACTIVE && latest.generation() == 1,
                        "Recovered physical airframe redeploys using its new legitimate"
                            + " generation");
                    proof.putUUID(
                        "active", latest.deployment().getCompound("entity").getUUID("UUID"));
                    AtomicNbt.write(path, proof);
                    at(p, npc);
                    CommerceService.checkpoint(p);
                    h.succeed();
                    com.mojang.logging.LogUtils.getLogger()
                        .info(
                            "AIRCRAFT_SALES_RESTART_VERIFIED: buyer"
                                + " purse/receipt/crew/generation/private returned native item"
                                + " persist; normal native item redeploys once");
                    return;
                  }
                  store.adminSet(
                      UUID.randomUUID(),
                      new UUID(0, 0),
                      p.getUUID(),
                      100000,
                      "Explicit private aircraft purchase funds fixture");
                  long initial = store.balance(p.getUUID());
                  var rows = CommerceService.snapshot(p, 1, 1, "").entries();
                  h.assertTrue(
                      rows.stream().filter(row -> row.state().equals("AIRCRAFT")).count() == 7,
                      "Corin actually lists all seven verified native aircraft in Royal counter");
                  AircraftSales.review(p, "air:buy:immersive_aircraft:biplane");
                  var quote = AircraftSales.quote(p).orElseThrow();
                  AircraftSales.confirm(p, quote.nonce());
                  var record = store.aircraft(quote.nonce()).orElseThrow();
                  h.assertTrue(
                      store.balance(p.getUUID()) == initial - quote.price()
                          && record.owner().equals(p.getUUID())
                          && record.phase() == AircraftState.Phase.PACKED,
                      "Real purchase commits exact debit, buyer and private native airframe"
                          + " together");
                  boolean repeated = false;
                  try {
                    AircraftSales.confirm(p, quote.nonce());
                  } catch (IllegalStateException expected) {
                    repeated = true;
                  }
                  h.assertTrue(
                      repeated && store.aircraft().size() == 1,
                      "Replayed GUI confirmation neither charges nor registers a second aircraft");
                  claim(p, record.id());
                  var item =
                      p.getInventory().items.stream()
                          .filter(AircraftSales::registered)
                          .findFirst()
                          .orElseThrow();
                  p.setItemInHand(InteractionHand.MAIN_HAND, item);
                  h.assertTrue(
                      !net.minecraft.world.item.crafting.Ingredient.of(item.getItem()).test(item)
                          && net.minecraft.world.item.crafting.Ingredient.of(item.getItem())
                              .test(new ItemStack(item.getItem())),
                      "Registration cannot be washed through native ingredient recipes;"
                          + " unregistered crafting remains available");
                  var duplicate = item.copy();
                  var site = project.origin().offset(40, 1, 13);
                  place(h, p, site);
                  var active = store.aircraft(record.id()).orElseThrow();
                  Entity plane =
                      level.getEntity(active.deployment().getCompound("entity").getUUID("UUID"));
                  h.assertTrue(
                      plane != null
                          && active.phase() == AircraftState.Phase.ACTIVE
                          && p.getMainHandItem().isEmpty(),
                      "Native item use actually consumes receipt, spawns saved native aircraft and"
                          + " activates its canonical UUID");
                  p.setItemInHand(InteractionHand.MAIN_HAND, duplicate);
                  var result = duplicate.getItem().use(level, p, InteractionHand.MAIN_HAND);
                  h.assertTrue(
                      result.getResult() == InteractionResult.FAIL
                          && p.getMainHandItem().getCount() == 1,
                      "A copied physical item cannot deploy a second active body");
                  p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                  peer.teleportTo(level, plane.getX(), plane.getY(), plane.getZ() - 3, 0, 0);
                  level.getChunkSource().move(peer);
                  h.assertTrue(
                      !peer.startRiding(plane, true)
                          && !AircraftInventoryAccess.entity(peer, plane),
                      "Unshared real player cannot board or access cargo even when native force"
                          + " flag is true");
                  at(p, npc);
                  CommerceService.openResident(p, npc);
                  AircraftSales.review(p, "air:grant:" + record.id() + ":" + peer.getUUID());
                  AircraftSales.confirm(p, AircraftSales.quote(p).orElseThrow().nonce());
                  h.assertTrue(
                      store.aircraft(record.id()).orElseThrow().permits(peer.getUUID())
                          && store.aircraft(record.id()).orElseThrow().owner().equals(p.getUUID()),
                      "Reviewed counter grants crew while retaining buyer identity");
                  var blocker =
                      ClaimRuntimeTests.vanillaPlayer(
                          level,
                          UUID.nameUUIDFromBytes(
                              "OfflinePlayer:AirClaimBlocker"
                                  .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                          "AirClaimBlocker");
                  ClaimRuntimeTests.ftbUnclaim(level, plane.blockPosition());
                  ClaimRuntimeTests.ftbClaim(blocker, plane.blockPosition());
                  h.assertTrue(
                      !peer.startRiding(plane, true)
                          && !AircraftInventoryAccess.entity(peer, plane),
                      "Shared crew does not override FTB Chunks aircraft permissions");
                  ClaimRuntimeTests.ftbUnclaim(level, plane.blockPosition());
                  ClaimRuntimeTests.opacUnclaim(level, plane.blockPosition());
                  ClaimRuntimeTests.opacClaim(level, blocker.getUUID(), plane.blockPosition());
                  h.assertTrue(
                      !peer.startRiding(plane, true)
                          && !AircraftInventoryAccess.entity(peer, plane),
                      "Shared crew does not override OPAC aircraft permissions");
                  ClaimRuntimeTests.opacUnclaim(level, plane.blockPosition());
                  h.assertTrue(
                      peer.startRiding(plane, true) && AircraftInventoryAccess.entity(peer, plane),
                      "Shared current player can genuinely mount and access native cargo");
                  at(p, npc);
                  AircraftSales.review(p, "air:revoke:" + record.id() + ":" + peer.getUUID());
                  AircraftSales.confirm(p, AircraftSales.quote(p).orElseThrow().nonce());
                  h.assertTrue(
                      !AircraftInventoryAccess.entity(peer, plane)
                          && !AircraftAccess.nativeLift(peer),
                      "Revocation immediately disables cargo and powered lift before periodic safe"
                          + " landing");
                  peer.stopRiding();
                  // The native drop call follows ordinary native removal/dropInventory in actual
                  // destruction.
                  // Empty native frame avoids injecting cargo or pretending to test crash combat
                  // here.
                  plane.discard();
                  var drop =
                      Class.forName("immersive_aircraft.entity.VehicleEntity")
                          .getDeclaredMethod("drop");
                  drop.setAccessible(true);
                  drop.invoke(plane);
                  var packed = store.aircraft(record.id()).orElseThrow();
                  var returned =
                      store.itemMailbox(p.getUUID()).stream()
                          .filter(m -> !m.claimed())
                          .findFirst()
                          .orElseThrow();
                  h.assertTrue(
                      packed.generation() == 1
                          && packed.phase() == AircraftState.Phase.PACKED
                          && store.itemMailbox(peer.getUUID()).isEmpty(),
                      "Transformed real native drop produces exactly one new-generation private"
                          + " airframe for buyer, never crew");
                  var stale =
                      net.minecraft.world.entity.EntityType.loadEntityRecursive(
                          active.deployment().getCompound("entity"), level, e -> e);
                  h.assertTrue(stale != null, "Saved stale native body fixture");
                  level.addFreshEntity(stale);
                  h.assertTrue(
                      AircraftSales.validateBody(stale) && stale.isRemoved(),
                      "A stale saved body is discarded without native drops after canonical"
                          + " packing");
                  var proof = new CompoundTag();
                  proof.putUUID("registration", record.id());
                  proof.putUUID("return", returned.id());
                  proof.putLong("balance", store.balance(p.getUUID()));
                  AtomicNbt.write(path, proof);
                  at(p, npc);
                  CommerceService.checkpoint(p);
                  CommerceService.checkpoint(peer);
                  h.succeed();
                  com.mojang.logging.LogUtils.getLogger()
                      .info(
                          "AIRCRAFT_SALES_PREPARED: actual Corin counter purchase/private native"
                              + " mail/deployment, copied item denied, buyer crew grant/revoke,"
                              + " native return and stale body suppression");
                }));
  }

  private static void place(GameTestHelper h, ServerPlayer p, BlockPos site) throws Exception {
    var level = p.serverLevel();
    for (var cell : BlockPos.betweenClosed(site.offset(-8, -1, -8), site.offset(8, -1, 8)))
      level.setBlock(cell, Blocks.STONE.defaultBlockState(), 18);
    p.teleportTo(level, site.getX() + .5, site.getY(), site.getZ() - 3.5, 0, 25);
    level.getChunkSource().move(p);
    var item = p.getMainHandItem();
    var result = item.getItem().use(level, p, InteractionHand.MAIN_HAND);
    h.assertTrue(
        result.getResult().consumesAction(),
        "Actual transformed native use succeeds at valid loaded ground site: "
            + result.getResult());
  }

  private static void claim(ServerPlayer p, UUID id) throws Exception {
    AuctionService.review(p, 2, id.toString(), 1, 0);
    AuctionService.confirm(p, AuctionService.quote(p).orElseThrow().nonce());
  }

  private static void at(ServerPlayer p, Entity npc) {
    p.stopRiding();
    p.teleportTo(p.serverLevel(), npc.getX() + .5, npc.getY(), npc.getZ() - 2.5, 0, 0);
    p.serverLevel().getChunkSource().move(p);
  }

  @FunctionalInterface
  private interface Step {
    void run() throws Exception;
  }

  private static void guard(GameTestHelper h, Step step) {
    try {
      step.run();
    } catch (Exception e) {
      com.mojang.logging.LogUtils.getLogger().error("Aircraft real sales assertion failed", e);
      h.runAfterDelay(1, () -> h.fail(e.toString()));
    }
  }
}
