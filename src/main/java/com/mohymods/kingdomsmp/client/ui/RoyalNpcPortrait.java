package com.mohymods.kingdomsmp.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/** Reusable framed portrait without a world nameplate overlapping the model. */
public final class RoyalNpcPortrait {
    private RoyalNpcPortrait(){ }
    public static void render(GuiGraphics g,int x,int y,int w,int h,int scale,float mouseX,float mouseY,LivingEntity npc){
        RoyalPanel.frame(g,x,y,w,h);var name=npc.getCustomName();boolean visible=npc.isCustomNameVisible();
        try{npc.setCustomName(null);npc.setCustomNameVisible(false);InventoryScreen.renderEntityInInventoryFollowsMouse(g,x+2,y+2,x+w-2,y+h-2,scale,0.0625f,mouseX,mouseY,npc);}
        finally{npc.setCustomName(name);npc.setCustomNameVisible(visible);}
    }
}
