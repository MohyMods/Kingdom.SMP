package com.mohymods.kingdomsmp.network;

import com.mohymods.kingdomsmp.economy.CommerceService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.*;
import java.util.function.Consumer;

/** Network messages use the same server transaction entry point as commands and NPC services. */
public final class CommerceNetworking {
    private static Consumer<CommerceView> receiver;
    private static final Map<ServerPlayer,Long> requests=new WeakHashMap<>();
    private CommerceNetworking(){ }
    public static void install(Consumer<CommerceView> client){receiver=Objects.requireNonNull(client);}
    public static void register(PayloadRegistrar registrar){
        registrar.playToClient(CommerceView.TYPE,CommerceView.STREAM_CODEC,(p,c)->{if(receiver!=null)receiver.accept(p);});
        registrar.playToServer(CommerceRequest.TYPE,CommerceRequest.STREAM_CODEC,(p,c)->{
            if(!(c.player() instanceof ServerPlayer player))return;
            if(p.action()==4){
                requests.remove(player);
                try{CommerceService.handle(player,p);}catch(IllegalArgumentException|IllegalStateException ignored){ }
                return;
            }
            long now=System.nanoTime();Long previous=requests.get(player);
            if(previous!=null&&now-previous<100_000_000L)return;
            requests.put(player,now);
            try{c.reply(CommerceService.handle(player,p));}
            catch(IllegalArgumentException|IllegalStateException failure){
                String message=failure.getMessage()==null?"Commerce unavailable. Refresh before retrying.":failure.getMessage();
                if(message.length()>512)message=message.substring(0,512);
                try{c.reply(CommerceService.snapshot(player,p.requestId(),p.tab(),message));}
                catch(IllegalArgumentException|IllegalStateException unavailable){
                    c.reply(new CommerceView(p.requestId(),0,false,p.tab(),1,0,List.of(),CommerceRequest.NONE,"",message));
                }
            }
        });
    }
    public static void open(ServerPlayer player,int tab){PacketDistributor.sendToPlayer(player,CommerceService.snapshot(player,0,tab,"").withOpen(true));}
}
