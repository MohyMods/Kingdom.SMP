package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Exact native world pressing after a real player's native single-item drop. No client item tags or simulated work. */
public final class CreatePressService {
 public static final String QUEST="kingdom_smp:engineer/a05",METRIC="pressed_sheets";
 public static final Map<String,String> OUTPUTS=Map.of("minecraft:iron_ingot","create:iron_sheet","minecraft:copper_ingot","create:copper_sheet","minecraft:gold_ingot","create:golden_sheet");
 public record Proof(ServerPlayer player,BlockEntity press,ItemEntity input,UUID identity,String output){}
 private CreatePressService(){}
 private static boolean eligible(ServerPlayer p){if(!CreateCraftQuests.supported()||!TreeQuestService.real(p)||ContributionService.kingdom(p.server).attainedTier()<4)return false;var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return false;var r=store.professions(p.getUUID());return r.isActive(Profession.ENGINEER)&&r.progressBand(Profession.ENGINEER)==ProfessionRank.APPRENTICE&&!r.track(Profession.ENGINEER).completedQuestIds().contains(QUEST);}
 private static boolean permitted(ServerPlayer p,BlockEntity press){var pos=press.getBlockPos();return p.serverLevel()==press.getLevel()&&p.serverLevel().hasChunkAt(pos)&&p.serverLevel().getBlockEntity(pos)==press&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=1024&&PlacementProtection.check(p.serverLevel(),p,pos.below(2),pos)==null;}
 public static Proof capture(BlockEntity press,ItemEntity input,boolean simulate){if(simulate||!(press.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread()||!(input.getOwner() instanceof ServerPlayer p))return null;try{
  var stack=input.getItem();String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),output=OUTPUTS.get(id);if(output==null||stack.getCount()!=1||!ItemStack.isSameItemSameComponents(stack,new ItemStack(stack.getItem()))||input.level()!=level||!input.isAlive()||!input.onGround()||!eligible(p)||!permitted(p,press)||!new net.minecraft.world.phys.AABB(press.getBlockPos().below()).intersects(input.getBoundingBox()))return null;
  if(!press.getClass().getName().equals("com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity"))return null;float speed=((Number)press.getClass().getMethod("getKineticSpeed").invoke(press)).floatValue();if(!Float.isFinite(speed)||speed==0)return null;
  var behaviour=press.getClass().getMethod("getPressingBehaviour").invoke(press);if(!behaviour.getClass().getField("running").getBoolean(behaviour)||!(Boolean)behaviour.getClass().getMethod("inWorld").invoke(behaviour))return null;
  var recipe=(Optional<?>)press.getClass().getMethod("getRecipe",ItemStack.class).invoke(press,stack);if(recipe.isEmpty()||!(recipe.get() instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder)||!holder.id().toString().equals("create:pressing/"+id.substring(id.indexOf(':')+1)))return null;
  return new Proof(p,press,input,input.getUUID(),output);
 }catch(ReflectiveOperationException|RuntimeException failure){TreeLifecycle.failure(level.getServer(),new IllegalStateException("Optional native press evidence unavailable",failure));return null;}}
 public static void commit(Proof proof,boolean success){if(proof==null||!success)return;var p=proof.player();try{
  var input=proof.input();var output=input.getItem();if(!eligible(p)||!permitted(p,proof.press())||!input.getUUID().equals(proof.identity())||!input.isAlive()||output.getCount()!=1||!BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals(proof.output())||!ItemStack.isSameItemSameComponents(output,new ItemStack(output.getItem())))return;
  var data=ProfessionEvidenceSavedData.get(p.server);if(data.evidence(Profession.ENGINEER).record(p.getUUID(),ProfessionRank.APPRENTICE,METRIC,1,"worldpress/"+proof.identity()))data.setDirty();
 }catch(RuntimeException failure){TreeLifecycle.failure(p.server,failure);}}
 public static int count(ServerPlayer p){return ProfessionEvidenceSavedData.get(p.server).evidence(Profession.ENGINEER).count(p.getUUID(),ProfessionRank.APPRENTICE,METRIC);}
 public static boolean ready(ServerPlayer p,int quantity){return eligible(p)&&count(p)>=quantity;}
 public static String status(ServerPlayer p,int quantity){return !CreateCraftQuests.supported()?"Requires verified Create6.0.10. Existing records remain preserved.":count(p)+" / "+quantity+" sheets from your single ingots dropped under a powered native press. Supervise within32blocks on permitted land, then permanently deliver plain iron/copper/gold sheets at Ivo. Old stock, simulation and belt jobs give no production proof.";}
}
