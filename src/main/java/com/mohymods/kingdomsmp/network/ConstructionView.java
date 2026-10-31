package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Small bounded projection: no client-provided entitlement or administrator identity. */
public record ConstructionView(long requestId,long revision,boolean open,UUID project,UUID npc,String winner,String phase,
        BlockPos origin,int rotation,boolean hasSite,boolean valid,int width,int depth,int height,int placed,int total,
        boolean owner,boolean admin,boolean hasPlan,int reviewAction,UUID review,String activity,String notice,String audit,ResourceLocation blueprint,String resident) implements CustomPacketPayload {
    public ConstructionView(long requestId,long revision,boolean open,UUID project,UUID npc,String winner,String phase,BlockPos origin,int rotation,boolean hasSite,boolean valid,int width,int depth,int height,int placed,int total,boolean owner,boolean admin,boolean hasPlan,int reviewAction,UUID review,String activity,String notice,String audit){this(requestId,revision,open,project,npc,winner,phase,origin,rotation,hasSite,valid,width,depth,height,placed,total,owner,admin,hasPlan,reviewAction,review,activity,notice,audit,com.mohymods.kingdomsmp.building.FoundersLodgeBlueprint.ID,"Mira Ashford");}
    public String title(){return com.mohymods.kingdomsmp.building.BlueprintRegistry.title(blueprint);}
    public boolean modern(){return !blueprint.equals(com.mohymods.kingdomsmp.building.FoundersLodgeBlueprint.ID);}
    public ConstructionView {
        if(blueprint==null||resident==null||resident.length()>48||requestId<0||revision<0||project==null||npc==null||winner==null||winner.length()>48||phase==null||phase.length()>32
                ||origin==null||rotation<0||rotation>3||width<1||width>64||depth<1||depth>64||height<1||height>64
                ||placed<0||total<1||total>32768||placed>total||reviewAction<0||reviewAction>10||review==null
                ||activity==null||activity.length()>512||notice==null||notice.length()>512||audit==null||audit.length()>2048)
            throw new IllegalArgumentException("Invalid construction view");
        origin=origin.immutable();
    }
    public static final Type<ConstructionView> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(KingdomSmp.MOD_ID,"construction_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ConstructionView> STREAM_CODEC=StreamCodec.of((b,p)->{
        b.writeVarLong(p.requestId);b.writeVarLong(p.revision);b.writeBoolean(p.open);b.writeUUID(p.project);b.writeUUID(p.npc);
        b.writeUtf(p.winner,48);b.writeUtf(p.phase,32);b.writeBlockPos(p.origin);b.writeVarInt(p.rotation);b.writeBoolean(p.hasSite);b.writeBoolean(p.valid);
        b.writeVarInt(p.width);b.writeVarInt(p.depth);b.writeVarInt(p.height);b.writeVarInt(p.placed);b.writeVarInt(p.total);
        b.writeBoolean(p.owner);b.writeBoolean(p.admin);b.writeBoolean(p.hasPlan);b.writeVarInt(p.reviewAction);b.writeUUID(p.review);
        b.writeUtf(p.activity,512);b.writeUtf(p.notice,512);b.writeUtf(p.audit,2048);b.writeResourceLocation(p.blueprint);b.writeUtf(p.resident,48);
    },b->new ConstructionView(b.readVarLong(),b.readVarLong(),b.readBoolean(),b.readUUID(),b.readUUID(),b.readUtf(48),b.readUtf(32),b.readBlockPos(),
            b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),
            b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readUUID(),b.readUtf(512),b.readUtf(512),b.readUtf(2048),b.readResourceLocation(),b.readUtf(48)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
