package com.mohymods.kingdomsmp.profession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.contribution.ContributionService;
public final class NativeGeology {
 public static final String QUEST="kingdom_smp:miner/j06";
 private NativeGeology(){}
 public static void worked(ServerPlayer p,BlockPos pos,String host){if(host==null||!TreeQuestService.real(p)||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>64)return;var r=CommerceService.store(p.server).professions(p.getUUID());if(!r.isActive(Profession.MINER)||r.progressBand(Profession.MINER)!=ProfessionRank.JOURNEYMAN||ContributionService.kingdom(p.server).attainedTier()<4)return;var data=NativeGeologySavedData.get(p.server);if(data.evidence().record(p.getUUID(),net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4),host))data.setDirty();}
 public static int count(ServerPlayer p){return NativeGeologySavedData.get(p.server).evidence().count(p.getUUID());}
 public static String status(ServerPlayer p){return count(p)+" / "+NativeOreRules.get(p.server).goal(QUEST)+" personally worked Overworld areas with different native stone settings. Extract tracked Journeyman redstone/lapis/gold/diamond ore in separate chunks originally generated in stone, deepslate, granite, diorite, andesite or tuff. Each setting and working chunk counts once. Placed neighboring terrain and unknown old ore hosts grant no geological history.";}
}
