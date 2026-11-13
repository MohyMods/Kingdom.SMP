package com.mohymods.kingdomsmp.profession;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModList;
/** Verified initial furnishings: actual storage, readable notice, lit sconce and anchored climbable rope. */
public final class SupplementariesInspection {
 public static final String QUEST="kingdom_smp:builder/m03";
 public static final Set<String> KINDS=Set.of("sack","notice_board","sconce","rope");
 private SupplementariesInspection(){}
 public static boolean supported(){var mods=ModList.get();return mods!=null&&mods.getModContainerById("supplementaries").map(c->c.getModInfo().getVersion().toString().equals("1.21.1-4.0.2")).orElse(false)&&mods.getModContainerById("moonlight").map(c->c.getModInfo().getVersion().toString().equals("1.21.1-3.7.2")).orElse(false);}
 static String kind(String id){return id.equals("supplementaries:sconce_wall")?"sconce":id.startsWith("supplementaries:")&&KINDS.contains(id.substring(16))?id.substring(16):null;}
 private static boolean usable(ServerLevel level,BlockPos pos,BlockPos center,Direction front){for(var side:Direction.Plane.HORIZONTAL)for(int dy=-2;dy<=1;dy++){if(front!=null&&front.getAxis().isHorizontal()&&side!=front)continue;var feet=pos.relative(side).offset(0,dy,0);if(Math.abs(feet.getX()-center.getX())>8||Math.abs(feet.getZ()-center.getZ())>8||feet.getY()<center.getY()-2||feet.getY()>center.getY()+2)continue;if(BuilderInspectionGeometry.clear(level,feet)&&level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP))return true;}return false;}
 private static boolean rope(ServerLevel level,BlockPos pos,BlockPos center){if(!level.getBlockState(pos).is(BlockTags.CLIMBABLE))return false;for(int dy=1;dy<=8&&pos.getY()+dy<=center.getY()+3;dy++){var above=pos.above(dy);var state=level.getBlockState(above);if(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals("supplementaries:rope"))continue;return state.isFaceSturdy(level,above,Direction.DOWN);}return false;}
 static String notice(ItemStack stack){if(stack.isEmpty()||stack.getCount()!=1)return null;var written=stack.get(DataComponents.WRITTEN_BOOK_CONTENT);var writable=stack.get(DataComponents.WRITABLE_BOOK_CONTENT);var text=new StringBuilder();if(written!=null){if(written.pages().size()>8)return null;for(var page:written.pages())text.append(page.raw().getString()).append('\n');}else if(writable!=null){if(writable.pages().size()>8)return null;for(var page:writable.pages())text.append(page.raw()).append('\n');}else return null;return readable(text.toString())?(written==null?"writable":written.author())+"|"+text:null;}
 static boolean readable(String text){return text!=null&&text.strip().length()>=12&&text.length()<=8192&&text.chars().filter(c->!Character.isWhitespace(c)).count()>=12;}
 public static String validate(ServerPlayer p,BlockPos center){if(!supported())throw new IllegalStateException("Requires verified Supplementaries1.21.1-4.0.2/Moonlight1.21.1-3.7.2; existing records stay preserved");for(String kind:KINDS)if(p.server.getRecipeManager().byKey(ResourceLocation.parse("supplementaries:"+kind)).isEmpty())throw new IllegalStateException("Enable accessible native "+kind+" recipe before this furnishing assessment; no retrospective substitution");var level=p.serverLevel();LayeredInteriorInspection.requirePlayerProject(p,center.offset(-8,-3,-8),center.offset(8,3,8));var verified=new TreeMap<String,String>();
  for(var cursor:BlockPos.betweenClosed(center.offset(-7,-2,-7),center.offset(7,2,7))){var pos=cursor.immutable();var state=level.getBlockState(pos);String kind=kind(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());Direction front=null;if("notice_board".equals(kind))for(var property:state.getProperties())if(property.getName().equals("facing")&&state.getValue(property) instanceof Direction d)front=d;if(kind==null||!state.canSurvive(level,pos)||!usable(level,pos,center,front))continue;String detail=null;var be=level.getBlockEntity(pos);
   if(kind.equals("sconce")&&state.getLightEmission(level,pos)>0)detail="lit";
   else if(kind.equals("rope")&&rope(level,pos,center))detail="anchored-climbable";
   else if(kind.equals("sack")&&be!=null&&be.getClass().getName().equals("net.mehvahdjukaar.supplementaries.common.block.tiles.SackBlockTile")&&be instanceof Container c&&c.getContainerSize()>0&&c.getContainerSize()<=54){var contents=new StringBuilder();boolean stored=false;for(int slot=0;slot<c.getContainerSize();slot++){var item=c.getItem(slot);if(!item.isEmpty()){stored=true;contents.append(slot).append(':').append(BuiltInRegistries.ITEM.getKey(item.getItem())).append('#').append(item.getCount()).append('|');}}if(stored)detail=contents.toString();}
   else if(kind.equals("notice_board")&&be!=null&&be.getClass().getName().equals("net.mehvahdjukaar.supplementaries.common.block.tiles.NoticeBoardBlockTile")&&be instanceof Container c&&c.getContainerSize()>0&&c.getContainerSize()<=4)detail=notice(c.getItem(0));
   if(detail!=null){String candidate=pos.asLong()+":"+detail;verified.merge(kind,candidate,(a,b)->a.compareTo(b)<=0?a:b);}
  }
  if(!verified.keySet().equals(KINDS))throw new IllegalStateException("Maintain four different working verified fittings: stocked accessible sack, readable book in notice board, lit supported sconce and anchored climbable rope; present="+verified.keySet());return "fittings=4,content="+digest(verified.toString());
 }
 private static String digest(String text){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,32);}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
