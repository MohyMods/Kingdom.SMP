package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.UUID;

public record BuilderDialoguePayload(UUID npcId, String name, String status, int topic,
                                     boolean hearthComplete, long requestId) implements CustomPacketPayload {
    public BuilderDialoguePayload {
        Objects.requireNonNull(npcId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(status);
        if (name.isBlank() || name.length() > 48 || status.length() > 256
                || topic < 0 || topic > 4 || requestId < 0 || (requestId == 0 && topic != 0)) {
            throw new IllegalArgumentException("Invalid builder dialogue snapshot");
        }
    }
    public static final Type<BuilderDialoguePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID, "builder_dialogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderDialoguePayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeUUID(payload.npcId());
                buffer.writeUtf(payload.name(), 48);
                buffer.writeUtf(payload.status(), 256);
                buffer.writeVarInt(payload.topic());
                buffer.writeBoolean(payload.hearthComplete());
                buffer.writeVarLong(payload.requestId());
            }, buffer -> new BuilderDialoguePayload(buffer.readUUID(), buffer.readUtf(48), buffer.readUtf(256),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
