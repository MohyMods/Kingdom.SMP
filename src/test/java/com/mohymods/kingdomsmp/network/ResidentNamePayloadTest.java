package com.mohymods.kingdomsmp.network;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ResidentNamePayloadTest {
  @Test
  void exactReviewedNameRoundTripsWithoutAddingClientEntitlementOrNpcIdentity() {
    var b = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    try {
      var q =
          new ConstructionRequest(
              1, 13, BlockPos.ZERO, 0, ConstructionRequest.NONE, "Cedric Valehart");
      ConstructionRequest.STREAM_CODEC.encode(b, q);
      assertEquals(q, ConstructionRequest.STREAM_CODEC.decode(b));
      assertEquals(0, b.readableBytes());
    } finally {
      b.release();
    }
  }

  @Test
  void forgedConfirmationTextAndHostileNamePayloadsAreRejected() {
    for (String bad : new String[] {"", "   ", "x".repeat(49), "Cedric\nAdmin", "§4King"})
      assertThrows(
          IllegalArgumentException.class,
          () -> new ConstructionRequest(1, 13, BlockPos.ZERO, 0, ConstructionRequest.NONE, bad));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ConstructionRequest(
                1, 14, BlockPos.ZERO, 0, UUID.randomUUID(), "Changed after review"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConstructionRequest(1, 14, BlockPos.ZERO, 0, ConstructionRequest.NONE));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConstructionRequest(1, 12, BlockPos.ZERO, 0, UUID.randomUUID()));
  }

  @Test
  void normalizerPreservesExistingReadableNamePolicy() {
    assertEquals(
        "Elara Vale of Dawn",
        com.mohymods.kingdomsmp.progression.WorldProgression.normalizeResidentName(
            " Elara Vale of Dawn "));
    assertThrows(
        IllegalArgumentException.class,
        () -> com.mohymods.kingdomsmp.progression.WorldProgression.normalizeResidentName("\t\n"));
  }
}
