package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.UUID;

/** An inspection intention; never an entitlement or construction command. */
public record RequestBuilderDialoguePayload(UUID npcId, int topic, long requestId) implements CustomPacketPayload {
    public RequestBuilderDialoguePayload {
        Objects.requireNonNull(npcId);
        if (topic < 0 || topic > 4 || requestId <= 0) throw new IllegalArgumentException("Invalid builder request");
    }
    public static final Type<RequestBuilderDialoguePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID, "request_builder_dialogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBuilderDialoguePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeUUID(payload.npcId());
                buffer.writeVarInt(payload.topic());
                buffer.writeVarLong(payload.requestId());
            }, buffer -> new RequestBuilderDialoguePayload(buffer.readUUID(), buffer.readVarInt(), buffer.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
