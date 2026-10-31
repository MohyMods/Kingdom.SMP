package com.mohymods.kingdomsmp.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneProjectionTest {
    @Test void boundedActualOverviewCodecRoundTripsAllTwelveServerDefinitions(){
        var details=new ArrayList<>(Collections.nCopies(12,"PROPOSAL — NOT ENABLED\nGlass: 256\nNo resources consumed."));
        details.set(0,"APPROVED HISTORICAL CONTRACT\nLogs: 1024 / 1024");
        var view=new KingdomSnapshotPayload(2,6,12,1024,2048,512,128,8192,details);details.clear();
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{KingdomSnapshotPayload.STREAM_CODEC.encode(buf,view);assertEquals(view,KingdomSnapshotPayload.STREAM_CODEC.decode(buf));assertEquals(0,buf.readableBytes());}finally{buf.release();}
    }
    @Test void oversizedProjectionAndAttackerSuppliedCountAreRejectedBeforeAllocation(){
        assertThrows(IllegalArgumentException.class,()->new KingdomSnapshotPayload(1,0,1,0,0,0,0,0,List.of()));
        assertThrows(IllegalArgumentException.class,()->new KingdomSnapshotPayload(1,0,1,0,0,0,0,0,Collections.nCopies(12,"x".repeat(4097))));
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{buf.writeVarInt(1);buf.writeVarLong(0);buf.writeVarLong(1);for(int i=0;i<5;i++)buf.writeVarInt(0);buf.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,()->KingdomSnapshotPayload.STREAM_CODEC.decode(buf));}finally{buf.release();}
    }
}
