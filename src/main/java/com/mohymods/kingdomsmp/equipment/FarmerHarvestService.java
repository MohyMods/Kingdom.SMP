package com.mohymods.kingdomsmp.equipment;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;
/** Powerful bounded ordinary player interactions: real drops, real seeds, normal claim/break/place hooks, no chunk tickets. */
public final class FarmerHarvestService {
 private record Work(ServerLevel level,ItemStack tool,ArrayDeque<BlockPos> cells){ }
 private static final Map<ServerPlayer,Work> WORK=new WeakHashMap<>();
 private FarmerHarvestService(){ }
 public static boolean start(ServerPlayer p,UseOnContext context,FarmerGear gear){
  if(context.getHand()!=InteractionHand.MAIN_HAND||p instanceof FakePlayer||p.isCreative()||p.isSpectator()||!p.isAlive()||p.server.getPlayerList().getPlayer(p.getUUID())!=p)return false;
  if(!p.serverLevel().hasChunkAt(context.getClickedPos())||p.distanceToSqr(Vec3.atCenterOf(context.getClickedPos()))>64)return false;
  var state=p.serverLevel().getBlockState(context.getClickedPos());if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state)||seed(crop)==null)return false;
  if(WORK.containsKey(p))return true;var cells=new ArrayDeque<BlockPos>();int radius=p.isShiftKeyDown()?0:gear.radius();var center=context.getClickedPos();for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++)cells.add(center.offset(dx,0,dz));WORK.put(p,new Work(p.serverLevel(),p.getMainHandItem(),cells));return true;
 }
 private static Item seed(Block crop){return crop==Blocks.WHEAT?Items.WHEAT_SEEDS:crop==Blocks.CARROTS?Items.CARROT:crop==Blocks.POTATOES?Items.POTATO:crop==Blocks.BEETROOTS?Items.BEETROOT_SEEDS:null;}
 public static void tick(ServerTickEvent.Post event){int global=128;for(var p:new ArrayList<>(WORK.keySet())){
  if(p.server!=event.getServer())continue;var work=WORK.get(p);if(p.server.getPlayerList().getPlayer(p.getUUID())!=p||!p.isAlive()||p.isCreative()||p.isSpectator()||p.serverLevel()!=work.level()||p.getMainHandItem()!=work.tool()||work.tool().isEmpty()){WORK.remove(p);continue;}
  for(int n=0;n<16&&global>0&&!work.cells().isEmpty();n++,global--){var pos=work.cells().removeFirst();var level=work.level();if(!level.hasChunkAt(pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>256)continue;var state=level.getBlockState(pos);if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state)||!level.getBlockState(pos.below()).is(Blocks.FARMLAND)||!CropBlock.hasSufficientLight(level,pos))continue;var seed=seed(crop);if(seed==null)continue;int seedSlot=-1;for(int slot=0;slot<36;slot++)if(ItemStack.isSameItemSameComponents(p.getInventory().getItem(slot),new ItemStack(seed))){seedSlot=slot;break;}if(seedSlot<0)continue;
   if(PlacementProtection.check(level,p,pos.below(),pos)!=null)continue;
   // destroyBlock invokes the normal claims/reservation events and Fortune loot using the actually held hoe.
   if(!p.gameMode.destroyBlock(pos)||!level.getBlockState(pos).isAir())continue;
   int selected=p.getInventory().selected;try{p.getInventory().selected=seedSlot;var stack=p.getMainHandItem();p.gameMode.useItemOn(p,level,stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false));}finally{p.getInventory().selected=selected;}
   work.tool().hurtAndBreak(1,p,EquipmentSlot.MAINHAND);
   if(!(level.getBlockState(pos).getBlock() instanceof CropBlock)){WORK.remove(p);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Harvest stopped: normal seed placement was refused. Remaining crops and seeds were left untouched."),false);break;}
   p.getInventory().setChanged();p.inventoryMenu.sendAllDataToRemote();if(work.tool().isEmpty())break;
  }
  if(work.cells().isEmpty()||work.tool().isEmpty())WORK.remove(p);if(global==0)break;
 }}
}
