package com.mohymods.kingdomsmp.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BuilderDialoguePayloadTest {
    private static final UUID NPC = UUID.randomUUID();
    @Test void realStreamCodecsRoundTripIdentityTopicAndCorrelation() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var request = new RequestBuilderDialoguePayload(NPC, 4, Long.MAX_VALUE);
            RequestBuilderDialoguePayload.STREAM_CODEC.encode(buffer, request);
            assertEquals(request, RequestBuilderDialoguePayload.STREAM_CODEC.decode(buffer));
            var response = new BuilderDialoguePayload(NPC, "Aldric Stonehand", "Routine paused: path blocked.", 4, false, Long.MAX_VALUE);
            BuilderDialoguePayload.STREAM_CODEC.encode(buffer, response);
            assertEquals(response, BuilderDialoguePayload.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void forgedTopicsAndUncorrelatedRequestsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RequestBuilderDialoguePayload(NPC, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> new RequestBuilderDialoguePayload(NPC, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RequestBuilderDialoguePayload(NPC, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuilderDialoguePayload(NPC, "Aldric", "", 1, false, 0));
    }
    @Test void snapshotStringsAreBoundedAndInitialGreetingIsDistinct() {
        assertDoesNotThrow(() -> new BuilderDialoguePayload(NPC, "Aldric", "", 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuilderDialoguePayload(NPC, "x".repeat(49), "", 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new BuilderDialoguePayload(NPC, "Aldric", "x".repeat(257), 0, false, 0));
    }
    @Test void oversizedWireStringFailsBeforeBecomingAClientSnapshot() {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            buffer.writeUUID(NPC);
            buffer.writeUtf("x".repeat(49));
            assertThrows(io.netty.handler.codec.DecoderException.class, () -> BuilderDialoguePayload.STREAM_CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
