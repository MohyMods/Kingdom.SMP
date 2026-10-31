package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.building.ConstructionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.*;
import java.util.function.Consumer;

public final class ConstructionNetworking {
    private static Consumer<ConstructionView> receiver;
    private static final Map<ServerPlayer,Long> viewers=new WeakHashMap<>();
    private ConstructionNetworking() { }
    public static void install(Consumer<ConstructionView> client){receiver=Objects.requireNonNull(client);}
    public static void register(PayloadRegistrar registrar){
        registrar.playToClient(ConstructionView.TYPE,ConstructionView.STREAM_CODEC,(p,c)->{if(receiver!=null)receiver.accept(p);});
        registrar.playToServer(ConstructionRequest.TYPE,ConstructionRequest.STREAM_CODEC,(p,c)->{
            if(!(c.player() instanceof ServerPlayer player))return;
            long now=System.nanoTime();Long previous=viewers.put(player,now);
            if(previous!=null&&now-previous<100_000_000L)return;
            String notice="";
            try{notice=ConstructionService.request(player,p);}catch(IllegalArgumentException|IllegalStateException e){notice=e.getMessage()==null?"Construction unavailable.":e.getMessage();}
            c.reply(ConstructionService.view(player,p.requestId(),false,notice));
        });
    }
    public static void open(ServerPlayer player,String notice){
        viewers.put(player,System.nanoTime());PacketDistributor.sendToPlayer(player,ConstructionService.view(player,0,true,notice));
    }
    public static void push(MinecraftServer server){
        for(ServerPlayer player:List.copyOf(viewers.keySet()))if(player.server==server&&!player.hasDisconnected())
            PacketDistributor.sendToPlayer(player,ConstructionService.view(player,0,false,""));
    }
}
