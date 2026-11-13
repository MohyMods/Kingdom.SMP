package com.mohymods.kingdomsmp.economy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HuntStoreTest {
    @TempDir Path dir;
    final UUID player=UUID.randomUUID(),other=UUID.randomUUID();
    EconomyStore open() throws IOException { return EconomyStore.open(dir); }
    HuntState accept(EconomyStore store) throws IOException { return store.acceptHunt(UUID.randomUUID(),player,10,8,160,2000); }
    @Test void acceptanceReservesPaymentWithoutMakingItClaimable() throws Exception {
        var store=open();var hunt=accept(store);
        assertEquals(0,store.balance(player));assertEquals(160,store.huntSpent(10));
        assertEquals(EconomyStore.MailKind.HUNT_ESCROW,store.mailbox(player).getFirst().kind());
        assertThrows(IllegalArgumentException.class,()->store.claimCoins(player,hunt.id()));
        assertEquals(hunt,open().hunt(player,10).orElseThrow());
        assertThrows(IllegalStateException.class,()->accept(store));
    }
    @Test void earnedReservationUnlocksAndClaimsExactlyOnceAcrossReopen() throws Exception {
        var store=open();var hunt=accept(store);var close=UUID.randomUUID();
        store.closeHunt(close,player,hunt.id(),10,true);store.closeHunt(close,player,hunt.id(),10,true);
        assertThrows(IllegalStateException.class,()->store.closeHunt(UUID.randomUUID(),player,hunt.id(),10,true));
        var restored=open();restored.claimCoins(player,hunt.id());restored.claimCoins(player,hunt.id());
        assertEquals(160,open().balance(player));assertTrue(open().hunt(player,10).orElseThrow().paid());
        assertEquals(160,restored.huntSpent(10));
    }
    @Test void noForeignPrematureOrLatePayment() throws Exception {
        var store=open();var hunt=accept(store);
        assertThrows(IllegalStateException.class,()->store.closeHunt(UUID.randomUUID(),other,hunt.id(),10,true));
        assertThrows(IllegalStateException.class,()->store.closeHunt(UUID.randomUUID(),player,hunt.id(),11,true));
        assertThrows(IllegalStateException.class,()->store.closeHunt(UUID.randomUUID(),player,hunt.id(),10,false));
        assertEquals(EconomyStore.MailKind.HUNT_ESCROW,store.mailbox(player).getFirst().kind());
    }
    @Test void expiryClosesOnlyUnpaidReservationAndAllowsNextDay() throws Exception {
        var store=open();var hunt=accept(store);store.closeHunt(UUID.randomUUID(),player,hunt.id(),11,false);
        var expired=open();assertTrue(expired.mailbox(player).getFirst().claimed());assertFalse(expired.hunt(player,10).orElseThrow().paid());
        assertThrows(IllegalArgumentException.class,()->expired.claimCoins(player,hunt.id()));
        assertDoesNotThrow(()->expired.acceptHunt(UUID.randomUUID(),player,11,8,160,2000));assertEquals(0,expired.balance(player));
    }
    @Test void poolCannotBeOversubscribedOrReconfiguredMidDay() throws Exception {
        var store=open();for(int n=0;n<12;n++)store.acceptHunt(UUID.randomUUID(),UUID.randomUUID(),10,8,160,2000);
        assertEquals(1920,store.huntSpent(10));
        assertThrows(IllegalStateException.class,()->store.acceptHunt(UUID.randomUUID(),player,10,8,160,2000));
        assertThrows(IllegalStateException.class,()->store.acceptHunt(UUID.randomUUID(),player,10,8,160,4000));
        assertEquals(12,open().hunts().size());
    }
    @Test void replayCannotAlterAcceptedTermsOrExpiredResult() throws Exception {
        var store=open();var id=UUID.randomUUID();store.acceptHunt(id,player,10,8,160,2000);store.acceptHunt(id,player,10,8,160,2000);
        assertThrows(IllegalArgumentException.class,()->store.acceptHunt(id,player,10,9,160,2000));
        var close=UUID.randomUUID();store.closeHunt(close,player,id,11,false);
        assertThrows(IllegalArgumentException.class,()->store.closeHunt(close,player,id,10,true));
    }
    @Test void failedPaymentWriteRetainsOnlyReservedEntitlement() throws Exception {
        var initial=open();var hunt=accept(initial);
        var failing=EconomyStore.open(dir,(path,tag)->{throw new IOException("simulated interrupted reward checkpoint");});
        assertThrows(IOException.class,()->failing.closeHunt(UUID.randomUUID(),player,hunt.id(),10,true));
        var recovered=open();assertFalse(recovered.hunt(player,10).orElseThrow().closed());
        assertEquals(EconomyStore.MailKind.HUNT_ESCROW,recovered.mailbox(player).getFirst().kind());
        recovered.closeHunt(UUID.randomUUID(),player,hunt.id(),10,true);recovered.claimCoins(player,hunt.id());assertEquals(160,open().balance(player));
    }
    @Test void invalidQuantitiesNeverConsumePoolOrCreateMail() throws Exception {
        var store=open();assertThrows(IllegalArgumentException.class,()->store.acceptHunt(UUID.randomUUID(),player,10,0,160,2000));
        assertThrows(IllegalArgumentException.class,()->store.acceptHunt(UUID.randomUUID(),player,10,8,160,1000));
        assertEquals(0,store.revision());assertTrue(store.mailbox(player).isEmpty());
    }
}
