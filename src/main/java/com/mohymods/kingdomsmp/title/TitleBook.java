package com.mohymods.kingdomsmp.title;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Cosmetic preferences only. Saved selections cannot grant the underlying achievement. */
public final class TitleBook {
  private static final long MAX_REVISION = 1_000_000_000L;
  public static final String NONE = "kingdom_smp:none";

  @FunctionalInterface
  interface Writer {
    void write(Path file, CompoundTag data) throws IOException;
  }

  private final Path file;
  private final UUID economy, identity;
  private final Writer writer;
  private Map<UUID, String> selections;
  private long revision;
  private boolean paused;

  private TitleBook(
      Path file,
      UUID economy,
      UUID identity,
      Map<UUID, String> selections,
      long revision,
      Writer writer) {
    this.file = file;
    this.economy = economy;
    this.identity = identity;
    this.selections = Map.copyOf(selections);
    this.revision = revision;
    this.writer = writer;
  }

  public long revision() {
    return revision;
  }

  public String selected(UUID player) {
    return selections.getOrDefault(player, NONE);
  }

  public boolean paused() {
    return paused;
  }

  public static TitleBook open(Path directory, UUID economy) throws IOException {
    return open(directory, economy, AtomicNbt::write);
  }

  static TitleBook open(Path directory, UUID economy, Writer writer) throws IOException {
    Objects.requireNonNull(economy);
    Objects.requireNonNull(writer);
    var file = directory.resolve("kingdomsmp_titles.dat");
    var marker = directory.resolve("kingdomsmp_titles.created");
    boolean present = Files.exists(file, LinkOption.NOFOLLOW_LINKS),
        marked = Files.exists(marker, LinkOption.NOFOLLOW_LINKS);
    if (present != marked)
      throw new IOException("Title preferences need matching identity/data recovery");
    if (!present) {
      var identity = UUID.randomUUID();
      var seal = new CompoundTag();
      seal.putUUID("economy", economy);
      seal.putUUID("identity", identity);
      AtomicNbt.write(marker, seal);
      var book = new TitleBook(file, economy, identity, Map.of(), 0, writer);
      book.persist(book.selections, 0);
      return book;
    }
    var seal = AtomicNbt.read(marker);
    var d = AtomicNbt.read(file);
    if (!seal.getAllKeys().equals(Set.of("economy", "identity"))
        || !seal.hasUUID("economy")
        || !seal.hasUUID("identity")
        || !economy.equals(seal.getUUID("economy"))
        || !d.getAllKeys().equals(Set.of("schema", "economy", "identity", "revision", "players"))
        || !d.contains("schema", Tag.TAG_INT)
        || d.getInt("schema") != 1
        || !d.hasUUID("economy")
        || !economy.equals(d.getUUID("economy"))
        || !d.hasUUID("identity")
        || !seal.getUUID("identity").equals(d.getUUID("identity"))
        || !d.contains("revision", Tag.TAG_LONG)
        || d.getLong("revision") < 0
        || d.getLong("revision") > MAX_REVISION
        || !(d.get("players") instanceof ListTag rows)
        || rows.size() > 128
        || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)
      throw new IOException("Malformed or foreign title preferences");
    var selected = new LinkedHashMap<UUID, String>();
    try {
      for (var tag : rows) {
        var row = (CompoundTag) tag;
        if (!row.getAllKeys().equals(Set.of("player", "title"))
            || !row.hasUUID("player")
            || !row.contains("title", Tag.TAG_STRING))
          throw new IllegalArgumentException("Malformed title selection");
        var id = row.getString("title");
        validate(id);
        if (id.equals(NONE) || selected.putIfAbsent(row.getUUID("player"), id) != null)
          throw new IllegalArgumentException("Duplicate or empty title selection");
      }
    } catch (RuntimeException e) {
      throw new IOException("Malformed title selections", e);
    }
    return new TitleBook(
        file, economy, seal.getUUID("identity"), selected, d.getLong("revision"), writer);
  }

  public boolean select(UUID actor, String title, Set<String> earned) throws IOException {
    Objects.requireNonNull(actor);
    validate(title);
    if (paused)
      throw new IllegalStateException("Title preferences are paused for matching-data recovery");
    if (!title.equals(NONE) && !earned.contains(title))
      throw new IllegalStateException("This display title has not been earned");
    if (title.equals(selected(actor))) return false;
    var next = new LinkedHashMap<>(selections);
    if (title.equals(NONE)) next.remove(actor);
    else next.put(actor, title);
    if (next.size() > 128 || revision == MAX_REVISION)
      throw new IllegalStateException("Title preference capacity reached");
    try {
      persist(next, revision + 1);
    } catch (IOException | RuntimeException e) {
      paused = true;
      throw e;
    }
    selections = Map.copyOf(next);
    revision++;
    return true;
  }

  static void validate(String id) {
    var key = id == null ? null : ResourceLocation.tryParse(id);
    if (key == null
        || !key.getNamespace().equals("kingdom_smp")
        || id.length() > 96
        || !id.equals(NONE)
            && !(key.getPath().startsWith("founder/")
                || key.getPath().startsWith("profession/")
                || key.getPath().startsWith("achievement/")))
      throw new IllegalArgumentException("Invalid earned title identity");
  }

  private void persist(Map<UUID, String> selected, long version) throws IOException {
    var d = new CompoundTag();
    d.putInt("schema", 1);
    d.putUUID("economy", economy);
    d.putUUID("identity", identity);
    d.putLong("revision", version);
    var rows = new ListTag();
    selected.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e -> {
              var row = new CompoundTag();
              row.putUUID("player", e.getKey());
              row.putString("title", e.getValue());
              rows.add(row);
            });
    d.put("players", rows);
    writer.write(file, d);
    if (!d.equals(AtomicNbt.read(file)))
      throw new IOException("Title checkpoint verification failed");
  }
}
