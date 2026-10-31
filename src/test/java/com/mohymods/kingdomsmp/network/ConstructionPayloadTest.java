package com.mohymods.kingdomsmp.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionPayloadTest {
    @Test void actualConstructionCodecsRoundTripBoundedIntentAndProjection(){
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            var request=new ConstructionRequest(9,3,new BlockPos(-10,64,5),2,ConstructionRequest.NONE);
            ConstructionRequest.STREAM_CODEC.encode(buffer,request);assertEquals(request,ConstructionRequest.STREAM_CODEC.decode(buffer));
            var view=new ConstructionView(9,4,false,UUID.randomUUID(),UUID.randomUUID(),"Alice","PROPOSED",new BlockPos(-10,64,5),2,true,true,
                    17,13,12,0,900,true,true,true,6,UUID.randomUUID(),"Awaiting exact site approval","No terrain changes","PROPOSE_SITE");
            ConstructionView.STREAM_CODEC.encode(buffer,view);assertEquals(view,ConstructionView.STREAM_CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());
        }finally{buffer.release();}
    }
    @Test void malformedCoordinatesActionsAndUnboundConfirmationsAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new ConstructionRequest(1,3,new BlockPos(Integer.MAX_VALUE,64,0),0,ConstructionRequest.NONE));
        assertThrows(IllegalArgumentException.class,()->new ConstructionRequest(1,6,BlockPos.ZERO,0,ConstructionRequest.NONE));
        assertThrows(IllegalArgumentException.class,()->new ConstructionRequest(1,3,BlockPos.ZERO,4,ConstructionRequest.NONE));
        assertThrows(IllegalArgumentException.class,()->new ConstructionRequest(1,99,BlockPos.ZERO,0,UUID.randomUUID()));
    }
}
