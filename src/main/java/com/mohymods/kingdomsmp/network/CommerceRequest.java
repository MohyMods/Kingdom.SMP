package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Bounded intentions only; prices, balances, entitlement owners and rewards are server derived. */
public record CommerceRequest(long requestId,int action,int tab,String selected,int quantity,UUID review,long amount) implements CustomPacketPayload {
    public CommerceRequest(long requestId,int action,int tab,String selected,int quantity,UUID review){this(requestId,action,tab,selected,quantity,review,0);}
    public static final UUID NONE=new UUID(0,0);
    public CommerceRequest {
        if(requestId<=0||action<0||action>4||tab<0||tab>5||selected==null||selected.length()>96
                ||selected.codePoints().anyMatch(Character::isISOControl)||quantity<1||quantity>2304||amount<0||amount>1_000_000_000L||review==null
                ||(action==1||action==3)&&selected.isBlank()||action==2&&review.equals(NONE))
            throw new IllegalArgumentException("Invalid commerce request");
    }
    public static final Type<CommerceRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID,"commerce_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CommerceRequest> STREAM_CODEC=StreamCodec.of((b,p)->{
        b.writeVarLong(p.requestId);b.writeVarInt(p.action);b.writeVarInt(p.tab);b.writeUtf(p.selected,96);b.writeVarInt(p.quantity);b.writeUUID(p.review);b.writeVarLong(p.amount);
    },b->new CommerceRequest(b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readUtf(96),b.readVarInt(),b.readUUID(),b.readVarLong()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
