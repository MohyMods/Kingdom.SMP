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
        var existing=ConstructionService.store(h.getLevel().getServer()).projects().stream().filter(p->p.blueprint().toString().equals(Profession.LUMBERJACK.buildingId())&&p.phase()==ConstructionStore.Phase.COMPLETE).findFirst().orElse(null);
        if(existing!=null&&CommerceService.store(h.getLevel().getServer()).professions(existing.owner()).track(Profession.LUMBERJACK).completedQuestIds().contains("kingdom_smp:lumberjack/j04")){resumeCultivation(h,existing);return;}
        // Only this generated private forest fixture: keep settlement sites clear before their authenticated construction.
        for(var pos:BlockPos.betweenClosed(new BlockPos(-10,-63,-10),new BlockPos(60,-30,100)))if(!h.getLevel().getBlockState(pos).isAir())h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),18);
        CommerceRuntimeTests.completePurchasedProfessionLoop(h,Profession.LUMBERJACK.buildingId(),()->{try{new Walk(h).start();}catch(Exception e){h.fail(e.toString());}});
    }
    private static void resumeCultivation(GameTestHelper h,ConstructionStore.Project lodge)throws Exception{
        var level=h.getLevel();var server=level.getServer();var p=ClaimRuntimeTests.vanillaPlayer(level,lodge.owner(),lodge.ownerName());var data=TreeSavedData.get(server).evidence();
        var sample=data.cultivated(p.getUUID(),true,128);h.assertTrue(!sample.isEmpty(),"Actual second JVM restores a partial owned tree and its once-only harvested marker");
        int before=data.evidence().count(p.getUUID(),ProfessionRank.JOURNEYMAN,"mature_cherry");h.assertTrue(before==17&&data.evidence().count(p.getUUID(),ProfessionRank.JOURNEYMAN,"mature_mangrove")==12,"Exact owned species counters survive restart");
        var entry=sample.entrySet().stream().filter(e->e.getValue().species().equals("cherry")).findFirst().orElseThrow();var pos=BlockPos.of(entry.getKey().position());level.getChunk(pos.getX()>>4,pos.getZ()>>4);p.moveTo(pos.getX()+2.5,pos.getY()+1,pos.getZ()+2.5);h.assertTrue(level.getBlockState(pos).toString().equals(entry.getValue().state())&&p.gameMode.destroyBlock(pos),"Real native harvest resumes on the same actual saved owned tree");h.assertTrue(data.evidence().count(p.getUUID(),ProfessionRank.JOURNEYMAN,"mature_cherry")==before,"The same mature tree cannot be counted again after a full process restart");
        for(var project:ConstructionService.store(server).projects())if(project.origin()!=null){var b=ConstructionService.bounds(project);for(int x=((int)b.minX)>>4;x<=((int)b.maxX)>>4;x++)for(int z=((int)b.minZ)>>4;z<=((int)b.maxZ)>>4;z++)level.getChunk(x,z);}
        h.runAfterDelay(20,()->{try{h.assertTrue(CommerceService.store(server).professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==2200&&ConstructionService.structureIntact(level,lodge)&&level.getEntity(lodge.npc()) instanceof com.mohymods.kingdomsmp.npc.ReeveEntity,"Earned forestry XP and original workshop/NPC survive native restart");CommerceService.verifyReceipt(p);finishForestry(h,p,lodge);h.succeed();com.mojang.logging.LogUtils.getLogger().info("NATIVE_TREE_RESTART_ASSERTIONS_PASSED: second real JVM, same owned native cherry tree, saved planter/band/species/claimed marker, no duplicate maturity,17cherry12mangrove/2400XP/all16AandJ/original lodge/NPC/native receipt retained");}catch(Exception failure){h.fail(failure.toString());}});
    }
    private static void finishForestry(GameTestHelper h,ServerPlayer p,ConstructionStore.Project lodge)throws Exception{
        var level=h.getLevel();var server=level.getServer();var center=lodge.origin().offset(35,0,0);
        for(int x=(center.getX()-10)>>4;x<=(center.getX()+10)>>4;x++)for(int z=(center.getZ()-10)>>4;z<=(center.getZ()+10)>>4;z++)level.getChunk(x,z); // Private visited-site preparation only.
        var hut=ConstructionService.store(server).projects().stream().filter(q->q.blueprint().toString().equals("kingdom_smp:builders_hut")).findFirst().orElseThrow();
        var admin=ClaimRuntimeTests.vanillaPlayer(level,hut.owner(),hut.ownerName());server.getPlayerList().op(admin.getGameProfile());
        try{
            Item[] items={Items.OAK_SAPLING,Items.BIRCH_SAPLING,Items.JUNGLE_SAPLING};BlockPos[] sites={center.east(6),center.west(6),center.north(6)};
            for(int i=0;i<sites.length;i++){var pos=sites[i];h.assertTrue(level.getBlockState(pos).isAir(),"Private nursery expansion never overwrites a structure");level.setBlock(pos.below(),Blocks.DIRT.defaultBlockState(),3);p.moveTo(pos.getX()+2.5,pos.getY()+1,pos.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(items[i]));p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false));h.assertTrue(p.getMainHandItem().isEmpty(),"Native nursery placement consumes each real sapling");}
            admin.moveTo(center.getX()+3.5,center.getY(),center.getZ()+5.5);p.moveTo(center.getX()+4.5,center.getY(),center.getZ()+5.5);
            var nonce=ProfessionInspectionService.review(admin,p,"kingdom_smp:lumberjack/j08",center,"Verified access diverse nursery native replanting and forestry authorship");ProfessionInspectionService.confirm(admin,nonce);
            var roster=CommerceService.store(server).professions(p.getUUID());h.assertTrue(roster.track(Profession.LUMBERJACK).xp()==2400&&roster.track(Profession.LUMBERJACK).completedQuestIds().size()==16&&roster.progressBand(Profession.LUMBERJACK)==ProfessionRank.MASTER&&roster.rank(Profession.LUMBERJACK,ProfessionService.quests())==ProfessionRank.JOURNEYMAN,"All16native A/J proofs earn2400XP and Master trials without granting Master rank/equipment");
            boolean refused=false;try{ProfessionInspectionService.review(admin,p,"kingdom_smp:lumberjack/j08",center,"Duplicate forest inspection must refuse");}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused,"Final forestry assessment cannot award twice");CommerceService.verifyReceipt(p);
        }finally{server.getPlayerList().deop(admin.getGameProfile());}
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
                h.assertTrue(p.getInventory().countItem(Items.STRIPPED_OAK_LOG)==16&&p.getInventory().countItem(Items.STRIPPED_BIRCH_LOG)==16,"Actual native dropped outputs enter inventory: oak="+p.getInventory().countItem(Items.STRIPPED_OAK_LOG)+",birch="+p.getInventory().countItem(Items.STRIPPED_BIRCH_LOG));
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
            clearTropical();var pos=tropicalPlot();move(pos);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ACACIA_SAPLING));use(pos.below(),Direction.UP);
            h.runAfterDelay(2,()->guard(()->{h.assertTrue(TreeSavedData.get(level.getServer()).evidence().plantAt(level.dimension().location().toString(),pos).orElseThrow().band()==null,"Journeyman planting before approved Tier3 cannot pre-earn future growth evidence");tierThreeCommission();}));
        }
        void tierThreeCommission()throws Exception{
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
            boolean denied=false;try{ProfessionService.review(p,"quest:"+advanced);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Advanced native commission cannot pay twice");tropicalPlant();
        }
        int tropical,attempts;final String[] tropicalSpecies={"acacia","dark_oak","jungle"};final Item[] tropicalSaplings={Items.ACACIA_SAPLING,Items.DARK_OAK_SAPLING,Items.JUNGLE_SAPLING};final int[] tropicalGoals={128,64,96};
        BlockPos tropicalPlot(){return plot.north(24);}
        int tropicalCount(String metric){return TreeSavedData.get(level.getServer()).evidence().evidence().count(p.getUUID(),ProfessionRank.JOURNEYMAN,metric);}
        void clearTropical(){var root=tropicalPlot();for(var pos:BlockPos.betweenClosed(root.offset(-12,0,-12),root.offset(12,40,12)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);for(var pos:BlockPos.betweenClosed(root.offset(-12,-1,-12),root.offset(12,-1,12)))level.setBlock(pos,Blocks.GRASS_BLOCK.defaultBlockState(),3);}
        void tropicalPlant(){
            if(++attempts>100)throw new IllegalStateException("Bounded real tropical tree fixture attempts exhausted");clearTropical();var root=tropicalPlot();int side=tropical==1?2:1;
            for(int x=0;x<side;x++)for(int z=0;z<side;z++){var pos=root.offset(x,0,z);move(pos);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tropicalSaplings[tropical]));use(pos.below(),Direction.UP);h.assertTrue(p.getMainHandItem().isEmpty(),"Each genuine tropical sapling consumes its actual item; dark oak requires4");}
            h.runAfterDelay(2,()->guard(this::tropicalGrow));
        }
        void tropicalGrow(){
            var root=tropicalPlot();move(root);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL,64));for(int n=0;n<64&&level.getBlockState(root).getBlock() instanceof SaplingBlock;n++)use(root,Direction.UP);
            h.assertTrue(TreeLifecycle.species(level.getBlockState(root))!=null,"Actual native"+tropicalSpecies[tropical]+"growth produces its mature trunk");h.runAfterDelay(3,()->guard(this::tropicalHarvest));
        }
        void tropicalHarvest(){
            var root=tropicalPlot();var source=TreeSavedData.get(level.getServer()).evidence();int before=tropicalCount(tropicalSpecies[tropical]),logs=0;
            for(var pos:BlockPos.betweenClosed(root.offset(-12,0,-12),root.offset(12,40,12)))if(TreeLifecycle.species(level.getBlockState(pos))!=null){h.assertTrue(source.logAt(level.dimension().location().toString(),pos).isPresent(),"Tropical trunks are actual successful native feature output, never leaf inference");move(pos);h.assertTrue(p.gameMode.destroyBlock(pos),"Actual native player harvest removes tropical timber");logs++;}
            h.assertTrue(logs>0&&tropicalCount(tropicalSpecies[tropical])==before+logs,"Real current-band species count matches actual harvested trunk positions");
            if(tropicalCount(tropicalSpecies[tropical])>=tropicalGoals[tropical]){if(tropical==0)h.assertTrue(!TreeQuestService.ready(p,"kingdom_smp:lumberjack/j01"),"128actual acacia cannot substitute for the missing64dark oak");tropical++;}
            if(tropical<3){tropicalPlant();return;}
            near();int sequence=51000;for(String id:List.of("kingdom_smp:lumberjack/j01","kingdom_smp:lumberjack/j02")){var view=CommerceService.handle(p,new CommerceRequest(++sequence,1,7,"quest:"+id,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal UI reviews actual full hardwood and jungle harvest goals");CommerceService.handle(p,new CommerceRequest(++sequence,2,7,"quest:"+id,1,view.review()));}
            h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==1400,"Actual64each hardwood/96jungle goals award200XP each once after earned Apprentice and Tier3");cyclePlant();
        }
        int cycleKind;boolean partialCherry;final Item[] cycleSaplings={Items.CHERRY_SAPLING,Items.MANGROVE_PROPAGULE};final String[] cycleMetrics={"mature_cherry","mature_mangrove"};final int[] cycleGoals={16,12};
        void cyclePlant(){clearTropical();var root=tropicalPlot();move(root);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(cycleSaplings[cycleKind]));use(root.below(),Direction.UP);h.assertTrue(p.getMainHandItem().isEmpty(),"Actual eligible planter consumes a real cherry sapling or mangrove propagule");h.runAfterDelay(2,()->guard(this::cycleGrow));}
        void cycleGrow(){
            var root=tropicalPlot();move(root);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL,64));for(int n=0;n<64&&level.getBlockState(root).getBlock() instanceof SaplingBlock;n++)use(root,Direction.UP);
            h.assertTrue(!(level.getBlockState(root).getBlock() instanceof SaplingBlock),"Native mature mangroves may replace the propagule origin with roots rather than a log");h.runAfterDelay(3,()->guard(this::cycleHarvest));
        }
        void cycleHarvest()throws Exception{
            var root=tropicalPlot();int before=tropicalCount(cycleMetrics[cycleKind]),logs=0;
            for(var pos:BlockPos.betweenClosed(root.offset(-12,0,-12),root.offset(12,40,12)))if(TreeLifecycle.species(level.getBlockState(pos))!=null){h.assertTrue(TreeSavedData.get(level.getServer()).evidence().logAt(level.dimension().location().toString(),pos).isPresent(),"Every cultivated trunk has real native successful growth provenance");move(pos);h.assertTrue(p.gameMode.destroyBlock(pos),"Original eligible planter personally harvests the actual matured tree");logs++;if(partialCherry)break;}
            h.assertTrue(logs>0&&tropicalCount(cycleMetrics[cycleKind])==before+1,"Exactly one owned mature species cycle is counted per real tree, regardless of trunk block count");
            if(partialCherry){h.assertTrue(tropicalCount("mature_cherry")==17,"A partial seventeenth tree prepares actual persistent duplicate-recognition testing");production();return;}
            if(tropicalCount(cycleMetrics[cycleKind])>=cycleGoals[cycleKind])cycleKind++;
            if(cycleKind<2){cyclePlant();return;}
            near();int sequence=52000;for(String id:List.of("kingdom_smp:lumberjack/j03","kingdom_smp:lumberjack/j04")){var view=CommerceService.handle(p,new CommerceRequest(++sequence,1,7,"quest:"+id,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal UI reviews actual owned mature cherry/mangrove harvests");CommerceService.handle(p,new CommerceRequest(++sequence,2,7,"quest:"+id,1,view.review()));}
            h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==1800,"Sixteen actual cherry and twelve mangrove trees award200XP each once, no injected cycle or quest proof");partialCherry=true;cycleKind=0;cyclePlant();
        }
        void production()throws Exception{
            var table=plot.offset(14,0,0);move(table);p.closeContainer();p.getInventory().clearContent();
            var menu=new net.minecraft.world.inventory.CraftingMenu(45,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,table));p.containerMenu=menu;
            // Actual native result slots/recipes, QA supplies ingredients only. Mixed species, one legitimate surplus fence.
            for(int n=0;n<11;n++){var wood=n<6?Items.OAK_PLANKS:Items.BIRCH_PLANKS;for(int i=1;i<=9;i++)menu.getSlot(i).set(ItemStack.EMPTY);for(int i:new int[]{1,3,4,6})menu.getSlot(i).set(new ItemStack(wood));for(int i:new int[]{2,5})menu.getSlot(i).set(new ItemStack(Items.STICK));h.assertTrue(menu.getSlot(0).getItem().is(n<6?Items.OAK_FENCE:Items.BIRCH_FENCE),"Native mixed fence recipe");menu.quickMoveStack(p,0);}
            for(int n=0;n<4;n++){for(int i=1;i<=9;i++)menu.getSlot(i).set(ItemStack.EMPTY);for(int i:new int[]{1,4,5,7,8,9})menu.getSlot(i).set(new ItemStack(Items.OAK_PLANKS));h.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_STAIRS),"Native wooden stair recipe");menu.quickMoveStack(p,0);}
            for(int n=0;n<8;n++){for(int i=1;i<=9;i++)menu.getSlot(i).set(ItemStack.EMPTY);for(int i=1;i<=6;i++)menu.getSlot(i).set(new ItemStack(Items.BIRCH_PLANKS));h.assertTrue(menu.getSlot(0).getItem().is(Items.BIRCH_TRAPDOOR),"Native wooden trapdoor recipe");menu.quickMoveStack(p,0);}p.closeContainer();
            h.assertTrue(CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"fences")==33&&CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"wood_stairs")==16&&CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"trapdoors")==16,"Real native crafted quantities scoped to actual current band");
            p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,7)); // Unrelated legitimate supplies must not enter these categories.
            deliver(CraftDeliveryQuests.FITTINGS,54000);h.assertTrue(p.getInventory().countItem(Items.OAK_FENCE)+p.getInventory().countItem(Items.BIRCH_FENCE)==1&&p.getInventory().countItem(Items.OAK_STAIRS)==0&&p.getInventory().countItem(Items.BIRCH_TRAPDOOR)==0&&p.getInventory().countItem(Items.OAK_PLANKS)==7,"Exact mixed fittings consumed once; unrelated material and surplus retained");
            var pos=table.south(7);h.assertTrue(level.getBlockState(pos).isAir(),"Private native furnace site is empty");level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.FURNACE));move(pos);use(pos.below(),Direction.UP);h.assertTrue(p.getMainHandItem().isEmpty()&&level.getBlockState(pos).is(Blocks.FURNACE),"Real supplied furnace item placed natively");
            var furnace=(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)level.getBlockEntity(pos);p.openMenu(furnace);var menuF=p.containerMenu;h.assertTrue(menuF instanceof net.minecraft.world.inventory.FurnaceMenu,"Real native furnace menu");p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,64));p.getInventory().setItem(1,new ItemStack(Items.COAL,8));menuF.quickMoveStack(p,30);menuF.quickMoveStack(p,31);
            h.assertTrue(furnace.getItem(0).getCount()==64&&furnace.getItem(1).getCount()==8,"Native input and fuel transfers consume inventory stacks");
            // Private waiting acceleration: actual vanilla serverTick consumes logs/fuel and records every recipe. No injected output or event.
            for(int tick=0;tick<13000;tick++)net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(level,pos,level.getBlockState(pos),furnace);
            h.assertTrue(furnace.getItem(0).isEmpty()&&furnace.getItem(1).isEmpty()&&furnace.getItem(2).is(Items.CHARCOAL)&&furnace.getItem(2).getCount()==64,"64actual logs/8coal become64native charcoal");
            var output=menuF.getSlot(2);var one=output.remove(1);output.onTake(p,one);h.assertTrue(p.getInventory().add(one),"Actual native charcoal output enters inventory");h.assertTrue(CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"minecraft:charcoal")==64,"Partial take certifies one real completed native64-output recipe batch");
            menuF.quickMoveStack(p,2);h.assertTrue(p.getInventory().countItem(Items.CHARCOAL)==64&&CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"minecraft:charcoal")==64,"Remaining shift-take cannot reuse cleared native recipe history");p.closeContainer();
            deliver(CraftDeliveryQuests.CHARCOAL,55000);h.assertTrue(p.getInventory().countItem(Items.CHARCOAL)==0&&store.professions(p.getUUID()).track(Profession.LUMBERJACK).xp()==2200,"Native64charcoal permanently consumed for200XPonce;15actual proofs");furnaceDenials(pos,furnace);saved();
        }
        void furnaceDenials(BlockPos pos,net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace)throws Exception{
            move(pos);p.openMenu(furnace);var menu=p.containerMenu;
            // Deliberately counterfeit private fixture stock is a NEGATIVE test only; no production proof is injected.
            furnace.setItem(2,new ItemStack(Items.CHARCOAL,1));menu.quickMoveStack(p,2);h.assertTrue(CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"minecraft:charcoal")==64,"Copied output without actual completed native recipe history grants no credit");
            p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG));menu.quickMoveStack(p,30);p.getInventory().setItem(1,new ItemStack(Items.COAL));menu.quickMoveStack(p,31);for(int tick=0;tick<210;tick++)net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(level,pos,level.getBlockState(pos),furnace);
            h.assertTrue(furnace.getItem(2).is(Items.CHARCOAL)&&furnace.getItem(2).getCount()==1,"Denial fixture also processes one real native recipe");
            var other=level.getServer().getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseThrow();ClaimRuntimeTests.ftbUnclaim(level,pos);ClaimRuntimeTests.ftbClaim(other,pos);h.assertTrue(com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,pos,pos)!=null,"Actual FTB ownership change revokes furnace plot consent");
            var output=menu.getSlot(2);var taken=output.remove(1);output.onTake(p,taken);p.getInventory().add(taken);h.assertTrue(furnace.getItem(2).isEmpty()&&CraftQuestEvidence.count(p,Profession.LUMBERJACK,ProfessionRank.JOURNEYMAN,"minecraft:charcoal")==64,"Revoked consent refuses evidence without interrupting native output extraction or duplicating an item");p.closeContainer();ClaimRuntimeTests.ftbUnclaim(level,pos);
        }
        void deliver(String id,int sequence)throws Exception{
            near();var view=CommerceService.handle(p,new CommerceRequest(sequence+1,1,7,"materials:"+id,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal review for actual produced materials "+id);CommerceService.handle(p,new CommerceRequest(sequence+2,2,7,"materials:"+id,1,view.review()));
            h.assertTrue(store.professions(p.getUUID()).track(Profession.LUMBERJACK).completedQuestIds().contains(id),"Actual materials receipt commits "+id);boolean denied=false;try{ProfessionService.review(p,"materials:"+id);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Production delivery cannot award twice");
        }
        void saved(){
            try{CommerceService.verifyReceipt(p);var data=TreeSavedData.get(level.getServer());data.setDirty();level.getDataStorage().save();net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();var saved=TreeSavedData.load(AtomicNbt.read(level.getServer().getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_tree_evidence_v1.dat")).getCompound("data"),level.registryAccess());h.assertTrue(saved.evidence().evidence().count(p.getUUID(),ProfessionRank.APPRENTICE,"grown")>=24,"Actual saved growth receipts/counters survive native file reload");var reopened=EconomyStore.open(level.getServer().getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.professions(p.getUUID()).equals(store.professions(p.getUUID())),"Canonical forestry awards reopen exactly");}catch(Exception failure){h.fail(failure.toString());}
            h.succeed();com.mojang.logging.LogUtils.getLogger().info("NATIVE_TREE_PREPARED: owned cherry/mangrove2200XPand native production/partial seventeenth tree saved for real restart");com.mojang.logging.LogUtils.getLogger().info("NATIVE_TREE_ASSERTIONS_PASSED: actual forest worldgen, real player planted-to-mature trees, canceled bonemeal transaction refusal, placed-log and native piston denial, full128oak64birch64spruce24replacement goals, real item use/harvest/consent, normal Royal packet, mixed128planks/32strippedlogs permanently consumed, real32strip/32craft actions, reviewed forest services, all8earned800XP/Journeyman, offline actual growth/logout/reconnect and revoked plot consent, native saved-file/canonical reopen; supplies explicit, actual earned Tier3/256logweekly plus128acacia overage/64darkoak/native4saplings/96jungle and native16cherry12mangrove original-planter cycles plus real64charcoal smelting/partial and shift take/native mixed32fences16stairs16trapdoors consumption/2200XP before final assessment, copied-output and revoked-claim denial; private furnace waiting and clock and daily/weekly period goals accelerated; no tree/craft quantities or XP/rank injected");
        }
        interface Checked{void run()throws Exception;}
        void guard(Checked action){try{action.run();}catch(Exception failure){h.fail(failure.toString());}}
    }
}
