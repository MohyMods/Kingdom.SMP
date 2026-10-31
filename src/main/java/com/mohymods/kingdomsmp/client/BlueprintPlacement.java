package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.network.ConstructionRequest;
import com.mohymods.kingdomsmp.placement.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Held plan targeting and complete block-model ghost; no world edits or client-granted authority. */
public final class BlueprintPlacement {
    private static final String CATEGORY="key.categories.kingdom_smp";
    public static final KeyMapping ROTATE=new KeyMapping("key.kingdom_smp.rotate",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,CATEGORY);
    public static final KeyMapping UP=new KeyMapping("key.kingdom_smp.height_up",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_PAGE_UP,CATEGORY);
    public static final KeyMapping DOWN=new KeyMapping("key.kingdom_smp.height_down",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_PAGE_DOWN,CATEGORY);
    public static final ModConfigSpec CONFIG;
    private static final ModConfigSpec.IntValue REACH,HEIGHT;
    static{var b=new ModConfigSpec.Builder();REACH=b.defineInRange("blueprintTargetReach",24,4,48);HEIGHT=b.defineInRange("blueprintHeightOffsetLimit",16,0,64);CONFIG=b.build();}
    private static final Map<Rotation,List<BuilderHutBlueprint.Placement>> GEOMETRY=new EnumMap<>(Rotation.class);
    private static Object connection;
    private static CompoundTag identity;
    private static int rotation,height;
    private static long serial,lastSent,seen;
    private static PlacementProbe candidate,pending;
    private static PlacementResult result;
    private static boolean held;
    private BlueprintPlacement(){}
    public static boolean active(){return held&&candidate!=null;}
    private static void clear(){candidate=null;pending=null;result=null;identity=null;held=false;rotation=0;height=0;lastSent=0;}
    public static void tick(){
        var c=Minecraft.getInstance();if(c.player==null||c.level==null||connection!=c.getConnection()) {clear();connection=c.getConnection();}
        if(c.player==null||c.level==null)return;
        var stack=c.player.getMainHandItem().is(KingdomBlueprints.LODGE.get())?c.player.getMainHandItem():c.player.getOffhandItem();
        if(!stack.is(KingdomBlueprints.LODGE.get())||c.level.dimension()!=Level.OVERWORLD){clear();return;}
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!Objects.equals(identity,data)){clear();identity=data;}
        held=true;
        if(c.screen!=null)return;
        while(ROTATE.consumeClick())rotation=Math.floorMod(rotation+(net.minecraft.client.gui.screens.Screen.hasShiftDown()?-1:1),4);
        while(UP.consumeClick())height=Math.min(HEIGHT.get(),height+1);
        while(DOWN.consumeClick())height=Math.max(-HEIGHT.get(),height-1);
        var hit=c.player.pick(REACH.get(),0,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK){candidate=null;pending=null;result=null;return;}
        var b=BlueprintRegistry.lodge();var r=Rotation.values()[rotation];
        BlockPos origin=block.getBlockPos().relative(block.getDirection()).offset(-b.width(r)/2,height,-b.depth(r)/2);
        long now=System.nanoTime();
        if(candidate==null||!candidate.origin().equals(origin)||candidate.rotation()!=rotation){candidate=new PlacementProbe(++serial,origin,rotation);result=null;}
        if(now-lastSent>=300_000_000L){pending=new PlacementProbe(++serial,origin,rotation);lastSent=now;PacketDistributor.sendToServer(pending);}
    }
    public static void receive(PlacementResult update){
        var c=Minecraft.getInstance();if(!held||candidate==null||pending==null||connection!=c.getConnection()||!pending.equals(update.candidate()))return;
        if(!candidate.origin().equals(pending.origin())||candidate.rotation()!=pending.rotation())return;
        result=update;seen=System.nanoTime();
    }
    private static boolean current(){return result!=null&&candidate!=null&&result.candidate().origin().equals(candidate.origin())&&result.candidate().rotation()==candidate.rotation()&&System.nanoTime()-seen<1_000_000_000L;}
    public static void use(InputEvent.InteractionKeyMappingTriggered event){
        var c=Minecraft.getInstance();if(!held||c.screen!=null||!event.isUseItem())return;
        event.setCanceled(true);event.setSwingHand(false);
        if(event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||candidate==null||ConstructionController.waiting())return;
        ConstructionController.request(3,candidate.origin(),candidate.rotation(),ConstructionRequest.NONE);
    }
    public static void hud(RenderGuiEvent.Post event){
        var c=Minecraft.getInstance();if(!active()||c.screen!=null||c.options.hideGui)return;
        var g=event.getGuiGraphics();int y=c.getWindow().getGuiScaledHeight()-84,w=Math.min(510,c.getWindow().getGuiScaledWidth()-20);
        int color=!current()?0xFFE4B858:result.valid()?0xFF65E68A:0xFFFF7474;
        g.fill(6,y-5,Math.min(c.getWindow().getGuiScaledWidth()-6,w+16),y+74,0xDA171A22);
        g.drawString(c.font,"Founders' Lodge · "+(current()?(result.valid()?"VALID":"BLOCKED"):"PENDING"),12,y,color,false);
        g.drawString(c.font,ROTATE.getTranslatedKeyMessage().getString()+" rotate · Shift reverse · "+UP.getTranslatedKeyMessage().getString()+" / "+DOWN.getTranslatedKeyMessage().getString()+" height "+height+" · Use: review",12,y+13,0xFFE6D1A4,false);
        String reason=current()?result.reason():"Aim at loaded ground; waiting for the server's site checks.";
        var lines=c.font.split(net.minecraft.network.chat.Component.literal(reason),w);for(int i=0;i<Math.min(3,lines.size());i++)g.drawString(c.font,lines.get(i),12,y+28+i*11,0xFFE8E4DA,false);
    }
    public static void render(RenderLevelStageEvent event){
        var c=Minecraft.getInstance();if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!active()||c.level==null||connection!=c.getConnection())return;
        var pose=event.getPoseStack();var camera=event.getCamera().getPosition();var o=candidate.origin();
        if(camera.distanceToSqr(o.getX(),o.getY(),o.getZ())>128*128)return;
        int red=!current()?225:result.valid()?75:255,green=!current()?172:result.valid()?240:75,blue=!current()?65:result.valid()?125:70;
        var type=RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);var buffers=c.renderBuffers().bufferSource();
        var tint=new GhostConsumer(buffers.getBuffer(type),red,green,blue);
        MultiBufferSource source=ignored->tint;
        pose.pushPose();pose.translate(o.getX()-camera.x,o.getY()-camera.y,o.getZ()-camera.z);
        var blocks=GEOMETRY.computeIfAbsent(Rotation.values()[candidate.rotation()],r->BlueprintRegistry.lodge().blocks(r));
        for(var block:blocks){pose.pushPose();pose.translate(block.offset().getX(),block.offset().getY(),block.offset().getZ());
            c.getBlockRenderer().renderSingleBlock(block.state(),pose,source,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,net.neoforged.neoforge.client.model.data.ModelData.EMPTY,type);pose.popPose();}
        pose.popPose();buffers.endBatch(type);
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);var lines=buffers.getBuffer(RenderType.lines());
        var def=BlueprintRegistry.lodge();LevelRenderer.renderLineBox(pose,lines,o.getX(),o.getY()+.01,o.getZ(),o.getX()+def.width(Rotation.values()[rotation]),o.getY()+.08,o.getZ()+def.depth(Rotation.values()[rotation]),red/255f,green/255f,blue/255f,1);
        if(current())for(var p:result.collisions())LevelRenderer.renderLineBox(pose,lines,p.getX()-.01,p.getY()-.01,p.getZ()-.01,p.getX()+1.01,p.getY()+1.01,p.getZ()+1.01,1,.15f,.15f,1);
        if(current())for(var p:result.unsupported())LevelRenderer.renderLineBox(pose,lines,p.getX(),p.getY(),p.getZ(),p.getX()+1,p.getY()+1,p.getZ()+1,1,.55f,.1f,1);
        pose.popPose();buffers.endBatch(RenderType.lines());
    }
    private record GhostConsumer(VertexConsumer delegate,int red,int green,int blue) implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z){delegate.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){delegate.setColor(red,green,blue,95);return this;}
        public VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){delegate.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){delegate.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){delegate.setNormal(x,y,z);return this;}
    }
}
