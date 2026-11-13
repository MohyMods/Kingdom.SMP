package com.mohymods.kingdomsmp.integration.aircraft;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure recovery decisions only. This class does not consume items, pay money or register events.
 */
public final class CargoReceiptRules {
  public enum Action {
    READY,
    WAIT_FOR_ACTOR,
    ACKNOWLEDGE,
    REVIEW_DELIVERED_DEBIT,
    REFUSE
  }

  private CargoReceiptRules() {}

  /**
   * All IDs come from typed canonical records for the same authenticated generation/body. A
   * source-count match without its saved receipt never independently authorizes payment.
   * REVIEW_DELIVERED_DEBIT requires an explicit administrative recovery review; it is not an
   * instruction to automatically consume goods on entity load.
   */
  public static Action decide(
      boolean typedReceipt,
      UUID actual,
      UUID pending,
      UUID pendingPrevious,
      UUID delivered,
      UUID deliveredPrevious,
      boolean exactBefore) {
    if (!typedReceipt) return Action.REFUSE;
    if (pending != null && pending.equals(actual)) return Action.ACKNOWLEDGE;
    if (Objects.equals(actual, delivered)) {
      if (pending == null) return Action.READY;
      return Objects.equals(pendingPrevious, delivered) && exactBefore
          ? Action.WAIT_FOR_ACTOR
          : Action.REFUSE;
    }
    if (delivered != null && Objects.equals(actual, deliveredPrevious) && exactBefore)
      return Action.REVIEW_DELIVERED_DEBIT;
    return Action.REFUSE;
  }
}
