package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Finite projection of actual shop stock and private mailbox state. */
public record CommerceView(long requestId,long revision,boolean open,int tab,int tier,long balance,List<Entry> entries,UUID review,String reviewText,String notice) implements CustomPacketPayload {
    public record Entry(String id,String title,String detail,long price,String state) {
        public Entry {
            if(id==null||id.isBlank()||id.length()>96||title==null||title.length()>64||detail==null||detail.length()>256
                    ||price<0||state==null||state.length()>48)throw new IllegalArgumentException("Invalid commerce entry");
        }
    }
    public CommerceView {
        if(requestId<0||revision<0||tab<0||tab>4||tier<1||tier>12||balance<0||entries==null||entries.size()>32
                ||review==null||reviewText==null||reviewText.length()>1024||notice==null||notice.length()>512)
            throw new IllegalArgumentException("Invalid commerce view");
        entries=List.copyOf(entries);
        if(entries.stream().map(Entry::id).distinct().count()!=entries.size())throw new IllegalArgumentException("Duplicate commerce entries");
    }
    public boolean reviewing(){return !review.equals(CommerceRequest.NONE);}
    public CommerceView withOpen(boolean value){return new CommerceView(requestId,revision,value,tab,tier,balance,entries,review,reviewText,notice);}
    public static final Type<CommerceView> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID,"commerce_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CommerceView> STREAM_CODEC=StreamCodec.of((b,p)->{
        b.writeVarLong(p.requestId);b.writeVarLong(p.revision);b.writeBoolean(p.open);b.writeVarInt(p.tab);b.writeVarInt(p.tier);b.writeVarLong(p.balance);
        b.writeVarInt(p.entries.size());
        for(var row:p.entries){b.writeUtf(row.id,96);b.writeUtf(row.title,64);b.writeUtf(row.detail,256);b.writeVarLong(row.price);b.writeUtf(row.state,48);}
        b.writeUUID(p.review);b.writeUtf(p.reviewText,1024);b.writeUtf(p.notice,512);
    },b->{
        long request=b.readVarLong(),revision=b.readVarLong();boolean open=b.readBoolean();int tab=b.readVarInt(),tier=b.readVarInt();long balance=b.readVarLong();
        int size=b.readVarInt();if(size<0||size>32)throw new IllegalArgumentException("Too many commerce entries");
        List<Entry> entries=new ArrayList<>(size);
        for(int i=0;i<size;i++)entries.add(new Entry(b.readUtf(96),b.readUtf(64),b.readUtf(256),b.readVarLong(),b.readUtf(48)));
        return new CommerceView(request,revision,open,tab,tier,balance,entries,b.readUUID(),b.readUtf(1024),b.readUtf(512));
    });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
