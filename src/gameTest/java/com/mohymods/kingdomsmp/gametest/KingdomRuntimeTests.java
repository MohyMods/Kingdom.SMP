package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData;
import com.mohymods.kingdomsmp.persistence.KingdomSavedData;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.TierOneProgress.Resource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;

import com.mohymods.kingdomsmp.building.BuilderHutBlueprint;
import com.mohymods.kingdomsmp.building.BuilderHutService;
import com.mohymods.kingdomsmp.building.ConstructionService;
import com.mohymods.kingdomsmp.building.BlueprintRegistry;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import com.mohymods.kingdomsmp.persistence.ConstructionStore.Phase;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.kingdom.KingdomState;
import com.mohymods.kingdomsmp.npc.AldricEntity;
import com.mohymods.kingdomsmp.npc.KingdomNpcs;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Loaded by the isolated GameTest run; never packaged in the playable mod. */
@Mod("kingdom_smp_tests")
@GameTestHolder("kingdom_smp_tests")
@PrefixGameTestTemplate(false)
public final class KingdomRuntimeTests {
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void hutLifecycle(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        helper.assertTrue(KingdomSavedData.get(server).snapshot().equals(KingdomState.initial()), "Real server starts at Tier 1");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        for (BlockPos support : BlockPos.betweenClosed(origin.below(), origin.offset(10, -1, 10))) {
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 18);
        }
        var operator = new RecordingPlayer(level);
        operator.moveTo(origin.getX() - 2, origin.getY() + 1, origin.getZ() + 4);
        for (Rotation rotation : Rotation.values()) {
            helper.assertTrue(BuilderHutService.validateSite(level, origin, rotation, false) == null, "Clear rotated footprint accepted: " + rotation);
        }
        level.setBlock(origin.above(), Blocks.CHEST.defaultBlockState(), 18);
        helper.assertTrue(BuilderHutService.validateSite(level, origin, Rotation.NONE, false) != null, "Container collision denied");
        level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 18);
        helper.assertTrue(BuilderHutService.validateSite(level, new BlockPos(25_000_000, 80, 25_000_000), Rotation.NONE, false)
                .contains("loaded"), "Unloaded site denied without loading it");
        var dispatcher = server.getCommands().getDispatcher();
        String preview = "kingdom hut preview " + origin.getX() + " " + origin.getY() + " " + origin.getZ() + " north runtime-test";
        try {
            dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(0));
            helper.fail("Unprivileged setup command accepted");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
        helper.assertTrue(dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4)) == 1, "Admin preview accepted");
        helper.assertTrue(operator.messages.getLast().getString().contains("within 5 minutes"), "Review advertises the five-minute deadline");
        String clickedCommand = operator.confirmationCommand();
        try {
            dispatcher.execute(clickedCommand.substring(1), operator.createCommandSourceStack().withPermission(0));
            helper.fail("Clickable confirmation bypassed operator permission");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
        var otherOperator = new RecordingPlayer(level);
        otherOperator.moveTo(operator.getX(), operator.getY(), operator.getZ());
        helper.assertTrue(dispatcher.execute(clickedCommand.substring(1), otherOperator.createCommandSourceStack().withPermission(4)) == 0,
                "Copied chat link cannot confirm another player's preview");
        helper.assertTrue(BuilderHutSavedData.get(server).hut().isEmpty(), "Copied link does not reserve a hut");
        operator.moveTo(origin.getX() + 70, origin.getY() + 1, origin.getZ() + 4);
        helper.assertTrue(dispatcher.execute(clickedCommand.substring(1), operator.createCommandSourceStack().withPermission(4)) == 0,
                "Clickable confirmation retains the distance check");
        operator.moveTo(origin.getX() - 2, origin.getY() + 1, origin.getZ() + 4);
        dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4));
        helper.assertTrue(dispatcher.execute("kingdom hut confirm " + UUID.randomUUID(), operator.createCommandSourceStack().withPermission(4)) == 0,
                "Mismatched token cannot confirm a preview");
        dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4));
        String supersededCommand = operator.confirmationCommand();
        dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4));
        helper.assertTrue(!supersededCommand.equals(operator.confirmationCommand()), "Each preview issues a fresh token");
        helper.assertTrue(dispatcher.execute(supersededCommand.substring(1), operator.createCommandSourceStack().withPermission(4)) == 0,
                "A previous preview's chat link cannot confirm a newer site review");
        helper.assertTrue(BuilderHutSavedData.get(server).hut().isEmpty(), "Rejected confirmations leave the world unreserved");
        dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4));
        clickedCommand = operator.confirmationCommand();
        level.setBlock(origin.above(), Blocks.STONE.defaultBlockState(), 18);
        helper.assertTrue(dispatcher.execute(clickedCommand.substring(1), operator.createCommandSourceStack().withPermission(4)) == 0,
                "Confirmation revalidates stale site");
        helper.assertTrue(BuilderHutSavedData.get(server).hut().isEmpty(), "Stale preview does not reserve a hut");
        level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 18);
        dispatcher.execute(preview, operator.createCommandSourceStack().withPermission(4));
        Consumer<BlockEvent.EntityMultiPlaceEvent> deny = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            helper.assertTrue(dispatcher.execute(operator.confirmationCommand().substring(1), operator.createCommandSourceStack().withPermission(4)) == 0,
                    "Protection listener cancellation honored");
        } finally { NeoForge.EVENT_BUS.unregister(deny); }
        var data = BuilderHutSavedData.get(server);
        var reserved = data.hut().orElseThrow();
        helper.assertTrue(!reserved.npcSpawnCommitted(), "Denied placement does not issue an NPC");
        helper.assertTrue(level.getBlockState(origin).isAir(), "Cancelled placement rolls back newly installed blocks");
        helper.assertTrue(dispatcher.execute("kingdom hut resume " + reserved.id() + " test-resume", operator.createCommandSourceStack().withPermission(4)) == 1,
                "Exact reservation resumes successfully");
        var hut = data.hut().orElseThrow();
        helper.assertTrue(BuilderHutService.structureIntact(level, hut), "Furnished building passes actual-world block verification");
        helper.assertTrue(hut.phase() == BuilderHutSavedData.Phase.ACTIVE, "Hut active only after verification and builder issuance");
        AldricEntity builder = (AldricEntity) level.getEntity(hut.npcId());
        helper.assertTrue(builder != null && BuilderHutService.isRegistered(builder), "One registered builder with stable home and UUID");
        helper.assertTrue(dispatcher.execute("kingdom hut resume " + hut.id() + " duplicate", operator.createCommandSourceStack().withPermission(4)) == 0,
                "Completed installation cannot be replayed");
        helper.assertTrue(dispatcher.execute(clickedCommand.substring(1), operator.createCommandSourceStack().withPermission(4)) == 0,
                "Old confirmation cannot be replayed");
        dispatcher.execute("kingdom hut name Master Aldric", operator.createCommandSourceStack().withPermission(4));
        helper.assertTrue(builder.getName().getString().equals("Master Aldric") && builder.getUUID().equals(hut.npcId()), "Rename preserves stable identity");
        operator.moveTo(builder.getX(), builder.getY(), builder.getZ() + 2);
        helper.assertTrue(BuilderHutService.canInteract(operator, builder), "Nearby registered builder permits dialogue");
        operator.moveTo(builder.getX() + 20, builder.getY(), builder.getZ());
        helper.assertTrue(!BuilderHutService.canInteract(operator, builder), "Remote dialogue denied");
        operator.moveTo(builder.getX(), builder.getY(), builder.getZ() + 2);
        level.setBlock(origin, Blocks.AIR.defaultBlockState(), 18);
        helper.assertTrue(!BuilderHutService.canInteract(operator, builder), "Damage suspends dialogue");
        level.setBlock(origin, BuilderHutBlueprint.create().placements().getFirst().state(), 18);
        // Native SavedData disk write and a fresh storage instance, not just an in-memory NBT copy.
        var folder = Files.createTempDirectory("kingdom-native-save-test");
        var storage = new DimensionDataStorage(folder.toFile(), DataFixers.getDataFixer(), server.registryAccess());
        data.setDirty();
        storage.set(BuilderHutSavedData.DATA_NAME, data);
        storage.set(KingdomSavedData.DATA_NAME, KingdomSavedData.get(server));
        storage.save();
        IOUtilities.waitUntilIOWorkerComplete();
        var reloadedStorage = new DimensionDataStorage(folder.toFile(), DataFixers.getDataFixer(), server.registryAccess());
        var restored = reloadedStorage.readTagFromDisk(BuilderHutSavedData.DATA_NAME, null, 3955).getCompound("data");
        helper.assertTrue(restored.equals(data.save(new CompoundTag(), server.registryAccess())), "Native disk reload preserves site, rotation, name and issuance");
        var restoredKingdom = reloadedStorage.readTagFromDisk(KingdomSavedData.DATA_NAME, null, 3955).getCompound("data");
        helper.assertTrue(restoredKingdom.getInt("tier") == 1, "Native kingdom disk reload retains Tier 1");
        var entityTag = builder.saveWithoutId(new CompoundTag());
        AldricEntity restoredEntity = KingdomNpcs.ALDRIC.get().create(level);
        restoredEntity.load(entityTag);
        helper.assertTrue(restoredEntity.getUUID().equals(builder.getUUID()) && restoredEntity.hutId().equals(hut.id())
                && restoredEntity.home().equals(builder.home()), "Entity serialization preserves registered identity/home");
        contributionLifecycle(helper, operator, otherOperator);
        constructionLifecycle(helper,operator,otherOperator,builder,origin);
        milestoneLifecycle(helper,operator);
        travelGateLifecycle(helper,operator);
        builder.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(data.hut().orElseThrow().npcDead(), "Death is persisted and cannot reissue a builder");
        helper.assertTrue(!BuilderHutService.canInteract(operator, restoredEntity), "Restored entity cannot bypass persisted death");
        helper.runAfterDelay(30,()->{
            helper.assertTrue(ConstructionService.structureIntact(level,ConstructionService.lodge(server)),"Furnished Lodge remains intact after scheduled neighbor updates");
            helper.succeed();
        });
    }

    private static void contributionLifecycle(GameTestHelper helper, RecordingPlayer alice, RecordingPlayer bob) throws Exception {
        var server = helper.getLevel().getServer();
        alice.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        give(alice, Items.OAK_LOG, 100);
        try { ContributionService.quote(alice, Resource.LOGS, 100); helper.fail("Creative contribution accepted"); }
        catch (IllegalStateException expected) { }
        alice.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        bob.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var offer = ContributionService.quote(alice, Resource.LOGS, 1024);
        com.mohymods.kingdomsmp.milestone.MilestoneService.reload(server);
        try{ContributionService.confirm(alice,offer.id());helper.fail("Pre-reload donation review accepted");}catch(IllegalStateException expected){}
        helper.assertTrue(ContributionService.eligible(alice,Resource.LOGS)==100&&ContributionService.ledger(server).progress().total(Resource.LOGS)==0,"Catalogue reload invalidates reviews without consuming or crediting resources");
        offer=ContributionService.quote(alice,Resource.LOGS,1024);
        helper.assertTrue(offer.amount() == 100, "Partial contribution capped to actual inventory");
        try { ContributionService.confirm(bob, offer.id()); helper.fail("Copied donation quote accepted"); }
        catch (IllegalStateException expected) { }
        ContributionService.confirm(alice, offer.id());
        helper.assertTrue(ContributionService.eligible(alice, Resource.LOGS) == 0, "Accepted resources consumed");
        helper.assertTrue(ContributionService.ledger(server).progress().total(Resource.LOGS) == 100, "Credit persisted after native inventory checkpoint");
        try { ContributionService.confirm(alice, offer.id()); helper.fail("Donation replay accepted"); }
        catch (IllegalStateException expected) { }
        for (int i=0;i<9;i++) {
            give(alice, Items.OAK_LOG, 1);
            var small = ContributionService.quote(alice, Resource.LOGS, 1);
            ContributionService.confirm(alice, small.id());
        }
        give(alice, Items.WHEAT, 64);
        var stale = ContributionService.quote(alice, Resource.WHEAT, 64);
        alice.getInventory().getItem(0).shrink(1);
        try { ContributionService.confirm(alice, stale.id()); helper.fail("Changed inventory accepted"); }
        catch (IllegalStateException expected) { }
        helper.assertTrue(ContributionService.eligible(alice, Resource.WHEAT) == 63, "Failed quote consumes nothing");
        give(alice, Items.IRON_INGOT, 80); give(bob, Items.IRON_INGOT, 80);
        var first = ContributionService.quote(alice, Resource.IRON_INGOTS, 80);
        var second = ContributionService.quote(bob, Resource.IRON_INGOTS, 80);
        ContributionService.confirm(alice, first.id());
        try { ContributionService.confirm(bob, second.id()); helper.fail("Stale final-resource quote accepted"); }
        catch (IllegalStateException expected) { }
        helper.assertTrue(ContributionService.eligible(bob, Resource.IRON_INGOTS) == 80, "Race loser retains all inventory");
        var capped = ContributionService.quote(bob, Resource.IRON_INGOTS, 80);
        helper.assertTrue(capped.amount() == 48, "Final contribution capped to outstanding requirement");
        ContributionService.confirm(bob, capped.id());
        helper.assertTrue(ContributionService.eligible(bob, Resource.IRON_INGOTS) == 32, "Excess inventory retained");
        // Inject an interruption after the removed inventory has reached its native file.
        var carol = new RecordingPlayer(helper.getLevel());
        carol.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        give(carol, Items.WHEAT, 1);
        var interrupted = ContributionService.quote(carol, Resource.WHEAT, 1);
        var saves = new java.util.concurrent.atomic.AtomicInteger();
        Consumer<net.neoforged.neoforge.event.entity.player.PlayerEvent.SaveToFile> failAfterSave = event -> {
            if(event.getEntity()==carol && saves.incrementAndGet()==2) throw new IllegalStateException("Injected interruption after native removal checkpoint");
        };
        NeoForge.EVENT_BUS.addListener(failAfterSave);
        try {
            try { ContributionService.confirm(carol, interrupted.id()); helper.fail("Injected checkpoint interruption ignored"); }
            catch (IllegalStateException expected) { }
        } finally { NeoForge.EVENT_BUS.unregister(failAfterSave); }
        helper.assertTrue(saves.get()==2, "Baseline and removal checkpoints both executed");
        helper.assertTrue(ContributionService.ledger(server).quarantined(), "Storage uncertainty pauses all donations");
        helper.assertTrue(ContributionService.ledger(server).progress().total(Resource.WHEAT) == 0, "Unverified save gives no premature credit");
        ContributionService.initialize(server);
        helper.assertTrue(ContributionService.ledger(server).progress().total(Resource.WHEAT) == 1, "Restart recovers durable removal exactly once");
        ContributionService.initialize(server);
        helper.assertTrue(ContributionService.ledger(server).progress().total(Resource.WHEAT) == 1, "Repeated recovery cannot duplicate credit");
        Item[] items = {Items.OAK_LOG, Items.COBBLESTONE, Items.WHEAT};
        Resource[] resources = {Resource.LOGS, Resource.COBBLESTONE, Resource.WHEAT};
        for (int i = 0; i < items.length; i++) {
            give(alice, items[i], resources[i].required());
            var finish = ContributionService.quote(alice, resources[i], resources[i].required());
            ContributionService.confirm(alice, finish.id());
        }
        var ledger = ContributionService.ledger(server);
        helper.assertTrue(ledger.progress().complete(), "All four targets complete");
        helper.assertTrue(ledger.progress().winner().orElseThrow().equals(alice.getUUID()), "Correct permanent weighted winner");
        helper.assertTrue(ContributionService.kingdom(server).attainedTier() == 2, "First Hearth advances to Tier 2, not Town Hall");
        long revision = ledger.progress().revision();
        ContributionService.initialize(server);
        var restored = ContributionService.ledger(server);
        helper.assertTrue(restored.progress().complete() && restored.progress().revision() == revision, "Ledger restart retains all contributions");
        helper.assertTrue(ContributionService.kingdom(server).attainedTier() == 2, "Tier 2 persists through ledger restart");
        helper.assertTrue(restored.history().size() == 16, "All accepted operations retained; failed operations absent");
        var latest = com.mohymods.kingdomsmp.network.ContributionNetworking.view(alice,1,"",null,0);
        var older = com.mohymods.kingdomsmp.network.ContributionNetworking.view(alice,2,"",null,1);
        helper.assertTrue(latest.historyPages()==2 && latest.history().size()==8 && older.historyPage()==1 && older.history().size()==6,
                "Every accepted personal donation is available through bounded history pages");
        helper.assertTrue(older.history().getLast().count()==100, "Oldest accepted donation remains inspectable");
        // Simulate the integrated host loading an old level.dat inventory with no receipt.
        alice.getPersistentData().remove(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        give(alice, Items.OAK_LOG, 1024);
        ContributionService.login(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(alice));
        helper.assertTrue(ContributionService.eligible(alice, Resource.LOGS) == 0, "Stale integrated-host inventory reconciles from durable native checkpoint");
    }
    private static void constructionLifecycle(GameTestHelper helper,RecordingPlayer alice,RecordingPlayer bob,AldricEntity builder,BlockPos hutOrigin) throws Exception {
        ServerLevel level=helper.getLevel();var server=level.getServer();ConstructionService.initialize(server);
        var awarded=ConstructionService.lodge(server);
        helper.assertTrue(awarded.owner().equals(alice.getUUID())&&awarded.phase()==Phase.AVAILABLE,"Permanent winner owns one available Lodge entitlement");
        try{ConstructionService.review(bob,1);helper.fail("Other player claimed winner's blueprint");}catch(IllegalStateException expected){ }
        for(int i=0;i<36;i++)alice.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        try{ConstructionService.review(alice,1);helper.fail("Full inventory accepted a claim");}catch(IllegalStateException expected){ }
        helper.assertTrue(ConstructionService.lodge(server).token()==null,"Full-inventory failure leaves entitlement untouched");
        alice.getInventory().clearContent();var claim=ConstructionService.review(alice,1);
        try{ConstructionService.confirm(bob,claim.nonce(),2);helper.fail("Copied blueprint claim nonce accepted");}catch(IllegalStateException expected){ }
        ConstructionService.confirm(alice,claim.nonce(),2);var claimed=ConstructionService.lodge(server);
        helper.assertTrue(ConstructionService.hasPlan(alice,claimed),"Authenticated physical plan delivered");
        ItemStack oldPlan=alice.getInventory().getItem(0).copy();UUID oldToken=claimed.token();
        var recovery=ConstructionService.review(alice,1);ConstructionService.confirm(alice,recovery.nonce(),2);
        var recovered=ConstructionService.lodge(server);ItemStack currentPlan=alice.getInventory().getItem(0).copy();
        helper.assertTrue(!oldToken.equals(recovered.token()),"Recovery durably invalidates the previous generation");
        alice.getInventory().setItem(0,oldPlan);
        helper.assertTrue(!ConstructionService.hasPlan(alice,recovered),"Restored old item has no entitlement authority");
        alice.getInventory().setItem(0,currentPlan);
        BlockPos origin=hutOrigin.offset(20,0,0);
        // Explicit fixture preparation, distinct from the engine: no production chunk tickets.
        for(BlockPos pos:BlockPos.betweenClosed(hutOrigin.offset(-3,-1,-3),origin.offset(20,-1,20)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
        alice.moveTo(origin.getX()-2,origin.getY()+1,origin.getZ());bob.moveTo(origin.getX()-3,origin.getY()+1,origin.getZ());
        for(Rotation rotation:Rotation.values()){
            var preview=ConstructionService.preview(alice,origin,rotation);helper.assertTrue(preview.denial().isEmpty(),"Clear rotated Lodge footprint accepted: "+rotation+" "+preview.denial());
        }
        level.setBlock(origin,Blocks.SNOW.defaultBlockState(),18);
        helper.assertTrue(!ConstructionService.preview(alice,origin,Rotation.NONE).denial().isEmpty(),"Pre-existing snow must be cleared before a proposal; no terrain exception at approval");
        level.setBlock(origin,Blocks.AIR.defaultBlockState(),18);
        BlockPos far=new BlockPos(24_000_000,80,24_000_000);
        helper.assertTrue(!level.hasChunkAt(far),"Distant test chunk starts unloaded");
        var distant=recovered.propose(far,Rotation.NONE);
        helper.assertTrue(ConstructionService.validateSite(level,distant,false).contains("loaded")&&!level.hasChunkAt(far),"Unloaded site refused without loading it");
        ConstructionService.preview(alice,origin,Rotation.NONE);var siteReview=ConstructionService.view(alice,1,false,"").review();
        level.setBlock(origin.above(),Blocks.CHEST.defaultBlockState(),18);
        try{ConstructionService.confirm(alice,siteReview,4);helper.fail("Stale obstructed proposal accepted");}catch(IllegalStateException expected){ }
        helper.assertTrue(ConstructionService.lodge(server).phase()==Phase.CLAIMED,"Failed proposal consumes no entitlement");
        level.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),18);
        ConstructionService.preview(alice,origin,Rotation.NONE);siteReview=ConstructionService.view(alice,2,false,"").review();ConstructionService.confirm(alice,siteReview,4);
        try{ConstructionService.review(bob,5);helper.fail("Unprivileged site approval accepted");}catch(IllegalStateException expected){ }
        bob.administrator=true;var approval=ConstructionService.review(bob,5);bob.administrator=false;
        try{ConstructionService.confirm(bob,approval.nonce(),6);helper.fail("Lost administrator permission bypassed");}catch(IllegalStateException expected){ }
        bob.administrator=true;approval=ConstructionService.review(bob,5);
        ConstructionService.preview(alice,origin,Rotation.NONE);ConstructionService.confirm(alice,ConstructionService.view(alice,3,false,"").review(),4);
        try{ConstructionService.confirm(bob,approval.nonce(),6);helper.fail("Superseded proposal approval accepted");}catch(IllegalStateException expected){ }
        approval=ConstructionService.review(bob,5);ConstructionService.confirm(bob,approval.nonce(),6);
        var start=ConstructionService.review(alice,7);level.setBlock(origin.above(),Blocks.CHEST.defaultBlockState(),18);
        try{ConstructionService.confirm(alice,start.nonce(),8);helper.fail("Stale blocked redemption accepted");}catch(IllegalStateException expected){ }
        helper.assertTrue(ConstructionService.hasPlan(alice,ConstructionService.lodge(server)),"Failed redemption retains current plan");
        level.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),18);start=ConstructionService.review(alice,7);ConstructionService.confirm(alice,start.nonce(),8);
        var p=ConstructionService.lodge(server);helper.assertTrue(p.phase()==Phase.BUILDING&&p.token()==null&&alice.getInventory().getItem(0).isEmpty(),"Reservation and token revocation precede physical plan removal");
        try{ConstructionService.confirm(alice,start.nonce(),8);helper.fail("Construction redemption replayed");}catch(IllegalStateException expected){ }
        try{ConstructionService.review(alice,1);helper.fail("Redeemed privilege recovered again");}catch(IllegalStateException expected){ }
        alice.getInventory().setItem(0,currentPlan);helper.assertTrue(!ConstructionService.hasPlan(alice,p),"Lagging restored physical plan cannot redeem again");alice.getInventory().clearContent();
        ConstructionService.step(server,p.id());helper.assertTrue(ConstructionService.lodge(server).placed()==0,"No remote block work before Aldric reaches his work post");
        BlockPos post=BlueprintRegistry.lodge().workPost(p.origin(),p.rotation());builder.moveTo(post.getX()+0.5,post.getY(),post.getZ()+0.5);
        Consumer<BlockEvent.EntityMultiPlaceEvent> denied=e->{if(e.getEntity()==builder)e.setCanceled(true);};NeoForge.EVENT_BUS.addListener(denied);
        try{ConstructionService.step(server,p.id());}finally{NeoForge.EVENT_BUS.unregister(denied);}
        helper.assertTrue(ConstructionService.lodge(server).placed()==0&&level.getBlockState(origin).isAir(),"Protection cancellation rolls back and grants no progress");
        level.setBlock(origin,Blocks.SNOW.defaultBlockState(),18);
        int drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(2)).size();
        ConstructionService.step(server,p.id());helper.assertTrue(ConstructionService.lodge(server).placed()==1,"One scheduler step places exactly one physical block, brushing post-reservation weather snow");
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(2)).size()==drops,"Brushing weather snow creates no item drops or reusable material grant");
        helper.assertTrue(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,origin,level.getBlockState(origin),bob)).isCanceled(),"Active construction denies block extraction");
        ConstructionService.initialize(server);p=ConstructionService.lodge(server);helper.assertTrue(p.placed()==1,"Native construction restart retains exact project and progress");
        var blocks=BlueprintRegistry.lodge().blocks(p.rotation());BlockPos next=p.origin().offset(blocks.get(1).offset());
        level.setBlock(next,Blocks.CHEST.defaultBlockState(),18);ConstructionService.step(server,p.id());
        helper.assertTrue(ConstructionService.lodge(server).placed()==1&&level.getBlockState(next).is(Blocks.CHEST),"Player edits preserved and construction paused");
        level.setBlock(next,Blocks.AIR.defaultBlockState(),18);level.setBlock(origin,Blocks.AIR.defaultBlockState(),18);ConstructionService.initialize(server);ConstructionService.step(server,p.id());
        helper.assertTrue(ConstructionService.lodge(server).placed()==1&&level.getBlockState(origin).isAir(),"Missing credited materials are never automatically regenerated");
        level.setBlock(origin,blocks.getFirst().state(),18);var resume=ConstructionService.review(bob,9);ConstructionService.confirm(bob,resume.nonce(),10);
        for(int i=1;i<blocks.size();i++)ConstructionService.step(server,p.id());
        var definition=BlueprintRegistry.lodge();BlockPos home=p.origin().offset(definition.rotate(definition.home(),p.rotation()));
        var occupant=net.minecraft.world.entity.EntityType.ARMOR_STAND.create(level);occupant.moveTo(home.getX()+0.5,home.getY(),home.getZ()+0.5);level.addFreshEntity(occupant);
        ConstructionService.step(server,p.id());
        helper.assertTrue(!ConstructionService.lodge(server).npcIssued(),"An occupied home delays NPC issuance without consuming its identity");
        occupant.discard();level.setBlock(home,Blocks.SNOW.defaultBlockState(),18);
        BlockPos aisle=p.origin().offset(definition.rotate(new BlockPos(3,1,7),p.rotation()));
        level.setBlock(aisle,Blocks.SNOW.defaultBlockState(),18);
        int beforeHandoverDrops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,ConstructionService.bounds(p)).size();
        ConstructionService.step(server,p.id());
        helper.assertTrue(level.getBlockState(home).isAir(),"Weather snow is brushed from the reserved NPC home before safe arrival");
        helper.assertTrue(level.getBlockState(aisle).isAir()&&level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,ConstructionService.bounds(p)).size()==beforeHandoverDrops,"Handover clears weather snow from unfurnished interior spaces without drops");
        var complete=ConstructionService.lodge(server);helper.assertTrue(complete.phase()==Phase.COMPLETE,"Full native progressive construction completes");
        helper.assertTrue(ConstructionService.structureIntact(level,complete),"Furnished Lodge passes required-block verification after neighbor updates");
        ReeveEntity reeve=(ReeveEntity)level.getEntity(complete.npc());helper.assertTrue(reeve!=null&&ConstructionService.registeredReeve(reeve),"Exactly one named registered Reeve arrives after completion");
        helper.assertTrue(reeve.getName().getString().equals("Mira Ashford"),"Original Reeve name present");
        ConstructionService.step(server,p.id());ConstructionService.initialize(server);
        helper.assertTrue(ConstructionService.lodge(server).npc().equals(reeve.getUUID())&&ConstructionService.lodge(server).phase()==Phase.COMPLETE,"Completed building and stable NPC identity survive reload");
        level.setBlock(aisle,Blocks.SNOW.defaultBlockState(),18);ConstructionService.step(server,p.id());
        helper.assertTrue(level.getBlockState(aisle).is(Blocks.SNOW),"Completed property is never weather-cleaned after handover");level.setBlock(aisle,Blocks.AIR.defaultBlockState(),18);
        var saved=reeve.saveWithoutId(new CompoundTag());var copy=KingdomNpcs.REEVE.get().create(level);copy.load(saved);
        helper.assertTrue(copy.getUUID().equals(reeve.getUUID())&&copy.buildingId().equals(complete.id())&&copy.home().equals(reeve.home()),"Reeve native entity persistence retains identity and home");
        level.setBlock(origin,Blocks.AIR.defaultBlockState(),18);helper.assertTrue(!ConstructionService.structureIntact(level,complete),"Building damage suspends Reeve services");level.setBlock(origin,blocks.getFirst().state(),18);
        reeve.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);ConstructionService.initialize(server);
        helper.assertTrue(ConstructionService.lodge(server).npcDead()&&!ConstructionService.registeredReeve(copy),"Persisted Reeve death refuses restored copies or automatic replacement");
        var unloaded=new ConstructionStore.Project(UUID.randomUUID(),"unloaded_test",complete.blueprint(),1,alice.getUUID(),"RuntimeOperator",Phase.BUILDING,null,far,Rotation.NONE,UUID.randomUUID(),bob.getUUID(),0,UUID.randomUUID(),false,false);
        ConstructionService.store(server).save(unloaded,bob.getUUID(),"TEST_UNLOADED_FIXTURE");ConstructionService.step(server,unloaded.id());
        helper.assertTrue(!level.hasChunkAt(far)&&ConstructionService.store(server).get(unloaded.id()).placed()==0,"Unloaded work pauses without creating chunk tickets or progress");
    }

    private static void milestoneLifecycle(GameTestHelper helper,RecordingPlayer player)throws Exception{
        var server=helper.getLevel().getServer();var service=com.mohymods.kingdomsmp.milestone.MilestoneService.current(server);
        helper.assertTrue(service.catalog()!=null&&service.report().issues(1).isEmpty()&&service.report().catalogIssues().isEmpty(),"Bundled server data resolves native item/tag registries and valid dependency graph");
        var glass=service.catalog().tier(2).requirements().stream().filter(r->r.ingredient().equals("minecraft:glass")).findFirst().orElseThrow();
        helper.assertTrue(glass.quantity()==256&&com.mohymods.kingdomsmp.milestone.MilestoneService.matches(new ItemStack(Items.GLASS),glass)&&!com.mohymods.kingdomsmp.milestone.MilestoneService.matches(new ItemStack(Items.GLASS_PANE),glass),"Generic exact ingredient matching accepts glass and refuses a different crafted item");
        helper.assertTrue(com.mohymods.kingdomsmp.milestone.MilestoneService.matches(new ItemStack(Items.BIRCH_LOG),service.catalog().tier(2).requirements().getFirst()),"Generic tag requirement matches native loaded log variants");
        helper.assertTrue(!service.report().issues(6).isEmpty(),"Absent/unverified technology recipes stay blocked rather than defaulting to Overworld availability");
        var before=ContributionService.ledger(server).progress();int inventory=ContributionService.eligible(player,Resource.LOGS);
        com.mohymods.kingdomsmp.milestone.MilestoneService.reload(server);
        var detail=com.mohymods.kingdomsmp.milestone.MilestoneService.descriptions(server).get(1);
        helper.assertTrue(detail.contains("PROPOSAL — NOT ENABLED")&&detail.contains("3072")&&detail.contains("completed in this world"),"Actual Lodge completion appears with disabled revised candidate costs in the server projection");
        helper.assertTrue(ContributionService.ledger(server).progress().equals(before)&&ContributionService.eligible(player,Resource.LOGS)==inventory&&ContributionService.kingdom(server).attainedTier()==2,"Reload and inspection preserve prior winner, exact scores, inventory and attained tier");
        try{server.getCommands().getDispatcher().execute("kingdom milestones validate",player.createCommandSourceStack().withPermission(0));helper.fail("Non-admin diagnostics command accepted");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){}
        helper.assertTrue(server.getCommands().getDispatcher().execute("kingdom milestones inspect 2",player.createCommandSourceStack().withPermission(0))==1,"Public read-only proposal inspection works without admin mutation authority");
    }
    private static void travelGateLifecycle(GameTestHelper helper,RecordingPlayer player){
        var server=helper.getLevel().getServer();var original=player.serverLevel();
        for(var key:List.of(net.minecraft.world.level.Level.NETHER,net.minecraft.world.level.Level.END)){
            var target=server.getLevel(key);helper.assertTrue(target!=null,"Native target realm exists for gate test");
            var gate=new net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent(player,key);
            helper.assertTrue(NeoForge.EVENT_BUS.post(gate).isCanceled(),"Actual NeoForge realm-entry event denies premature access: "+key.location());
            var transition=new net.minecraft.world.level.portal.DimensionTransition(target,new net.minecraft.world.phys.Vec3(0,80,0),net.minecraft.world.phys.Vec3.ZERO,0,0,net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
            helper.assertTrue(player.changeDimension(transition)==null&&player.serverLevel()==original,"ServerPlayer.changeDimension is denied before moving the player");
            player.teleportTo(target,0,80,0,0,0);helper.assertTrue(player.serverLevel()==original,"Direct cross-realm teleportTo cannot bypass kingdom gate");
            var item=new net.minecraft.world.entity.item.ItemEntity(original,player.getX(),player.getY(),player.getZ(),new ItemStack(Items.IRON_INGOT,64));
            helper.assertTrue(item.changeDimension(transition)==null&&item.level()==original&&item.getItem().getCount()==64,"Item-entity portal transfer denied without loss or duplication");
        }
    }

    private static void give(RecordingPlayer player, Item item, int count) {
        player.getInventory().clearContent();
        for (int slot = 0; count > 0; slot++) { int size = Math.min(64, count); player.getInventory().setItem(slot, new ItemStack(item, size)); count -= size; }
    }

    private static final class RecordingPlayer extends FakePlayer {
        private final List<Component> messages = new ArrayList<>();
        private boolean administrator;
        RecordingPlayer(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "RuntimeOperator")); }
        @Override public boolean hasPermissions(int level){return administrator&&level<=2;}
        @Override public void sendSystemMessage(Component message) { messages.add(message.copy()); }
        String confirmationCommand() {
            var pattern = Pattern.compile("/kingdom hut confirm [a-f0-9-]{36}");
            for (int index = messages.size() - 1; index >= 0; index--) {
                Optional<String> command = messages.get(index).visit((style, text) -> {
                    ClickEvent click = style.getClickEvent();
                    return click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND
                            && text.equals("[Confirm Builder's Hut]") && pattern.matcher(click.getValue()).matches()
                            ? Optional.of(click.getValue()) : Optional.empty();
                }, Style.EMPTY);
                if (command.isPresent()) return command.get();
            }
            throw new IllegalStateException("No clickable confirmation command delivered to operator");
        }
    }
}
