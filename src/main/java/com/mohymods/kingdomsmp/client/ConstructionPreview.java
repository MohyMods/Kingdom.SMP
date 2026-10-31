package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.network.ConstructionView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Advisory translucent footprint only: never places client or server blocks. */
public final class ConstructionPreview {
    private static ConstructionView view;
    private static Object connection;
    private static long seen;
    private ConstructionPreview(){ }
    public static void accept(ConstructionView updated){
        if(!updated.hasSite()||updated.phase().equals("COMPLETE")){clear();return;}
        view=updated;connection=Minecraft.getInstance().getConnection();seen=System.nanoTime();
    }
    public static void clear(){view=null;connection=null;}
    public static void render(RenderLevelStageEvent event){
        var client=Minecraft.getInstance();
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||view==null||client.level==null
                ||client.level.dimension()!=Level.OVERWORLD||connection!=client.getConnection()||System.nanoTime()-seen>300_000_000_000L)return;
        var origin=view.origin();var camera=event.getCamera().getPosition();
        if(camera.distanceToSqr(origin.getX(),origin.getY(),origin.getZ())>128*128)return;
        var pose=event.getPoseStack();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=client.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        float red=view.valid()?0.9f:1f,green=view.valid()?0.72f:0.18f,blue=view.valid()?0.22f:0.15f;
        LevelRenderer.renderLineBox(pose,lines,origin.getX(),origin.getY(),origin.getZ(),origin.getX()+view.width(),origin.getY()+view.height(),origin.getZ()+view.depth(),red,green,blue,0.7f);
        LevelRenderer.renderLineBox(pose,lines,origin.getX(),origin.getY()+0.03,origin.getZ(),origin.getX()+view.width(),origin.getY()+0.08,origin.getZ()+view.depth(),red,green,blue,0.95f);
        pose.popPose();buffers.endBatch(RenderType.lines());
    }
}
