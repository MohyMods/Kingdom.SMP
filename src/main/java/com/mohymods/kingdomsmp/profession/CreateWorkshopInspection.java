package com.mohymods.kingdomsmp.profession;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
/** Sparse, read-only loaded workshop/campus assessment. Never loads or permanently tickets a chunk. */
public final class CreateWorkshopInspection {
 public static final String QUEST="kingdom_smp:engineer/a08";
 private static final Set<String> MACHINES=Set.of("create:water_wheel","create:mechanical_press","create:millstone","create:shaft","create:cogwheel","create:large_cogwheel","create:clutch");
 private CreateWorkshopInspection(){}
 private static boolean within(BlockPos p,BlockPos center){return Math.abs(p.getX()-center.getX())<=40&&Math.abs(p.getY()-center.getY())<=8&&Math.abs(p.getZ()-center.getZ())<=40;}
 private static String id(ServerPlayer p,BlockPos pos){return BuiltInRegistries.BLOCK.getKey(p.serverLevel().getBlockState(pos).getBlock()).toString();}
 private static boolean access(ServerPlayer p,BlockPos pos){var level=p.serverLevel();for(var face:Direction.Plane.HORIZONTAL)for(int dy=0;dy>=-2;dy--){var at=pos.relative(face).offset(0,dy,0);if(!level.hasChunkAt(at)||!level.hasChunkAt(at.below())||!level.hasChunkAt(at.above()))continue;var feet=level.getBlockState(at);if(feet.getCollisionShape(level,at).isEmpty()&&level.getBlockState(at.above()).getCollisionShape(level,at.above()).isEmpty()&&!feet.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)&&level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP)&&PlacementProtection.check(level,p,at.below(),at.above())==null)return true;}return false;}
 private static float speed(BlockEntity be)throws ReflectiveOperationException{return ((Number)be.getClass().getMethod("getSpeed").invoke(be)).floatValue();}
 private static boolean shutoff(ServerPlayer p,BlockEntity be,BlockPos center)throws ReflectiveOperationException{var level=p.serverLevel();var pos=be.getBlockPos();if(!level.getBlockState(pos).getValue(BlockStateProperties.POWERED)||!level.hasNeighborSignal(pos))return false;var source=(BlockPos)be.getClass().getField("source").get(be);if(source==null||pos.distManhattan(source)!=1||!within(source,center)||!level.hasChunkAt(source))return false;var sourceBe=level.getBlockEntity(source);if(sourceBe==null||!Float.isFinite(speed(sourceBe))||speed(sourceBe)==0||PlacementProtection.check(level,p,source,source)!=null)return false;var direction=Direction.fromDelta(pos.getX()-source.getX(),pos.getY()-source.getY(),pos.getZ()-source.getZ());if(direction==null)return false;var output=pos.relative(direction);if(!within(output,center)||!level.hasChunkAt(output)||!id(p,output).equals("create:shaft")||PlacementProtection.check(level,p,output,output)!=null)return false;var outBe=level.getBlockEntity(output);if(outBe==null||speed(outBe)!=0||((Number)be.getClass().getMethod("getRotationSpeedModifier",Direction.class).invoke(be,direction)).floatValue()!=0)return false;for(var face:Direction.values()){var at=pos.relative(face);if(within(at,center)&&level.hasChunkAt(at)&&id(p,at).equals("minecraft:lever")&&level.getBlockState(at).getValue(BlockStateProperties.POWERED)&&PlacementProtection.check(level,p,at,at)==null)return true;}return false;}
 public static String validate(ServerPlayer p,BlockPos center){if(!CreateCraftQuests.supported())throw new IllegalStateException("Requires verified Create6.0.10");try{
  var level=p.serverLevel();boolean wheel=false,press=false,mill=false,transmission=false,stop=false;List<String> snapshot=new ArrayList<>();int examined=0;
  for(int x=(center.getX()-40)>>4;x<=(center.getX()+40)>>4;x++)for(int z=(center.getZ()-40)>>4;z<=(center.getZ()+40)>>4;z++){
   var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;for(var be:chunk.getBlockEntities().values()){
    var pos=be.getBlockPos();if(!within(pos,center)||be.isRemoved()||!MACHINES.contains(id(p,pos)))continue;if(++examined>256)throw new IllegalStateException("Focus the workshop assessment within256loaded machines");if(level.getBlockEntity(pos)!=be||PlacementProtection.check(level,p,pos.below(2),pos.above())!=null||!access(p,pos))continue;
    float speed=speed(be);if(!Float.isFinite(speed)||speed==0)continue;var network=be.getClass().getField("network").get(be);if(!(network instanceof Long))continue;String id=id(p,pos);switch(id){case "create:water_wheel"->wheel|=((Number)be.getClass().getMethod("getGeneratedSpeed").invoke(be)).floatValue()==speed;case "create:mechanical_press"->press=true;case "create:millstone"->mill=true;case "create:shaft","create:cogwheel","create:large_cogwheel"->transmission=true;case "create:clutch"->stop|=shutoff(p,be,center);default->{}}
    var local=new StringBuilder();for(var nearby:BlockPos.betweenClosed(pos.offset(-1,-3,-1),pos.offset(1,2,1)))local.append(nearby.asLong()).append(':').append(level.hasChunkAt(nearby)?Block.getId(level.getBlockState(nearby)):-1).append(';');
    snapshot.add(id+":"+pos.asLong()+":"+Float.floatToIntBits(speed)+":"+network+":"+local);
   }
  }
  int lights=0,storage=0;for(var pos:BlockPos.betweenClosed(center.offset(-8,-3,-8),center.offset(8,3,8))){var state=level.getBlockState(pos);if(state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH)||state.is(Blocks.LANTERN))lights++;if((state.is(Blocks.BARREL)||state.is(Blocks.CHEST))&&access(p,pos))storage++;}
  if(!wheel||!press||!mill||!transmission||!stop||lights<1||storage<1)throw new IllegalStateException("Maintain loaded permitted water power, turning transmission, powered press/millstone, real stopped clutch output, safe maintenance access, light and accessible storage. Center on the work area; unloaded/foreign machines never qualify. Observed: power="+wheel+",press="+press+",mill="+mill+",transmission="+transmission+",stop="+stop+",lights="+lights+",storage="+storage);
  Collections.sort(snapshot);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("|",snapshot).getBytes(StandardCharsets.UTF_8))).substring(0,32);return "workshop="+hash+",storage="+storage+",light="+lights;
 }catch(ReflectiveOperationException|java.security.NoSuchAlgorithmException failure){throw new IllegalStateException("Verified native workshop API unavailable; nothing approved",failure);}}
}
