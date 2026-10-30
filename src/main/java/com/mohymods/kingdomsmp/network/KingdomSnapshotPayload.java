package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import com.mohymods.kingdomsmp.kingdom.KingdomState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded read-only view. Contains no client-granted progression or mutable collections. */
public record KingdomSnapshotPayload(int tier, long revision, long requestId,
        int logs, int cobblestone, int wheat, int ironIngots, int personalScoreUnits) implements CustomPacketPayload {
    public KingdomSnapshotPayload {
        if (tier < 1 || tier > KingdomState.MAX_TIER || revision < 0 || requestId <= 0) {
            throw new IllegalArgumentException("Invalid kingdom snapshot");
        }
        if (logs < 0 || logs > 1024 || cobblestone < 0 || cobblestone > 2048
                || wheat < 0 || wheat > 512 || ironIngots < 0 || ironIngots > 128
                || personalScoreUnits < 0 || personalScoreUnits > logs * 2 + cobblestone + wheat * 4 + ironIngots * 16) {
            throw new IllegalArgumentException("Invalid contribution snapshot");
        }
    }
    public static final Type<KingdomSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID, "kingdom_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KingdomSnapshotPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.tier());
                buffer.writeVarLong(payload.revision());
                buffer.writeVarLong(payload.requestId());
                buffer.writeVarInt(payload.logs());
                buffer.writeVarInt(payload.cobblestone());
                buffer.writeVarInt(payload.wheat());
                buffer.writeVarInt(payload.ironIngots());
                buffer.writeVarInt(payload.personalScoreUnits());
            }, buffer -> new KingdomSnapshotPayload(buffer.readVarInt(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
