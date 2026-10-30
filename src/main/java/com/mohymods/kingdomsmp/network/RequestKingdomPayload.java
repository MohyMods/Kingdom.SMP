package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** No client-provided state, grants, target player, or arbitrary strings. */
public record RequestKingdomPayload(long requestId) implements CustomPacketPayload {
    public RequestKingdomPayload {
        if (requestId <= 0) throw new IllegalArgumentException("Invalid overview request id");
    }
    public static final Type<RequestKingdomPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID, "request_kingdom"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestKingdomPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeVarLong(payload.requestId()),
                    buffer -> new RequestKingdomPayload(buffer.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
