package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.progression.WorldProgression;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

/** Complete new-profile loop against the actual pinned claim dependencies on a normal server. */
@GameTestHolder("kingdom_smp_townhall_tests")
@PrefixGameTestTemplate(false)
public final class TownHallRuntimeTests {
    @GameTest(template="empty",timeoutTicks=1800)
    public static void newWorldLoop(GameTestHelper h)throws Exception{
        if(ConstructionService.lodge(h.getLevel().getServer())!=null){restart(h);return;}
        newWorldLoop(h,()->{h.succeed();com.mojang.logging.LogUtils.getLogger().info("TOWN_HALL_TEST_ASSERTIONS_PASSED: owner Hut, real contributions, exclusive winner Hall, both claim APIs, furnished two floors, progressive handover, unique named Steward and persistent records");});
    }
    private static void restart(GameTestHelper h)throws Exception{
      var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);var hut=BuilderHutSavedData.get(server).hut().orElseThrow();var owner=ClaimRuntimeTests.player(h.getLevel(),ConstructionService.store(server).reward(ConstructionService.STARTING_HUT).orElseThrow().owner(),ConstructionService.store(server).reward(ConstructionService.STARTING_HUT).orElseThrow().ownerName());owner.moveTo(hut.origin().getX()+12,hut.origin().getY()+1,hut.origin().getZ()+5);h.getLevel().getChunkSource().move(owner);for(var p:ConstructionService.store(server).projects()){var bounds=ConstructionService.bounds(p);for(int x=((int)bounds.minX)>>4;x<=((int)bounds.maxX)>>4;x++)for(int z=((int)bounds.minZ)>>4;z<=((int)bounds.maxZ)>>4;z++)h.getLevel().getChunk(x,z);}
      h.runAfterDelay(40,()->{try{h.assertTrue(BuilderHutService.structureIntact(h.getLevel(),hut)&&ConstructionService.structureIntact(h.getLevel(),hall),"Actual server restart restores converted Hut and complete Hall blocks");h.assertTrue(h.getLevel().getEntity(hut.npcId()) instanceof com.mohymods.kingdomsmp.npc.AldricEntity&&h.getLevel().getEntity(hall.npc()) instanceof com.mohymods.kingdomsmp.npc.ReeveEntity,"Actual restart restores original Aldric and Steward UUIDs");h.assertTrue(h.getLevel().getEntities(com.mohymods.kingdomsmp.npc.KingdomNpcs.STEWARD.get(),e->true).size()==1,"No duplicate Steward on restart");HutFurnitureRegression.use(h,owner,hut);h.succeed();com.mojang.logging.LogUtils.getLogger().info("TOWN_HALL_RESTART_VERIFIED: same owner/Hut/converted table inventory/Aldric and completed Hall/Steward persisted, no project or NPC duplication");}catch(Exception e){h.fail(e.toString());}});
    }
    /** Shared real-world fixture; completion runs only after full native Hall handover checks. */
    public static void newWorldLoop(GameTestHelper h,Runnable completion)throws Exception{
        var level=h.getLevel();var server=level.getServer();
        var profile=WorldProgression.get(server);
        h.assertTrue(profile.mode()==WorldProgression.Mode.ROYAL_HALL&&profile.owner()==null,"Actual new dedicated world uses new profile and requires explicit owner");
        var owner=ClaimRuntimeTests.player(level,"HallOwner");var winner=ClaimRuntimeTests.player(level,"HallWinner");
        BlockPos hutOrigin=h.absolutePos(new BlockPos(2,1,2)),site=hutOrigin.offset(24,0,0);
        for(var p:BlockPos.betweenClosed(hutOrigin.offset(-4,-1,-4),site.offset(25,-1,22)))level.setBlock(p,Blocks.STONE.defaultBlockState(),18);
        owner.moveTo(hutOrigin.getX()-2,hutOrigin.getY()+1,hutOrigin.getZ());winner.moveTo(owner.getX()-1,owner.getY(),owner.getZ());
        owner.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,64));
        try{ContributionService.quote(owner,Resource.LOGS,1);h.fail("New-world donation allowed before Hut bootstrap");}catch(IllegalStateException expected){}
        owner.getInventory().clearContent();
        var dispatcher=server.getCommands().getDispatcher();
        h.assertTrue(dispatcher.execute("kingdom admin owner HallOwner confirm",server.createCommandSourceStack())==1,"Owner assignment is explicit and durable");
        var store=ConstructionService.store(server);var starter=store.reward(ConstructionService.STARTING_HUT).orElseThrow();
        h.assertTrue(starter.owner().equals(owner.getUUID()),"Only actual starting owner earns free Hut");
        owner.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);boolean creativeDenied=false;try{ConstructionService.review(owner,1);}catch(IllegalStateException expected){creativeDenied=expected.getMessage().contains("Creative");}h.assertTrue(creativeDenied,"Creative claim is denied with an explicit Survival/Adventure explanation");owner.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var claim=ConstructionService.review(owner,1);h.assertTrue(!ConstructionService.hasPlan(owner,starter),"Claim review alone does not deliver a plan; explicit confirmation is required");String claimFeedback=ConstructionService.confirm(owner,claim.nonce(),2);h.assertTrue(claimFeedback.contains("slot 1")&&ConstructionService.hasPlan(owner,store.get(starter.id())),"Confirmed free Hut plan goes to exact first hotbar slot with usable feedback");
        h.assertTrue(ConstructionService.heldPlan(owner,store.get(starter.id())),"Claim gives authenticated physical Hut item");
        ClaimRuntimeTests.ftbClaim(owner,hutOrigin);ClaimRuntimeTests.opacClaim(level,owner.getUUID(),hutOrigin);
        var hutPreview=ConstructionService.preview(owner,hutOrigin,Rotation.NONE);
        h.assertTrue(hutPreview.denial().isEmpty(),"Owner site passes native Hut and both claim checks: "+hutPreview.denial());
        var review=ConstructionService.view(owner,1,false,"");ConstructionService.confirm(owner,review.review(),4);
        h.assertTrue(store.get(starter.id()).phase()==ConstructionStore.Phase.APPROVED,"Valid new-profile site needs no routine admin click");
        var start=ConstructionService.review(owner,7);ConstructionService.confirm(owner,start.nonce(),8);
        h.assertTrue(store.get(starter.id()).phase()==ConstructionStore.Phase.COMPLETE,"Free Hut bootstrap finishes through actual native installer: "+ConstructionService.activity(starter.id()));
        var hut=BuilderHutSavedData.get(server).hut().orElseThrow();
        h.assertTrue(hut.npcId().equals(starter.npc())&&BuilderHutService.structureIntact(level,hut),"One Aldric identity and complete furnished Hut");
        HutFurnitureRegression.use(h,owner,hut);
        donate(owner,Resource.LOGS,Items.OAK_LOG,1024);
        donate(winner,Resource.COBBLESTONE,Items.COBBLESTONE,2048);
        donate(winner,Resource.WHEAT,Items.WHEAT,512);
        donate(winner,Resource.IRON_INGOTS,Items.IRON_INGOT,128);
        h.assertTrue(ContributionService.kingdom(server).attainedTier()==2&&ContributionService.ledger(server).progress().winner().orElseThrow().equals(winner.getUUID()),"Actual consumed normalized contributions award permanent highest contributor, not initial owner");
        var hall=ConstructionService.lodge(server);
        h.assertTrue(ConstructionService.current(owner).id().equals(hall.id()),"Completed Hut screen transitions to the newly awarded Hall");
        h.assertTrue(hall.blueprint().equals(TownHallBlueprint.ID)&&store.projects().size()==2,"New winner gets Hall, no legacy Lodge or duplicate award");
        var hallClaim=ConstructionService.review(winner,1);ConstructionService.confirm(winner,hallClaim.nonce(),2);
        h.assertTrue(winner.getMainHandItem().is(KingdomBlueprints.HALL.get()),"Winner holds distinct authenticated Hall item");
        h.assertTrue(dispatcher.execute("kingdom lodge name Elara Vale of Dawn",winner.createCommandSourceStack())==1,"Holder can select new Steward display name without changing persistent identity");
        var b=BlueprintRegistry.get(TownHallBlueprint.ID,1);
        h.assertTrue(b.width()==19&&b.depth()==15&&b.height()>10,"Actual two-floor Hall bounds");
        for(int cx=site.getX()>>4;cx<=(site.getX()+18)>>4;cx++)for(int cz=(site.getZ()-1)>>4;cz<=(site.getZ()+14)>>4;cz++){
            var chunkPoint=new BlockPos(cx*16,site.getY(),cz*16);ClaimRuntimeTests.ftbClaim(winner,chunkPoint);ClaimRuntimeTests.opacClaim(level,winner.getUUID(),chunkPoint);
        }
        winner.moveTo(site.getX()-2,site.getY()+1,site.getZ());
        for(var rotation:Rotation.values()){
            var preview=ConstructionService.inspectCandidate(winner,site,rotation);
            // Rectangular rotated sites may need one extra chunk claim. Exercise geometry natively below.
            h.assertTrue(b.blocks(rotation).size()==b.blocks().size(),"Rotation preserves every furnished block");
        }
        var preview=ConstructionService.preview(winner,site,Rotation.NONE);h.assertTrue(preview.denial().isEmpty(),"Full Hall footprint accepted: "+preview.denial());
        var quote=ConstructionService.view(winner,2,false,"");h.assertTrue(quote.blueprint().equals(TownHallBlueprint.ID)&&quote.width()==19,"Actual network projection selects Hall geometry");
        ConstructionService.confirm(winner,quote.review(),4);
        var begin=ConstructionService.review(winner,7);ConstructionService.confirm(winner,begin.nonce(),8);
        var builder=level.getEntity(hut.npcId());var post=b.workPost(site,Rotation.NONE);builder.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);
        int prefix=store.get(hall.id()).placed();var wall=hut.origin().offset(new BlockPos(1,2,1));var wallState=level.getBlockState(wall);level.setBlock(wall,Blocks.AIR.defaultBlockState(),18);ConstructionService.step(server,hall.id());h.assertTrue(store.get(hall.id()).placed()==prefix&&!BuilderHutService.structureIntact(level,hut),"Real Hut wall loss pauses same Town Hall project without granting blocks");level.setBlock(wall,wallState,18);ConstructionService.step(server,hall.id());h.assertTrue(store.get(hall.id()).placed()>prefix&&store.get(hall.id()).npc().equals(hall.npc()),"Exact Hut repair resumes original Hall prefix and reserved NPC identity");
        boolean[] verifying={false};
        h.onEachTick(()->{
            if(verifying[0])return;
            try{
                for(int i=0;i<2&&store.get(hall.id()).phase()==ConstructionStore.Phase.BUILDING;i++)ConstructionService.step(server,hall.id());
                var project=store.get(hall.id());
                if(project.phase()!=ConstructionStore.Phase.COMPLETE)return;
                h.assertTrue(project.placed()==b.blocks().size()&&ConstructionService.structureIntact(level,project),"Every furnished block verified at handover");
                var npc=level.getEntity(project.npc());
                h.assertTrue(npc!=null&&npc.getType()==com.mohymods.kingdomsmp.npc.KingdomNpcs.STEWARD.get()&&npc.getName().getString().equals("Elara Vale of Dawn"),"One named permanent Steward, independent of legacy Mira");
                h.assertTrue(level.getEntities(com.mohymods.kingdomsmp.npc.KingdomNpcs.STEWARD.get(),e->true).size()==1,"Exactly one Steward");
                h.assertTrue(level.getEntities(com.mohymods.kingdomsmp.npc.KingdomNpcs.REEVE.get(),e->true).isEmpty(),"No Mira generated in new-world Hall profile");
                var reloaded=ConstructionStore.open(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data"));
                h.assertTrue(reloaded.get(project.id()).equals(project),"Durable native journal reload retains entire Hall and resident identity");
                verifying[0]=true;
                h.runAfterDelay(20,()->{
                    h.assertTrue(ConstructionService.structureIntact(level,store.get(hall.id())),"Hall remains intact after neighbor updates");
                    for(var block:b.blocks()){
                        var position=site.offset(block.offset());
                        h.assertTrue(level.getBlockState(position).canSurvive(level,position),"Furnishing has valid physical support: "+position);
                    }
                    // The Steward normally stays within four blocks of her workstation.
                    // Remove only this fixture restriction while checking the physical stair route.
                    var resident=(net.minecraft.world.entity.PathfinderMob)npc;
                    resident.clearRestriction();
                    // Start the geometry check at the verified home, rather than during
                    // an arbitrary AI jump/wander tick (createPath requires grounded state).
                    resident.getNavigation().stop();var stairStart=site.offset(b.home());
                    h.assertTrue(level.getBlockState(stairStart).isAir()&&level.getBlockState(stairStart.above()).isAir()&&level.getBlockState(stairStart.below()).isFaceSturdy(level,stairStart.below(),net.minecraft.core.Direction.UP),"Stair navigation starts on clear supported floor");
                    resident.moveTo(stairStart.getX()+.5,stairStart.getY(),stairStart.getZ()+.5);resident.setOnGround(true);
                    h.assertTrue(level.getBlockState(site.offset(b.board())).is(Blocks.LECTERN),"Service board is an actual unobstructed furnished lectern");
                    var route=resident.getNavigation().createPath(site.offset(10,6,11),0);
                    h.assertTrue(route!=null&&route.canReach(),"Physical ground-to-gallery stairs are navigable: "+(route==null?"no path":route.getEndNode().asBlockPos()));
                    resident.restrictTo(stairStart,4); // Restore the production workstation routine after this isolated geometry check.
                    // GameTestInfo iterates its scheduled-callback map here. Queue the
                    // next phase outside that iterator before it registers another per-tick task.
                    server.tell(new net.minecraft.server.TickTask(server.getTickCount()+1,completion));
                });
            }catch(Exception e){h.fail(e.getMessage()==null?e.toString():e.getMessage());}
        });
    }
    private static void donate(net.minecraft.server.level.ServerPlayer p,Resource resource,Item item,int count){
        p.getInventory().clearContent();for(int slot=0;count>0;slot++){int amount=Math.min(64,count);p.getInventory().setItem(slot,new ItemStack(item,amount));count-=amount;}
        var offer=ContributionService.quote(p,resource,resource.required());ContributionService.confirm(p,offer.id());
    }
}
