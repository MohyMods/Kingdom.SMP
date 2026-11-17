package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import com.mohymods.kingdomsmp.kingdom.KingdomState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Bounded read-only view. Contains no client-granted progression or mutable collections. */
public record KingdomSnapshotPayload(int tier, long revision, long requestId,
        int logs, int cobblestone, int wheat, int ironIngots, int personalScoreUnits,
        List<String> milestoneDetails) implements CustomPacketPayload {
    public KingdomSnapshotPayload {
        if (tier < 1 || tier > KingdomState.MAX_TIER || revision < 0 || requestId <= 0) {
            throw new IllegalArgumentException("Invalid kingdom snapshot");
        }
        if (logs < 0 || logs > 1024 || cobblestone < 0 || cobblestone > 2048
                || wheat < 0 || wheat > 512 || ironIngots < 0 || ironIngots > 128
                || personalScoreUnits < 0 || personalScoreUnits > logs * 2 + cobblestone + wheat * 4 + ironIngots * 16) {
            throw new IllegalArgumentException("Invalid contribution snapshot");
        }
        milestoneDetails=List.copyOf(milestoneDetails);
        if(milestoneDetails.size()!=12||milestoneDetails.stream().anyMatch(s->s.length()>4096))throw new IllegalArgumentException("Invalid milestone projection");
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
                buffer.writeVarInt(payload.milestoneDetails().size());
                payload.milestoneDetails().forEach(s->buffer.writeUtf(s,4096));
            }, buffer -> new KingdomSnapshotPayload(buffer.readVarInt(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),readDetails(buffer)));
    private static List<String> readDetails(RegistryFriendlyByteBuf buffer){
        int count=buffer.readVarInt();if(count!=12)throw new IllegalArgumentException("Expected twelve milestone details");
        var details=new java.util.ArrayList<String>();for(int i=0;i<count;i++)details.add(buffer.readUtf(4096));return List.copyOf(details);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
