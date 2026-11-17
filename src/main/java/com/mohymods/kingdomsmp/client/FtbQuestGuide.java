package com.mohymods.kingdomsmp.client;

import com.mohymods.kingdomsmp.integration.ftbquests.FtbQuestsIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Optional client boundary; FTB-linked code loads only after exact presence/version checks. */
public final class FtbQuestGuide {
    private FtbQuestGuide() { }
    public static boolean available(){
        var mod=ModList.get().getModContainerById("ftbquests");
        return mod.isPresent()&&FtbQuestsIntegration.VERIFIED_VERSION.equals(mod.orElseThrow().getModInfo().getVersion().toString())&&Verified.ready();
    }
    public static Component help(){
        return Component.literal(available()?"Open the kingdom's settlement guide and recorded milestones.":"The quest guide is unavailable or still loading. Refresh after the server guide loads; ask the owner if it remains unavailable.");
    }
    public static void open(){
        if(available())Verified.open();
        else if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(help(),true);
    }
    private static final class Verified {
        static boolean ready(){return dev.ftb.mods.ftbquests.client.FTBQuestsClient.isClientDataLoaded();}
        static void open(){
            var screen=dev.ftb.mods.ftbquests.client.ClientQuestFile.openGui();
            if(screen==null)return;
            var chapter=dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE.get(com.mohymods.kingdomsmp.integration.ftbquests.KingdomQuestGoal.CHAPTER_ID);
            if(chapter!=null)screen.open(chapter,true);
            // Royal entry opens a centered overview, including after a window/GUI-scale change.
            // FTB's own hotkey remains free to preserve its last panned view.
            screen.questPanel.resetScroll();
        }
    }
}
