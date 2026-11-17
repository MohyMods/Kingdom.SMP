package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.economy.CommerceService;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.ModList;
/** Exact optional binary boundary. Evidence failures must never escape native hive production/removal. */
public final class ApiaryProduction {
 public record Capture(BlockEntity hive,Bee bee,String type,int count){}
 private ApiaryProduction(){}
 public static boolean supported(){var mods=ModList.get();return mods!=null&&mods.getModContainerById("productivebees").map(c->c.getModInfo().getVersion().toString().equals("1.21.1-13.14.0")).orElse(false)&&mods.getModContainerById("productivelib").map(c->c.getModInfo().getVersion().toString().equals("1.21.1-0.2.0")).orElse(false);}
 @SuppressWarnings("unchecked") public static String species(net.minecraft.world.item.ItemStack stack){if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("productivebees:configurable_honeycomb"))return null;var component=(DataComponentType<ResourceLocation>)BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("productivebees:bee_type"));if(component==null)return null;var type=stack.get(component);return type==null?null:type.toString();}
 public static String beeType(Bee bee)throws ReflectiveOperationException{return bee.getClass().getMethod("getBeeType").invoke(bee).toString();}
 static int count(BlockEntity hive,String species){if(!(hive instanceof Container inventory)||inventory.getContainerSize()>128)return -1;int total=0;for(int slot=0;slot<inventory.getContainerSize();slot++){var stack=inventory.getItem(slot);if(species.equals(species(stack)))total=Math.addExact(total,stack.getCount());}return total;}
 public static Capture capture(BlockEntity hive,Bee bee,BeehiveBlockEntity.BeeReleaseStatus status){try{if(!supported()||status!=BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED||!(hive.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread()||level.dimension()!=net.minecraft.world.level.Level.OVERWORLD||CommerceService.initializedStore(level.getServer()).isEmpty()||!hive.getClass().getName().equals("cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntity")||level.getBlockEntity(hive.getBlockPos())!=hive)return null;String type=beeType(bee);if(!ApiaryEvidence.SPECIES.contains(type))return null;int before=count(hive,type);return before<0?null:new Capture(hive,bee,type,before);}catch(ReflectiveOperationException|RuntimeException e){failure(e);return null;}}
 public static void commit(Capture capture){if(capture==null)return;try{var hive=capture.hive();if(!(hive.getLevel() instanceof ServerLevel level)||level.getBlockEntity(hive.getBlockPos())!=hive)return;int produced=count(hive,capture.type())-capture.count();if(produced<1||produced>4096)return;var data=ApiarySavedData.get(level.getServer());data.evidence().record(new ApiaryEvidence.Site(level.dimension().location().toString(),hive.getBlockPos().asLong()),new ApiaryEvidence.Observation(capture.type(),capture.bee().getUUID(),level.getGameTime(),produced));data.setDirty();ApiaryJobs.produced(hive,capture.type(),produced);}catch(RuntimeException e){failure(e);}}
 public static void changed(LevelChunk chunk,BlockPos pos,BlockState before,BlockState after){try{if(before==null||before.getBlock()==after.getBlock()||!(chunk.getLevel() instanceof ServerLevel level)||!level.getServer().isSameThread()||level.dimension()!=net.minecraft.world.level.Level.OVERWORLD||!BuiltInRegistries.BLOCK.getKey(before.getBlock()).getNamespace().equals("productivebees")||CommerceService.initializedStore(level.getServer()).isEmpty())return;var data=ApiarySavedData.get(level.getServer());var site=new ApiaryEvidence.Site(level.dimension().location().toString(),pos.asLong());data.evidence().remove(site);data.setDirty();var jobs=ApiaryJobsSavedData.get(level.getServer());jobs.evidence().remove(site);jobs.setDirty();}catch(RuntimeException e){failure(e);}}
 private static void failure(Exception failure){com.mojang.logging.LogUtils.getLogger().error("Optional apiary evidence refused; native hive transaction continues",failure);}
}
