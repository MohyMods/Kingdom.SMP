package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.io.IOException;
import java.util.*;

/** Server events produce bounded evidence; only a reviewed NPC interaction may commit an earned quest. */
public final class FarmerQuestService {
    private static final Map<MinecraftServer,ArrayDeque<Runnable>> PENDING=new WeakHashMap<>();
    private FarmerQuestService() { }
    public static void register(IEventBus bus){
        bus.addListener(EventPriority.LOWEST,false,BlockEvent.BlockToolModificationEvent.class,FarmerQuestService::till);
        bus.addListener(EventPriority.LOWEST,false,BlockEvent.EntityPlaceEvent.class,FarmerQuestService::plant);
        bus.addListener(EventPriority.LOWEST,false,BlockDropsEvent.class,FarmerQuestService::harvest);
        bus.addListener(EventPriority.LOWEST,false,BabyEntitySpawnEvent.class,FarmerQuestService::breed);
        bus.addListener(FarmerQuestService::grown);
        bus.addListener(FarmerQuestService::crafted);bus.addListener(FarmerQuestService::tick);
    }
    public static void initialize(MinecraftServer server){PENDING.remove(server);FarmerQuestRules.clear(server);FarmerSavedData.get(server);}
    private static boolean real(ServerPlayer p){return !(p instanceof FakePlayer)&&!p.isCreative()&&!p.isSpectator()&&p.isAlive()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
    /** A Master's trial band starts after16proofs, before the Master rank itself is earned. */
    public static ProfessionRank band(ProfessionRoster roster){return FarmerQuests.band(roster);}
    private static ProfessionRank eligible(ServerPlayer p){if(!real(p))return null;var s=CommerceService.initializedStore(p.server).orElse(null);if(s==null||!s.professions(p.getUUID()).isActive(Profession.FARMER))return null;return band(s.professions(p.getUUID()));}
    private static void later(MinecraftServer server,Runnable task){var q=PENDING.computeIfAbsent(server,k->new ArrayDeque<>());if(q.size()<8192)q.add(task);}
    private static void record(ServerPlayer p,ProfessionRank band,String metric,int amount,String identity){
        if(eligible(p)!=band)return;var data=FarmerSavedData.get(p.server);if(data.evidence().record(p.getUUID(),band,metric,amount,identity))data.setDirty();
    }
    private static boolean permitted(ServerPlayer p,ServerLevel level,BlockPos pos){return p.serverLevel()==level&&com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,pos,pos)==null;}
    private static String location(ServerLevel level,BlockPos pos){return level.dimension().location()+"/"+pos.asLong();}
    private static void till(BlockEvent.BlockToolModificationEvent event){
        if(event.isSimulated()||!(event.getPlayer() instanceof ServerPlayer p)||!(event.getLevel() instanceof ServerLevel level)||event.getItemAbility()!=net.neoforged.neoforge.common.ItemAbilities.HOE_TILL||event.getState().is(Blocks.FARMLAND))return;
        var band=eligible(p);if(band==null)return;var pos=event.getPos().immutable();
        later(p.server,()->{if(level.hasChunkAt(pos)&&level.getBlockState(pos).is(Blocks.FARMLAND)&&permitted(p,level,pos))record(p,band,"soil",1,location(level,pos));});
    }
    /** Only genuine age-zero planting creates harvest provenance, never placed mature blocks or administrative copies. */
    private static void plant(BlockEvent.EntityPlaceEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||!real(p)||!(event.getLevel() instanceof ServerLevel level))return;
        var state=event.getPlacedBlock();
        var pos=event.getPos().immutable();later(p.server,()->{if(!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(state))return;
            var data=FarmerSavedData.get(p.server);data.harvest(level.dimension().location().toString(),pos.asLong());
            boolean seed=state.getBlock() instanceof CropBlock crop&&crop.getAge(state)==0||state.getBlock() instanceof StemBlock&&state.getValue(StemBlock.AGE)==0;
            if(seed){var band=eligible(p);if(data.replant(level.dimension().location().toString(),pos.asLong(),p.getUUID(),band)&&band!=null)record(p,band,"replant",1,null);data.plant(new FarmerSavedData.Plant(level.dimension().location().toString(),pos.asLong(),BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),p.getUUID()));}});
    }
    private static void grown(net.neoforged.neoforge.event.level.block.CropGrowEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getOriginalState().getBlock() instanceof StemBlock)||!(event.getState().getBlock() instanceof AttachedStemBlock))return;
        var pos=event.getPos();if(!level.hasChunkAt(pos)||!level.getBlockState(pos).equals(event.getState()))return;var data=FarmerSavedData.get(level.getServer());var stem=data.provenance(level.dimension().location().toString(),pos.asLong()).orElse(null);if(stem==null||!stem.block().equals(BuiltInRegistries.BLOCK.getKey(event.getOriginalState().getBlock()).toString()))return;
        var fruit=pos.relative(event.getState().getValue(AttachedStemBlock.FACING));if(!level.hasChunkAt(fruit))return;var block=level.getBlockState(fruit).getBlock();if(block!=Blocks.MELON&&block!=Blocks.PUMPKIN)return;data.plant(new FarmerSavedData.Plant(level.dimension().location().toString(),fruit.asLong(),BuiltInRegistries.BLOCK.getKey(block).toString(),stem.planter()));
    }
    private static void harvest(BlockDropsEvent event){
        var block=event.getState().getBlock();boolean crop=block instanceof CropBlock,fruit=block==Blocks.MELON||block==Blocks.PUMPKIN,stem=block instanceof StemBlock||block instanceof AttachedStemBlock;if(!crop&&!fruit&&!stem)return;
        boolean mature=fruit||crop&&((CropBlock)block).isMaxAge(event.getState());
        var level=event.getLevel();var pos=event.getPos().immutable();var state=event.getState();
        ServerPlayer p=event.getBreaker() instanceof ServerPlayer player?player:null;var band=p==null?null:eligible(p);
        later(level.getServer(),()->{
            if(!level.hasChunkAt(pos)||level.getBlockState(pos).equals(state))return;
            var data=FarmerSavedData.get(level.getServer());var plant=data.harvest(level.dimension().location().toString(),pos.asLong()).orElse(null);
            if(!mature||p==null||band==null||plant==null||!permitted(p,level,pos)||!plant.block().equals(BuiltInRegistries.BLOCK.getKey(block).toString()))return;
            String metric=block==Blocks.WHEAT?"wheat":block==Blocks.CARROTS?"carrot":block==Blocks.POTATOES?"potato":block==Blocks.BEETROOTS?"beetroot":block==Blocks.MELON?"melon":block==Blocks.PUMPKIN?"pumpkin":null;
            if(metric!=null){record(p,band,metric,1,null);if(crop&&plant.planter().equals(p.getUUID()))data.awaitReplant(level.dimension().location().toString(),pos.asLong(),p.getUUID(),band);}
        });
    }
    private static void breed(BabyEntitySpawnEvent event){
        if(!(event.getCausedByPlayer() instanceof ServerPlayer p)||event.getChild()==null)return;var band=eligible(p);if(band==null)return;
        var child=event.getChild();String type=BuiltInRegistries.ENTITY_TYPE.getKey(child.getType()).toString();String metric=switch(type){case "minecraft:cow"->"cow";case "minecraft:sheep"->"sheep";case "minecraft:chicken"->"chicken";case "minecraft:pig"->"pig";default->null;};if(metric==null)return;
        later(p.server,()->{if(child.isAlive()&&!child.isRemoved()&&child.level() instanceof ServerLevel level&&level.getEntity(child.getUUID())==child&&permitted(p,level,child.blockPosition()))record(p,band,metric,1,child.getUUID().toString());});
    }
    private static void crafted(PlayerEvent.ItemCraftedEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;var band=eligible(p);if(band==null)return;var stack=event.getCrafting();String metric=stack.is(Items.BREAD)?"bread":stack.is(Items.CAKE)?"cake":null;
        if(metric!=null&&stack.getCount()>0&&stack.getCount()<=4096)record(p,band,metric,stack.getCount(),null);
    }
    private static void tick(ServerTickEvent.Post event){var q=PENDING.remove(event.getServer());if(q==null)return;while(!q.isEmpty())try{q.removeFirst().run();}catch(RuntimeException failure){LogUtils.getLogger().error("Farmer evidence event refused; no quest completion granted",failure);}}
    public static int count(ServerPlayer p,ProfessionRank band,String metric){return FarmerSavedData.get(p.server).evidence().count(p.getUUID(),band,metric);}
    public static String status(ServerPlayer p,String id){
        var d=FarmerQuests.catalog().definition(id).orElseThrow();var roster=CommerceService.store(p.server).professions(p.getUUID());if(roster.track(Profession.FARMER).completedQuestIds().contains(id))return "Complete. Your profession XP is recorded.";
        if(!roster.isActive(Profession.FARMER))return "Activate Farmer at its completed premises to earn personal progress.";
        if(!FarmerQuests.catalog().mayAttempt(Profession.FARMER,id,roster.track(Profession.FARMER).completedQuestIds()))return "Complete the previous objective band first.";
        String slot=id.substring(id.lastIndexOf('/')+1);var rules=FarmerQuestRules.get(p.server);
        return switch(slot){
            case "a01"->progress(p,d.rank(),"soil",rules.goal(slot))+" distinct cells tilled.";
            case "a02"->progress(p,d.rank(),"wheat",rules.goal(slot))+" mature wheat. Plant crops after enrollment and harvest them at maturity.";
            case "a03"->progress(p,d.rank(),"carrot",rules.goal(slot))+" mature carrots grown from planted crops.";
            case "a04"->progress(p,d.rank(),"potato",rules.goal(slot))+" mature potatoes grown from planted crops.";
            case "a05"->births(p,d.rank())+" / "+rules.goal(slot)+" unique farm offspring.";
            case "a06"->progress(p,d.rank(),"bread",rules.goal(slot))+" crafted bread. Complete Bread for the community to prove permanent consumption.";
            case "a07","j05","j06","m03"->"Deliver to the Farmer: "+FarmerDeliveryService.describe(FarmerDeliveryService.inputs(p,id))+". Food is consumed; empty containers are returned. Review before delivery.";
            case "a08","j08","m07","m08"->"Requires an administrator’s on-site approval of your working farm. Your farm must have crops, water, paths and appropriate storage.";
            case "j01"->progress(p,d.rank(),"beetroot",rules.goal(slot))+" beetroot; "+progress(p,d.rank(),"wheat",rules.goal(slot))+" wheat.";
            case "j02"->progress(p,d.rank(),"carrot",rules.goal(slot))+" carrots; "+progress(p,d.rank(),"potato",rules.goal(slot))+" potatoes.";
            case "j03"->Math.min(count(p,d.rank(),"melon")+count(p,d.rank(),"pumpkin"),rules.goal(slot))+" / "+rules.goal(slot)+" verified stem-grown melon/pumpkin blocks. Placed produce never grants credit.";
            case "j04","m02"->"Unique births in this band: cow "+count(p,d.rank(),"cow")+", sheep "+count(p,d.rank(),"sheep")+", chicken "+count(p,d.rank(),"chicken")+", pig "+count(p,d.rank(),"pig")+". "+(slot.equals("j04")?"Require "+rules.goal(slot)+" each cow, sheep and chicken.":"Require "+rules.goal(slot)+" total across all four species; at least "+rules.goal("m02_category")+" each.");
            case "j07","m04"->CommerceService.store(p.server).farmerContractPeriods(p.getUUID(),d.rank(),slot.equals("j07")?"daily":"weekly")+" / "+rules.goal(slot)+" distinct "+(slot.equals("j07")?"daily":"weekly")+" contracts completed during this objective band.";
            case "m05"->CommerceService.store(p.server).communityMasterProofs(p.getUUID())+" / "+rules.goal(slot)+" completed Bounty Hall food projects with at least10% personally supplied in this Master trial band. Tier3 and a completed Royal Bounty Hall required.";
            case "m01"->"This Master trial band: wheat "+count(p,d.rank(),"wheat")+", carrots "+count(p,d.rank(),"carrot")+", potatoes "+count(p,d.rank(),"potato")+", beetroot "+count(p,d.rank(),"beetroot")+". Need "+rules.goal(slot)+" total and "+rules.goal("m01_category")+" in each category.";
            case "m06"->progress(p,d.rank(),"replant",rules.goal(slot))+" personally planted seed-to-mature harvests followed by real seed replanting in this band.";
            default->throw new IllegalArgumentException("Unknown Farmer objective");
        };
    }
    private static String progress(ServerPlayer p,ProfessionRank band,String metric,int goal){return Math.min(count(p,band,metric),goal)+" / "+goal;}
    private static int births(ServerPlayer p,ProfessionRank band){return count(p,band,"cow")+count(p,band,"sheep")+count(p,band,"chicken")+count(p,band,"pig");}
    public static boolean ready(ServerPlayer p,String id){
        var roster=CommerceService.store(p.server).professions(p.getUUID());if(!roster.isActive(Profession.FARMER)||roster.track(Profession.FARMER).completedQuestIds().contains(id)||!FarmerQuests.catalog().mayAttempt(Profession.FARMER,id,roster.track(Profession.FARMER).completedQuestIds()))return false;
        var d=FarmerQuests.catalog().definition(id).orElseThrow();String slot=id.substring(id.lastIndexOf('/')+1);var rules=FarmerQuestRules.get(p.server);
        return switch(slot){
            case "a01"->count(p,d.rank(),"soil")>=rules.goal(slot);
            case "a02"->count(p,d.rank(),"wheat")>=rules.goal(slot);
            case "a03"->count(p,d.rank(),"carrot")>=rules.goal(slot);
            case "a04"->count(p,d.rank(),"potato")>=rules.goal(slot);
            case "a05"->births(p,d.rank())>=rules.goal(slot);
            case "a06"->count(p,d.rank(),"bread")>=rules.goal(slot)&&CommerceService.store(p.server).farmerFoodConsumed(p.getUUID(),ProfessionRank.APPRENTICE,"minecraft:bread")>=rules.goal(slot);
            case "j01"->count(p,d.rank(),"beetroot")>=rules.goal(slot)&&count(p,d.rank(),"wheat")>=rules.goal(slot);
            case "j02"->count(p,d.rank(),"carrot")>=rules.goal(slot)&&count(p,d.rank(),"potato")>=rules.goal(slot);
            case "j03"->count(p,d.rank(),"melon")+count(p,d.rank(),"pumpkin")>=rules.goal(slot);
            case "j04"->count(p,d.rank(),"cow")>=rules.goal(slot)&&count(p,d.rank(),"sheep")>=rules.goal(slot)&&count(p,d.rank(),"chicken")>=rules.goal(slot);
            case "j07","m04"->CommerceService.store(p.server).farmerContractPeriods(p.getUUID(),d.rank(),slot.equals("j07")?"daily":"weekly")>=rules.goal(slot);
            case "m01"->List.of("wheat","carrot","potato","beetroot").stream().mapToInt(metric->count(p,d.rank(),metric)).sum()>=rules.goal(slot)&&List.of("wheat","carrot","potato","beetroot").stream().allMatch(metric->count(p,d.rank(),metric)>=rules.goal("m01_category"));
            case "m02"->births(p,d.rank())>=rules.goal(slot)&&List.of("cow","sheep","chicken","pig").stream().allMatch(metric->count(p,d.rank(),metric)>=rules.goal("m02_category"));
            case "m05"->CommerceService.store(p.server).communityMasterProofs(p.getUUID())>=rules.goal(slot);
            case "m06"->count(p,d.rank(),"replant")>=rules.goal(slot);
            default->false;
        };
    }
    public static void complete(ServerPlayer p,UUID operation,String id)throws IOException{
        CommerceService.requireResidentCounter(p);if(!ProfessionService.available(p,Profession.FARMER)||!ready(p,id))throw new IllegalStateException("This objective is not proven. "+status(p,id));
        CommerceService.store(p.server).completeFarmerQuest(operation,p.getUUID(),id,status(p,id));
    }
}
