package com.mohymods.kingdomsmp.contribution;

import com.mohymods.kingdomsmp.kingdom.*;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.persistence.ContributionLedger.Receipt;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Only entry point for live donations. Called exclusively on the server thread. */
public final class ContributionService {
    private static final String RECEIPT = "KingdomContributionReceipt";
    private static final Map<MinecraftServer, ContributionLedger> LEDGERS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Offer> OFFERS = new WeakHashMap<>();
    public record Offer(UUID id, long revision, Resource resource, int amount, long issued, List<ItemStack> inventory) { }
    private ContributionService() { }
    public static void catalogueReloaded(MinecraftServer server){OFFERS.keySet().removeIf(player->player.server==server);}
    private static void requireMilestone(MinecraftServer server){String denial=com.mohymods.kingdomsmp.milestone.MilestoneService.consumptionDenial(server);if(!denial.isEmpty())throw new IllegalStateException(denial);}
    public static void initialize(MinecraftServer server) { LEDGERS.remove(server); OFFERS.clear(); ledger(server); }
    public static ContributionLedger ledger(MinecraftServer server) {
        var existing = LEDGERS.get(server);
        if (existing != null) return existing;
        try {
            var ledger = ContributionLedger.open(server.getWorldPath(LevelResource.ROOT).resolve("data"), KingdomSavedData.get(server).tierOneProgress());
            if (ledger.pending().isPresent()) {
                var pending = ledger.pending().orElseThrow();
                Path file = playerFile(server, pending.donation().player());
                ledger.recover(Files.exists(file) ? receipt(AtomicNbt.read(file)) : Receipt.empty());
            }
            LEDGERS.put(server, ledger); return ledger;
        } catch (IOException error) { throw new IllegalStateException("Contribution data requires recovery; restore a complete matching world backup", error); }
    }
    public static KingdomState kingdom(MinecraftServer server) {
        KingdomState baseline = KingdomSavedData.get(server).snapshot();
        var progress = ledger(server).progress();
        return new KingdomState(baseline.schemaVersion(), Math.addExact(progress.revision(),com.mohymods.kingdomsmp.building.ConstructionService.royal(server)?com.mohymods.kingdomsmp.progression.TierTwoService.ledger(server).revision():0), Math.max(Math.max(baseline.attainedTier(), progress.complete() ? 2 : 1),com.mohymods.kingdomsmp.progression.TierTwoService.attainedTier(server)));
    }
    public static int eligible(ServerPlayer player, Resource resource) {
        int amount = 0;
        for (int slot = 0; slot < 36; slot++) { var stack = player.getInventory().getItem(slot); if (matches(stack, resource)) amount += stack.getCount(); }
        return amount;
    }
    public static boolean matches(ItemStack stack, Resource resource) {
        return switch (resource) { case LOGS -> stack.is(ItemTags.LOGS); case COBBLESTONE -> stack.is(Items.COBBLESTONE);
            case WHEAT -> stack.is(Items.WHEAT); case IRON_INGOTS -> stack.is(Items.IRON_INGOT); };
    }
    public static Offer quote(ServerPlayer player, Resource resource, int requested) {
        requirePlayer(player);
        requireMilestone(player.server);
        var ledger = ledger(player.server);
        if (ledger.quarantined() || ledger.pending().isPresent()) throw new IllegalStateException("Contributions are paused for recovery.");
        if (requested < 1 || requested > resource.required()) throw new IllegalArgumentException("Invalid donation quantity.");
        verifyLoadedReceipt(player, ledger);
        int amount = Math.min(requested, Math.min(eligible(player, resource), resource.required() - ledger.progress().total(resource)));
        if (amount == 0) throw new IllegalStateException("No eligible items remain, or this resource target is already full.");
        var offer = new Offer(UUID.randomUUID(), ledger.progress().revision(), resource, amount, System.nanoTime(), inventory(player));
        OFFERS.put(player, offer); return offer;
    }
    public static String confirm(ServerPlayer player, UUID id) {
        requirePlayer(player);
        requireMilestone(player.server);
        Offer offer = OFFERS.remove(player);
        if (offer == null || !offer.id().equals(id) || System.nanoTime() - offer.issued() >= 30_000_000_000L) {
            throw new IllegalStateException("Donation review expired. Review the amount again.");
        }
        var ledger = ledger(player.server);
        if (ledger.quarantined() || ledger.pending().isPresent()) throw new IllegalStateException("Contributions are paused for recovery.");
        verifyLoadedReceipt(player, ledger);
        if (offer.revision() != ledger.progress().revision() || !sameInventory(player, offer.inventory())) {
            throw new IllegalStateException("Inventory or kingdom progress changed. Review the new amount before donating.");
        }
        boolean completeBefore = ledger.progress().complete();
        CompoundTag oldPersistent = player.getPersistentData().copy();
        boolean checkpointStarted = false;
        try {
            // Persist the current pre-donation inventory too, including a new player’s first save.
            checkpoint(player);
            var pending = ledger.prepare(offer.id(), player.getUUID(), player.getGameProfile().getName(), offer.resource(), offer.amount());
            int remaining = offer.amount();
            for (int slot = 0; slot < 36 && remaining > 0; slot++) {
                var stack = player.getInventory().getItem(slot);
                if (matches(stack, offer.resource())) { int count = Math.min(remaining, stack.getCount()); stack.shrink(count); remaining -= count; }
            }
            if (remaining != 0) throw new IllegalStateException("Inventory changed during donation.");
            putReceipt(player, new Receipt(ledger.id(), pending.donation().sequence()));
            checkpointStarted = true;
            checkpoint(player);
            if (!ledger.recover(receipt(AtomicNbt.read(playerFile(player.server, player.getUUID()))))) throw new IOException("Missing committed inventory receipt");
            player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
            LogUtils.getLogger().info("Donation committed operation={} player={} resource={} count={} revision={}",
                    offer.id(), player.getUUID(), offer.resource(), offer.amount(), ledger.progress().revision());
            if (!completeBefore && ledger.progress().complete()) {
                UUID winner = ledger.progress().winner().orElseThrow();
                String name = name(ledger, winner);
                Component announcement = Component.literal("The First Hearth is complete! Kingdom Tier 2 — Roads of Commerce. Highest contributor: " + name + ". "+com.mohymods.kingdomsmp.building.ConstructionService.rewardTitle(player.server)+" construction privilege earned.");
                player.server.getPlayerList().broadcastSystemMessage(announcement, false);
                LogUtils.getLogger().info("First Hearth completed: winner={}; Tier 2; {} entitlement", winner, com.mohymods.kingdomsmp.building.ConstructionService.rewardTitle(player.server));
            }
            return "Donated " + offer.amount() + " " + label(offer.resource()) + ". Your contribution is saved.";
        } catch (IOException | RuntimeException failure) {
            if (!checkpointStarted) {
                for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, offer.inventory().get(slot).copy());
                for (String key : new HashSet<>(player.getPersistentData().getAllKeys())) player.getPersistentData().remove(key);
                player.getPersistentData().merge(oldPersistent);
            }
            ledger.quarantine();
            LogUtils.getLogger().error("Donation interrupted; no further donations until restart/reconciliation: {}", offer.id(), failure);
            player.connection.disconnect(Component.literal("Donation storage needs recovery. Contact the administrator before retrying."));
            throw new IllegalStateException("Donation paused for safe recovery.", failure);
        }
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            var ledger = ledger(player.server); verifyLoadedReceipt(player, ledger);
            if (ledger.progress().complete()) player.sendSystemMessage(Component.literal("First Hearth complete (reached Tier 2). Current Kingdom Tier "+kingdom(player.server).attainedTier()+". Highest contributor: "
                    + name(ledger, ledger.progress().winner().orElseThrow()) + ". "+com.mohymods.kingdomsmp.building.ConstructionService.rewardTitle(player.server)+" privilege earned."));
        }
        catch (RuntimeException error) {
            LogUtils.getLogger().error("Player contribution checkpoint mismatch: {}", player.getUUID(), error);
            player.connection.disconnect(Component.literal("Your contribution receipt needs recovery. Restore matching player and kingdom data."));
        }
    }
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) OFFERS.remove(player); }
    private static void requirePlayer(ServerPlayer player) {
        if(com.mohymods.kingdomsmp.building.ConstructionService.royal(player.server)){
            var hut=com.mohymods.kingdomsmp.persistence.BuilderHutSavedData.get(player.server).hut().orElse(null);
            if(hut==null||hut.phase()!=com.mohymods.kingdomsmp.persistence.BuilderHutSavedData.Phase.ACTIVE)throw new IllegalStateException("Establish the owner's Builder's Hut before contributing to First Hearth.");
        }
        GameType type = player.gameMode.getGameModeForPlayer();
        if (!player.isAlive() || type != GameType.SURVIVAL && type != GameType.ADVENTURE || player.containerMenu != player.inventoryMenu || !player.containerMenu.getCarried().isEmpty()) {
            throw new IllegalStateException("Donate while alive in Survival or Adventure mode, with other containers closed.");
        }
    }
    private static List<ItemStack> inventory(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>(); for (int slot = 0; slot < 36; slot++) result.add(player.getInventory().getItem(slot).copy()); return List.copyOf(result);
    }
    private static boolean sameInventory(ServerPlayer player, List<ItemStack> before) {
        for (int slot = 0; slot < 36; slot++) if (!ItemStack.matches(player.getInventory().getItem(slot), before.get(slot))) return false;
        return true;
    }
    private static Path playerFile(MinecraftServer server, UUID player) { return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player + ".dat"); }
    private static Receipt loadedReceipt(ServerPlayer player) { return receiptFromPersistent(player.getPersistentData()); }
    private static Receipt receipt(CompoundTag playerFile) { return receiptFromPersistent(playerFile.getCompound("NeoForgeData")); }
    private static Receipt receiptFromPersistent(CompoundTag persistent) {
        if (persistent.contains(Player.PERSISTED_NBT_TAG) && !persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) throw new IllegalStateException("Invalid persistent player data");
        CompoundTag root = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(RECEIPT)) return Receipt.empty();
        if (!root.contains(RECEIPT, Tag.TAG_COMPOUND)) throw new IllegalStateException("Invalid player donation receipt");
        CompoundTag tag = root.getCompound(RECEIPT);
        if (!tag.hasUUID("store") || !tag.contains("sequence", Tag.TAG_LONG)) throw new IllegalStateException("Invalid player donation receipt");
        return new Receipt(tag.getUUID("store"), tag.getLong("sequence"));
    }
    private static void putReceipt(ServerPlayer player, Receipt receipt) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(Player.PERSISTED_NBT_TAG).copy();
        CompoundTag tag = new CompoundTag(); tag.putUUID("store", receipt.store()); tag.putLong("sequence", receipt.sequence());
        root.put(RECEIPT, tag); persistent.put(Player.PERSISTED_NBT_TAG, root);
    }
    private static void verifyLoadedReceipt(ServerPlayer player, ContributionLedger ledger) {
        long expected = ledger.expectedReceipt(player.getUUID());
        Receipt loaded = loadedReceipt(player);
        if (loaded.sequence() == expected && (expected == 0 && loaded.store() == null || ledger.id().equals(loaded.store()))) return;
        // Integrated hosts may load an older level.dat player copy instead of the
        // native playerdata checkpoint. Restore only from an exact matching receipt.
        try {
            CompoundTag disk = AtomicNbt.read(playerFile(player.server, player.getUUID()));
            Receipt saved = receipt(disk);
            if (!disk.hasUUID("UUID") || !disk.getUUID("UUID").equals(player.getUUID()) || !ledger.id().equals(saved.store())
                    || saved.sequence() != expected || loaded.sequence() > expected) throw new IOException("Receipt mismatch");
            player.getInventory().load(disk.getList("Inventory", Tag.TAG_COMPOUND));
            putReceipt(player, saved); player.inventoryMenu.sendAllDataToRemote();
            LogUtils.getLogger().warn("Reconciled stale loaded inventory from verified player checkpoint: {}", player.getUUID());
        } catch (IOException error) { throw new IllegalStateException("Player inventory/ledger restore mismatch", error); }
    }
    private static void checkpoint(ServerPlayer player) throws IOException {
        ListTag expectedInventory = player.getInventory().save(new ListTag());
        CompoundTag serialized = player.saveWithoutId(new CompoundTag());
        if (!serialized.getList("Inventory", Tag.TAG_COMPOUND).equals(expectedInventory)) throw new IOException("Inventory serialization changed during donation");
        Path file = playerFile(player.server, player.getUUID());
        if (Files.exists(file)) AtomicNbt.write(file.resolveSibling(player.getUUID() + ".dat_old"), AtomicNbt.read(file));
        AtomicNbt.write(file, serialized);
        net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(player, file.getParent().toFile(), player.getStringUUID());
        CompoundTag saved = AtomicNbt.read(file);
        if (!receipt(saved).equals(loadedReceipt(player)) || !saved.getList("Inventory", Tag.TAG_COMPOUND).equals(expectedInventory)
                || !player.getInventory().save(new ListTag()).equals(expectedInventory)) throw new IOException("Player checkpoint verification failed");
    }
    public static String label(Resource resource) { return switch (resource) { case LOGS -> "logs"; case COBBLESTONE -> "cobblestone"; case WHEAT -> "wheat"; case IRON_INGOTS -> "iron ingots"; }; }
    public static String name(ContributionLedger ledger, UUID player) {
        for (int index = ledger.history().size() - 1; index >= 0; index--) if (ledger.history().get(index).player().equals(player)) return ledger.history().get(index).name();
        return player.toString().substring(0, 8);
    }
}
