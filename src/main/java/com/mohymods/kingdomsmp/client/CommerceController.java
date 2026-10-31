package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.client.screen.CommerceScreen;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

/** Single-flight correlated commerce requests; uncertain results require an explicit refresh. */
public final class CommerceController {
    private static final OverviewRequestTracker REQUESTS=new OverviewRequestTracker(10_000_000_000L);
    private static boolean refreshRequired;
    private static String status="";
    private static int lastTab;
    private CommerceController(){ }
    public static void open(){open(0);}
    public static void open(int tab){request(0,tab,"",1,CommerceRequest.NONE);}
    public static void request(int action,int tab,String selected,int quantity,UUID review){request(action,tab,selected,quantity,review,0);}
    public static void request(int action,int tab,String selected,int quantity,UUID review,long amount){
        tick();var client=Minecraft.getInstance();
        if(client.player==null||client.getConnection()==null||refreshRequired&&action!=0)return;
        long id=REQUESTS.begin(System.nanoTime(),client.getConnection(),client.screen);if(id==0)return;
        lastTab=tab;status=action==2?"Saving your transaction…":"Reading the Royal Exchange…";
        PacketDistributor.sendToServer(new CommerceRequest(id,action,tab,selected,quantity,review,amount));
    }
    public static void receive(CommerceView view){
        tick();var client=Minecraft.getInstance();if(client.player==null||client.getConnection()==null)return;
        if(view.requestId()==0){
            if(view.open()&&client.screen==null)client.setScreen(new CommerceScreen(view,null));
            else if(client.screen instanceof CommerceScreen screen)screen.accept(view);
            return;
        }
        if(!REQUESTS.complete(view.requestId(),client.getConnection(),client.screen,System.nanoTime()))return;
        refreshRequired=false;status="";lastTab=view.tab();
        if(client.screen instanceof CommerceScreen screen)screen.accept(view);
        else client.setScreen(new CommerceScreen(view,client.screen));
    }
    public static void tick(){
        var client=Minecraft.getInstance();
        if(client.player==null||client.getConnection()==null){REQUESTS.clear();refreshRequired=false;status="";return;}
        var state=REQUESTS.tick(System.nanoTime(),client.getConnection(),client.screen);
        if(state==OverviewRequestTracker.Status.TIMED_OUT){
            refreshRequired=true;status="No reply. Refresh to inspect the saved result before retrying.";
            if(client.screen instanceof CommerceScreen screen)screen.invalidateReview();
        }else if(state==OverviewRequestTracker.Status.CANCELLED){refreshRequired=false;status="";}
    }
    public static boolean waiting(){return REQUESTS.waiting();}
    public static boolean refreshRequired(){return refreshRequired;}
    public static String status(){return status;}
    public static void close(){
        REQUESTS.clear();refreshRequired=false;status="";var client=Minecraft.getInstance();
        if(client.player!=null&&client.getConnection()!=null)PacketDistributor.sendToServer(new CommerceRequest(1,4,lastTab,"",1,CommerceRequest.NONE));
    }
}
