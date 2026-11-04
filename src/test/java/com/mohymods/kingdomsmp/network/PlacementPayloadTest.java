package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.placement.*;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlacementPayloadTest {
    @Test void candidateAndBoundedDiagnosticCodecsRoundTrip(){
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{var p=new PlacementProbe(7,new BlockPos(-22,64,8),3);var r=new PlacementResult(p,false,"Support is missing",List.of(p.origin()),List.of(p.origin().below()));
            PlacementResult.STREAM_CODEC.encode(b,r);assertEquals(r,PlacementResult.STREAM_CODEC.decode(b));assertEquals(0,b.readableBytes());
        }finally{b.release();}
    }
    @Test void invalidCoordinatesRotationIdentityAndOversizedDiagnosticsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new PlacementProbe(0,BlockPos.ZERO,0));
        assertThrows(IllegalArgumentException.class,()->new PlacementProbe(1,new BlockPos(Integer.MIN_VALUE,0,0),0));
        assertThrows(IllegalArgumentException.class,()->new PlacementProbe(1,BlockPos.ZERO,4));
        var p=new PlacementProbe(1,BlockPos.ZERO,0);
        assertThrows(IllegalArgumentException.class,()->new PlacementResult(p,false,"x".repeat(513),List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new PlacementResult(p,false,"",Collections.nCopies(33,BlockPos.ZERO),List.of()));
    }
    @Test void hostileDiagnosticCountIsRejectedBeforeAllocation(){
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{PlacementProbe.STREAM_CODEC.encode(b,new PlacementProbe(1,BlockPos.ZERO,0));b.writeBoolean(false);b.writeUtf("");b.writeVarInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,()->PlacementResult.STREAM_CODEC.decode(b));
        }finally{b.release();}
    }
}
