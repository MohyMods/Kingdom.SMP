package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record ConstructionRequest(long requestId,int action,BlockPos origin,int rotation,UUID review) implements CustomPacketPayload {
    public static final UUID NONE=new UUID(0,0);
    public ConstructionRequest {
        if(requestId<=0||action<0||action>11||origin==null||Math.abs((long)origin.getX())>30_000_000
                ||Math.abs((long)origin.getZ())>30_000_000||origin.getY()<-2048||origin.getY()>2048||rotation<0||rotation>3||review==null
                ||action>0&&action%2==0&&review.equals(NONE))throw new IllegalArgumentException("Invalid construction request");
        origin=origin.immutable();
    }
    public static final Type<ConstructionRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID,"construction_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ConstructionRequest> STREAM_CODEC=StreamCodec.of((b,p)->{
        b.writeVarLong(p.requestId);b.writeVarInt(p.action);b.writeBlockPos(p.origin);b.writeVarInt(p.rotation);b.writeUUID(p.review);
    },b->new ConstructionRequest(b.readVarLong(),b.readVarInt(),b.readBlockPos(),b.readVarInt(),b.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
