package com.mohymods.kingdomsmp.profession;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
/** Read-only native on/off demonstration; only the selected lever/clutch power bits may change after review. */
public final class CreateTransmissionInspection {
 public static final String QUEST="kingdom_smp:engineer/a04";
 public record Running(BlockPos wheel,BlockPos cogA,BlockPos cogB,BlockPos clutch,BlockPos lever,BlockPos input,BlockPos output,long network,float generated,float speedA,float speedB,float inputSpeed){
  public String digest(){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(toString().getBytes(StandardCharsets.UTF_8))).substring(0,32);}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
 }
 private record Node(BlockPos pos,BlockEntity entity,long network,float speed){}
 private CreateTransmissionInspection(){}
 private static String id(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();}
 private static Node node(ServerPlayer p,BlockPos pos)throws ReflectiveOperationException{
  var be=p.serverLevel().getBlockEntity(pos);if(be==null||!Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity").isInstance(be))return null;var network=be.getClass().getField("network").get(be);float speed=((Number)be.getClass().getMethod("getSpeed").invoke(be)).floatValue();return network instanceof Long n&&Float.isFinite(speed)&&speed!=0?new Node(pos.immutable(),be,n,speed):null;
 }
 private static boolean inside(BlockPos pos,BlockPos center){return Math.abs(pos.getX()-center.getX())<=8&&pos.getY()>=center.getY()-3&&pos.getY()<=center.getY()+3&&Math.abs(pos.getZ()-center.getZ())<=8;}
 private static boolean same(Node node,long network,float speed){return node!=null&&node.network()==network&&node.speed()==speed;}
 private static Direction.Axis axis(ServerPlayer p,BlockPos pos)throws ReflectiveOperationException{var state=p.serverLevel().getBlockState(pos);return (Direction.Axis)state.getBlock().getClass().getMethod("getRotationAxis",BlockState.class).invoke(state.getBlock(),state);}
 public static Running capture(ServerPlayer p,BlockPos center){
  if(!CreateCraftQuests.supported())throw new IllegalStateException("Requires verified Create6.0.10");try{
   var level=p.serverLevel();List<Node> wheels=new ArrayList<>(),cogs=new ArrayList<>(),clutches=new ArrayList<>();for(var cursor:BlockPos.betweenClosed(center.offset(-8,-3,-8),center.offset(8,3,8))){var pos=cursor.immutable();String id=id(level.getBlockState(pos));if(!Set.of("create:water_wheel","create:cogwheel","create:large_cogwheel","create:clutch").contains(id))continue;var node=node(p,pos);if(node==null)continue;switch(id){case "create:water_wheel"->wheels.add(node);case "create:clutch"->clutches.add(node);default->cogs.add(node);}if(wheels.size()+cogs.size()+clutches.size()>64)throw new IllegalStateException("Choose a focused inspection site with at most64transmission machines");}
   var connected=Class.forName("com.simibubi.create.content.kinetics.RotationPropagator").getMethod("isConnected",Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity"),Class.forName("com.simibubi.create.content.kinetics.base.KineticBlockEntity"));
   for(var wheel:wheels){float generated=((Number)wheel.entity().getClass().getMethod("getGeneratedSpeed").invoke(wheel.entity())).floatValue();if(generated==0||generated!=wheel.speed())continue;
    for(var a:cogs)for(var b:cogs){if(a.pos().asLong()>=b.pos().asLong()||a.network()!=wheel.network()||b.network()!=wheel.network()||a.pos().distSqr(b.pos())>5||axis(p,a.pos())!=axis(p,b.pos())||Math.signum(a.speed())==Math.signum(b.speed())||Math.abs(a.speed())==Math.abs(b.speed())||!(Boolean)connected.invoke(null,a.entity(),b.entity()))continue;
     for(var clutch:clutches){var state=level.getBlockState(clutch.pos());if(clutch.network()!=wheel.network()||state.getValue(BlockStateProperties.POWERED)||level.hasNeighborSignal(clutch.pos()))continue;var input=(BlockPos)clutch.entity().getClass().getField("source").get(clutch.entity());if(input==null||!inside(input,center)||clutch.pos().distManhattan(input)!=1)continue;var direction=Direction.fromDelta(clutch.pos().getX()-input.getX(),clutch.pos().getY()-input.getY(),clutch.pos().getZ()-input.getZ());if(direction==null)continue;var output=clutch.pos().relative(direction);if(!inside(output,center)||!id(level.getBlockState(output)).equals("create:shaft"))continue;var upstream=node(p,input);var downstream=node(p,output);if(upstream==null||upstream.network()!=wheel.network()||!same(downstream,wheel.network(),clutch.speed())||!clutch.pos().equals(downstream.entity().getClass().getField("source").get(downstream.entity())))continue;
      for(var face:Direction.values()){var lever=clutch.pos().relative(face);var leverState=level.getBlockState(lever);if(!inside(lever,center)||!id(leverState).equals("minecraft:lever")||leverState.getValue(BlockStateProperties.POWERED))continue;return new Running(wheel.pos(),a.pos(),b.pos(),clutch.pos(),lever,input,output,wheel.network(),generated,a.speed(),b.speed(),upstream.speed());}
     }
    }
   }
   throw new IllegalStateException("Run a real water-driven connected cog ratio/reversal and clutch-output shaft with an adjacent unpowered lever. Review while output turns; then switch that lever on to stop the output before confirming");
  }catch(ReflectiveOperationException failure){throw new IllegalStateException("Verified native transmission API unavailable; nothing approved",failure);}
 }
 public static BlockState snapshot(BlockPos pos,BlockState state,Running running){return running!=null&&(pos.equals(running.clutch())||pos.equals(running.lever()))&&state.hasProperty(BlockStateProperties.POWERED)?state.setValue(BlockStateProperties.POWERED,false):state;}
 public static void stopped(ServerPlayer p,Running running){try{
  var level=p.serverLevel();var clutchState=level.getBlockState(running.clutch());var lever=level.getBlockState(running.lever());if(!id(clutchState).equals("create:clutch")||!id(lever).equals("minecraft:lever")||!clutchState.getValue(BlockStateProperties.POWERED)||!lever.getValue(BlockStateProperties.POWERED)||!level.hasNeighborSignal(running.clutch()))throw new IllegalStateException("Turn on the reviewed lever to engage the native clutch shutoff, then confirm");
  var wheel=node(p,running.wheel());if(!same(wheel,running.network(),running.generated())||((Number)wheel.entity().getClass().getMethod("getGeneratedSpeed").invoke(wheel.entity())).floatValue()!=running.generated()||!same(node(p,running.cogA()),running.network(),running.speedA())||!same(node(p,running.cogB()),running.network(),running.speedB())||!same(node(p,running.input()),running.network(),running.inputSpeed()))throw new IllegalStateException("Keep the reviewed source, ratio/reversal and upstream power working; stopping the entire drive cannot demonstrate a controlled output shutoff");
  var output=level.getBlockEntity(running.output());var clutch=level.getBlockEntity(running.clutch());var direction=Direction.fromDelta(running.output().getX()-running.clutch().getX(),running.output().getY()-running.clutch().getY(),running.output().getZ()-running.clutch().getZ());if(output==null||clutch==null||!id(level.getBlockState(running.output())).equals("create:shaft")||direction==null||((Number)clutch.getClass().getMethod("getRotationSpeedModifier",Direction.class).invoke(clutch,direction)).floatValue()!=0||((Number)output.getClass().getMethod("getSpeed").invoke(output)).floatValue()!=0)throw new IllegalStateException("Wait for the actual selected output shaft to stop while its upstream power remains active");
 }catch(ReflectiveOperationException failure){throw new IllegalStateException("Verified native shutoff API unavailable; nothing approved",failure);}}
}
