package com.mohymods.kingdomsmp.placement;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Candidate intent only; held entitlement and identity are resolved by the server. */
public record PlacementProbe(long id,BlockPos origin,int rotation) implements CustomPacketPayload {
    public PlacementProbe {if(id<=0||origin==null||Math.abs((long)origin.getX())>30_000_000||Math.abs((long)origin.getZ())>30_000_000||origin.getY()<-2048||origin.getY()>2048||rotation<0||rotation>3)throw new IllegalArgumentException("Invalid placement candidate");origin=origin.immutable();}
    public static final Type<PlacementProbe> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID,"placement_probe"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlacementProbe> STREAM_CODEC=StreamCodec.of((b,p)->{b.writeVarLong(p.id);b.writeBlockPos(p.origin);b.writeVarInt(p.rotation);},b->new PlacementProbe(b.readVarLong(),b.readBlockPos(),b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
