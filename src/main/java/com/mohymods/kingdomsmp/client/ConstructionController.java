package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.client.screen.ConstructionScreen;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

/** Correlated intent/reply lifecycle; an uncertain response requires explicit refresh. */
public final class ConstructionController {
    private static final OverviewRequestTracker REQUESTS=new OverviewRequestTracker(10_000_000_000L);
    private static boolean refreshRequired;
    private static String status="";
    private ConstructionController(){ }
    public static void open(){request(0,BlockPos.ZERO,0,ConstructionRequest.NONE);}
    public static void request(int action,BlockPos origin,int rotation,UUID review){
        tick();var client=Minecraft.getInstance();
        if(client.player==null||client.getConnection()==null||refreshRequired&&action!=0)return;
        long id=REQUESTS.begin(System.nanoTime(),client.getConnection(),client.screen);if(id==0)return;
        status=action==0?"Reading the construction record…":"Waiting for the kingdom…";
        PacketDistributor.sendToServer(new ConstructionRequest(id,action,origin,rotation,review));
    }
    public static void receive(ConstructionView view){
        tick();var client=Minecraft.getInstance();if(client.player==null||client.getConnection()==null)return;
        if(view.requestId()==0){
            if(view.open()&&(client.screen==null||client.screen instanceof com.mohymods.kingdomsmp.client.screen.CommerceScreen))client.setScreen(new ConstructionScreen(view,null));
            else if(!view.open()&&client.screen instanceof ConstructionScreen screen)screen.accept(view);
            ConstructionPreview.accept(view);return;
        }
        if(!REQUESTS.complete(view.requestId(),client.getConnection(),client.screen,System.nanoTime()))return;
        refreshRequired=false;status="";ConstructionPreview.accept(view);
        if(client.screen instanceof ConstructionScreen screen)screen.accept(view);
        else client.setScreen(new ConstructionScreen(view,client.screen));
    }
    public static void tick(){
        var client=Minecraft.getInstance();
        if(client.player==null||client.getConnection()==null){REQUESTS.clear();refreshRequired=false;status="";ConstructionPreview.clear();return;}
        var state=REQUESTS.tick(System.nanoTime(),client.getConnection(),client.screen);
        if(state==OverviewRequestTracker.Status.TIMED_OUT){refreshRequired=true;status="No reply. Refresh to inspect the saved result before retrying.";}
        else if(state==OverviewRequestTracker.Status.CANCELLED){refreshRequired=false;status="";}
    }
    public static boolean waiting(){return REQUESTS.waiting();}
    public static boolean refreshRequired(){return refreshRequired;}
    public static String status(){return status;}
    public static void close(){
        REQUESTS.clear();refreshRequired=false;status="";
        var client=Minecraft.getInstance();if(client.player!=null&&client.getConnection()!=null)
            PacketDistributor.sendToServer(new ConstructionRequest(1,11,BlockPos.ZERO,0,ConstructionRequest.NONE));
    }
}
