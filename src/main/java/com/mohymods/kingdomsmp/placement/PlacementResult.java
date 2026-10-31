package com.mohymods.kingdomsmp.placement;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Bounded diagnostics; never transmits containers, block-entity data or construction authority. */
public record PlacementResult(PlacementProbe candidate,boolean valid,String reason,List<BlockPos> collisions,List<BlockPos> unsupported) implements CustomPacketPayload {
    public PlacementResult {Objects.requireNonNull(candidate);if(reason==null||reason.length()>512||collisions.size()>32||unsupported.size()>32)throw new IllegalArgumentException("Invalid placement diagnostics");collisions=collisions.stream().map(BlockPos::immutable).toList();unsupported=unsupported.stream().map(BlockPos::immutable).toList();}
    public static final Type<PlacementResult> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID,"placement_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PlacementResult> STREAM_CODEC=StreamCodec.of((b,p)->{PlacementProbe.STREAM_CODEC.encode(b,p.candidate);b.writeBoolean(p.valid);b.writeUtf(p.reason,512);write(b,p.collisions);write(b,p.unsupported);},b->new PlacementResult(PlacementProbe.STREAM_CODEC.decode(b),b.readBoolean(),b.readUtf(512),read(b),read(b)));
    private static void write(RegistryFriendlyByteBuf b,List<BlockPos> list){b.writeVarInt(list.size());list.forEach(b::writeBlockPos);}
    private static List<BlockPos> read(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<0||n>32)throw new IllegalArgumentException("Too many diagnostic cells");var list=new ArrayList<BlockPos>();for(int i=0;i<n;i++)list.add(b.readBlockPos());return List.copyOf(list);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
