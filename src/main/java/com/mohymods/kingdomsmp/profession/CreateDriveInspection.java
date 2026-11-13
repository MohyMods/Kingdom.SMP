package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
/** Read-only exact6.0.10 native generation/network/speed checks, within the shared loaded claim-approved site. */
public final class CreateDriveInspection {
 public static final String QUEST="kingdom_smp:engineer/a03";
 private CreateDriveInspection(){}
 public static String validate(ServerPlayer p,BlockPos center){
  if(!CreateCraftQuests.supported())throw new IllegalStateException("Requires verified Create6.0.10; existing records remain preserved");
  try{
   var kinetic=Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity");var wheelType=Class.forName("com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity");var level=p.serverLevel();List<String> drives=new ArrayList<>();
   for(var cursor:BlockPos.betweenClosed(center.offset(-8,-3,-8),center.offset(8,3,8))){var pos=cursor.immutable();if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals("create:water_wheel"))continue;var wheel=level.getBlockEntity(pos);if(!wheelType.isInstance(wheel))continue;
    float generated=((Number)kinetic.getMethod("getGeneratedSpeed").invoke(wheel)).floatValue(),speed=((Number)kinetic.getMethod("getSpeed").invoke(wheel)).floatValue();var network=kinetic.getField("network").get(wheel);
    if(!Float.isFinite(generated)||!Float.isFinite(speed)||generated==0||speed!=generated||network==null)continue;
    boolean water=false,shaft=false,access=false;
    for(var face:Direction.values()){
     var nearby=pos.relative(face);var state=level.getBlockState(nearby);if(!state.getFluidState().isEmpty()){var flow=(net.minecraft.world.phys.Vec3)wheelType.getMethod("getFlowVectorAtPosition",BlockPos.class).invoke(wheel,nearby);if(flow.lengthSqr()>0)water=true;}
     var entity=level.getBlockEntity(nearby);if(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("create:shaft")&&kinetic.isInstance(entity)&&network.equals(kinetic.getField("network").get(entity))&&((Number)kinetic.getMethod("getSpeed").invoke(entity)).floatValue()==speed)shaft=true;
     if(face.getAxis().isHorizontal()&&state.getCollisionShape(level,nearby).isEmpty()&&level.getBlockState(nearby.above()).getCollisionShape(level,nearby.above()).isEmpty()&&level.getBlockState(nearby.below()).isFaceSturdy(level,nearby.below(),Direction.UP))access=true;
    }
    if(water&&shaft&&access)drives.add(pos.asLong()+":"+Float.floatToIntBits(speed)+":"+network);
   }
   if(drives.isEmpty())throw new IllegalStateException("Operate an accessible native small waterwheel in real flowing water with an adjacent shaft turning on its actual network; furniture or idle/wrong-network shafts do not qualify");Collections.sort(drives);return "waterwheel="+drives.getFirst();
  }catch(ReflectiveOperationException failure){throw new IllegalStateException("Verified Create native API unavailable; nothing approved",failure);}
 }
}
