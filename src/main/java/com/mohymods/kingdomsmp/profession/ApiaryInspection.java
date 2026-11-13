package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.*;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;
/** Live flowering woodland plus proven native resource production; old stocked combs are not evidence. */
public final class ApiaryInspection {
 public static final String QUEST="kingdom_smp:lumberjack/m03";
 private ApiaryInspection(){}
 public static String validate(ServerPlayer p,BlockPos center){
  if(!ApiaryProduction.supported())throw new IllegalStateException("Requires verified Productive Bees1.21.1-13.14.0 and ProductiveLib1.21.1-0.2.0; no substitution");
  var level=p.serverLevel();if(level.dimension()!=net.minecraft.world.level.Level.OVERWORLD)throw new IllegalStateException("This verified pre-End apiary uses Overworld iron/copper bees");
  if(!level.getWorldBorder().isWithinBounds(center.offset(-8,0,-8))||!level.getWorldBorder().isWithinBounds(center.offset(8,0,8)))throw new IllegalStateException("Keep the documented apiary inside the currently unlocked kingdom border");
  for(var species:List.of("iron","copper"))if(p.server.getRecipeManager().byKey(ResourceLocation.parse("productivebees:bee_breeding/raw_materials/"+species+"_bee")).isEmpty())throw new IllegalStateException("Enable the actual native pre-End "+species+" bee breeding recipe first");
  LayeredInteriorInspection.requirePlayerProject(p,center.offset(-8,-3,-8),center.offset(8,3,8));int flowers=0,logs=0,leaves=0;Set<String> forage=new HashSet<>();var proofs=new TreeMap<String,String>();var data=ApiarySavedData.get(p.server).evidence();
  for(var cursor:BlockPos.betweenClosed(center.offset(-8,-3,-8),center.offset(8,3,8))){var pos=cursor.immutable();var state=level.getBlockState(pos);if(state.is(BlockTags.FLOWERS)&&state.canSurvive(level,pos))flowers++;if(state.is(BlockTags.LOGS))logs++;if(state.is(BlockTags.LEAVES))leaves++;
   for(var type:List.of("ferric","cupric"))if(state.is(TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.parse("productivebees:flowers/"+type))))forage.add(type);
   if(!(level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive)||!hive.getClass().getName().equals("cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntity")||!state.hasProperty(BeehiveBlock.FACING))continue;
   var front=pos.relative(state.getValue(BeehiveBlock.FACING));if(!level.getBlockState(front).getCollisionShape(level,front).isEmpty()||!level.getBlockState(front.above()).getCollisionShape(level,front.above()).isEmpty())continue;
   Set<String> present=new HashSet<>();var saved=hive.saveWithoutMetadata(level.registryAccess()).getList("bees",net.minecraft.nbt.Tag.TAG_COMPOUND);if(saved.size()>32)throw new IllegalStateException("Apiary occupant record exceeds verified inspection bounds");for(var value:saved){var nativeBee=((net.minecraft.nbt.CompoundTag)value).getCompound("entity_data");String type=nativeBee.getString("type");if(nativeBee.getString("id").equals("productivebees:configurable_bee")&&ApiaryEvidence.SPECIES.contains(type))present.add(type);}
   for(var bee:level.getEntitiesOfClass(Bee.class,new AABB(pos).inflate(8),b->b.isAlive()&&!b.isBaby()&&pos.equals(b.getHivePos())))try{String type=ApiaryProduction.beeType(bee);if(ApiaryEvidence.SPECIES.contains(type))present.add(type);}catch(ReflectiveOperationException invalid){/* Unknown native species cannot qualify. */}
   for(var observation:data.at(new ApiaryEvidence.Site(level.dimension().location().toString(),pos.asLong())).values()){long age=level.getGameTime()-observation.time();if(!present.contains(observation.species())||age<0||age>24000)continue;String proof=pos.asLong()+":"+observation.bee()+":"+observation.time()+":"+observation.produced();proofs.merge(observation.species(),proof,(a,b)->a.compareTo(b)<0?a:b);}
  }
  if(flowers<1||logs<1||leaves<1||forage.size()!=2||!proofs.keySet().equals(ApiaryEvidence.SPECIES))throw new IllegalStateException("Maintain flowering woodland with native iron/copper forage and accessible occupied hives. Both permitted species must actually produce resource combs here within one game day; stored combs or untracked old apiaries do not prove operation. flowers="+flowers+",logs="+logs+",leaves="+leaves+",forage="+forage+",producing="+proofs.keySet());
  try{return "apiary="+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(proofs.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),0,16)+",flowers="+flowers+",woodland="+logs+"/"+leaves;}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
}
