package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ShopStoreTest {
    @TempDir Path directory;
    private final UUID buyer = UUID.randomUUID(), other = UUID.randomUUID(), admin = UUID.randomUUID();
    private static final String IRON = "kingdom_smp:supplies/iron";
    private static final String WHEAT = "kingdom_smp:supplies/wheat";
    private static final long DAY = 21_000;

    private EconomyStore open() throws IOException { return EconomyStore.open(directory); }
    private Path file() { return directory.resolve("kingdomsmp_economy.dat"); }
    private void fund(EconomyStore store, UUID player, long coins) throws IOException {
        store.adminSet(UUID.randomUUID(), admin, player, coins, "Isolated NPC shop fixture");
    }
    private CompoundTag item(int quantity) { return item("minecraft:iron_ingot", quantity); }
    private CompoundTag item(String id, int quantity) {
        var tag = new CompoundTag();tag.putString("id", id);tag.putInt("count", quantity);return tag;
    }
    private EconomyStore.ItemMail buy(EconomyStore store, UUID operation, int quantity) throws IOException {
        return store.shopBuy(operation, buyer, IRON, "Iron shop purchase", item(quantity), quantity, DAY, 8, 2, 5);
    }

    @Test void purchaseAtomicallyDebitsMarginalCostConsumesStockAndCreatesPrivateMail() throws Exception {
        var store = open();fund(store, buyer, 100);
        var operation = UUID.randomUUID();var mail = buy(store, operation, 4);
        assertEquals(90, store.balance(buyer)); // 2 + 2 + 3 + 3, not four times the first price.
        assertEquals(4, store.shopSold(DAY, IRON));
        assertEquals(10, store.shopCoinsSpent());assertEquals(0, store.taxRemoved());
        assertEquals(buyer, mail.owner());assertEquals(operation, mail.id());assertFalse(mail.claimed());
        assertEquals(4, mail.item().getInt("count"));assertTrue(store.shopDelivery(operation));
        assertTrue(store.itemMailbox(other).isEmpty());assertTrue(store.mailbox(buyer).isEmpty());
        assertEquals(0, store.expectedReceipt(buyer));assertTrue(store.pendingItem().isEmpty());
        var reloaded = open();assertEquals(store.id(), reloaded.id());
        assertEquals(90, reloaded.balance(buyer));assertEquals(4, reloaded.shopSold(DAY, IRON));
        assertEquals(10, reloaded.shopCoinsSpent());assertEquals(mail, reloaded.itemMailbox(buyer).getFirst());
    }

    @Test void inputAndReturnedItemDataCannotMutatePersistedDelivery() throws Exception {
        var store = open();fund(store, buyer, 100);var source = item(4);
        var mail = store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", source, 4, DAY, 8, 2, 5);
        source.putString("id", "minecraft:diamond");source.putInt("count", 64);
        var returned = mail.item();returned.putInt("count", 64);
        assertEquals("minecraft:iron_ingot", store.itemMailbox(buyer).getFirst().item().getString("id"));
        assertEquals(4, open().itemMailbox(buyer).getFirst().item().getInt("count"));
    }

    @Test void exactReplayAfterLaterPurchasesRetainsOneDebitAndDelivery() throws Exception {
        var store = open();fund(store, buyer, 100);var operation = UUID.randomUUID();
        buy(store, operation, 4);buy(store, UUID.randomUUID(), 1);long revision = store.revision();
        long balance = store.balance(buyer), spent = store.shopCoinsSpent();
        var reloaded = open();buy(reloaded, operation, 4);
        assertEquals(revision, reloaded.revision());assertEquals(balance, reloaded.balance(buyer));
        assertEquals(spent, reloaded.shopCoinsSpent());assertEquals(5, reloaded.shopSold(DAY, IRON));
        assertEquals(2, reloaded.itemMailbox(buyer).size());
    }

    @Test void reusedOperationCannotChangeBuyerResourcePolicyQuantityOrItem() throws Exception {
        var store = open();fund(store, buyer, 100);var operation = UUID.randomUUID();buy(store, operation, 4);
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, other, IRON, "Iron shop purchase", item(4), 4, DAY, 8, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, WHEAT, "Iron shop purchase", item(4), 4, DAY, 8, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, IRON, "Changed title", item(4), 4, DAY, 8, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, IRON, "Iron shop purchase", item(3), 3, DAY, 8, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, IRON, "Iron shop purchase", item(4), 4, DAY + 1, 8, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, IRON, "Iron shop purchase", item(4), 4, DAY, 16, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, IRON, "Iron shop purchase", item(4), 4, DAY, 8, 3, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(operation, buyer, IRON, "Iron shop purchase", item("minecraft:diamond", 4), 4, DAY, 8, 2, 5));
        assertEquals(90, store.balance(buyer));assertEquals(4, store.shopSold(DAY, IRON));assertEquals(1, store.itemMailbox(buyer).size());
    }

    @Test void interruptedPreClaimCancelsOnlyTheNativeClaimAndRetainsPurchasedOwnership() throws Exception {
        var store = open();fund(store, buyer, 100);var mail = buy(store, UUID.randomUUID(), 4);
        var prepared = store.prepareItemClaim(UUID.randomUUID(), buyer, mail.id());
        var reloaded = open();assertEquals(prepared, reloaded.pendingItem().orElseThrow());
        assertFalse(reloaded.recoverItem(EconomyStore.Receipt.empty()));
        assertTrue(reloaded.pendingItem().isEmpty());assertFalse(reloaded.itemMailbox(buyer).getFirst().claimed());
        assertEquals(90, reloaded.balance(buyer));assertEquals(4, reloaded.shopSold(DAY, IRON));
        assertEquals(0, reloaded.expectedReceipt(buyer));
        assertThrows(IllegalStateException.class, () -> reloaded.prepareItemClaim(UUID.randomUUID(), other, mail.id()));
    }

    @Test void savedPostClaimReceiptCommitsOnceAndPurchaseReplayCannotRecreateItems() throws Exception {
        var store = open();fund(store, buyer, 100);var operation = UUID.randomUUID();var mail = buy(store, operation, 4);
        var prepared = store.prepareItemClaim(UUID.randomUUID(), buyer, mail.id());
        var receipt = new EconomyStore.Receipt(store.id(), prepared.sequence());
        var recovered = open();assertTrue(recovered.recoverItem(receipt));
        var reloaded = open();assertTrue(reloaded.itemMailbox(buyer).getFirst().claimed());
        assertEquals(prepared.sequence(), reloaded.expectedReceipt(buyer));assertFalse(reloaded.recoverItem(receipt));
        assertTrue(buy(reloaded, operation, 4).claimed());
        assertThrows(IllegalStateException.class, () -> reloaded.prepareItemClaim(UUID.randomUUID(), buyer, mail.id()));
        assertEquals(90, reloaded.balance(buyer));assertEquals(4, reloaded.shopSold(DAY, IRON));
        assertEquals(1, reloaded.itemMailbox(buyer).size());
    }

    @Test void shopPurchasePreservesAnExistingReceiptAndRejectsForeignClaimRecovery() throws Exception {
        var store = open();fund(store, buyer, 100);
        var sale = store.prepareSale(UUID.randomUUID(), buyer, IRON, 1, 1, DAY, 100, 8, 100);
        var previous = new EconomyStore.Receipt(store.id(), sale.sequence());assertTrue(store.recoverSale(previous));
        var mail = buy(store, UUID.randomUUID(), 1);assertEquals(sale.sequence(), store.expectedReceipt(buyer));
        var prepared = store.prepareItemClaim(UUID.randomUUID(), buyer, mail.id());
        var reloaded = open();assertThrows(IOException.class, () -> reloaded.recoverItem(new EconomyStore.Receipt(UUID.randomUUID(), prepared.sequence())));
        assertTrue(reloaded.pendingItem().isPresent());assertFalse(reloaded.recoverItem(previous));
        assertFalse(reloaded.itemMailbox(buyer).getFirst().claimed());assertEquals(sale.sequence(), reloaded.expectedReceipt(buyer));
    }

    @Test void stockIsSharedAcrossBuyersButIsolatedByResourceAndUtcDay() throws Exception {
        var store = open();fund(store, buyer, 1000);fund(store, other, 1000);
        store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(2), 2, DAY, 8, 2, 5);
        store.shopBuy(UUID.randomUUID(), other, IRON, "Iron", item(1), 1, DAY, 8, 2, 5);
        store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(3), 3, DAY + 1, 8, 2, 5);
        store.shopBuy(UUID.randomUUID(), buyer, WHEAT, "Wheat", item("minecraft:wheat", 4), 4, DAY, 8, 2, 5);
        var reloaded = open();assertEquals(3, reloaded.shopSold(DAY, IRON));assertEquals(3, reloaded.shopSold(DAY + 1, IRON));
        assertEquals(4, reloaded.shopSold(DAY, WHEAT));assertEquals(0, reloaded.shopSold(DAY + 1, WHEAT));
        assertEquals(0, reloaded.shopSold(DAY + 2, IRON));assertEquals(3, reloaded.itemMailbox(buyer).size());
        assertEquals(1, reloaded.itemMailbox(other).size());
    }

    @Test void advancingDayDoesNotResetAnEarlierDaysConsumedStock() throws Exception {
        var store = open();fund(store, buyer, 1000);
        store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(8), 8, DAY, 8, 2, 5);
        store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(8), 8, DAY + 1, 8, 2, 5);
        var reloaded = open();assertEquals(8, reloaded.shopSold(DAY, IRON));assertEquals(8, reloaded.shopSold(DAY + 1, IRON));
        assertThrows(IllegalArgumentException.class, () -> buy(reloaded, UUID.randomUUID(), 1));
        assertEquals(0, reloaded.shopSold(DAY + 2, IRON));
    }

    @Test void competingBuyersOfTheLastStockProduceExactlyOnePurchase() throws Exception {
        var store = open();fund(store, buyer, 100);fund(store, other, 100);
        var start = new CountDownLatch(1);var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (UUID player : List.of(buyer, other)) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {store.shopBuy(UUID.randomUUID(), player, IRON, "Last iron", item(1), 1, DAY, 1, 2, 5);return true;}
                    catch (IllegalArgumentException | IllegalStateException exhausted) {return false;}
                }));
            }
            start.countDown();int accepted = 0;for (var result : results) if (result.get()) accepted++;
            assertEquals(1, accepted);assertEquals(1, store.shopSold(DAY, IRON));
            assertEquals(198, store.balance(buyer) + store.balance(other));assertEquals(2, store.shopCoinsSpent());
            assertEquals(1, store.itemMailbox(buyer).size() + store.itemMailbox(other).size());
        } finally {pool.shutdownNow();}
    }

    @Test void insufficientBalanceOrMalformedPurchaseCannotPartiallyConsumeStock() throws Exception {
        var store = open();fund(store, buyer, 1);long revision = store.revision();
        assertThrows(IllegalStateException.class, () -> buy(store, UUID.randomUUID(), 1));
        assertThrows(IllegalArgumentException.class, () -> buy(store, UUID.randomUUID(), 0));
        assertThrows(IllegalArgumentException.class, () -> buy(store, UUID.randomUUID(), 65));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(1), 2, DAY, 8, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(1), 1, DAY, 0, 2, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(1), 1, DAY, 8, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> store.shopBuy(UUID.randomUUID(), buyer, IRON, "Iron", item(1), 1, -1, 8, 2, 5));
        assertEquals(revision, store.revision());assertEquals(1, store.balance(buyer));assertEquals(0, store.shopSold(DAY, IRON));
        assertEquals(0, store.shopCoinsSpent());assertTrue(store.itemMailbox(buyer).isEmpty());assertFalse(store.quarantined());
    }

    @Test void pendingNativeSaleAndExistingEscrowBothBlockNewShopPurchases() throws Exception {
        var store = open();fund(store, buyer, 100);
        store.prepareSale(UUID.randomUUID(), buyer, IRON, 1, 1, DAY, 100, 8, 100);
        assertThrows(IllegalStateException.class, () -> buy(store, UUID.randomUUID(), 1));
        assertTrue(store.pendingSale().isPresent());assertEquals(0, store.shopSold(DAY, IRON));
        assertFalse(store.recoverSale(EconomyStore.Receipt.empty()));
        store.prepareListing(UUID.randomUUID(), buyer, "Buyer", "Old escrow", 10, false, 1000, 2000, item(1));
        assertThrows(IllegalStateException.class, () -> buy(store, UUID.randomUUID(), 1));
        assertTrue(store.pendingItem().isPresent());assertEquals(100, store.balance(buyer));
        assertFalse(store.recoverItem(EconomyStore.Receipt.empty()));
        buy(store, UUID.randomUUID(), 1);assertEquals(1, store.shopSold(DAY, IRON));
    }

    @Test void failureBeforePurchaseRenameLeavesNoDebitStockOrMailOnDisk() throws Exception {
        var store = open();fund(store, buyer, 100);var operation = UUID.randomUUID();
        var failing = EconomyStore.open(directory, (path, tag) -> {throw new IOException("Before shop rename");});
        assertThrows(IOException.class, () -> buy(failing, operation, 4));assertTrue(failing.quarantined());
        assertEquals(100, failing.balance(buyer));assertEquals(0, failing.shopSold(DAY, IRON));
        var reloaded = open();assertEquals(100, reloaded.balance(buyer));assertEquals(0, reloaded.shopSold(DAY, IRON));
        assertTrue(reloaded.itemMailbox(buyer).isEmpty());buy(reloaded, operation, 4);
        assertEquals(90, reloaded.balance(buyer));assertEquals(1, reloaded.itemMailbox(buyer).size());
    }

    @Test void failureAfterPurchaseRenameRecoversTheWholePurchaseAndIdempotentRetry() throws Exception {
        var store = open();fund(store, buyer, 100);var operation = UUID.randomUUID();
        var failing = EconomyStore.open(directory, (path, tag) -> {AtomicNbt.write(path, tag);throw new IOException("After shop rename");});
        assertThrows(IOException.class, () -> buy(failing, operation, 4));assertTrue(failing.quarantined());
        assertEquals(100, failing.balance(buyer));assertEquals(0, failing.shopSold(DAY, IRON));
        var reloaded = open();assertEquals(90, reloaded.balance(buyer));assertEquals(4, reloaded.shopSold(DAY, IRON));
        assertEquals(1, reloaded.itemMailbox(buyer).size());buy(reloaded, operation, 4);
        assertEquals(90, reloaded.balance(buyer));assertEquals(10, reloaded.shopCoinsSpent());
        assertEquals(1, reloaded.itemMailbox(buyer).size());
    }

    @Test void claimCommitFailureBeforeRenameRetainsTheMatchingRecoveryOperation() throws Exception {
        var store = open();fund(store, buyer, 100);var mail = buy(store, UUID.randomUUID(), 4);
        var pending = store.prepareItemClaim(UUID.randomUUID(), buyer, mail.id());
        var receipt = new EconomyStore.Receipt(store.id(), pending.sequence());
        var failing = EconomyStore.open(directory, (path, tag) -> {throw new IOException("Before claim rename");});
        assertThrows(IOException.class, () -> failing.recoverItem(receipt));
        var reloaded = open();assertTrue(reloaded.pendingItem().isPresent());assertFalse(reloaded.itemMailbox(buyer).getFirst().claimed());
        assertTrue(reloaded.recoverItem(receipt));assertTrue(open().itemMailbox(buyer).getFirst().claimed());
        assertEquals(90, reloaded.balance(buyer));assertEquals(4, reloaded.shopSold(DAY, IRON));
    }

    @Test void claimCommitFailureAfterRenameNeverRestoresTheClaimedEntitlement() throws Exception {
        var store = open();fund(store, buyer, 100);var mail = buy(store, UUID.randomUUID(), 4);
        var pending = store.prepareItemClaim(UUID.randomUUID(), buyer, mail.id());
        var receipt = new EconomyStore.Receipt(store.id(), pending.sequence());
        var failing = EconomyStore.open(directory, (path, tag) -> {AtomicNbt.write(path, tag);throw new IOException("After claim rename");});
        assertThrows(IOException.class, () -> failing.recoverItem(receipt));
        var reloaded = open();assertTrue(reloaded.pendingItem().isEmpty());assertTrue(reloaded.itemMailbox(buyer).getFirst().claimed());
        assertFalse(reloaded.recoverItem(receipt));assertEquals(pending.sequence(), reloaded.expectedReceipt(buyer));
        assertEquals(90, reloaded.balance(buyer));assertEquals(4, reloaded.shopSold(DAY, IRON));
    }

    @Test void malformedPinnedAmountStockAndCountAreRejectedDuringReplay() throws Exception {
        var store = open();fund(store, buyer, 100);buy(store, UUID.randomUUID(), 4);
        var original = AtomicNbt.read(file());
        List<Consumer<CompoundTag>> corruptions = List.of(
                op -> op.putLong("amount", op.getLong("amount") + 1),
                op -> op.getCompound("payload").putInt("stock", 0),
                op -> op.getCompound("payload").putInt("stock", 3),
                op -> op.getCompound("payload").putInt("stock", 16),
                op -> op.getCompound("payload").getCompound("item").putInt("count", 3),
                op -> op.getCompound("payload").putLong("stock", 8),
                op -> op.getCompound("payload").putLong("minimum", 0),
                op -> op.getCompound("payload").putLong("maximum", Long.MAX_VALUE)
        );
        for (var corruption : corruptions) {
            var changed = original.copy();var history = changed.getList("history", Tag.TAG_COMPOUND);
            corruption.accept(history.getCompound(history.size() - 1));AtomicNbt.write(file(), changed);
            assertThrows(IOException.class, this::open);
        }
        AtomicNbt.write(file(), original);assertEquals(90, open().balance(buyer));
    }

    @Test void journalPrefixAndCanonicalMailOriginSurviveNewShopOperations() throws Exception {
        var store = open();fund(store, buyer, 100);
        var ordinaryMail = store.queueCoins(UUID.randomUUID(), buyer, 7, "NPC shop: misleading coin title");
        var before = AtomicNbt.read(file()).getList("history", Tag.TAG_COMPOUND).copy();
        var delivery = buy(store, UUID.randomUUID(), 4);var after = AtomicNbt.read(file()).getList("history", Tag.TAG_COMPOUND);
        assertEquals(before.size() + 1, after.size());for (int i = 0; i < before.size(); i++) assertEquals(before.get(i), after.get(i));
        var reloaded = open();assertFalse(reloaded.shopDelivery(ordinaryMail.id()));assertFalse(reloaded.shopDelivery(UUID.randomUUID()));
        assertTrue(reloaded.shopDelivery(delivery.id()));assertEquals(0, reloaded.expectedReceipt(buyer));
        assertEquals(4, reloaded.shopSold(DAY, IRON));assertEquals(10, reloaded.shopCoinsSpent());
    }
}
