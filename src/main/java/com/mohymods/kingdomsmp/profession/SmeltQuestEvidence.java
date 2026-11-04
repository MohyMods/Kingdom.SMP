package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.mixin.*;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
/** Actual vanilla completed charcoal recipes and successful player collection, not stock possession/client counts. */
public final class SmeltQuestEvidence {
    private static final ResourceLocation CHARCOAL=ResourceLocation.withDefaultNamespace("charcoal");
    public record Proof(ServerPlayer player,AbstractFurnaceBlockEntity furnace,ProfessionRank band,int quantity) { }
    private SmeltQuestEvidence() { }
    static int batch(int taken,int remaining,int cooked){if(taken<1||taken>64||remaining<0||remaining>64||taken+remaining>64||cooked<1)return 0;return Math.min(cooked,taken+remaining);}
    public static Proof capture(Player player,Container container,ItemStack output,int taken){
        if(!(player instanceof ServerPlayer p))return null;
        try{
            if(!TreeQuestService.real(p)||!(container instanceof AbstractFurnaceBlockEntity furnace)||furnace.getLevel()!=p.serverLevel()||((NativeItemIdentityAccessor)(Object)output).kingdom$nativeItem()!=Items.CHARCOAL)return null;
            var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return null;var roster=store.professions(p.getUUID());var band=roster.progressBand(Profession.LUMBERJACK);
            if(!roster.isActive(Profession.LUMBERJACK)||band!=ProfessionRank.JOURNEYMAN||com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()<3||PlacementProtection.check(p.serverLevel(),p,furnace.getBlockPos(),furnace.getBlockPos())!=null)return null;
            var recipe=p.server.getRecipeManager().byKey(CHARCOAL).orElse(null);if(recipe==null||!(recipe.value() instanceof SmeltingRecipe smelt)||!smelt.getResultItem(p.registryAccess()).is(Items.CHARCOAL)||smelt.getIngredients().size()!=1||!smelt.getIngredients().getFirst().test(new ItemStack(Items.OAK_LOG))||smelt.getIngredients().getFirst().test(new ItemStack(Items.STONE)))return null;
            var remaining=furnace.getItem(2);if(!remaining.isEmpty()&&!remaining.is(Items.CHARCOAL))return null;
            int quantity=batch(taken,remaining.getCount(),((NativeFurnaceRecipesAccessor)furnace).kingdom$recipes().getInt(CHARCOAL));return quantity==0?null:new Proof(p,furnace,band,quantity);
        }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}
    }
    public static void commit(Proof proof){
        if(proof==null)return;var p=proof.player();try{
            // Vanilla must have consumed the actual native completed-recipe receipt. A second take cannot reuse it.
            if(!TreeQuestService.real(p)||proof.furnace().getLevel()!=p.serverLevel()||((NativeFurnaceRecipesAccessor)proof.furnace()).kingdom$recipes().getInt(CHARCOAL)!=0||PlacementProtection.check(p.serverLevel(),p,proof.furnace().getBlockPos(),proof.furnace().getBlockPos())!=null)return;
            var data=ProfessionEvidenceSavedData.get(p.server);if(data.evidence(Profession.LUMBERJACK).record(p.getUUID(),proof.band(),"smelt_charcoal",proof.quantity(),null))data.setDirty();
        }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}
    }
}
