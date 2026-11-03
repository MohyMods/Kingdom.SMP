package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_commission_tests") @PrefixGameTestTemplate(false)
public final class CommissionQuestRuntimeTests {
    @GameTest(template="empty",timeoutTicks=6000)
    public static void realBuilderCommissionFromConsumedNativeDelivery(GameTestHelper h)throws Exception{
        CommerceRuntimeTests.completePurchasedProfessionLoop(h,"kingdom_smp:builders_workshop",()->{
            try{commission(h,Profession.BUILDER);}catch(Exception e){h.fail(e.toString());}
        });
    }
    @GameTest(template="empty",templateNamespace="kingdom_smp_woodland_tests",timeoutTicks=6000)
    public static void realLumberjackDeliveryAndForestInspection(GameTestHelper h)throws Exception{
        CommerceRuntimeTests.completePurchasedProfessionLoop(h,"kingdom_smp:lumberjacks_lodge",()->{
            try{commission(h,Profession.LUMBERJACK);}catch(Exception e){h.fail(e.toString());}
        });
    }
    private static void commission(GameTestHelper h,Profession role)throws Exception{
        var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);
        var player=server.getPlayerList().getPlayer(hall.owner());var store=CommerceService.store(server);
        var project=ConstructionService.store(server).projects().stream().filter(p->p.blueprint().toString().equals(role.buildingId())).findFirst().orElseThrow();
        var resident=(com.mohymods.kingdomsmp.npc.ReeveEntity)h.getLevel().getEntity(project.npc());
        var post=project.origin().offset(BlueprintRegistry.get(project.blueprint(),project.blueprintVersion()).home());
        player.moveTo(post.getX()+.5,post.getY(),post.getZ()-1.5);
        CommerceService.openResident(player,resident);
        var enrollment=ProfessionService.review(player,"learn:"+role.id());ProfessionService.confirm(player,enrollment.nonce());
        String id=role.questId(ProfessionRank.APPRENTICE,7);
        // Explicit private balancing acceleration, never injected proof/XP or a changed production default.
        Path config=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commission-quests.json");
        CommissionQuestRules.load(config);Files.writeString(config,Files.readString(config).replace(": 3",": 1"));CommissionQuestRules.clear(server);
        h.assertTrue(!ProfessionQuestService.ready(player,id),"No quest before an acknowledged actual native delivery");
        boolean denied=false;try{ProfessionService.review(player,"quest:"+id);}catch(IllegalStateException expected){denied=true;}
        h.assertTrue(denied&&store.professions(player.getUUID()).track(role).xp()==0,"Client-selected quest cannot manufacture progress");
        var terms=ContractRules.get(server).get(role.id()+"/daily");
        player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(terms.item(),terms.quantity()));
        var contract=ContractService.review(player,role.id()+"/daily");ContractService.confirm(player,contract.nonce());
        h.assertTrue(player.getInventory().getItem(0).isEmpty()&&store.professionContractPeriods(player.getUUID(),role,ProfessionRank.APPRENTICE,"daily")==1,"64 real cobblestone permanently consumed and one canonical distinct-period proof");
        h.assertTrue(ProfessionQuestService.ready(player,id),"Actual acknowledged delivery unlocks the reviewed quest");
        var review=ProfessionService.review(player,"quest:"+id);long balance=store.balance(player.getUUID());
        ProfessionService.confirm(player,review.nonce());
        h.assertTrue(store.professions(player.getUUID()).track(role).xp()==100&&store.balance(player.getUUID())==balance,"Reviewed objective records exactly100earnedXP without minting coins");
        denied=false;try{ProfessionService.confirm(player,review.nonce());}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Repeated client confirmation refuses another award");
        h.assertTrue(!ProfessionQuestService.ready(player,id)&&store.professions(player.getUUID()).rank(role,ProfessionService.quests())==ProfessionRank.APPRENTICE,"One quest does not fabricate the other seven proofs or a rank");
        CommerceService.verifyReceipt(player);var restored=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
        h.assertTrue(restored.professions(player.getUUID()).track(role).xp()==100&&restored.professionContractPeriods(player.getUUID(),role,ProfessionRank.APPRENTICE,"daily")==1,"Independent canonical reopen retains XP, native receipt and one period");
        inspectProject(h,player,project,role);
        if(role==Profession.BUILDER)craftDelivery(h,player,project);
        h.succeed();com.mojang.logging.LogUtils.getLogger().info("PROFESSION_COMMISSION_ASSERTIONS_PASSED: ordinary earned buyer, actual constructed profession premises/NPC, active enrollment, native exact-material receipt, privately accelerated1period goal, Royal review/confirm,100XPonce, no fabricated rank/coins, canonical reopen, OP2 live geometry inspection, changed-geometry refusal and audited one-time100XP");
    }
    private static void inspectProject(GameTestHelper h,ServerPlayer player,ConstructionStore.Project project,Profession role)throws Exception{
        var server=h.getLevel().getServer();var store=CommerceService.store(server);
        var admin=server.getPlayerList().getPlayers().stream().filter(p->!p.getUUID().equals(player.getUUID())).findFirst().orElseThrow();
        var center=project.origin().offset(4,0,-10);var level=h.getLevel();
        // Explicit physical personal-project fixture, not generated event/retained-material evidence.
        if(role==Profession.BUILDER){
        level.setBlock(center.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var door=net.minecraft.world.level.block.Blocks.OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING,net.minecraft.core.Direction.NORTH);
        level.setBlock(center,door,3);level.setBlock(center.above(),door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
        level.setBlock(center.east(2),net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS.defaultBlockState(),3);
        }else{
            for(int i=-3;i<=4;i++)level.setBlock(center.offset(i,-1,-2),net.minecraft.world.level.block.Blocks.DIRT_PATH.defaultBlockState(),3);
            level.setBlock(center.south(3).below(),net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),3);
            level.setBlock(center.south(3),net.minecraft.world.level.block.Blocks.OAK_SAPLING.defaultBlockState(),3);
            level.setBlock(center.north(3),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),3);
            for(var pos:List.of(center.east(2),center.west(2))){
                level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                level.setBlock(pos,net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState(),3);
            }
        }
        player.moveTo(center.getX()+3.5,center.getY(),center.getZ()+3.5);
        var dispatcher=server.getCommands().getDispatcher();String id=role.questId(ProfessionRank.APPRENTICE,role==Profession.BUILDER?4:6);
        String command="kingdom admin profession inspect "+player.getGameProfile().getName()+" "+id+" "+center.getX()+" "+center.getY()+" "+center.getZ()+" Verified owner project clear entry stairs and consent";
        boolean denied=false;try{dispatcher.execute(command,player.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
        h.assertTrue(denied,"Ordinary command source cannot invoke inspection");
        admin.moveTo(center.getX()+3.5,center.getY(),center.getZ()+4.5);
        server.getPlayerList().op(admin.getGameProfile());
        h.assertTrue(dispatcher.execute(command,admin.createCommandSourceStack())==1,"Actual OP2 command checks a working physical entrance and consent");
        var nonce=ProfessionInspectionService.currentReview(admin).orElseThrow();
        h.assertTrue(store.professions(player.getUUID()).track(role).xp()==100,"Inspection review grants nothing");
        level.setBlock(center.east(4),net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState(),3);
        h.assertTrue(dispatcher.execute("kingdom admin profession confirm "+nonce,admin.createCommandSourceStack())==0,"Changed inspected snapshot invalidates confirmation without XP");
        h.assertTrue(dispatcher.execute(command,admin.createCommandSourceStack())==1,"Changed site can receive a fresh full review");
        nonce=ProfessionInspectionService.currentReview(admin).orElseThrow();String confirm="kingdom admin profession confirm "+nonce;
        h.assertTrue(dispatcher.execute(confirm,admin.createCommandSourceStack())==1,"Actual explicit inspection confirmation saves objective and100XP together");
        h.assertTrue(dispatcher.execute(confirm,admin.createCommandSourceStack())==0,"Repeated inspection confirmation cannot award twice");
        h.assertTrue(store.professions(player.getUUID()).track(role).xp()==200,"One contract and one separate inspection objective earn200totalXP");
        var restored=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
        h.assertTrue(restored.professions(player.getUUID()).equals(store.professions(player.getUUID())),"Inspection actor/site/snapshot and roster survive independent reopen");
        server.getPlayerList().deop(admin.getGameProfile());
    }

    private static void craftDelivery(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project)throws Exception{
        var server=h.getLevel().getServer();var store=CommerceService.store(server);
        String id=CraftDeliveryQuests.MASONRY;
        var npc=(com.mohymods.kingdomsmp.npc.ReeveEntity)h.getLevel().getEntity(project.npc());
        var home=project.origin().offset(BlueprintRegistry.get(project.blueprint(),project.blueprintVersion()).home());
        p.moveTo(home.getX()+.5,home.getY(),home.getZ()-1.5);CommerceService.openResident(p,npc);
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.STONE_BRICK_STAIRS,32));p.getInventory().setItem(1,new ItemStack(Items.STONE_BRICK_SLAB,32));
        boolean denied=false;try{ProfessionService.review(p,"materials:"+id);}catch(IllegalStateException expected){denied=true;}
        h.assertTrue(denied&&p.getInventory().getItem(0).getCount()==32&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==200,"Possessing outputs without native crafting cannot qualify or consume materials");
        p.getInventory().clearContent();var table=project.origin().offset(9,0,-10);
        h.getLevel().setBlock(table,net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        p.moveTo(table.getX()+.5,table.getY(),table.getZ()+1.5);
        var menu=new net.minecraft.world.inventory.CraftingMenu(42,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(),table));p.containerMenu=menu;
        for(int n=0;n<8;n++){
            for(int slot:List.of(1,4,5,7,8,9))menu.getSlot(slot).set(new ItemStack(Items.STONE_BRICKS));
            h.assertTrue(menu.getSlot(0).getItem().is(Items.STONE_BRICK_STAIRS),"Actual vanilla stone-brick stair recipe resolves");
            h.assertTrue(menu.quickMoveStack(p,0).is(Items.STONE_BRICK_STAIRS),"Actual crafting result taken and all real ingredients consumed");
        }
        for(int n=0;n<6;n++){
            for(int slot:List.of(1,2,3))menu.getSlot(slot).set(new ItemStack(Items.STONE_BRICKS));
            h.assertTrue(menu.getSlot(0).getItem().is(Items.STONE_BRICK_SLAB),"Actual vanilla stone-brick slab recipe resolves");
            h.assertTrue(menu.quickMoveStack(p,0).is(Items.STONE_BRICK_SLAB),"Actual slab result taken and ingredients consumed");
        }
        p.closeContainer();
        h.assertTrue(CraftQuestEvidence.count(p,Profession.BUILDER,ProfessionRank.APPRENTICE,"minecraft:stone_brick_stairs")==32&&CraftQuestEvidence.count(p,Profession.BUILDER,ProfessionRank.APPRENTICE,"minecraft:stone_brick_slab")==36,"Actual native result events credit32stairs and36slabs in the current personal band");
        h.assertTrue(CraftQuestEvidence.count(p,Profession.BUILDER,ProfessionRank.JOURNEYMAN,"minecraft:stone_brick_stairs")==0,"No future-band crafting credit");
        p.moveTo(home.getX()+.5,home.getY(),home.getZ()-1.5);CommerceService.openResident(p,npc);
        var stale=ProfessionService.review(p,"materials:"+id);int changed=-1;
        for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.STONE_BRICK_STAIRS)){changed=i;break;}
        var original=p.getInventory().getItem(changed).copy();p.getInventory().getItem(changed).shrink(1);
        denied=false;try{ProfessionService.confirm(p,stale.nonce());}catch(IllegalStateException expected){denied=true;}
        h.assertTrue(denied&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==200,"Changed native delivery inventory consumes nothing and grants no XP");p.getInventory().setItem(changed,original);
        var request=new com.mohymods.kingdomsmp.network.CommerceRequest(10001,1,7,"materials:"+id,1,com.mohymods.kingdomsmp.network.CommerceRequest.NONE);
        var view=CommerceService.handle(p,request);h.assertTrue(view.reviewing(),"Normal Royal profession packet route reviews crafted material consumption");
        CommerceService.handle(p,new com.mohymods.kingdomsmp.network.CommerceRequest(10002,2,7,"materials:"+id,1,view.review()));
        int stairs=0,slabs=0;for(int i=0;i<36;i++){var item=p.getInventory().getItem(i);if(item.is(Items.STONE_BRICK_STAIRS))stairs+=item.getCount();if(item.is(Items.STONE_BRICK_SLAB))slabs+=item.getCount();}
        h.assertTrue(stairs==0&&slabs==4&&store.professions(p.getUUID()).track(Profession.BUILDER).xp()==300,"Exactly32stairs and32slabs permanently consumed;4extra actual crafted slabs preserved; exactly100newXP");
        denied=false;try{ProfessionService.review(p,"materials:"+id);}catch(IllegalStateException expected){denied=true;}h.assertTrue(denied,"Completed craft/delivery cannot redeem again");
        CommerceService.verifyReceipt(p);
        var evidence=ProfessionEvidenceSavedData.get(server);evidence.setDirty();server.overworld().getDataStorage().save();
        net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
        var stored=AtomicNbt.read(server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_profession_evidence_v1.dat"));
        h.assertTrue(ProfessionEvidenceSavedData.load(stored.getCompound("data"),server.registryAccess()).evidence(Profession.BUILDER).count(p.getUUID(),ProfessionRank.APPRENTICE,"stone_stairs")==32,"Actual native SavedData persists original personal crafting evidence");
        var reopened=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
        h.assertTrue(reopened.professions(p.getUUID()).track(Profession.BUILDER).xp()==300&&reopened.expectedReceipt(p.getUUID())==store.expectedReceipt(p.getUUID()),"Canonical native receipt and crafted objective survive independent reopen");
        com.mojang.logging.LogUtils.getLogger().info("CRAFTED_DELIVERY_ASSERTIONS_PASSED: native vanilla14recipes,32stairs36slabs personally crafted, possession-only and stale refusal, normal Royal packet dispatch,32plus32permanently consumed,4slabs preserved,100XPonce, native evidence and canonical receipt reopen");
    }

}
