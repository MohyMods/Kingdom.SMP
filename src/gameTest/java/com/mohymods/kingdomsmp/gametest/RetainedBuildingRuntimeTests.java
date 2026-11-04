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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_retained_tests") @PrefixGameTestTemplate(false)
public final class RetainedBuildingRuntimeTests {
    @GameTest(template="empty",timeoutTicks=6000)
    public static void actualPlayerPlacementsEarnTheWholeBuilderApprenticeBand(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();server.tickRateManager().setTickRate(200);var store=CommerceService.store(server);
        var old=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().toString().equals(Profession.BUILDER.buildingId())).findFirst().orElse(null);
        if(old!=null&&store.professions(old.owner()).track(Profession.BUILDER).completedQuestIds().contains("kingdom_smp:builder/a08")){resume(h,old);return;}
        CommerceRuntimeTests.completePurchasedProfessionLoop(h,Profession.BUILDER.buildingId(),()->{
            try{new Walk(h).start();}catch(Exception e){h.fail(e.toString());}
        });
    }
    private static void resume(GameTestHelper h,ConstructionStore.Project building)throws Exception{
        var level=h.getLevel();var server=level.getServer();var store=CommerceService.store(server);var p=ClaimRuntimeTests.vanillaPlayer(level,building.owner(),building.ownerName());
        var site=store.builderWorksite(p.getUUID(),"kingdom_smp:builder/a01").orElseThrow();
        for(var project:ConstructionService.store(server).projects())if(project.origin()!=null){var b=ConstructionService.bounds(project);for(int x=((int)b.minX)>>4;x<=((int)b.maxX)>>4;x++)for(int z=((int)b.minZ)>>4;z<=((int)b.maxZ)>>4;z++)level.getChunk(x,z);}
        for(var work:store.builderWorksites())for(int x=work.from().getX()>>4;x<=work.to().getX()>>4;x++)for(int z=work.from().getZ()>>4;z<=work.to().getZ()>>4;z++)level.getChunk(x,z);
        boolean corrupt=java.nio.file.Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("retained-corrupt-fixture"));
        if(corrupt){
            var pos=site.from().offset(1,1,1);p.moveTo(pos.getX()+2.5,pos.getY()+2,pos.getZ()+2.5);var held=p.getMainHandItem().copy();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE));
            p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false));
            h.assertTrue(level.getBlockState(pos).is(Blocks.COBBLESTONE)&&p.getMainHandItem().isEmpty(),"Malformed saved evidence cannot throw through the native item transaction or duplicate a placed block item");p.setItemInHand(InteractionHand.MAIN_HAND,held);
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==expectedXp(store,p.getUUID()),"Malformed event evidence awards no extra progression");
            boolean refused=false;try{RetainedBuildingService.inspectCompletedProject(p,"kingdom_smp:builder/a01");}catch(RuntimeException expected){refused=true;}h.assertTrue(refused,"Malformed existing evidence refuses inspection and never silently resets");
            CommerceService.verifyReceipt(p);h.succeed();com.mojang.logging.LogUtils.getLogger().info("RETAINED_CORRUPT_EVENT_ASSERTIONS_PASSED: second real JVM, intentionally corrupt private saved evidence, actual native block placed/item consumed once, no duplicate or XP, inspection refuses reset, canonical receipt preserved");return;
        }
        h.runAfterDelay(20,()->{try{
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==expectedXp(store,p.getUUID())&&store.professions(p.getUUID()).rank(Profession.BUILDER,ProfessionService.quests())==ProfessionRank.JOURNEYMAN,"Third real JVM retains original eight proofs and earned rank");
            for(var id:RetainedBuildingQuests.ids().stream().filter(q->store.professions(p.getUUID()).track(Profession.BUILDER).completedQuestIds().contains(q)).toList())h.assertTrue(RetainedBuildingService.inspectCompletedProject(p,id).count()==RetainedBuildingQuests.goal(id),"Matching restored evidence and native retained geometry survive restart: "+id);
            h.assertTrue(ConstructionService.structureIntact(level,building)&&level.getEntity(building.npc()) instanceof com.mohymods.kingdomsmp.npc.ReeveEntity,"Original intact workshop and registered permanent NPC identity survived restart");
            CommerceService.verifyReceipt(p);h.succeed();com.mojang.logging.LogUtils.getLogger().info("RETAINED_RESTART_ASSERTIONS_PASSED: third clean JVM, matching private evidence restored, unchanged Apprentice and facade proofs2400XP/Journeyman and Master trials, all432retained native positions, same workshop/NPC, canonical receipt and no automatic reward duplication");
        }catch(Exception failure){h.fail(failure.toString());}});
    }
    private static long expectedXp(EconomyStore store,UUID player){return store.professions(player).track(Profession.BUILDER).completedQuestIds().stream().mapToLong(ProfessionQuestService::xp).sum();}
    static final class Walk {
        final GameTestHelper h;final net.minecraft.server.level.ServerLevel level;final ServerPlayer p,admin;final EconomyStore store;final ConstructionStore.Project building;
        final BlockPos base,routeFrom,routeTo;final Map<String,BuilderWorksite> sites=new HashMap<>();
        Walk(GameTestHelper h){
            this.h=h;level=h.getLevel();var server=level.getServer();var hall=ConstructionService.lodge(server);p=server.getPlayerList().getPlayer(hall.owner());admin=server.getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseThrow();store=CommerceService.store(server);
            building=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().toString().equals(Profession.BUILDER.buildingId())).findFirst().orElseThrow();
            base=building.origin().offset(0,0,-18);routeFrom=base.offset(-6,0,0);routeTo=base.offset(6,0,17);
        }
        void near(){var home=building.origin().offset(BlueprintRegistry.get(building.blueprint(),building.blueprintVersion()).home());p.moveTo(home.getX()+.5,home.getY(),home.getZ()-1.5);CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)level.getEntity(building.npc()));}
        void start()throws Exception{
            near();var enrollment=ProfessionService.review(p,"learn:builder");ProfessionService.confirm(p,enrollment.nonce());
            h.assertTrue(!p.hasPermissions(2)&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==0,"Real ordinary active Builder begins without quest proofs");
            // Private fixture terrain and supplied items only; no earned placement/XP is injected.
            for(var pos:BlockPos.betweenClosed(base.offset(-8,-1,-2),base.offset(18,-1,17)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            for(int cx=routeFrom.getX()>>4;cx<=(base.getX()+15)>>4;cx++)for(int cz=base.getZ()>>4;cz<=routeTo.getZ()>>4;cz++){
                var point=new BlockPos(cx*16,base.getY(),cz*16);ClaimRuntimeTests.ftbUnclaim(level,point);ClaimRuntimeTests.ftbClaim(p,point);ClaimRuntimeTests.opacUnclaim(level,point);ClaimRuntimeTests.opacClaim(level,p.getUUID(),point);
            }
            var source=level.getServer().getCommands().getDispatcher();String unauthorized="kingdom admin worksite inspect "+p.getGameProfile().getName()+" kingdom_smp:builder/a01 "+coords(base)+" "+coords(base.offset(15,5,7))+" Consented native supplied material project";
            boolean refused=false;try{source.execute(unauthorized,p.createCommandSourceStack());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){refused=true;}h.assertTrue(refused,"Ordinary players cannot authorize their own worksite through the admin command");
            admin.moveTo(base.getX()+7.5,base.getY()+1,base.getZ()+10.5);p.moveTo(base.getX()+8.5,base.getY()+1,base.getZ()+10.5);level.getServer().getPlayerList().op(admin.getGameProfile());
            refused=false;try{RetainedBuildingService.reviewSite(admin,p,"kingdom_smp:builder/a01",building.origin(),building.origin().offset(2,2,2),"Cannot count the Aldric-built reward structure");}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused,"Original Royal building/reservation cannot become personal placement evidence");
            for(int n:List.of(1,2,3,6)){
                String id=Profession.BUILDER.questId(ProfessionRank.APPRENTICE,n);var from=n==6?routeFrom:base;var to=n==6?routeTo:base.offset(15,5,7);
                UUID nonce=RetainedBuildingService.reviewSite(admin,p,id,from,to,"Consented supplied personal frame roof and route project");
                h.assertTrue(store.builderWorksite(p.getUUID(),id).isEmpty(),"Preview alone creates no authorization");
                h.assertTrue(source.execute("kingdom admin worksite confirm "+nonce,admin.createCommandSourceStack())==1,"Actual OP2 explicit site confirmation commits once");sites.put(id,store.builderWorksite(p.getUUID(),id).orElseThrow());
            }
            level.getServer().getPlayerList().deop(admin.getGameProfile());
            // Keep the original collision-protected builder identity; move him off the finished doorway in this private fixture only.
            var hut=BuilderHutSavedData.get(level.getServer()).hut().orElseThrow();var aldric=level.getEntity(hut.npcId());
            ((net.minecraft.world.entity.Mob)aldric).setNoAi(true);aldric.moveTo(base.getX()+17.5,base.getY(),base.getZ()+3.5);
            level.setBlock(base,Blocks.COBBLESTONE.defaultBlockState(),3);
            h.assertTrue(RetainedBuildingService.scan(p,"kingdom_smp:builder/a01").count()==0,"Administrative/native automatic world writes cannot fabricate player provenance");
            level.setBlock(base,Blocks.AIR.defaultBlockState(),3);
            var untracked=base.above();level.setBlock(untracked,Blocks.OAK_LOG.defaultBlockState(),3);p.moveTo(untracked.getX()+2.5,untracked.getY()+1,untracked.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(untracked),Direction.SOUTH,untracked,false));
            h.assertTrue(level.getBlockState(untracked).is(Blocks.STRIPPED_OAK_LOG),"Native axe really transforms an untracked pre-existing log");
            h.runAfterDelay(3,()->guard(()->{h.assertTrue(RetainedBuildingService.scan(p,"kingdom_smp:builder/a02").count()==0,"Stripping an untracked/admin/natural log cannot fabricate supplied-placement evidence");level.setBlock(untracked,Blocks.AIR.defaultBlockState(),3);placeApprentice();}));
        }
        void placeApprentice(){
            for(int x=0;x<16;x++)for(int z=0;z<8;z++)place(base.offset(x,0,z),Items.COBBLESTONE);
            for(int x=0;x<16;x+=4)for(int z=0;z<8;z+=2)for(int y=1;y<=4;y++)place(base.offset(x,y,z),Items.OAK_LOG);
            for(int z=0;z<8;z++)for(int x=0;x<8;x++)place(base.offset(x,5,z),Items.OAK_PLANKS);
            for(int z=0;z<18;z++)for(int x=-2;x<0;x++)place(base.offset(x,0,z),Items.COBBLESTONE);
            for(int x=0;x<=6;x++)place(base.offset(x,0,17),x==6?Items.DIRT:Items.COBBLESTONE);
            var path=base.offset(6,0,17);p.moveTo(path.getX()+2.5,path.getY()+1,path.getZ()-2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SHOVEL));
            p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(path).add(0,.5,0),Direction.UP,path,false));
            h.assertTrue(level.getBlockState(path).is(Blocks.DIRT_PATH)&&p.getMainHandItem().getDamageValue()==1,"Actual native shovel transform creates the final doorway approach and consumes durability");
            for(int x=-6;x<-2;x++)place(base.offset(x,0,0),Items.COBBLESTONE);
            place(base.offset(-6,0,1),Items.COBBLESTONE);
            h.runAfterDelay(3,()->guard(this::checkAndInterrupt));
        }
        void place(BlockPos pos,Item item){
            Direction facing=null;BlockPos clicked=null;
            for(var direction:Direction.values()){
                var neighbor=pos.relative(direction);if(!level.getBlockState(neighbor).getCollisionShape(level,neighbor).isEmpty()){clicked=neighbor;facing=direction.getOpposite();break;}
            }
            if(clicked==null)throw new IllegalStateException("No actual native supporting neighbor at "+pos);
            p.moveTo(pos.getX()+2.5,pos.getY()+2,pos.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            var hit=new BlockHitResult(Vec3.atCenterOf(clicked).add(facing.getStepX()*.5,facing.getStepY()*.5,facing.getStepZ()*.5),facing,clicked,false);
            p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            h.assertTrue(level.getBlockState(pos).getBlock().asItem()==item&&p.getMainHandItem().isEmpty(),"Native BlockItem use consumes one real supplied item and places the actual block at "+pos);
        }
        void checkAndInterrupt()throws Exception{
            near();for(var id:sites.keySet())h.assertTrue(RetainedBuildingService.scan(p,id).count()==RetainedBuildingQuests.goal(id),"Actual distinct retained placements reach the unchanged approved goal "+id);
            var log=base.above();p.moveTo(log.getX()+2.5,log.getY()+1,log.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));
            p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(log),Direction.SOUTH,log,false));
            h.assertTrue(level.getBlockState(log).is(Blocks.STRIPPED_OAK_LOG)&&p.getMainHandItem().getDamageValue()==1,"Actual native axe transformation consumes durability");
            h.runAfterDelay(3,()->guard(this::breakAndRestore));
        }
        void breakAndRestore()throws Exception{
            h.assertTrue(RetainedBuildingService.scan(p,"kingdom_smp:builder/a02").count()==64,"Legitimate native stripping preserves original owner provenance without increasing position count");
            p.moveTo(base.getX()+2.5,base.getY()+1,base.getZ()+2.5);h.assertTrue(p.gameMode.destroyBlock(base),"Native actual player removal succeeds");
            h.assertTrue(RetainedBuildingService.scan(p,"kingdom_smp:builder/a01").count()==127,"Removing one real foundation invalidates its proof rather than leaving a lifetime placement counter");
            place(base,Items.COBBLESTONE);h.runAfterDelay(3,()->guard(this::claimsAndAwards));
        }
        void claimsAndAwards()throws Exception{
            near();h.assertTrue(RetainedBuildingService.scan(p,"kingdom_smp:builder/a01").count()==128,"Replacing the same cell restores128distinct final positions, never129");
            var review=ProfessionService.review(p,"quest:kingdom_smp:builder/a01");
            ClaimRuntimeTests.ftbUnclaim(level,base);ClaimRuntimeTests.ftbClaim(admin,base);
            boolean refused=false;try{ProfessionService.confirm(p,review.nonce());}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==0,"Current FTB edit revocation after review refuses completion without XP or block changes");
            ClaimRuntimeTests.ftbUnclaim(level,base);ClaimRuntimeTests.ftbClaim(p,base);
            RetainedBuildingService.changed(p.getUUID());review=ProfessionService.review(p,"quest:kingdom_smp:builder/a01");
            ClaimRuntimeTests.opacUnclaim(level,base);ClaimRuntimeTests.opacClaim(level,admin.getUUID(),base);
            refused=false;try{ProfessionService.confirm(p,review.nonce());}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==0,"Independent OPAC revocation after review also refuses without awarding");
            ClaimRuntimeTests.opacUnclaim(level,base);ClaimRuntimeTests.opacClaim(level,p.getUUID(),base);RetainedBuildingService.changed(p.getUUID());
            for(int n:List.of(1,2,3,6)){
                String id=Profession.BUILDER.questId(ProfessionRank.APPRENTICE,n);var view=CommerceService.handle(p,new CommerceRequest(20000+n*2,1,7,"quest:"+id,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal journal reviews actual retained proof");
                CommerceService.handle(p,new CommerceRequest(20001+n*2,2,7,"quest:"+id,1,view.review()));
            }
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==400,"Four actual retained objectives award400XP without wallet/item rewards");
            // Existing real contract service, with a labeled private period target acceleration only.
            var config=level.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commission-quests.json");CommissionQuestRules.load(config);java.nio.file.Files.writeString(config,java.nio.file.Files.readString(config).replace(": 3",": 1"));CommissionQuestRules.clear(level.getServer());
            p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,64));var contract=ContractService.review(p,"builder/daily");ContractService.confirm(p,contract.nonce());
            var commission=ProfessionService.review(p,"quest:kingdom_smp:builder/a07");ProfessionService.confirm(p,commission.nonce());
            CommissionQuestRuntimeTests.craftDelivery(h,p,building);
            entranceAndAssessment();
        }
        void entranceAndAssessment()throws Exception{
            var center=base.offset(4,1,8);level.setBlock(center.below(),Blocks.COBBLESTONE.defaultBlockState(),3);level.setBlock(center.south().below(),Blocks.COBBLESTONE.defaultBlockState(),3);
            var door=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.NORTH);level.setBlock(center,door,3);level.setBlock(center.above(),door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);level.setBlock(center.south(2).below(),Blocks.COBBLESTONE_STAIRS.defaultBlockState(),3);
            admin.moveTo(center.getX()+3.5,center.getY(),center.getZ()+3.5);p.moveTo(center.getX()+4.5,center.getY(),center.getZ()+3.5);level.getServer().getPlayerList().op(admin.getGameProfile());
            UUID nonce=ProfessionInspectionService.review(admin,p,"kingdom_smp:builder/a04",center,"Verified supplied personal entrance clear stairs and consent");ProfessionInspectionService.confirm(admin,nonce);
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==700&&store.professions(p.getUUID()).rank(Profession.BUILDER,ProfessionService.quests())==ProfessionRank.APPRENTICE,"Seven earned proofs do not prematurely unlock Journeyman");
            nonce=ProfessionInspectionService.review(admin,p,"kingdom_smp:builder/a08",center,"Personally inspected retained frame roof route and usable entry");
            level.setBlock(base.offset(1,5,1),Blocks.AIR.defaultBlockState(),3);
            boolean refused=false;try{ProfessionInspectionService.confirm(admin,nonce);}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==700,"Changing an earlier retained roof invalidates final assessment even after its earlier proof was awarded");
            place(base.offset(1,5,1),Items.OAK_PLANKS);
            h.runAfterDelay(3,()->guard(()->finish(center)));
        }
        void finish(BlockPos center)throws Exception{
            admin.moveTo(center.getX()+3.5,center.getY(),center.getZ()+3.5);p.moveTo(center.getX()+4.5,center.getY(),center.getZ()+3.5);
            // Completed proof cannot manufacture fresh events: restore exact source-owned roof for final stability.
            // The original owner genuinely placed this replacement; canonical completed proofs stay permanent.
            var data=RetainedBuildingSavedData.get(level.getServer());
            h.assertTrue(RetainedBuildingService.inspectCompletedProject(p,"kingdom_smp:builder/a03").count()==64,"The completed roof retains all64genuine source positions after legitimate restoration");
            UUID nonce=ProfessionInspectionService.review(admin,p,"kingdom_smp:builder/a08",center,"Personally inspected retained frame roof route and usable entry");ProfessionInspectionService.confirm(admin,nonce);
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).completedQuestIds().size()==8&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==800&&store.professions(p.getUUID()).rank(Profession.BUILDER,ProfessionService.quests())==ProfessionRank.JOURNEYMAN,"All eight actual approved Apprentice objectives earn exactly800XP and Journeyman once");
            level.getServer().getPlayerList().deop(admin.getGameProfile());advancedFacade();
        }
        void advancedFacade()throws Exception{
            var hall=ConstructionService.lodge(level.getServer());var home=hall.origin().offset(BlueprintRegistry.get(hall.blueprint(),hall.blueprintVersion()).home());p.moveTo(home.getX()+.5,home.getY(),home.getZ()-1.5);CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)level.getEntity(hall.npc()));
            for(var resource:com.mohymods.kingdomsmp.progression.TierTwoService.Resource.values()){int remaining=com.mohymods.kingdomsmp.progression.TierTwoService.ledger(level.getServer()).requirements().get(resource.id);while(remaining>0){int batch=Math.min(2304,remaining);p.getInventory().clearContent();for(int slot=0,left=batch;left>0;slot++){int n=Math.min(64,left);p.getInventory().setItem(slot,new ItemStack(resource.item==null?Items.OAK_LOG:resource.item,n));left-=n;}var nonce=com.mohymods.kingdomsmp.progression.TierTwoService.handleReview(p,resource.id,batch);com.mohymods.kingdomsmp.progression.TierTwoService.confirm(p,nonce);remaining-=batch;}}
            level.getServer().getCommands().getDispatcher().execute("kingdom admin milestone road confirm Isolated supplied inspected settlement road fixture",level.getServer().createCommandSourceStack());
            final String id="kingdom_smp:builder/j02";var from=base.offset(24,0,0);var to=from.offset(15,4,0);
            for(int x=0;x<16;x++)level.setBlock(from.offset(x,-1,0),Blocks.STONE.defaultBlockState(),3);
            for(int cx=from.getX()>>4;cx<=to.getX()>>4;cx++)for(int cz=(from.getZ()-1)>>4;cz<=(to.getZ()+1)>>4;cz++){var point=new BlockPos(cx*16,from.getY(),cz*16);ClaimRuntimeTests.ftbUnclaim(level,point);ClaimRuntimeTests.ftbClaim(p,point);ClaimRuntimeTests.opacUnclaim(level,point);ClaimRuntimeTests.opacClaim(level,p.getUUID(),point);}
            admin.moveTo(from.getX()+7.5,from.getY()+1,from.getZ()+4.5);p.moveTo(from.getX()+8.5,from.getY()+1,from.getZ()+4.5);level.getServer().getPlayerList().op(admin.getGameProfile());var nonce=RetainedBuildingService.reviewSite(admin,p,id,from,to,"Consented personally supplied masonry and glass facade");RetainedBuildingService.confirmSite(admin,nonce);level.getServer().getPlayerList().deop(admin.getGameProfile());sites.put(id,store.builderWorksite(p.getUUID(),id).orElseThrow());
            for(int y=0;y<5;y++)for(int x=0;x<16;x++)place(from.offset(x,y,0),Items.STONE_BRICKS);
            h.runAfterDelay(3,()->guard(()->{near();h.assertTrue(RetainedBuildingService.scan(p,id).count()==64,"Eighty native masonry blocks cannot replace the required sixteen glass blocks");for(int x=0;x<16;x++){var pos=from.offset(x,4,0);p.moveTo(pos.getX()+2.5,pos.getY()+1,pos.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));h.assertTrue(p.gameMode.destroyBlock(pos),"Actual native removal replaces only supplied private facade stock");place(pos,Items.GLASS);}h.runAfterDelay(3,()->guard(this::finishFacade));}));
        }
        void finishFacade()throws Exception{
            final String id="kingdom_smp:builder/j02";near();h.assertTrue(RetainedBuildingService.scan(p,id).count()==80,"Exactly64supported native masonry plus16actual glass positions complete the facade");var view=CommerceService.handle(p,new CommerceRequest(95001,1,7,"quest:"+id,1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal Royal interface reviews earned facade");CommerceService.handle(p,new CommerceRequest(95002,2,7,"quest:"+id,1,view.review()));h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==1000,"Actual full facade awards200XPonce after earned Apprentice and real Tier3");
            inspectedProjects();
        }
        void claimArea(BlockPos center)throws Exception{claimArea(center,9);}
        void claimArea(BlockPos center,int radius)throws Exception{
            for(int cx=(center.getX()-radius)>>4;cx<=(center.getX()+radius)>>4;cx++)for(int cz=(center.getZ()-radius)>>4;cz<=(center.getZ()+radius)>>4;cz++){level.getChunk(cx,cz);var point=new BlockPos(cx*16,center.getY(),cz*16);ClaimRuntimeTests.ftbUnclaim(level,point);ClaimRuntimeTests.ftbClaim(p,point);ClaimRuntimeTests.opacUnclaim(level,point);ClaimRuntimeTests.opacClaim(level,p.getUUID(),point);}
        }
        void inspect(String id,BlockPos center,String reason)throws Exception{
            admin.moveTo(center.getX()+2.5,center.getY()+1,center.getZ()+2.5);p.moveTo(center.getX()+3.5,center.getY()+1,center.getZ()+2.5);level.getServer().getPlayerList().op(admin.getGameProfile());int radius=id.endsWith("j05")||id.endsWith("j08")?26:9;for(int cx=(center.getX()-radius)>>4;cx<=(center.getX()+radius)>>4;cx++)for(int cz=(center.getZ()-radius)>>4;cz<=(center.getZ()+radius)>>4;cz++)level.getChunk(cx,cz);var nonce=ProfessionInspectionService.review(admin,p,id,center,reason);ProfessionInspectionService.confirm(admin,nonce);level.getServer().getPlayerList().deop(admin.getGameProfile());
        }
        void inspectedProjects()throws Exception{
            var bridge=base.offset(24,2,-8);claimArea(bridge.east(7));
            for(var pos:BlockPos.betweenClosed(bridge.offset(-1,-2,-1),bridge.offset(14,2,1)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            for(int x:List.of(0,13)){level.setBlock(bridge.offset(x,-3,0),Blocks.STONE.defaultBlockState(),3);place(bridge.offset(x,-2,0),Items.STONE_BRICKS);place(bridge.offset(x,-1,0),Items.STONE_BRICKS);}
            for(int x=0;x<14;x++)place(bridge.east(x),Items.OAK_PLANKS);
            inspect("kingdom_smp:builder/j01",bridge.east(7),"Personally inspected supplied twelve block bridge and grounded approaches");
            for(int z=8;z<17;z++)place(base.offset(1,0,z),Items.COBBLESTONE);var access=building.origin().offset(6,0,-3);claimArea(access);inspect("kingdom_smp:builder/j03",access,"Personally verified accessible protected improved Builder workshop approach");
            var room=base.offset(24,0,8);claimArea(room.offset(4,1,4));for(var pos:BlockPos.betweenClosed(room.offset(-1,0,-1),room.offset(10,5,10)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            for(int x=0;x<10;x++)for(int z=0;z<10;z++){level.setBlock(room.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);place(room.offset(x,0,z),Items.OAK_PLANKS);}
            for(int y=1;y<=3;y++)for(int x=0;x<10;x++)for(int z=0;z<10;z++)if((x==0||x==9||z==0||z==9)&&!(x==5&&z==0&&y<3))place(room.offset(x,y,z),Items.OAK_PLANKS);
            for(int z=0;z<10;z++)for(int x=0;x<10;x++)place(room.offset(x,4,z),Items.OAK_PLANKS);
            place(room.offset(5,1,0),Items.OAK_DOOR);place(room.offset(2,1,2),Items.CRAFTING_TABLE);place(room.offset(5,1,2),Items.FURNACE);place(room.offset(7,1,2),Items.CHEST);place(room.offset(2,1,5),Items.LECTERN);place(room.offset(5,1,5),Items.RED_BED);place(room.offset(7,1,5),Items.LOOM);place(room.offset(1,1,1),Items.TORCH);place(room.offset(8,1,8),Items.TORCH);
            inspect("kingdom_smp:builder/j04",room.offset(4,1,4),"Personally inspected roofed accessible room with six spaced functional areas");
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==1600,"Actual bridge public access and six zone interior award three inspected200XP proofs once");
            near();p.getInventory().clearContent();for(int slot=0;slot<8;slot++)p.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));var config=level.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commission-quests.json");java.nio.file.Files.writeString(config,java.nio.file.Files.readString(config).replace(": 2",": 1"));CommissionQuestRules.clear(level.getServer());var contract=ContractService.review(p,"builder/weekly");ContractService.confirm(p,contract.nonce());var commission=ProfessionService.review(p,"quest:kingdom_smp:builder/j07");ProfessionService.confirm(p,commission.nonce());
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==1800,"Actual full512cobblestone weekly commission at Tier3 adds200XP without bypassing missing band objectives");
            gradedRoute();
        }
        void gradedRoute()throws Exception{
            var route=base.offset(48,2,-16);var center=route.offset(0,1,24);claimArea(center,26);
            for(int n=0;n<48;n++){var pos=route.offset(0,n<24?0:1,n);for(int y=base.getY()-1;y<pos.getY();y++)level.setBlock(new BlockPos(pos.getX(),y,pos.getZ()),Blocks.STONE.defaultBlockState(),3);var channel=pos.east();level.setBlock(channel,Blocks.AIR.defaultBlockState(),18);level.setBlock(channel.above(),Blocks.AIR.defaultBlockState(),18);level.setBlock(channel.below(),Blocks.STONE.defaultBlockState(),3);place(pos,n==23?Items.STONE_BRICK_STAIRS:Items.STONE_BRICKS);}
            inspect("kingdom_smp:builder/j05",center,"Personally inspected supplied graded48cell route shallow drainage and consent");
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==2000,"Approved48cell drainage and grading inspection awards200XPonce");commissionProject(0);
        }
        BlockPos commissionOrigin(int n){return base.offset(64,0,4+n*6);}
        void authorizeCommission(int n)throws Exception{
            var from=commissionOrigin(n);claimArea(from);for(int x=0;x<4;x++)for(int z=0;z<4;z++)level.setBlock(from.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
            admin.moveTo(from.getX()+1.5,from.getY()+1,from.getZ()+5.5);p.moveTo(from.getX()+2.5,from.getY()+1,from.getZ()+5.5);level.getServer().getPlayerList().op(admin.getGameProfile());var nonce=RetainedBuildingService.reviewSite(admin,p,"kingdom_smp:builder/j06",from,from.offset(3,2,3),"Separately authorized supplied timber masonry or glass construction");RetainedBuildingService.confirmSite(admin,nonce);level.getServer().getPlayerList().deop(admin.getGameProfile());
        }
        void commissionProject(int n)throws Exception{
            authorizeCommission(n);var from=commissionOrigin(n);Item material=n==0?Items.OAK_PLANKS:n==1?Items.STONE_BRICKS:Items.GLASS;for(int x=0;x<4;x++)for(int z=0;z<4;z++)place(from.offset(x,0,z),material);
            h.runAfterDelay(3,()->guard(()->{if(n<2)commissionProject(n+1);else verifyHistoricalCommission();}));
        }
        void verifyHistoricalCommission()throws Exception{
            var center=commissionOrigin(0).offset(1,1,1);var old=commissionOrigin(0);p.moveTo(old.getX()+2.5,old.getY()+1,old.getZ()+2.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));h.assertTrue(p.gameMode.destroyBlock(old),"Real player removes a retained block from a superseded commission authorization");
            admin.moveTo(center.getX()+2.5,center.getY()+1,center.getZ()+2.5);p.moveTo(center.getX()+3.5,center.getY()+1,center.getZ()+2.5);level.getServer().getPlayerList().op(admin.getGameProfile());boolean refused=false;try{ProfessionInspectionService.review(admin,p,"kingdom_smp:builder/j06",center,"Historical native project must remain retained and protected");}catch(IllegalStateException expected){refused=true;}level.getServer().getPlayerList().deop(admin.getGameProfile());h.assertTrue(refused,"Breaking a historical project's material invalidates aggregate commission evidence");
            authorizeCommission(0);for(int x=0;x<4;x++)for(int z=0;z<4;z++){var pos=old.offset(x,0,z);if(!level.getBlockState(pos).isAir()){p.moveTo(pos.getX()+2.5,pos.getY()+1,pos.getZ()+2.5);h.assertTrue(p.gameMode.destroyBlock(pos),"Actual native reconstruction supplies fresh scoped proof");}place(pos,Items.OAK_PLANKS);}h.runAfterDelay(3,()->guard(this::handover));
        }
        void handover()throws Exception{
            inspect("kingdom_smp:builder/j06",commissionOrigin(0).offset(1,1,1),"Personally inspected three separate supplied timber stone and glass commissions");
            var center=base.offset(34,1,9);claimArea(center,26);inspect("kingdom_smp:builder/j08",center,"Personally inspected retained materials route furnished room and site consent");
            h.assertTrue(store.professions(p.getUUID()).track(Profession.BUILDER).xp()==2400&&store.professions(p.getUUID()).track(Profession.BUILDER).completedQuestIds().size()==16&&store.professions(p.getUUID()).progressBand(Profession.BUILDER)==ProfessionRank.MASTER&&store.professions(p.getUUID()).rank(Profession.BUILDER,ProfessionService.quests())==ProfessionRank.JOURNEYMAN,"All16real Apprentice and Journeyman objectives earn2400XP and open Master trials without fabricating Master rank");
            var data=RetainedBuildingSavedData.get(level.getServer());data.setDirty();level.getDataStorage().save();net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
            var file=level.getServer().getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_retained_buildings_v1.dat");var persisted=RetainedBuildingSavedData.load(AtomicNbt.read(file).getCompound("data"),level.registryAccess());
            h.assertTrue(persisted.evidence().positions(sites.get("kingdom_smp:builder/a01").id()).size()==128,"Actual native SavedData retains distinct placement provenance");
            var reopened=EconomyStore.open(level.getServer().getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.builderWorksites().equals(store.builderWorksites())&&reopened.professions(p.getUUID()).equals(store.professions(p.getUUID())),"Canonical operator authorizations, current sites, awards and rank independently reopen");CommerceService.verifyReceipt(p);
            h.succeed();com.mojang.logging.LogUtils.getLogger().info("RETAINED_BUILDING_ASSERTIONS_PASSED: actual304native supplied placements plus native stripping/shovel use,128foundation64logs64roof48route, actor authorization, reservation denial, automatic-write refusal, break/replace distinct proof, FTB/OPACrevocation, Royal packet completion, native craft/consumed contract and on-site inspections, all8earnedBuilderApprentice800XP/Journeyman plus actual Tier3/64masonry16glass200XPfacade and native inspected bridge/public access/6zone interior/full512weekly plus graded48cellroute and3native commissions/final handover for all16objectives2400XP/Master trials, unknown-log strip refusal, final retained-roof invalidation, native/canonical saved reopen; supplies explicit, private clock200TPS and daily/weekly period goals accelerated");
        }
        void guard(Checked action){try{action.run();}catch(Exception e){h.fail(e.toString());}}
        interface Checked{void run()throws Exception;}
        static String coords(BlockPos p){return p.getX()+" "+p.getY()+" "+p.getZ();}
    }
}
