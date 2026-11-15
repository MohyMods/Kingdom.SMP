package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.building.ConstructionService;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

/** Current functional civic installation, human-confirmed; no retrospective natural-source or placement credit. */
public final class SmithCommunityForge {
 public static final String QUEST="kingdom_smp:blacksmith/m07",FINAL="kingdom_smp:blacksmith/m08";
 public static boolean contains(String id){return QUEST.equals(id)||FINAL.equals(id);}
 private SmithCommunityForge() { }
 public static String scan(ServerPlayer player,BlockPos center){
  var level=player.serverLevel();var from=center.offset(-8,-3,-8);var to=center.offset(8,3,8);
  var area=new net.minecraft.world.phys.AABB(from.getX(),from.getY(),from.getZ(),to.getX()+1,to.getY()+1,to.getZ()+1);
  for(var project:ConstructionService.store(player.server).projects())if(project.origin()!=null&&ConstructionService.bounds(project).intersects(area))throw new IllegalStateException("The civic installation must be outside kingdom reward buildings and reserved construction sites");
  var hut=com.mohymods.kingdomsmp.persistence.BuilderHutSavedData.get(player.server).hut().orElse(null);
  if(hut!=null){var b=com.mohymods.kingdomsmp.building.BuilderHutBlueprint.create();var o=hut.origin();if(new net.minecraft.world.phys.AABB(o.getX(),o.getY()-4,o.getZ(),o.getX()+b.width(hut.rotation()),o.getY()+9,o.getZ()+b.depth(hut.rotation())).intersects(area))throw new IllegalStateException("The original Hut cannot substitute for the community forge");}
  var walk=new HashSet<BlockPos>();for(var cursor:BlockPos.betweenClosed(from,to)){
   var pos=cursor.immutable();if(!level.hasChunkAt(pos)||!level.hasChunkAt(pos.above(2))||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))throw new IllegalStateException("Visit a loaded civic site within world limits; inspection never loads chunks");
   if(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()&&level.getFluidState(pos).isEmpty()&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()&&level.getFluidState(pos.above()).isEmpty()&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))walk.add(pos);
  }
  if(!walk.contains(center))throw new IllegalStateException("Review from the supported clear civic entrance");
  var connected=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();connected.add(center);queue.add(center);while(!queue.isEmpty()){var pos=queue.removeFirst();for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){var next=pos.relative(direction).offset(0,dy,0);if(walk.contains(next)&&connected.add(next))queue.addLast(next);}}
  int furnaces=0,anvils=0,storage=0,water=0,lights=0,working=0;long digest=0xcbf29ce484222325L;
  for(var cursor:BlockPos.betweenClosed(from,to)){
   var pos=cursor.immutable();var state=level.getBlockState(pos);digest=(digest^pos.asLong())*0x100000001b3L;digest=(digest^Block.getId(state))*0x100000001b3L;
   boolean accessible=false;for(var side:Direction.Plane.HORIZONTAL)if(connected.contains(pos.relative(side)))accessible=true;if(!accessible)continue;
   var entity=level.getBlockEntity(pos);
   if(entity instanceof AbstractFurnaceBlockEntity furnace){furnaces++;if(state.hasProperty(BlockStateProperties.LIT)&&state.getValue(BlockStateProperties.LIT)&&((com.mohymods.kingdomsmp.mixin.NativeFurnaceRecipesAccessor)furnace).kingdom$litTime()>0&&(!furnace.getItem(0).isEmpty()||!furnace.getItem(2).isEmpty()))working++;}
   else if(entity instanceof Container container&&(state.is(Blocks.BARREL)||state.is(Blocks.CHEST))){storage++;for(int slot=0;slot<Math.min(128,container.getContainerSize());slot++){var stack=container.getItem(slot);digest=(digest^stack.getCount())*0x100000001b3L;if(!stack.isEmpty())digest=(digest^BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode())*0x100000001b3L;}}
   if(state.getBlock() instanceof AnvilBlock)anvils++;
   if(state.is(Blocks.WATER_CAULDRON)&&state.getValue(BlockStateProperties.LEVEL_CAULDRON)>0)water++;
   if(state.is(Blocks.LANTERN)||state.is(Blocks.TORCH)||state.is(Blocks.WALL_TORCH))lights++;
  }
  if(furnaces<2||working<1||anvils<1||storage<2||water<1||lights<1)throw new IllegalStateException("Civic forge needs connected safe access, two furnaces including one genuinely running native furnace, an anvil, two accessible storage containers, water and lighting");
  return "civicforge=f"+furnaces+",active="+working+",anvil="+anvils+",storage="+storage+",water="+water+",light="+lights+",snapshot="+Long.toUnsignedString(digest,16);
 }
}
