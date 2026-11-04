package com.mohymods.kingdomsmp.title;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.profession.ProfessionService;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Self-only cosmetic selection. No scoreboard teams, permission mutation or client-supplied label.
 */
public final class TitleService {
  public record Quote(
      UUID nonce, long economy, long preferences, String selected, long issued, String text) {}

  private static final Map<MinecraftServer, TitleBook> BOOKS = new WeakHashMap<>();
  private static final Map<MinecraftServer, String> ERRORS = new WeakHashMap<>();
  private static final Map<ServerPlayer, Quote> QUOTES = new WeakHashMap<>();

  private TitleService() {}

  public static void register(IEventBus bus) {
    bus.addListener(TitleService::name);
    bus.addListener(TitleService::tab);
    bus.addListener(TitleService::login);
  }

  private static void login(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer p) {
      // Vanilla builds an early name cache before its online-player map contains this actor.
      // Refresh only the cosmetic caches after normal authenticated login has completed.
      p.refreshDisplayName();
      p.refreshTabListName();
    }
  }

  public static void initialize(MinecraftServer server) {
    BOOKS.remove(server);
    ERRORS.remove(server);
    QUOTES.keySet().removeIf(p -> p.server == server);
    if (Files.exists(directory(server).resolve("kingdomsmp_titles.dat"), LinkOption.NOFOLLOW_LINKS)
        || Files.exists(
            directory(server).resolve("kingdomsmp_titles.created"), LinkOption.NOFOLLOW_LINKS))
      try {
        book(server);
      } catch (IllegalStateException e) {
        com.mojang.logging.LogUtils.getLogger()
            .error("Cosmetic title preferences paused; other gameplay remains available", e);
      }
  }

  private static Path directory(MinecraftServer server) {
    return server.getWorldPath(LevelResource.ROOT).resolve("data");
  }

  public static TitleBook book(MinecraftServer server) {
    if (!server.isSameThread())
      throw new IllegalStateException("Title selection requires the server thread");
    if (ERRORS.containsKey(server)) throw new IllegalStateException(ERRORS.get(server));
    return BOOKS.computeIfAbsent(
        server,
        s -> {
          try {
            return TitleBook.open(directory(s), CommerceService.store(s).id());
          } catch (IOException e) {
            ERRORS.put(
                s,
                "Title preferences require matching-data recovery; other services remain"
                    + " available.");
            throw new IllegalStateException(ERRORS.get(s), e);
          }
        });
  }

  public static long revision(MinecraftServer server) {
    var b = BOOKS.get(server);
    return b == null ? 0 : b.revision();
  }

  public static List<EarnedTitles.Earned> earned(ServerPlayer p) {
    var store = CommerceService.store(p.server);
    return EarnedTitles.project(
        p.getUUID(),
        store.purchases(),
        CommerceService.catalog(p.server).buildings(),
        store.professions(p.getUUID()),
        ProfessionService.quests());
  }

  private static void require(ServerPlayer p) {
    if (!p.server.isSameThread()
        || p.server.getPlayerList().getPlayer(p.getUUID()) != p
        || !p.isAlive()
        || p.isSpectator()
        || p instanceof net.neoforged.neoforge.common.util.FakePlayer)
      throw new IllegalStateException("Choose a title as your living online player");
  }

  public static void close(ServerPlayer p) {
    QUOTES.remove(p);
  }

  public static Optional<Quote> quote(ServerPlayer p) {
    var q = QUOTES.get(p);
    if (q != null
        && (q.economy() != CommerceService.store(p.server).revision()
            || q.preferences() != book(p.server).revision()
            || System.nanoTime() - q.issued() > 30_000_000_000L)) {
      close(p);
      q = null;
    }
    return Optional.ofNullable(q);
  }

  public static void review(ServerPlayer p, String selected) {
    require(p);
    close(p);
    var b = book(p.server);
    String label =
        selected.equals(TitleBook.NONE)
            ? "No display title"
            : earned(p).stream()
                .filter(t -> t.id().equals(selected))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("This title has not been earned"))
                .label();
    QUOTES.put(
        p,
        new Quote(
            UUID.randomUUID(),
            CommerceService.store(p.server).revision(),
            b.revision(),
            selected,
            System.nanoTime(),
            "Display choice: "
                + label
                + ". This changes only your visible name in chat and the player list. It costs no"
                + " coins, grants no permissions, and does not activate professions or their"
                + " bonuses. Your underlying founder and profession records are preserved."));
  }

  public static String confirm(ServerPlayer p, UUID nonce) {
    require(p);
    var q = QUOTES.remove(p);
    var b = book(p.server);
    if (q == null
        || !q.nonce().equals(nonce)
        || q.economy() != CommerceService.store(p.server).revision()
        || q.preferences() != b.revision()
        || System.nanoTime() - q.issued() > 30_000_000_000L)
      throw new IllegalStateException(
          "Title review expired or earned records changed; review again");
    try {
      b.select(
          p.getUUID(),
          q.selected(),
          earned(p).stream()
              .map(EarnedTitles.Earned::id)
              .collect(java.util.stream.Collectors.toSet()));
    } catch (IOException e) {
      ERRORS.put(
          p.server,
          "Title preferences require matching-data recovery; other services remain available.");
      throw new IllegalStateException(ERRORS.get(p.server), e);
    }
    p.refreshDisplayName();
    p.refreshTabListName();
    return q.selected().equals(TitleBook.NONE)
        ? "Display title cleared. Your earned recognition is retained."
        : "Earned display title selected. Gameplay permissions and profession bonuses are"
            + " unchanged.";
  }

  public static List<CommerceView.Entry> entries(ServerPlayer p) {
    require(p);
    var selected = book(p.server).selected(p.getUUID());
    var rows = new ArrayList<CommerceView.Entry>();
    rows.add(
        new CommerceView.Entry(
            TitleBook.NONE,
            "No display title",
            "Show your ordinary player name. Earned founder recognition and inactive profession"
                + " history remain saved.",
            0,
            selected.equals(TitleBook.NONE) ? "TITLE_SELECTED" : "TITLE_AVAILABLE"));
    for (var t : earned(p))
      rows.add(
          new CommerceView.Entry(
              t.id(),
              t.label(),
              t.detail(),
              0,
              selected.equals(t.id()) ? "TITLE_SELECTED" : "TITLE_AVAILABLE"));
    return List.copyOf(rows);
  }

  private static Component prefix(ServerPlayer p, Component base) {
    if (!p.server.isSameThread()
        || p.server.getPlayerList().getPlayer(p.getUUID()) != p
        || ERRORS.containsKey(p.server)) return base;
    var book = BOOKS.get(p.server);
    if (book == null || book.paused()) return base;
    try {
      String choice = book.selected(p.getUUID());
      var title = earned(p).stream().filter(t -> t.id().equals(choice)).findFirst();
      return title.isEmpty()
          ? base
          : Component.literal("[" + title.orElseThrow().label() + "] ")
              .withStyle(ChatFormatting.GOLD)
              .append(base.copy());
    } catch (RuntimeException e) {
      return base;
    }
  }

  private static void name(PlayerEvent.NameFormat e) {
    if (e.getEntity() instanceof ServerPlayer p) e.setDisplayname(prefix(p, e.getDisplayname()));
  }

  private static void tab(PlayerEvent.TabListNameFormat e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      var existing = e.getDisplayName();
      var base =
          existing == null
              ? net.minecraft.world.scores.PlayerTeam.formatNameForTeam(p.getTeam(), p.getName())
              : existing;
      var formatted = prefix(p, base);
      if (formatted != base) e.setDisplayName(formatted);
    }
  }

  public static com.mojang.brigadier.builder.LiteralArgumentBuilder<
          net.minecraft.commands.CommandSourceStack>
      commands() {
    return Commands.literal("title")
        .executes(
            c -> {
              var p = c.getSource().getPlayerOrException();
              require(p);
              CommerceNetworking.open(p, 9);
              return 1;
            });
  }
}
