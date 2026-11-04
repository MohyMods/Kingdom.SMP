package com.mohymods.kingdomsmp.persistence;

import com.mohymods.kingdomsmp.kingdom.TierOneProgress;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import net.minecraft.nbt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Canonical append-only accepted history plus one durable prepared operation. */
public final class ContributionLedger {
    public static final int MAX_OPERATIONS = 3712;
    public record Receipt(UUID store, long sequence) {
        public Receipt { if (sequence < 0 || (store == null && sequence != 0)) throw new IllegalArgumentException("Invalid receipt"); }
        public static Receipt empty() { return new Receipt(null, 0); }
    }
    public record Donation(long sequence, UUID operation, UUID player, String name, Resource resource, int amount, long time) {
        public Donation {
            Objects.requireNonNull(operation); Objects.requireNonNull(player); Objects.requireNonNull(resource); Objects.requireNonNull(name);
            if (sequence <= 0 || amount <= 0 || amount > resource.required() || time < 0 || name.isBlank() || name.length() > 48
                    || name.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid donation");
        }
    }
    public record Pending(Donation donation, long previousReceipt) {
        public Pending { Objects.requireNonNull(donation); if (previousReceipt < 0 || previousReceipt >= donation.sequence()) throw new IllegalArgumentException("Invalid pending receipt"); }
    }
    @FunctionalInterface interface Writer { void save(Path file, CompoundTag tag) throws IOException; }
    private final Path file;
    private final Writer writer;
    private final UUID id;
    private final TierOneProgress base;
    private List<Donation> history;
    private TierOneProgress progress;
    private Pending pending;
    private boolean quarantined;

    public static ContributionLedger open(Path directory, TierOneProgress legacy) throws IOException {
        return open(directory, legacy, AtomicNbt::write);
    }
    static ContributionLedger open(Path directory, TierOneProgress legacy, Writer writer) throws IOException {
        Path file = directory.resolve("kingdomsmp_contributions.dat");
        Path marker = directory.resolve("kingdomsmp_contributions.created");
        boolean exists = Files.exists(file, LinkOption.NOFOLLOW_LINKS), initialized = Files.exists(marker, LinkOption.NOFOLLOW_LINKS);
        if (exists != initialized) throw new IOException("Contribution ledger/initialization marker mismatch; restore a matching world backup");
        if (!exists) {
            UUID id = UUID.randomUUID();
            var ledger = new ContributionLedger(file, writer, id, legacy, List.of(), null);
            CompoundTag mark = new CompoundTag(); mark.putUUID("store", id);
            AtomicNbt.write(marker, mark);
            writer.save(file, ledger.encode(List.of(), null));
            return ledger;
        }
        CompoundTag tag = AtomicNbt.read(file);
        require(tag, "schema", Tag.TAG_INT); require(tag, "base", Tag.TAG_COMPOUND);
        if (tag.getInt("schema") != 1 || !tag.hasUUID("store")) throw new IOException("Unsupported or corrupt contribution ledger");
        UUID id = tag.getUUID("store");
        CompoundTag mark = AtomicNbt.read(marker);
        if (!mark.hasUUID("store") || !id.equals(mark.getUUID("store"))) throw new IOException("Contribution store identity mismatch");
        require(tag, "history", Tag.TAG_LIST);
        ListTag list = tag.getList("history", Tag.TAG_COMPOUND);
        if (list.size() != ((ListTag) tag.get("history")).size() || list.size() > MAX_OPERATIONS) throw new IOException("Invalid contribution history");
        List<Donation> history = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) history.add(decodeDonation(list.getCompound(index)));
        Pending pending = null;
        if (tag.contains("pending")) {
            require(tag, "pending", Tag.TAG_COMPOUND);
            CompoundTag p = tag.getCompound("pending"); require(p, "previousReceipt", Tag.TAG_LONG);
            pending = new Pending(decodeDonation(p), p.getLong("previousReceipt"));
        }
        return new ContributionLedger(file, writer, id, KingdomSavedData.loadTierOne(tag.getCompound("base")), history, pending);
    }
    private ContributionLedger(Path file, Writer writer, UUID id, TierOneProgress base, List<Donation> history, Pending pending) {
        this.file = file; this.writer = writer; this.id = id; this.base = base;
        this.history = List.copyOf(history); this.progress = base;
        Set<UUID> operations = new HashSet<>();
        for (Donation donation : history) {
            if (!operations.add(donation.operation()) || donation.sequence() != progress.revision() + 1) throw new IllegalArgumentException("Duplicate or unordered donation");
            var result = progress.contribute(donation.player(), donation.resource(), donation.amount());
            if (result.acceptedCount() != donation.amount()) throw new IllegalArgumentException("Unbacked or excessive credit");
            progress = result.progress();
        }
        this.pending = pending;
        if (pending != null) {
            var d = pending.donation();
            if (d.sequence() != progress.revision() + 1 || operations.contains(d.operation()) || pending.previousReceipt() != expectedReceipt(d.player())
                    || progress.contribute(d.player(), d.resource(), d.amount()).acceptedCount() != d.amount()) throw new IllegalArgumentException("Invalid pending donation");
        }
    }
    public UUID id() { return id; }
    public TierOneProgress progress() { return progress; }
    public List<Donation> history() { return history; }
    public Optional<Pending> pending() { return Optional.ofNullable(pending); }
    public boolean quarantined() { return quarantined; }
    public void quarantine() { quarantined = true; }
    public long expectedReceipt(UUID player) {
        for (int index = history.size() - 1; index >= 0; index--) if (history.get(index).player().equals(player)) return history.get(index).sequence();
        return 0;
    }
    public Pending prepare(UUID operation, UUID player, String name, Resource resource, int amount) throws IOException {
        if (quarantined || pending != null || history.size() >= MAX_OPERATIONS) throw new IllegalStateException("Contributions require recovery");
        if (history.stream().anyMatch(d -> d.operation().equals(operation))) throw new IllegalArgumentException("Replayed donation");
        if (amount <= 0 || progress.contribute(player, resource, amount).acceptedCount() != amount) throw new IllegalArgumentException("Invalid accepted amount");
        var next = new Pending(new Donation(Math.incrementExact(progress.revision()), operation, player, name, resource, amount,
                System.currentTimeMillis()), expectedReceipt(player));
        persist(history, next);
        pending = next;
        return next;
    }
    /** True means credit committed. False means pre-removal preparation cancelled. */
    public boolean recover(Receipt receipt) throws IOException {
        if (quarantined) throw new IllegalStateException("Ledger quarantined; restart after repairing storage");
        if (pending == null) return false;
        Donation d = pending.donation();
        boolean sameStore = id.equals(receipt.store());
        if (sameStore && receipt.sequence() == d.sequence()) {
            List<Donation> next = new ArrayList<>(history); next.add(d);
            TierOneProgress nextProgress = progress.contribute(d.player(), d.resource(), d.amount()).progress();
            persist(next, null);
            history = List.copyOf(next); progress = nextProgress; pending = null;
            return true;
        }
        if (receipt.sequence() == pending.previousReceipt() && (sameStore || receipt.sequence() == 0 && receipt.store() == null)) {
            persist(history, null); pending = null; return false;
        }
        throw new IOException("Player receipt does not match pending donation; restore matching data");
    }
    private void persist(List<Donation> history, Pending pending) throws IOException {
        try { writer.save(file, encode(history, pending)); }
        catch (IOException | RuntimeException error) { quarantined = true; throw error; }
    }
    private CompoundTag encode(List<Donation> history, Pending pending) {
        CompoundTag tag = new CompoundTag(); tag.putInt("schema", 1); tag.putUUID("store", id);
        tag.put("base", KingdomSavedData.saveTierOne(base));
        ListTag list = new ListTag(); history.forEach(d -> list.add(encodeDonation(d))); tag.put("history", list);
        if (pending != null) { CompoundTag p = encodeDonation(pending.donation()); p.putLong("previousReceipt", pending.previousReceipt()); tag.put("pending", p); }
        return tag;
    }
    private static CompoundTag encodeDonation(Donation d) {
        CompoundTag tag = new CompoundTag(); tag.putLong("sequence", d.sequence()); tag.putUUID("operation", d.operation()); tag.putUUID("player", d.player());
        tag.putString("name", d.name()); tag.putString("resource", d.resource().name()); tag.putInt("amount", d.amount()); tag.putLong("time", d.time()); return tag;
    }
    private static Donation decodeDonation(CompoundTag tag) {
        for (String k : List.of("sequence", "time")) require(tag, k, Tag.TAG_LONG);
        for (String k : List.of("name", "resource")) require(tag, k, Tag.TAG_STRING);
        require(tag, "amount", Tag.TAG_INT);
        if (!tag.hasUUID("operation") || !tag.hasUUID("player")) throw new IllegalArgumentException("Invalid donation identity");
        return new Donation(tag.getLong("sequence"), tag.getUUID("operation"), tag.getUUID("player"), tag.getString("name"),
                Resource.valueOf(tag.getString("resource")), tag.getInt("amount"), tag.getLong("time"));
    }
    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or invalid contribution field: " + key);
    }
}
