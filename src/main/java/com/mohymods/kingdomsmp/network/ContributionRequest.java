package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record ContributionRequest(long requestId, int action, int resource, int amount, UUID offer) implements CustomPacketPayload {
    public static final UUID NONE = new UUID(0, 0);
    public ContributionRequest {
        if (requestId <= 0 || action < 0 || action > 3 || resource < 0 || resource > 3 || amount < 0 || amount > 2048 || offer == null
                || action == 1 && amount == 0 || action == 2 && offer.equals(NONE)) throw new IllegalArgumentException("Invalid contribution request");
    }
    public static final Type<ContributionRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID, "contribution_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContributionRequest> STREAM_CODEC = StreamCodec.of((b, p) -> {
        b.writeVarLong(p.requestId); b.writeVarInt(p.action); b.writeVarInt(p.resource); b.writeVarInt(p.amount); b.writeUUID(p.offer);
    }, b -> new ContributionRequest(b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
