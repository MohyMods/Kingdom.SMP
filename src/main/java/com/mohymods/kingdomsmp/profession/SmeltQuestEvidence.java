package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.mixin.*;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
/** Actual native completed recipes and successful player collection. Stock possession alone never supplies MAKE proof. */
public final class SmeltQuestEvidence {
    private static final ResourceLocation CHARCOAL=ResourceLocation.withDefaultNamespace("charcoal");
    public record Proof(ServerPlayer player,AbstractFurnaceBlockEntity furnace,Profession role,ProfessionRank band,String metric,int quantity,Set<ResourceLocation> recipes){public Proof{recipes=Set.copyOf(recipes);}}
    private SmeltQuestEvidence() { }
    static int batch(int taken,int remaining,int cooked){if(taken<1||taken>64||remaining<0||remaining>64||taken+remaining>64||cooked<1)return 0;return Math.min(cooked,taken+remaining);}
    private static boolean metalRecipe(ServerPlayer p,ResourceLocation id,Item metal){
        if(!id.getNamespace().equals("minecraft"))return false;var recipe=p.server.getRecipeManager().byKey(id).orElse(null);
        if(recipe==null||!(recipe.value() instanceof SmeltingRecipe||recipe.value() instanceof BlastingRecipe)||!(recipe.value() instanceof AbstractCookingRecipe cook)||!cook.getResultItem(p.registryAccess()).is(metal)||cook.getResultItem(p.registryAccess()).getCount()!=1||cook.getIngredients().size()!=1)return false;
        var inputs=cook.getIngredients().getFirst().getItems();return inputs.length>0&&inputs.length<=16&&Arrays.stream(inputs).allMatch(i->metal==Items.IRON_INGOT?(i.is(Items.RAW_IRON)||i.is(Items.IRON_ORE)||i.is(Items.DEEPSLATE_IRON_ORE)):(i.is(Items.RAW_GOLD)||i.is(Items.GOLD_ORE)||i.is(Items.DEEPSLATE_GOLD_ORE)));
    }
    public static Proof capture(Player player,Container container,ItemStack output,int taken){
        if(!(player instanceof ServerPlayer p))return null;
        try{
            if(!TreeQuestService.real(p)||!(container instanceof AbstractFurnaceBlockEntity furnace)||furnace.getLevel()!=p.serverLevel()||!p.serverLevel().hasChunkAt(furnace.getBlockPos())||p.serverLevel().getBlockEntity(furnace.getBlockPos())!=furnace)return null;
            var nativeItem=((NativeItemIdentityAccessor)(Object)output).kingdom$nativeItem();var role=nativeItem==Items.CHARCOAL?Profession.LUMBERJACK:nativeItem==Items.IRON_INGOT||nativeItem==Items.GOLD_INGOT?Profession.BLACKSMITH:null;if(role==null)return null;
            var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return null;var roster=store.professions(p.getUUID());var band=roster.progressBand(role);
            if(!roster.isActive(role)||role==Profession.LUMBERJACK&&band!=ProfessionRank.JOURNEYMAN||com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(p.server).attainedTier()<ApprovedProfessionContent.get(role.questId(band,1)).minimumTier()||PlacementProtection.check(p.serverLevel(),p,furnace.getBlockPos(),furnace.getBlockPos())!=null)return null;
            var remaining=furnace.getItem(2);if(!remaining.isEmpty()&&!remaining.is(nativeItem))return null;
            var history=((NativeFurnaceRecipesAccessor)furnace).kingdom$recipes();Set<ResourceLocation> recipes=new HashSet<>();long cooked=0;
            for(var entry:history.object2IntEntrySet()){
                boolean valid;if(role==Profession.LUMBERJACK){var recipe=p.server.getRecipeManager().byKey(entry.getKey()).orElse(null);valid=entry.getKey().equals(CHARCOAL)&&recipe!=null&&recipe.value() instanceof SmeltingRecipe smelt&&smelt.getResultItem(p.registryAccess()).is(Items.CHARCOAL)&&smelt.getResultItem(p.registryAccess()).getCount()==1&&smelt.getIngredients().size()==1&&smelt.getIngredients().getFirst().test(new ItemStack(Items.OAK_LOG))&&!smelt.getIngredients().getFirst().test(new ItemStack(Items.STONE));}else valid=metalRecipe(p,entry.getKey(),nativeItem);
                if(valid&&entry.getIntValue()>0){recipes.add(entry.getKey());cooked+=entry.getIntValue();}
            }
            int quantity=batch(taken,remaining.getCount(),(int)Math.min(64,cooked));return quantity==0?null:new Proof(p,furnace,role,band,role==Profession.LUMBERJACK?"smelt_charcoal":nativeItem==Items.GOLD_INGOT?"gold_ingots":"smelt_iron",quantity,recipes);
        }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);return null;}
    }
    public static void commit(Proof proof){
        if(proof==null)return;var p=proof.player();try{
            // Successful native result extraction clears the completed-recipe receipt exactly once.
            if(!TreeQuestService.real(p)||proof.furnace().getLevel()!=p.serverLevel()||!p.serverLevel().hasChunkAt(proof.furnace().getBlockPos())||p.serverLevel().getBlockEntity(proof.furnace().getBlockPos())!=proof.furnace()||proof.recipes().stream().anyMatch(id->((NativeFurnaceRecipesAccessor)proof.furnace()).kingdom$recipes().getInt(id)!=0)||PlacementProtection.check(p.serverLevel(),p,proof.furnace().getBlockPos(),proof.furnace().getBlockPos())!=null)return;
            var roster=CommerceService.store(p.server).professions(p.getUUID());if(!roster.isActive(proof.role())||roster.progressBand(proof.role())!=proof.band())return;
            var data=ProfessionEvidenceSavedData.get(p.server);if(data.evidence(proof.role()).record(p.getUUID(),proof.band(),proof.metric(),proof.quantity(),null))data.setDirty();
        }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}
    }
}
