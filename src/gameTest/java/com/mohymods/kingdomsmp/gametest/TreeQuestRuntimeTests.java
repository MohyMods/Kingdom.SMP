package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_tree_tests") @PrefixGameTestTemplate(false)
public final class TreeQuestRuntimeTests {
    @GameTest(template="empty",timeoutTicks=6000)
    public static void actualNativeTreesHarvestReplacementAndReviewedXp(GameTestHelper h)throws Exception{
        h.getLevel().getServer().tickRateManager().setTickRate(200); // Explicit private QA clock acceleration, no goal/rank/XP changes.
        // Only this generated private forest fixture: keep settlement sites clear before their authenticated construction.
        for(var pos:BlockPos.betweenClosed(new BlockPos(-10,-63,-10),new BlockPos(60,-30,100)))if(!h.getLevel().getBlockState(pos).isAir())h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),18);
        CommerceRuntimeTests.completePurchasedProfessionLoop(h,Profession.LUMBERJACK.buildingId(),()->{try{new Walk(h).start();}catch(Exception e){h.fail(e.toString());}});
    }
    static final class Walk {
        final GameTestHelper h;final net.minecraft.server.level.ServerLevel level;ServerPlayer p;final EconomyStore store;final ConstructionStore.Project lodge;final BlockPos plot;
        int species,trees;String[] names={"oak","birch","spruce"};Item[] saplings={Items.OAK_SAPLING,Items.BIRCH_SAPLING,Items.SPRUCE_SAPLING};boolean cancel=true;int beforeGrowth;
        Walk(GameTestHelper h){this.h=h;level=h.getLevel();var server=level.getServer();lodge=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().toString().equals(Profession.LUMBERJACK.buildingId())).findFirst().orElseThrow();p=server.getPlayerList().getPlayer(lodge.owner());store=CommerceService.store(server);plot=lodge.origin().offset(35,0,0);}
        void near(){var home=lodge.origin().offset(BlueprintRegistry.get(lodge.blueprint(),lodge.blueprintVersion()).home());p.moveTo(home.getX()+.5,home.getY(),home.getZ()-1.5);CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)level.getEntity(lodge.npc()));}
        void start()throws Exception{
            near();var enrollment=ProfessionService.review(p,"learn:lumberjack");ProfessionService.confirm(p,enrollment.nonce());h.assertTrue(!p.hasPermissions(2),"Native forestry actor is an ordinary survival player");
            TreeLifecycle.drain(level.getServer(),8192);var natural=TreeSavedData.get(level.getServer()).evidence().sampleNatural(128).entrySet().stream().filter(e->level.hasChunkAt(BlockPos.of(e.getKey().position()))&&level.getBlockState(BlockPos.of(e.getKey().position())).toString().equals(e.getValue().state())).findFirst().orElseThrow(()->new IllegalStateException("No real generated native forest provenance"));
            var naturalPos=BlockPos.of(natural.getKey().position());int old=count(natural.getValue().species());move(naturalPos);h.assertTrue(p.gameMode.destroyBlock(naturalPos),"Actual natural worldgen log is harvested by player");h.assertTrue(count(natural.getValue().species())==old+1,"Real generated tree callback supplies actual harvest proof");
            for(var pos:BlockPos.betweenClosed(plot.offset(-9,0,-9),plot.offset(9,24,9)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            for(var pos:BlockPos.betweenClosed(plot.offset(-10,-1,-10),plot.offset(10,-1,10)))level.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),3);
            move(plot);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.OAK_LOG));use(plot.below(),Direction.UP);h.assertTrue(p.getMainHandItem().isEmpty(),"Placed construction log consumes actual item");int oak=count("oak");h.assertTrue(p.gameMode.destroyBlock(plot),"Player removes placed log");h.assertTrue(count("oak")==oak,"Placed construction logs never become forestry credit");
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,false,net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent.class,event->{if(cancel&&event.getLevel()==level&&Math.abs(event.getPos().getX()-plot.getX())<2&&Math.abs(event.getPos().getZ()-plot.getZ())<2)event.setCanceled(true);});
            plant();
        }
        int count(String metric){return TreeSavedData.get(level.getServer()).evidence().evidence().count(p.getUUID(),ProfessionRank.APPRENTICE,metric);}
        void move(BlockPos pos){p.moveTo(pos.getX()+2.5,pos.getY()+1,pos.getZ()+2.5);}
        void use(BlockPos clicked,Direction face){p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),face,clicked,false));}
        void plant(){move(plot);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(saplings[species]));use(plot.below(),Direction.UP);h.assertTrue(p.getMainHandItem().isEmpty()&&level.getBlockState(plot).getBlock() instanceof SaplingBlock,"Real native sapling placement consumes one actual item");h.runAfterDelay(2,()->guard(this::grow));}
        void grow(){
            beforeGrowth=count("grown");move(plot);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL,64));
            for(int n=0;n<64&&level.getBlockState(plot).getBlock() instanceof SaplingBlock;n++){int before=p.getMainHandItem().getCount();use(plot,Direction.UP);if(cancel&&p.getMainHandItem().getCount()==before&&level.getBlockState(plot).getBlock() instanceof SaplingBlock){cancel=false;h.runAfterDelay(2,()->guard(()->{h.assertTrue(count("grown")==beforeGrowth,"Canceled native feature cannot grant replacement recognition");grow();}));return;}}
            h.assertTrue(TreeLifecycle.species(level.getBlockState(plot))!=null,"Actual native growth creates mature trunk");h.runAfterDelay(3,()->guard(this::harvest));
        }
        void harvest(){
            var evidence=TreeSavedData.get(level.getServer()).evidence();int before=count(names[species]),logs=0;
            for(var pos:BlockPos.betweenClosed(plot.offset(-8,0,-8),plot.offset(8,24,8))){if(TreeLifecycle.species(level.getBlockState(pos))==null)continue;var target=pos.immutable();h.assertTrue(evidence.logAt(level.dimension().location().toString(),target).isPresent(),"Every grown trunk has actual native provenance after transaction confirmation");move(target);h.assertTrue(p.gameMode.destroyBlock(target),"Actual player harvest consumes a real grown trunk block");logs++;}
            h.assertTrue(logs>0&&count(names[species])==before+logs,"Actual personally harvested species count equals real native log blocks");trees++;
            TreeLifecycle.auditGrowth(level.getServer());
            for(var pos:BlockPos.betweenClosed(plot.offset(-8,0,-8),plot.offset(8,24,8)))if(!level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            if(count("oak")>=128&&species==0)species++;if(count("birch")>=64&&species==1)species++;if(count("spruce")>=64&&species==2&&count("grown")>=24){finish();return;}
            if(trees>120)throw new IllegalStateException("Native forestry loop exceeded bounded fixture attempts");plant();
        }
        void finish(){
            move(plot);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SPRUCE_SAPLING));use(plot.below(),Direction.UP);
            h.assertTrue(p.getMainHandItem().isEmpty(),"Real replacement sapling planted before owner logout");h.runAfterDelay(2,()->guard(this::offlineGrowth));
        }
        void offlineGrowth(){
            int before=count("grown");var server=level.getServer();var owner=p.getUUID();String name=p.getGameProfile().getName();server.getPlayerList().remove(p);
            for(int n=0;n<4&&level.getBlockState(plot).getBlock() instanceof SaplingBlock;n++)((SaplingBlock)level.getBlockState(plot).getBlock()).advanceTree(level,plot,level.getBlockState(plot),level.getRandom());
            h.assertTrue(TreeLifecycle.species(level.getBlockState(plot))!=null&&server.getPlayerList().getPlayer(owner)==null,"Actual native random-tick growth occurs while original planter is offline");
            h.assertTrue(TreeSavedData.get(server).evidence().growth().stream().anyMatch(g->g.player().equals(owner)),"Offline owner recognition persists as an unclaimed permission-audit receipt");
            try{
                var other=server.getPlayerList().getPlayers().stream().filter(v->!v.getUUID().equals(owner)).findFirst().orElseThrow();ClaimRuntimeTests.ftbUnclaim(level,plot);ClaimRuntimeTests.ftbClaim(other,plot);
                p=ClaimRuntimeTests.vanillaPlayer(level,owner,name);TreeLifecycle.auditGrowth(server);h.assertTrue(count("grown")==before,"Revoked plot consent after offline growth cannot grant recognition on reconnect");
                ClaimRuntimeTests.ftbUnclaim(level,plot);ClaimRuntimeTests.ftbClaim(p,plot);TreeLifecycle.auditGrowth(server);h.assertTrue(count("grown")==before+1,"Original planter reconnects and verifies exactly one offline replacement after current consent returns");
            }catch(Exception failure){throw new IllegalStateException(failure);}
            piston();
        }
        void piston(){
            var target=plot.above(2);h.assertTrue(level.getBlockState(target).is(Blocks.SPRUCE_LOG),"Actual grown trunk supplies the piston provenance fixture");var piston=target.east();level.setBlock(piston,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.WEST),3);level.setBlock(piston.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
            h.runAfterDelay(4,()->guard(()->{
                h.assertTrue(level.getBlockState(target.west()).is(Blocks.SPRUCE_LOG),"Real native piston moves the grown trunk");var data=TreeSavedData.get(level.getServer()).evidence();String dim=level.dimension().location().toString();
                h.assertTrue(data.logAt(dim,target).isEmpty()&&data.logAt(dim,target.west()).isEmpty(),"Successful piston invalidates both source and destination; no tree credit transfers to moved construction stock");int before=count("spruce");move(target.west());h.assertTrue(p.gameMode.destroyBlock(target.west()),"Native player harvest removes moved stock");h.assertTrue(count("spruce")==before,"Moved native timber never counts as an in-place sustainable harvest");awards();
            }));
        }
        void awards(){
            near();for(int n=1;n<=4;n++){String id=Profession.LUMBERJACK.questId(ProfessionRank.APPRENTICE,n);h.assertTrue(TreeQuestService.ready(p,id),"Approved full native tree goal is ready: "+id);var view=CommerceService.handle(p,new CommerceRequest(30000+n*2,1,7,"quest:"+id,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal packet reviews earned native tree objective");CommerceService.handle(p,new CommerceRequest(30001+n*2,2,7,"quest:"+id,1,view.review()));}
            h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==400&&store.professions(p.getUUID()).rank(Profession.LUMBERJACK,ProfessionService.quests())==ProfessionRank.APPRENTICE,"Four real proofs award400XPonce and do not fabricate the other four rank requirements");
            timber();
        }
        void timber(){
            near();boolean denied=false;try{ProfessionService.review(p,"materials:"+CraftDeliveryQuests.TIMBER);}catch(Exception expected){denied=true;}h.assertTrue(denied,"Harvesting alone cannot substitute for native crafted/stripped timber production");
            var table=plot.offset(14,0,0);for(var pos:BlockPos.betweenClosed(table.offset(-1,0,-1),table.offset(10,2,5)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            for(var pos:BlockPos.betweenClosed(table.offset(-1,-1,-1),table.offset(10,-1,5)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);move(table);p.getInventory().clearContent();
            var menu=new net.minecraft.world.inventory.CraftingMenu(42,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,table));p.containerMenu=menu;
            for(int i=0;i<32;i++){menu.getSlot(1).set(new ItemStack(i<16?Items.OAK_LOG:Items.BIRCH_LOG));h.assertTrue(menu.getSlot(0).getItem().is(i<16?Items.OAK_PLANKS:Items.BIRCH_PLANKS),"Actual native mixed-species plank recipe");menu.quickMoveStack(p,0);}p.closeContainer();
            h.assertTrue(CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.APPRENTICE,"planks")==128,"Actual recipes produce and attribute128planks across two species");
            h.assertTrue(p.getInventory().getItem(0).isEmpty(),"Reserved test hand is empty; actual crafted plank stacks remain intact");p.getInventory().selected=0;var axe=new ItemStack(Items.IRON_AXE);
            for(int i=0;i<32;i++){
                var pos=table.offset(2+i%8,0,1+i/8);move(pos);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(i<16?Items.OAK_LOG:Items.BIRCH_LOG));use(pos.below(),Direction.UP);h.assertTrue(p.getMainHandItem().isEmpty(),"Real supplied stock log placed before stripping");p.setItemInHand(InteractionHand.MAIN_HAND,axe);use(pos,Direction.SOUTH);h.assertTrue(level.getBlockState(pos).is(i<16?Blocks.STRIPPED_OAK_LOG:Blocks.STRIPPED_BIRCH_LOG),"Native axe strips actual mixed timber");
            }
            h.assertTrue(axe.getDamageValue()==32,"All32native strip actions consume real durability");h.runAfterDelay(3,()->guard(()->collectTimber(table)));
        }
        void collectTimber(BlockPos table){
            h.assertTrue(CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.APPRENTICE,"stripped_logs")==32,"Each native transformation is credited after actual successful state change");
            for(int i=0;i<32;i++){var pos=table.offset(2+i%8,0,1+i/8);move(pos);h.assertTrue(p.gameMode.destroyBlock(pos),"Native stripping outputs are harvested as actual items");}
            h.runAfterDelay(3,()->guard(()->collectTimberItems(table)));
        }
        void collectTimberItems(BlockPos table){
            for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(table.offset(1,0,0)),net.minecraft.world.phys.Vec3.atLowerCornerOf(table.offset(11,3,6))))){item.setNoPickUpDelay();item.playerTouch(p);}
            near();try{
                h.assertTrue(p.getInventory().countItem(Items.STRIPPED_OAK_LOG)==16&&p.getInventory().countItem(Items.STRIPPED_BIRCH_LOG)==16,"Actual native dropped outputs enter the inventory after entity registration");
                var view=CommerceService.handle(p,new CommerceRequest(40001,1,7,"materials:"+CraftDeliveryQuests.TIMBER,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal UI reviews mixed plain timber categories");CommerceService.handle(p,new CommerceRequest(40002,2,7,"materials:"+CraftDeliveryQuests.TIMBER,1,view.review()));
                h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==500,"Exact128mixedplanks/32mixedstrippedlogs consumed atomically with100XPonce");
                boolean denied=false;try{ProfessionService.review(p,"materials:"+CraftDeliveryQuests.TIMBER);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Mixed-category delivery cannot redeem twice");
                var config=level.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commission-quests.json");CommissionQuestRules.load(config);java.nio.file.Files.writeString(config,java.nio.file.Files.readString(config).replace(": 3",": 1"));CommissionQuestRules.clear(level.getServer());
                p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,32));var contract=ContractService.review(p,"lumberjack/daily");ContractService.confirm(p,contract.nonce());var commission=ProfessionService.review(p,"quest:kingdom_smp:lumberjack/a07");ProfessionService.confirm(p,commission.nonce());
                woodland();
            }catch(Exception failure){throw new IllegalStateException(failure);}
        }
        void woodland()throws Exception{
            for(int i=-4;i<4;i++){var pos=plot.offset(i,-1,-4);level.setBlock(pos,Blocks.DIRT_PATH.defaultBlockState(),3);level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),18);level.setBlock(pos.above(2),Blocks.AIR.defaultBlockState(),18);}
            for(var pos:List.of(plot.east(4),plot.west(4))){level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(pos,Blocks.TORCH.defaultBlockState(),3);}var sapling=plot.south(4);level.setBlock(sapling.below(),Blocks.DIRT.defaultBlockState(),3);level.setBlock(sapling,Blocks.OAK_SAPLING.defaultBlockState(),3);
            var admin=level.getServer().getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseThrow();admin.moveTo(plot.getX()+3.5,plot.getY(),plot.getZ()+5.5);p.moveTo(plot.getX()+4.5,plot.getY(),plot.getZ()+5.5);level.getServer().getPlayerList().op(admin.getGameProfile());
            var review=ProfessionInspectionService.review(admin,p,"kingdom_smp:lumberjack/a06",plot,"Verified safe marked forest route consent and replacement saplings");ProfessionInspectionService.confirm(admin,review);
            h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==700&&store.professions(p.getUUID()).rank(Profession.LUMBERJACK,ProfessionService.quests())==ProfessionRank.APPRENTICE,"Seven earned objectives cannot bypass final woodland assessment");
            review=ProfessionInspectionService.review(admin,p,"kingdom_smp:lumberjack/a08",plot,"Verified native species harvest history and real growing replacements");ProfessionInspectionService.confirm(admin,review);level.getServer().getPlayerList().deop(admin.getGameProfile());
            h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==800&&store.professions(p.getUUID()).track(Profession.LUMBERJACK).completedQuestIds().size()==8&&store.professions(p.getUUID()).rank(Profession.LUMBERJACK,ProfessionService.quests())==ProfessionRank.JOURNEYMAN,"All8actual Lumberjack Apprentice objectives earn800XP/Journeymanonce");
            advancedCommission();
        }
        void advancedCommission()throws Exception{
            final String advanced="kingdom_smp:lumberjack/j07";
            h.assertTrue(!ProfessionQuestService.ready(p,advanced),"Earned Journeyman rank at Tier2 cannot bypass approved Tier3 advanced objectives");
            var hall=ConstructionService.lodge(level.getServer());var home=hall.origin().offset(BlueprintRegistry.get(hall.blueprint(),hall.blueprintVersion()).home());p.moveTo(home.getX()+.5,home.getY(),home.getZ()-1.5);CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)level.getEntity(hall.npc()));
            for(var resource:com.mohymods.kingdomsmp.progression.TierTwoService.Resource.values()){
                int remaining=com.mohymods.kingdomsmp.progression.TierTwoService.ledger(level.getServer()).requirements().get(resource.id);
                while(remaining>0){int batch=Math.min(2304,remaining);p.getInventory().clearContent();for(int slot=0,left=batch;left>0;slot++){int count=Math.min(64,left);p.getInventory().setItem(slot,new ItemStack(resource.item==null?Items.OAK_LOG:resource.item,count));left-=count;}var nonce=com.mohymods.kingdomsmp.progression.TierTwoService.handleReview(p,resource.id,batch);com.mohymods.kingdomsmp.progression.TierTwoService.confirm(p,nonce);remaining-=batch;}
            }
            level.getServer().getCommands().getDispatcher().execute("kingdom admin milestone road confirm Isolated supplied inspected settlement road fixture",level.getServer().createCommandSourceStack());
            h.assertTrue(com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(level.getServer()).attainedTier()==3,"Actual native eight-resource consumption and audited road approval reach Tier3");
            var config=level.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commission-quests.json");java.nio.file.Files.writeString(config,java.nio.file.Files.readString(config).replace(": 2",": 1"));CommissionQuestRules.clear(level.getServer());
            near();p.getInventory().clearContent();for(int slot=0;slot<4;slot++)p.getInventory().setItem(slot,new ItemStack(Items.OAK_LOG,64));
            var contract=ContractService.review(p,"lumberjack/weekly");ContractService.confirm(p,contract.nonce());h.assertTrue(store.commissionContractPeriods(p.getUUID(),advanced)==1,"Actual native weekly receipt pins real Tier3, current Journeyman band and one unique period");
            var review=ProfessionService.review(p,"quest:"+advanced);ProfessionService.confirm(p,review.nonce());h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==1000,"Real advanced commission awards200XP once after earned Apprentice band and actual Tier3");
            boolean denied=false;try{ProfessionService.review(p,"quest:"+advanced);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Advanced native commission cannot pay twice");saved();
        }
        void saved(){
            try{CommerceService.verifyReceipt(p);var data=TreeSavedData.get(level.getServer());data.setDirty();level.getDataStorage().save();net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();var saved=TreeSavedData.load(AtomicNbt.read(level.getServer().getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_tree_evidence_v1.dat")).getCompound("data"),level.registryAccess());h.assertTrue(saved.evidence().evidence().count(p.getUUID(),ProfessionRank.APPRENTICE,"grown")>=24,"Actual saved growth receipts/counters survive native file reload");var reopened=EconomyStore.open(level.getServer().getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.professions(p.getUUID()).equals(store.professions(p.getUUID())),"Canonical forestry awards reopen exactly");}catch(Exception failure){h.fail(failure.toString());}
            h.succeed();com.mojang.logging.LogUtils.getLogger().info("NATIVE_TREE_ASSERTIONS_PASSED: actual forest worldgen, real player planted-to-mature trees, canceled bonemeal transaction refusal, placed-log and native piston denial, full128oak64birch64spruce24replacement goals, real item use/harvest/consent, normal Royal packet, mixed128planks/32strippedlogs permanently consumed, real32strip/32craft actions, reviewed forest services, all8earned800XP/Journeyman, offline actual growth/logout/reconnect and revoked plot consent, native saved-file/canonical reopen; supplies explicit, actual earned Tier3/256logweekly and1000totalXP, private clock and daily/weekly period goals accelerated; no tree/craft quantities or XP/rank injected");
        }
        void guard(Runnable action){try{action.run();}catch(Exception failure){h.fail(failure.toString());}}
    }
}
