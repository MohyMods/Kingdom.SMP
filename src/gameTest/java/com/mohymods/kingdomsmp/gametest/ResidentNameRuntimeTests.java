package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.progression.WorldProgression;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_resident_name_tests")
@PrefixGameTestTemplate(false)
public final class ResidentNameRuntimeTests {
  static final String BUILDING = "kingdom_smp:builders_workshop", NAME = "Cedric Valehart";

  @GameTest(template = "empty", timeoutTicks = 3000)
  public static void reviewedBuyerNameSurvivesConstructionAndThreeRealBoots(GameTestHelper h)
      throws Exception {
    var server = h.getLevel().getServer();
    var hall = ConstructionService.lodge(server);
    // The stopped purchased-aircraft fixture leaves Aldric at his previous Aerodrome work post.
    // A bounded private QA ticket loads that exact original body before reviewing construction;
    // no production loading or replacement policy changes.
    var airport =
        ConstructionService.store(server).projects().stream()
            .filter(v -> v.blueprint().equals(RoyalAerodromeBlueprint.ID))
            .findFirst()
            .orElseThrow();
    var oldPost =
        BlueprintRegistry.get(airport.blueprint(), airport.blueprintVersion())
            .workPost(airport.origin(), airport.rotation());
    h.getLevel()
        .getChunkSource()
        .addRegionTicket(
            net.minecraft.server.level.TicketType.PORTAL,
            new net.minecraft.world.level.ChunkPos(oldPost),
            3,
            oldPost);
    h.getLevel().getChunk(oldPost);

    var p = ClaimRuntimeTests.vanillaPlayer(h.getLevel(), hall.owner(), hall.ownerName());
    CommerceService.verifyReceipt(p);
    var file = server.getWorldPath(LevelResource.ROOT).resolve("resident-name-proof.nbt");
    var proof = Files.exists(file) ? AtomicNbt.read(file) : new CompoundTag();
    if (Boolean.getBoolean("kingdom.testResumeResidentName")
        && (proof.getInt("phase") < 1 || proof.getInt("phase") > 2))
      throw new IllegalStateException(
          "Private name resume requires verified prepared/constructed proof");
    var point = FlightRuntimeTests.post(hall);
    p.teleportTo(h.getLevel(), point.x, point.y, point.z - 3, 0, 0);
    h.getLevel().getChunkSource().move(p);
    if (proof.hasUUID("project")) {
      var project = ConstructionService.store(server).get(proof.getUUID("project"));
      if (project.origin() != null) h.getLevel().getChunk(project.origin());
    }
    h.runAfterDelay(
        80,
        () ->
            guard(
                h,
                () -> {
                  var store = CommerceService.store(server);
                  var projects = ConstructionService.store(server);
                  if (proof.getInt("phase") == 0) {
                    var steward = (ReeveEntity) h.getLevel().getEntity(hall.npc());
                    h.assertTrue(steward != null, "Original verified Steward loads");
                    CommerceService.openResident(p, steward);
                    var review =
                        CommerceService.handle(
                            p, new CommerceRequest(1, 1, 0, BUILDING, 1, CommerceRequest.NONE));
                    h.assertTrue(
                        review.reviewing(), "Real Architect reviews the new Builder listing");
                    CommerceService.handle(p, new CommerceRequest(2, 2, 0, "", 1, review.review()));
                    var project = projects.reward("market:" + BUILDING).orElseThrow();
                    ConstructionService.select(p, project.id());
                    var originalNpc = project.npc();
                    String original = ConstructionService.resident(server, project);
                    var nameReview =
                        new ConstructionRequest(
                            3, 13, BlockPos.ZERO, 0, ConstructionRequest.NONE, NAME);
                    ConstructionService.request(p, nameReview);
                    var view = ConstructionService.view(p, 3, false, "");
                    h.assertTrue(
                        view.reviewAction() == 14
                            && view.resident().equals(NAME)
                            && ConstructionService.resident(server, project).equals(original),
                        "Real name review changes no saved name or resident identity");
                    var outsider =
                        ClaimRuntimeTests.vanillaPlayer(
                            h.getLevel(),
                            UUID.nameUUIDFromBytes(
                                "OfflinePlayer:NameVisitor"
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            "NameVisitor");
                    ConstructionService.select(outsider, project.id());
                    denied(
                        h,
                        () -> ConstructionService.request(outsider, nameReview),
                        "Another player cannot name this buyer's new NPC");
                    denied(
                        h,
                        () -> ConstructionService.confirm(outsider, view.review(), 14),
                        "A stolen confirmation nonce has no authority for another actor");
                    // The approved older command shares exactly the same naming policy/storage. It
                    // invalidates
                    // the pending reviewed spelling even though it changes no construction
                    // revision.
                    server
                        .getCommands()
                        .getDispatcher()
                        .execute(
                            "kingdom lodge name Cedric Oldstone", p.createCommandSourceStack());
                    denied(
                        h,
                        () -> ConstructionService.confirm(p, view.review(), 14),
                        "A separately changed saved name invalidates old review");
                    ConstructionService.request(p, nameReview);
                    var freshReview = ConstructionService.view(p, 4, false, "");
                    var nonce = freshReview.review();
                    long balance = store.balance(p.getUUID());
                    projects.save(hall, p.getUUID(), "PRIVATE_UNRELATED_NAME_REVIEW");
                    ConstructionService.confirm(p, nonce, 14);
                    denied(
                        h,
                        () -> ConstructionService.confirm(p, nonce, 14),
                        "Repeated name confirmation is refused");
                    h.assertTrue(
                        projects.get(project.id()).npc().equals(originalNpc)
                            && ConstructionService.resident(server, project).equals(NAME)
                            && store.balance(p.getUUID()) == balance,
                        "Only the new display name changes; permanent NPC, entitlement and purse"
                            + " remain");
                    ConstructionService.select(p, hall.id());
                    denied(
                        h,
                        () -> ConstructionService.request(p, nameReview),
                        "Existing completed Steward cannot be renamed");
                    var hutId = BuilderHutServiceProject(server);
                    ConstructionService.select(p, hutId);
                    h.assertTrue(
                        !ConstructionService.current(p).id().equals(hutId),
                        "Existing completed-Hut navigation selects an unused plan instead of"
                            + " exposing an Aldric naming action");
                    h.assertTrue(
                        WorldProgression.get(server)
                            .resident(hutId, "Aldric Stonehand")
                            .equals("Aldric Stonehand"),
                        "Starting Aldric's naming policy remains untouched");
                    ConstructionService.select(p, project.id());
                    proof.putUUID("project", project.id());
                    proof.putUUID("npc", originalNpc);
                    proof.putLong("balance", balance);
                    proof.putString("steward", steward.getName().getString());
                    proof.putInt("phase", 1);
                    save(
                        h,
                        p,
                        file,
                        proof,
                        "RESIDENT_NAME_PREPARED: actual purchase and exact reviewed name saved"
                            + " without consuming the blueprint or changing NPC identity");
                    return;
                  }
                  var project = projects.get(proof.getUUID("project"));
                  h.assertTrue(
                      ConstructionService.resident(server, project).equals(NAME)
                          && project.npc().equals(proof.getUUID("npc"))
                          && store.balance(p.getUUID()) == proof.getLong("balance"),
                      "Actual restart retains chosen name, same identity and exact purse");
                  if (proof.getInt("phase") == 2) {
                    h.runAfterDelay(
                        40,
                        () ->
                            guard(
                                h,
                                () -> {
                                  var npc = h.getLevel().getEntity(project.npc());
                                  h.assertTrue(
                                      npc instanceof ReeveEntity
                                          && npc.getName().getString().equals(NAME)
                                          && ConstructionService.registeredReeve((ReeveEntity) npc),
                                      "Third boot retains the one completed named service"
                                          + " resident");
                                  h.assertTrue(
                                      h.getLevel()
                                              .getEntities(
                                                  KingdomNpcs.RESIDENT.get(),
                                                  e -> e.buildingId().equals(project.id()))
                                              .size()
                                          == 1,
                                      "No duplicate resident for the renamed project");
                                  h.assertTrue(
                                      ConstructionService.structureIntact(h.getLevel(), project),
                                      "Actual progressively constructed workshop persists intact");
                                  h.assertTrue(
                                      h.getLevel()
                                          .getEntity(hall.npc())
                                          .getName()
                                          .getString()
                                          .equals(proof.getString("steward")),
                                      "Original Steward name and identity are preserved");
                                  release(h);
                                  h.succeed();
                                  com.mojang.logging.LogUtils.getLogger()
                                      .info(
                                          "RESIDENT_NAME_RESTART_VERIFIED: one named completed"
                                              + " workshop resident, original Steward, structure,"
                                              + " entitlement and purse survive third JVM");
                                }));
                    return;
                  }
                  ConstructionService.select(p, project.id());
                  var claim = ConstructionService.review(p, 1);
                  ConstructionService.confirm(p, claim.nonce(), 2);
                  var b = BlueprintRegistry.get(project.blueprint(), project.blueprintVersion());
                  var site = hall.origin().offset(0, 0, -32);
                  h.getLevel().getChunk(site);
                  for (var cell :
                      BlockPos.betweenClosed(
                          site.offset(-4, -1, -4), site.offset(b.width() + 4, -1, b.depth() + 4))) {
                    h.assertTrue(
                        h.getLevel().getBlockState(cell).isAir()
                            || h.getLevel().getBlockState(cell).is(Blocks.STONE),
                        "Private landing/build support never replaces a valuable block");
                    if (h.getLevel().getBlockState(cell).isAir())
                      h.getLevel().setBlock(cell, Blocks.STONE.defaultBlockState(), 18);
                  }
                  for (int cx = (site.getX() - 3) >> 4;
                      cx <= (site.getX() + b.width() + 3) >> 4;
                      cx++)
                    for (int cz = (site.getZ() - 3) >> 4;
                        cz <= (site.getZ() + b.depth() + 3) >> 4;
                        cz++) {
                      var pos = new BlockPos(cx * 16, site.getY(), cz * 16);
                      ClaimRuntimeTests.ftbClaim(p, pos);
                      ClaimRuntimeTests.opacClaim(h.getLevel(), p.getUUID(), pos);
                    }
                  p.teleportTo(h.getLevel(), site.getX() - 2, site.getY() + 1, site.getZ(), 0, 0);
                  h.getLevel().getChunkSource().move(p);
                  var preview = ConstructionService.preview(p, site, Rotation.NONE);
                  h.assertTrue(
                      preview.denial().isEmpty(),
                      "Current complete site/claims remain required: " + preview.denial());
                  ConstructionService.confirm(
                      p, ConstructionService.view(p, 5, false, "").review(), 4);
                  var begin = ConstructionService.review(p, 7);
                  ConstructionService.confirm(p, begin.nonce(), 8);
                  denied(
                      h,
                      () ->
                          ConstructionService.request(
                              p,
                              new ConstructionRequest(
                                  6, 13, BlockPos.ZERO, 0, ConstructionRequest.NONE, "Too Late")),
                      "Naming cannot change an already redeemed construction");
                  var hut = BuilderHutSavedData.get(server).hut().orElseThrow();
                  var builder = h.getLevel().getEntity(hut.npcId());
                  h.assertTrue(builder != null, "Original registered Aldric remains loaded");
                  var post = b.workPost(site, Rotation.NONE);
                  builder.moveTo(post.getX() + .5, post.getY(), post.getZ() + .5);
                  advance(h, p, project.id(), file, proof, 0);
                }));
  }

  static UUID BuilderHutServiceProject(net.minecraft.server.MinecraftServer server) {
    return ConstructionService.store(server).projects().stream()
        .filter(v -> v.blueprint().equals(BlueprintRegistry.HUT))
        .findFirst()
        .orElseThrow()
        .id();
  }

  static void advance(
      GameTestHelper h, ServerPlayer p, UUID id, Path path, CompoundTag proof, int step) {
    h.runAfterDelay(
        1,
        () ->
            guard(
                h,
                () -> {
                  ScoutRuntimeTests.ack(p);
                  var project = ConstructionService.store(p.server).get(id);
                  if (project.phase() == ConstructionStore.Phase.COMPLETE) {
                    h.assertTrue(
                        h.getLevel().getEntity(project.npc()).getName().getString().equals(NAME),
                        "Actual native NPC spawn uses the persisted chosen name");
                    proof.putInt("phase", 2);
                    save(
                        h,
                        p,
                        path,
                        proof,
                        "RESIDENT_NAME_CONSTRUCTED: authenticated plan redeemed once, actual"
                            + " progressive workshop completed and one chosen-name native resident"
                            + " issued");
                    return;
                  }
                  if (step > 2400)
                    throw new IllegalStateException(
                        "Named project stalled: " + ConstructionService.activity(id));
                  for (int n = 0; n < 2; n++)
                    if (ConstructionService.store(p.server).get(id).phase()
                        == ConstructionStore.Phase.BUILDING) ConstructionService.step(p.server, id);
                  advance(h, p, id, path, proof, step + 1);
                }));
  }

  static void save(GameTestHelper h, ServerPlayer p, Path path, CompoundTag proof, String marker)
      throws Exception {
    AtomicNbt.write(path, proof);
    p.server.getPlayerList().saveAll();
    p.server.saveEverything(false, true, true);
    release(h);
    h.succeed();
    com.mojang.logging.LogUtils.getLogger().info(marker);
  }

  static void release(GameTestHelper h) {
    var server = h.getLevel().getServer();
    for (var p : ConstructionService.store(server).projects())
      if (p.blueprint().equals(RoyalAerodromeBlueprint.ID)) {
        var post =
            BlueprintRegistry.get(p.blueprint(), p.blueprintVersion())
                .workPost(p.origin(), p.rotation());
        h.getLevel()
            .getChunkSource()
            .removeRegionTicket(
                net.minecraft.server.level.TicketType.PORTAL,
                new net.minecraft.world.level.ChunkPos(post),
                3,
                post);
      }
  }

  @FunctionalInterface
  interface Work {
    void run() throws Exception;
  }

  static void denied(GameTestHelper h, Work action, String reason) throws Exception {
    boolean denied = false;
    try {
      action.run();
    } catch (IllegalStateException expected) {
      denied = true;
    }
    h.assertTrue(denied, reason);
  }

  static void guard(GameTestHelper h, Work work) {
    try {
      work.run();
    } catch (Exception e) {
      release(h);
      com.mojang.logging.LogUtils.getLogger().error("Resident name native assertion failed", e);
      h.runAfterDelay(1, () -> h.fail(e.toString()));
    }
  }
}
