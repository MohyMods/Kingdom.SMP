package com.mohymods.kingdomsmp.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContributionPayloadTest {
    @Test void requestAndBoundedSnapshotRoundTripThroughMinecraftBuffer() {
        var b = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var request = new ContributionRequest(17, 1, 2, 64, ContributionRequest.NONE);
            ContributionRequest.STREAM_CODEC.encode(b, request); assertEquals(request, ContributionRequest.STREAM_CODEC.decode(b));
            var p = new ContributionView(17, 3, 1, List.of(64,0,0,0), List.of(64,0,0,0), List.of(5,0,0,0),
                    List.of(new ContributionView.Row("Alice",128,List.of(64,0,0,0))),1,
                    List.of(new ContributionView.History(0,64,1000)), 0, 1, UUID.randomUUID(),0,5,"Review donation",true,false,"");
            ContributionView.STREAM_CODEC.encode(b,p); assertEquals(p,ContributionView.STREAM_CODEC.decode(b)); assertEquals(0,b.readableBytes());
        } finally { b.release(); }
    }
    @Test void clientCannotRequestNegativeHugeOrUnboundConfirmation() {
        assertThrows(IllegalArgumentException.class,()->new ContributionRequest(1,1,0,-1,ContributionRequest.NONE));
        assertThrows(IllegalArgumentException.class,()->new ContributionRequest(1,1,0,Integer.MAX_VALUE,ContributionRequest.NONE));
        assertThrows(IllegalArgumentException.class,()->new ContributionRequest(1,2,0,0,ContributionRequest.NONE));
        assertThrows(IllegalArgumentException.class,()->new ContributionRequest(1,1,4,64,ContributionRequest.NONE));
    }
    @Test void oversizedLeaderboardIsRejectedBeforeReadingRows() {
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            b.writeVarLong(1);b.writeVarLong(0);b.writeVarInt(1);
            for(int i=0;i<12;i++)b.writeVarInt(0);
            b.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,()->ContributionView.STREAM_CODEC.decode(b));
        } finally {b.release();}
    }
}
