package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.title.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

/** Normal server handlers and vanilla name hooks; no synthetic earned profession progress. */
@GameTestHolder("kingdom_smp_title_tests")
@PrefixGameTestTemplate(false)
public final class TitleRuntimeTests {
  @GameTest(template = "empty", timeoutTicks = 200)
  public static void selfOnlyCosmeticChoicePreservesCanonicalRecordsAndSurvivesRestart(
      GameTestHelper h) throws Exception {
    var server = h.getLevel().getServer();
    var store = CommerceService.store(server);
    var purchase = store.purchase("kingdom_smp:royal_bank").orElseThrow();
    var p = ClaimRuntimeTests.vanillaPlayer(h.getLevel(), purchase.buyer(), purchase.buyerName());
    CommerceService.verifyReceipt(p);
    var file = server.getWorldPath(LevelResource.ROOT).resolve("title-runtime-proof.nbt");
    var proof = Files.exists(file) ? AtomicNbt.read(file) : new CompoundTag();
    String title = "kingdom_smp:founder/royal_bank";
    String label = CommerceService.catalog(server).building(purchase.building()).founder();
    h.assertTrue(
        !p.hasPermissions(2) && !p.getAbilities().mayfly,
        "Ordinary title owner has no operator or flight exemption");
    if (proof.getBoolean("prepared")) {
      h.assertTrue(
          TitleService.book(server).selected(p.getUUID()).equals(title),
          "Actual second JVM retains the selected title");
      h.assertTrue(
          p.getDisplayName().getString().contains(label),
          "Ordinary login restores selected chat display without explicit refresh");
      h.assertTrue(
          p.getTabListDisplayName() != null
              && p.getTabListDisplayName().getString().contains(label),
          "Ordinary login restores selected player-list name");
      h.assertTrue(
          store.balance(p.getUUID()) == proof.getLong("balance")
              && store.revision() == proof.getLong("economyRevision")
              && store.aircraft().size() == proof.getInt("frames"),
          "Cosmetic restart preserves exact financial and registered aircraft authority");
      h.assertTrue(
          TitleService.entries(p).stream()
              .anyMatch(e -> e.id().equals(title) && e.state().equals("TITLE_SELECTED")),
          "Royal selector restores the selected earned row");
      h.succeed();
      com.mojang.logging.LogUtils.getLogger()
          .info(
              "TITLE_RESTART_VERIFIED: selected chat/tab name, exact purse, buyer/aircraft records"
                  + " and no gameplay privilege survive actual JVM restart");
      return;
    }
    long balance = store.balance(p.getUUID()), revision = store.revision();
    var positions = p.position();
    // Cosmetics are usable away from a resident, without weakening any real trade counter.
    p.teleportTo(h.getLevel(), -100, 80, -100, 0, 0);
    h.getLevel().getChunkSource().move(p);
    boolean counterDenied = false;
    try {
      CommerceService.requireResidentCounter(p);
    } catch (IllegalStateException expected) {
      counterDenied = true;
    }
    h.assertTrue(
        counterDenied, "Control: ordinary trading still requires its real resident counter");
    var view = CommerceService.handle(p, new CommerceRequest(1, 0, 9, "", 1, CommerceRequest.NONE));
    h.assertTrue(
        view.entries().stream().anyMatch(e -> e.id().equals(title)),
        "Actual server selector projects the original canonical buyer");
    h.assertTrue(
        p.getTabListDisplayName() == null,
        "Untitled player retains vanilla null tab-list override");
    boolean refused = false;
    try {
      CommerceService.handle(
          p,
          new CommerceRequest(
              2, 1, 9, "kingdom_smp:profession/farmer/master", 1, CommerceRequest.NONE));
    } catch (IllegalStateException expected) {
      refused = true;
    }
    h.assertTrue(refused, "A forged Master title without canonical quests is refused");
    review(h, p, title);
    var nonce = TitleService.quote(p).orElseThrow().nonce();
    confirm(h, p, nonce);
    h.assertTrue(
        p.getDisplayName().getString().contains(label)
            && p.getTabListDisplayName().getString().contains(label),
        "Actual vanilla name hooks use only the earned server label");
    refused = false;
    try {
      CommerceService.handle(p, new CommerceRequest(4, 2, 9, "", 1, nonce));
    } catch (IllegalStateException expected) {
      refused = true;
    }
    h.assertTrue(refused, "Stale cosmetic confirmation cannot repeat a change");
    var outsider =
        ClaimRuntimeTests.vanillaPlayer(
            h.getLevel(),
            UUID.nameUUIDFromBytes(
                "OfflinePlayer:TitleVisitor".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            "TitleVisitor");
    h.assertTrue(
        TitleService.book(server).selected(outsider.getUUID()).equals(TitleBook.NONE)
            && TitleService.earned(outsider).isEmpty(),
        "One player's selection or founder achievement grants nothing to another player");
    review(h, p, TitleBook.NONE);
    confirm(h, p, TitleService.quote(p).orElseThrow().nonce());
    h.assertTrue(
        p.getTabListDisplayName() == null && !p.getDisplayName().getString().contains(label),
        "Clear restores ordinary chat and tab formatting without erasing earned records");
    review(h, p, title);
    confirm(h, p, TitleService.quote(p).orElseThrow().nonce());
    h.assertTrue(
        balance == store.balance(p.getUUID())
            && revision == store.revision()
            && !store.quarantined(),
        "No cosmetic operation debits coins, changes founder records or quarantines the economy");
    p.teleportTo(h.getLevel(), positions.x, positions.y, positions.z, 0, 0);
    h.getLevel().getChunkSource().move(p);
    proof.putBoolean("prepared", true);
    proof.putLong("balance", balance);
    proof.putLong("economyRevision", revision);
    proof.putInt("frames", store.aircraft().size());
    AtomicNbt.write(file, proof);
    server.getPlayerList().saveAll();
    server.saveEverything(false, true, true);
    h.succeed();
    com.mojang.logging.LogUtils.getLogger()
        .info(
            "TITLE_SELECTION_PREPARED: actual Royal selection/clear, forged Master refusal,"
                + " self-only name hooks and unchanged financial records saved before restart");
  }

  static void review(GameTestHelper h, ServerPlayer p, String title) {
    var v = CommerceService.handle(p, new CommerceRequest(5, 1, 9, title, 1, CommerceRequest.NONE));
    h.assertTrue(v.reviewing(), "Actual title review succeeds: " + v.notice());
  }

  static void confirm(GameTestHelper h, ServerPlayer p, UUID nonce) {
    var v = CommerceService.handle(p, new CommerceRequest(6, 2, 9, "", 1, nonce));
    h.assertTrue(!v.reviewing() && !v.notice().isBlank(), "Actual cosmetic confirmation succeeds");
  }
}
