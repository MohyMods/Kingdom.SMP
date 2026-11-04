package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.building.BuilderHutBlueprint;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.List;

/** One immutable GPU model mesh, with bounded periodic translucent-index sorting. */
final class BlueprintGhostMesh implements AutoCloseable {
    private final VertexBuffer vertices=new VertexBuffer(VertexBuffer.Usage.STATIC);
    private final ByteBufferBuilder indices=new ByteBufferBuilder(65536);
    private final MeshData.SortState sorting;
    private long sortedAt;
    private Vec3 sortedFrom;
    BlueprintGhostMesh(List<BuilderHutBlueprint.Placement> blocks,RenderType type){
        try(var memory=new ByteBufferBuilder(262144)){
            var builder=new BufferBuilder(memory,VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);
            var tint=new BlueprintPlacement.GhostConsumer(builder,255,255,255);MultiBufferSource source=ignored->tint;
            var pose=new PoseStack();var client=Minecraft.getInstance();
            for(var block:blocks)if(block.state().getRenderShape()==RenderShape.MODEL){
                pose.pushPose();pose.translate(block.offset().getX(),block.offset().getY(),block.offset().getZ());
                client.getBlockRenderer().renderSingleBlock(block.state(),pose,source,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,net.neoforged.neoforge.client.model.data.ModelData.EMPTY,type);pose.popPose();
            }
            var mesh=builder.buildOrThrow();sorting=mesh.sortQuads(indices,VertexSorting.DISTANCE_TO_ORIGIN);
            vertices.bind();try{vertices.upload(mesh);}finally{VertexBuffer.unbind();}indices.clear();
        }catch(RuntimeException|Error failure){vertices.close();indices.close();throw failure;}
    }
    void draw(RenderType type,Matrix4f modelView,Vec3 cameraLocal,int red,int green,int blue){
        type.setupRenderState();RenderSystem.setShaderColor(red/255f,green/255f,blue/255f,1);
        vertices.bind();
        try{
            long now=System.nanoTime();
            if(sorting!=null&&(sortedFrom==null||now-sortedAt>=200_000_000L&&cameraLocal.distanceToSqr(sortedFrom)>0.04)){
                var sorted=sorting.buildSortedIndexBuffer(indices,VertexSorting.byDistance((float)cameraLocal.x,(float)cameraLocal.y,(float)cameraLocal.z));
                if(sorted!=null)vertices.uploadIndexBuffer(sorted);
                indices.clear();sortedAt=now;sortedFrom=cameraLocal;
            }
            vertices.drawWithShader(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(modelView),RenderSystem.getProjectionMatrix(),RenderSystem.getShader());
        }finally{VertexBuffer.unbind();RenderSystem.setShaderColor(1,1,1,1);type.clearRenderState();}
    }
    @Override public void close(){vertices.close();indices.close();}
}
