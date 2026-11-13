package com.mohymods.kingdomsmp.integration.aircraft;

import static com.mohymods.kingdomsmp.integration.aircraft.CargoReceiptRules.Action.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CargoReceiptRulesTest {
  @Test
  void legacyFramesWithNoCargoHistoryRemainUsable() {
    assertEquals(READY, CargoReceiptRules.decide(true, null, null, null, null, null, false));
  }

  @Test
  void aReservedButUnperformedDebitNeedsItsExactBeforeStateAndOriginalActor() {
    var pending = UUID.randomUUID();
    assertEquals(
        WAIT_FOR_ACTOR, CargoReceiptRules.decide(true, null, pending, null, null, null, true));
    assertEquals(REFUSE, CargoReceiptRules.decide(true, null, pending, null, null, null, false));
  }

  @Test
  void aSavedPendingReceiptAcknowledgesWithoutASecondInventoryDebit() {
    var ticket = UUID.randomUUID();
    for (boolean before : new boolean[] {true, false})
      assertEquals(
          ACKNOWLEDGE, CargoReceiptRules.decide(true, ticket, ticket, null, null, null, before));
  }

  @Test
  void aCompletedReceiptAllowsLaterLegitimateInventoryChanges() {
    var delivered = UUID.randomUUID();
    for (boolean before : new boolean[] {true, false})
      assertEquals(
          READY, CargoReceiptRules.decide(true, delivered, null, null, delivered, null, before));
  }

  @Test
  void aWorldRollbackCanOnlyReconcileTheKnownEarlierReceiptAndExactSource() {
    var previous = UUID.randomUUID();
    var delivered = UUID.randomUUID();
    assertEquals(
        REVIEW_DELIVERED_DEBIT,
        CargoReceiptRules.decide(true, previous, null, null, delivered, previous, true));
    assertEquals(
        REFUSE, CargoReceiptRules.decide(true, previous, null, null, delivered, previous, false));
    assertEquals(
        REFUSE,
        CargoReceiptRules.decide(true, UUID.randomUUID(), null, null, delivered, previous, true));
  }

  @Test
  void sourceCountsOrMalformedDataAloneNeverAuthorizeAcknowledgement() {
    var pending = UUID.randomUUID();
    assertEquals(REFUSE, CargoReceiptRules.decide(false, pending, pending, null, null, null, true));
    assertEquals(
        REFUSE, CargoReceiptRules.decide(true, UUID.randomUUID(), pending, null, null, null, true));
    var earlier = UUID.randomUUID();
    assertEquals(
        REFUSE,
        CargoReceiptRules.decide(true, earlier, pending, UUID.randomUUID(), earlier, null, true));
  }
}
