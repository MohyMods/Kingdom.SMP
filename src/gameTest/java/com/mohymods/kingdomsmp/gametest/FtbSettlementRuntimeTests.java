package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.ftbquests.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** Real rewardless FTB team projection following the real earned settlement, never injected FTB completions. */
@GameTestHolder("kingdom_smp_quest_tests") @PrefixGameTestTemplate(false)
public final class FtbSettlementRuntimeTests {
    private static long requests;
    @GameTest(template="empty",timeoutTicks=9000)
    public static void separateTeamsLateJoinResetAndRestart(GameTestHelper h)throws Exception{
        var walk=new Walk(h);h.onEachTick(walk::tick);
        if(Files.exists(walk.path))walk.restore();
        else BankRuntimeTests.completeBankLoop(h,()->walk.begin());
    }
    private static final class Ftb {
        final Object file;final Class<?> team=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData"),task=Class.forName("dev.ftb.mods.ftbquests.quest.task.Task");
        Ftb()throws Exception {file=((Optional<?>)Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getMethod("getInstance").invoke(null)).orElseThrow();}
        void icons(net.minecraft.core.HolderLookup.Provider registries,boolean custom)throws Exception{
            for(var goal:KingdomQuestGoal.values()){
                var tag=new CompoundTag();task(goal).getClass().getMethod("writeData",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class).invoke(task(goal),tag,registries);
                if(!tag.getCompound("icon").getString("id").equals(custom&&goal==KingdomQuestGoal.PUBLIC_TRADE?"minecraft:emerald":goal.iconItemId()))throw new IllegalStateException("Actual FTB task icon failed to load: "+goal);
            }
        }
        Object data(ServerPlayer p)throws Exception{return team.getMethod("get",net.minecraft.world.entity.player.Player.class).invoke(null,p);}
        Object task(KingdomQuestGoal g)throws Exception{return file.getClass().getMethod("getTask",long.class).invoke(file,g.taskId());}
        long progress(ServerPlayer p,KingdomQuestGoal g)throws Exception{return (long)team.getMethod("getProgress",task).invoke(data(p),task(g));}
        UUID id(ServerPlayer p)throws Exception{return (UUID)team.getMethod("getTeamId").invoke(data(p));}
        void reset(ServerPlayer p,KingdomQuestGoal g)throws Exception{
            // Use the actual quest reset operation. TeamData.resetProgress(Task) alone only
            // removes its numeric value; it deliberately leaves completion timestamps intact.
            var change=Class.forName("dev.ftb.mods.ftbquests.util.ProgressChange");
            var request=change.getMethod("createServerSide",long.class,boolean.class,UUID.class,boolean.class).invoke(null,g.questId(),true,p.getUUID(),false);
            change.getMethod("maybeForceProgress",UUID.class).invoke(request,id(p));
        }
        void save()throws Exception{file.getClass().getMethod("saveNow").invoke(file);}
        static byte[] legacyChapter()throws Exception{
            try(var in=FtbSettlementRuntimeTests.class.getResourceAsStream("/data/kingdom_smp_quest_tests/legacy_settlement.snbt")){return Objects.requireNonNull(in).readAllBytes();}
        }
        static byte[] customizedLegacyChapter()throws Exception{
            var tag=net.minecraft.nbt.TagParser.parseTag(new String(legacyChapter(),java.nio.charset.StandardCharsets.UTF_8));
            var quest=tag.getList("quests",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(8);var icon=new CompoundTag();icon.putString("id","minecraft:emerald");quest.put("icon",icon.copy());quest.getList("tasks",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).put("icon",icon.copy());
            return tag.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        static Path chapter(){return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests/chapters/"+KingdomQuestCampaign.FILENAME);}

    }
    private static final class Walk {
        final GameTestHelper h;final Path path;final Ftb ftb;final net.minecraft.server.MinecraftServer server;
        ServerPlayer owner,other,late;int phase;long waitUntil;CompoundTag proof;
        Walk(GameTestHelper h)throws Exception{this.h=h;server=h.getLevel().getServer();path=server.getWorldPath(LevelResource.ROOT).resolve("ftb-settlement-proof.nbt");ftb=new Ftb();ftb.icons(server.registryAccess(),Files.exists(path)&&AtomicNbt.read(path).getBoolean("customIcon"));}
        void begin(){try{
            var bank=bank();owner=server.getPlayerList().getPlayer(bank.owner());other=server.getPlayerList().getPlayers().stream().filter(p->p!=owner).findFirst().orElseThrow();
            // Architectury excludes ALL ServerPlayer subclasses from automatic quest checks.
            // Preserve saved native receipts/identities, then reconnect exact vanilla peers.
            var ownerId=owner.getUUID();var otherId=other.getUUID();server.getPlayerList().remove(owner);server.getPlayerList().remove(other);
            owner=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),ownerId,"HallWinner");other=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),otherId,"HallOwner");
            h.assertTrue(owner.getClass()==ServerPlayer.class&&other.getClass()==ServerPlayer.class,"Actual vanilla player classes, not subclass-excluded fake peers");
            h.assertTrue(!owner.hasPermissions(2)&&!other.hasPermissions(2),"Both actual registered peers are ordinary players");
            h.assertTrue(!ftb.id(owner).equals(ftb.id(other)),"Initial guide readers have distinct real FTB teams");
            for(var line:FtbQuestsIntegration.inspect(server))com.mojang.logging.LogUtils.getLogger().info("FTB_NATIVE_BINDING {}",line);
            var state=AuthoritativeQuestSnapshot.read(server).orElseThrow();for(var goal:KingdomQuestGoal.values())h.assertTrue(goal.complete(state)==(goal!=KingdomQuestGoal.PUBLIC_TRADE),"Actual earned settlement state, no fabricated FTB completion: "+goal);
            com.mojang.logging.LogUtils.getLogger().info("FTB_SETTLEMENT_STAGE: observing first eight goals on normal player tick");waitUntil=h.getTick()+1100;phase=1;
        }catch(Exception e){fail(e);}}
        void restore()throws Exception{
            proof=AtomicNbt.read(path);var chapter=net.minecraft.nbt.TagParser.parseTag(Files.readString(Ftb.chapter()));h.assertTrue(chapter.getList("quests",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getDouble("x")==0,"Legacy owner layout is retained; installer does not replace it with the fresh centered layout");owner=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),proof.getUUID("owner"),"HallWinner");other=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),proof.getUUID("other"),"HallOwner");late=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),proof.getUUID("late"),"GuideLateJoin");
            for(int x=-1;x<=6;x++)for(int z=0;z<=4;z++)h.getLevel().getChunk(x,z);
            h.assertTrue(ContributionService.kingdom(server).attainedTier()==3,"Actual earned Tier3 restores; no seeded tiers in this test");phase=5;waitUntil=h.getTick()+200;
        }
        void tick(){if(phase<=0)return;try{
            // Embedded channels are not accepted sockets in ServerConnectionListener. They never run
            // ServerGamePacketListenerImpl.tick(), which normally drives this native player tick.
            // Drive the real API here so Architectury/FTB auto-submit hooks actually execute;
            // never set FTB progress or fabricate a task-check result.
            for(var p:new ServerPlayer[]{owner,other,late})if(p!=null&&!p.isRemoved())p.doTick();
            switch(phase){
                case 1->{if(!ready(owner,false)||!ready(other,false)){timeout("Normal FTB task timer failed to reflect earned first eight goals");return;}
                    h.assertTrue(ftb.progress(owner,KingdomQuestGoal.PUBLIC_TRADE)==0&&ftb.progress(other,KingdomQuestGoal.PUBLIC_TRADE)==0,"Unperformed trade is not granted by guide/team membership");trade();phase=2;waitUntil=h.getTick()+1100;}
                case 2->{if(!ready(owner,true)||!ready(other,true)){timeout("Actual taxed trade failed to project to both separate teams");return;}
                    proof=capture();late=ClaimRuntimeTests.vanillaPlayer(h.getLevel(),UUID.randomUUID(),"GuideLateJoin");late.moveTo(other.getX(),other.getY(),other.getZ());h.assertTrue(!ftb.id(late).equals(ftb.id(owner))&&!ftb.id(late).equals(ftb.id(other)),"Late joiner has another separate real team");phase=3;waitUntil=h.getTick()+1100;}
                case 3->{if(!ready(late,true)){timeout("Late joiner did not receive authoritative community guide");return;}
                    unchanged(proof);ftb.reset(owner,KingdomQuestGoal.HUT);h.assertTrue(ftb.progress(owner,KingdomQuestGoal.HUT)==0,"Explicit FTB-only reset really resets guide task");unchanged(proof);phase=4;waitUntil=h.getTick()+1100;}
                case 4->{if(!ready(owner,true)){timeout("Reset task did not reproject from canonical building");return;}
                    unchanged(proof);joinParty();phase=6;waitUntil=h.getTick()+1100;}
                case 6->{if(!ready(owner,true)||!ready(other,true)||!ready(late,true)){timeout("Guide progress lost after actual owner-created party/invited join");return;}
                    h.assertTrue(ftb.id(owner).equals(ftb.id(other))&&!ftb.id(owner).equals(ftb.id(late)),"Real invited party shares its guide while late reader remains separate");
                    unchanged(proof);proof.putUUID("owner",owner.getUUID());proof.putUUID("other",other.getUUID());proof.putUUID("late",late.getUUID());proof.putUUID("ownerTeam",ftb.id(owner));proof.putUUID("otherTeam",ftb.id(other));proof.putUUID("lateTeam",ftb.id(late));
                    ftb.save();Files.write(Ftb.chapter(),Ftb.customizedLegacyChapter());proof.putBoolean("customIcon",true);AtomicNbt.write(path,proof);finish("FTB_SETTLEMENT_PREPARED: actual earned Hut/Hall/resources/paid Farmer/Tier3/Bank and actual taxed native sale; native player/FTB task timers, separate teams, late join, reset/reprojection, actual invited party join, no Kingdom reward/record mutation");}
                case 5->{if(h.getTick()<waitUntil)return;h.assertTrue(ftb.id(owner).equals(proof.getUUID("ownerTeam"))&&ftb.id(other).equals(proof.getUUID("otherTeam"))&&ftb.id(late).equals(proof.getUUID("lateTeam")),"All real team identities survive server process restart");
                    h.assertTrue(ready(owner,true)&&ready(other,true)&&ready(late,true),"All nine guide task completions survive actual restart for all three teams");unchanged(proof);finish("FTB_SETTLEMENT_RESTART_VERIFIED: legacy layout/custom icon preservation and repaired real item icons, party and separate late-reader identities/nine tasks, earned tier/receipts/purses/entitlements/construction/NPC records intact across actual dedicated JVM restart");}
            }
        }catch(Exception e){fail(e);}}
        boolean ready(ServerPlayer p,boolean trade)throws Exception{for(var g:KingdomQuestGoal.values())if(g!=KingdomQuestGoal.PUBLIC_TRADE||trade)if(ftb.progress(p,g)!=1)return false;return true;}
        void timeout(String message){if(h.getTick()>waitUntil){phase=-1;h.fail(message);}}
        void trade()throws Exception{
            var b=bank();var npc=h.getLevel().getEntity(b.npc());h.assertTrue(npc!=null,"Actual Bank resident is loaded");var home=b.origin().offset(BlueprintRegistry.get(b.blueprint(),1).home());
            for(var p:List.of(owner,other)){p.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);p.getInventory().clearContent();CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)npc);}
            var s=CommerceService.store(server);s.adminSet(UUID.randomUUID(),new UUID(0,0),other.getUUID(),200,"Explicit FTB native trade fixture funds");owner.getInventory().setItem(0,new ItemStack(Items.DIAMOND));owner.getInventory().selected=0;
            var sale=review(owner,"sell",1,100);confirm(owner,sale);var listing=s.listings().getFirst();confirm(other,review(other,"buy:"+listing.id(),1,0));
            h.assertTrue(s.taxRemoved()==5&&s.balance(other.getUUID())==100&&!s.listing(listing.id()).orElseThrow().active(),"Actual native sale burns five coins, funds escrow and charges buyer exactly once");
        }
        void joinParty()throws Exception{
            var api=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
            var manager=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API").getMethod("getManager").invoke(api);
            var type=Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");var color=Class.forName("dev.ftb.mods.ftblibrary.icon.Color4I");
            var party=type.getMethod("createPartyTeam",ServerPlayer.class,String.class,String.class,color).invoke(manager,owner,"Kingdom Guide QA","Explicit ordinary invited-party fixture",color.getField("WHITE").get(null));
            var partyType=Class.forName("dev.ftb.mods.ftbteams.data.PartyTeam");partyType.getMethod("invite",ServerPlayer.class,Collection.class).invoke(party,owner,List.of(other.getGameProfile()));
            partyType.getMethod("join",ServerPlayer.class).invoke(party,other);
        }
        CompoundTag capture()throws Exception{
            var s=CommerceService.store(server);var p=new CompoundTag();p.putLong("economy",s.revision());p.putLong("ownerBalance",s.balance(owner.getUUID()));p.putLong("otherBalance",s.balance(other.getUUID()));p.putLong("ownerReceipt",s.expectedReceipt(owner.getUUID()));p.putLong("otherReceipt",s.expectedReceipt(other.getUUID()));
            p.putLong("contributions",ContributionService.ledger(server).progress().revision());p.putLong("tierTwo",com.mohymods.kingdomsmp.progression.TierTwoService.ledger(server).revision());p.put("construction",AtomicNbt.read(server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_construction.dat")));return p;
        }
        void unchanged(CompoundTag before)throws Exception{
            var now=capture();for(var key:List.of("economy","ownerBalance","otherBalance","ownerReceipt","otherReceipt","contributions","tierTwo","construction"))h.assertTrue(Objects.equals(before.get(key),now.get(key)),"FTB synchronization never mutates Kingdom authority: "+key);
            CommerceService.verifyReceipt(owner);CommerceService.verifyReceipt(other);
        }
        ConstructionStore.Project bank(){return ConstructionService.store(server).projects().stream().filter(p->p.blueprint().equals(RoyalBankBlueprint.ID)).findFirst().orElseThrow();}
        void finish(String marker){phase=-1;h.succeed();com.mojang.logging.LogUtils.getLogger().info(marker);}
        void fail(Exception e){phase=-1;com.mojang.logging.LogUtils.getLogger().error("Actual FTB settlement test failed",e);h.fail(e.toString());}
    }
    private static CommerceView review(ServerPlayer p,String id,int qty,long amount){var v=CommerceService.handle(p,new CommerceRequest(++requests,1,5,id,qty,CommerceRequest.NONE,amount));if(!v.reviewing())throw new IllegalStateException(v.notice());return v;}
    private static void confirm(ServerPlayer p,CommerceView review){CommerceService.handle(p,new CommerceRequest(++requests,2,5,"",1,review.review()));}
}
