package com.mohymods.kingdomsmp.client;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.world.entity.npc.Villager;

/** Preserve the approved villager appearance while making construction swings visible. */
public final class AldricRenderer extends VillagerRenderer {
    public AldricRenderer(EntityRendererProvider.Context context){
        super(context);
        model=new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)){
            @Override public void setupAnim(Villager entity,float limbSwing,float limbAmount,float age,float yaw,float pitch){
                super.setupAnim(entity,limbSwing,limbAmount,age,yaw,pitch);
                var arms=root().getChild("arms");
                var rest=arms.getInitialPose();
                arms.xRot=rest.xRot-(float)Math.sin(attackTime*Math.PI)*0.65f;
                arms.zRot=rest.zRot+(float)Math.sin(attackTime*Math.PI)*0.12f;
            }
        };
    }
}
