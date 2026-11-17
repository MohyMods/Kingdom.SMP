package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.bounty.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

/** Real normal-dedicated building/counter/death flow; controlled NATURAL finalize hooks, no injected kill counters. */
@GameTestHolder("kingdom_smp_hunt_tests") @PrefixGameTestTemplate(false)
public final class HuntRuntimeTests {
    @GameTest(template="empty",timeoutTicks=10000)
    public static void acceptedNaturalHuntPaysOnce(GameTestHelper h) throws Exception {
        BountyRuntimeTests.completeBountyHall(h,project->guard(h,()->new Walk(h,project).start()));
    }
    private static final class Walk {
        final GameTestHelper h;final ServerLevel level;final ServerPlayer p;final ConstructionStore.Project project;final EconomyStore store;
        HuntState hunt;long before;Mob canceled;Consumer<LivingDeathEvent> cancelListener;
        final BlockPos site=new BlockPos(100,-63,66);
        Walk(GameTestHelper h,ConstructionStore.Project project){this.h=h;this.level=h.getLevel();this.project=project;p=level.getServer().getPlayerList().getPlayer(project.owner());store=CommerceService.store(p.server);}
        void start() throws Exception {
            counter();h.assertTrue(CommerceService.snapshot(p,1,8,"").entries().size()==4,"Garrick exposes one hunt and three shared provisions");
            var view=CommerceService.handle(p,new CommerceRequest(2,1,8,"hunt:daily",1,CommerceRequest.NONE));h.assertTrue(view.reviewing(),"Normal in-game contract review opens");
            CommerceService.handle(p,new CommerceRequest(3,2,8,"",1,view.review()));hunt=store.hunt(p.getUUID(),HuntService.today()).orElseThrow();before=store.balance(p.getUUID());
            h.assertTrue(store.mailbox(p.getUUID()).stream().anyMatch(m->m.id().equals(hunt.id())&&m.kind()==EconomyStore.MailKind.HUNT_ESCROW),"Accepted reward remains locked in private mail");
            boolean locked=false;try{store.claimCoins(p.getUUID(),hunt.id());}catch(IllegalArgumentException expected){locked=true;}h.assertTrue(locked,"Forged early coin claim cannot unlock hunt escrow");
            boolean premature=false;try{HuntService.review(p,"hunt:daily");}catch(IllegalStateException expected){premature=true;}h.assertTrue(premature,"No ready review before actual kills");
            level.getServer().setDifficulty(Difficulty.NORMAL,true);
            for(var pos:BlockPos.betweenClosed(site.offset(-4,-1,-4),site.offset(4,-1,4)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
            p.teleportTo(level,site.getX()+.5,site.getY(),site.getZ()+.5,Set.of(),0,0);level.getChunkSource().move(p);
            h.runAfterDelay(5,()->guard(h,()->awaitPlot(0)));
        }
        void awaitPlot(int attempts) throws Exception {
            if(!level.isPositionEntityTicking(site)){h.assertTrue(attempts<80,"Vanilla teleport destination must become entity ticking");h.runAfterDelay(5,()->guard(h,()->awaitPlot(attempts+1)));return;}
            rejectDeaths();
        }
        Mob mob(MobSpawnType reason){var entity=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(hunt.target())).create(level);h.assertTrue(entity instanceof Mob,"Registered rotating target is a mob");var mob=(Mob)entity;mob.moveTo(site.getX()+2.5,site.getY(),site.getZ()+.5);EventHooks.finalizeMobSpawn(mob,level,level.getCurrentDifficultyAt(mob.blockPosition()),reason,null);mob.setNoAi(true);h.assertTrue(level.addFreshEntity(mob)&&level.getEntity(mob.getUUID())==mob,"Destination mob is fully tracked before damage");return mob;}
        void rejectDeaths() throws Exception {
            for(var reason:List.of(MobSpawnType.SPAWNER,MobSpawnType.SPAWN_EGG,MobSpawnType.COMMAND)){var mob=mob(reason);mob.hurt(p.damageSources().playerAttack(p),1000);}
            var indirect=mob(MobSpawnType.NATURAL);indirect.hurt(indirect.damageSources().generic(),1000);
            var other=p.server.getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseThrow();var claimed=site.east(16);ClaimRuntimeTests.ftbClaim(other,claimed);ClaimRuntimeTests.opacClaim(level,other.getUUID(),claimed);
            h.assertTrue(com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,claimed,claimed)!=null,"Foreign claim remains denied by installed claim adapters");var protectedMob=mob(MobSpawnType.NATURAL);protectedMob.moveTo(claimed.getX()+.5,claimed.getY(),claimed.getZ()+.5);protectedMob.hurt(p.damageSources().playerAttack(p),1000);
            canceled=mob(MobSpawnType.NATURAL);cancelListener=event->{if(event.getEntity()==canceled){event.setCanceled(true);}};
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,LivingDeathEvent.class,cancelListener);canceled.hurt(p.damageSources().playerAttack(p),1000);
            h.runAfterDelay(3,()->guard(h,()->{NeoForge.EVENT_BUS.unregister(cancelListener);canceled.discard();h.assertTrue(HuntSavedData.get(p.server).evidence().count(hunt.id())==0,"Spawner, egg, command, indirect, foreign-claim and canceled deaths cannot count");kill(0);}));
        }
        void kill(int n) throws Exception {
            if(n==hunt.goal()){finish();return;}
            var mob=mob(MobSpawnType.NATURAL);h.assertTrue(mob.getPersistentData().contains("kingdomsmp_hunt_origin"),"Real natural-spawn hook authenticates UUID-bound origin");
            var source=p.damageSources().playerAttack(p);if(n==0){var arrow=new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW,level);arrow.setOwner(p);source=p.damageSources().arrow(arrow,p);}
            h.assertTrue(mob.hurt(source,1000),"Actual attributed player or owned-arrow damage defeats target");
            h.runAfterDelay(3,()->guard(h,()->{h.assertTrue(HuntSavedData.get(p.server).evidence().count(hunt.id())==n+1,"One actual natural player kill grants one personal proof");kill(n+1);}));
        }
        void finish() throws Exception {
            var data=HuntSavedData.get(p.server);var restored=HuntSavedData.load(data.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()),level.registryAccess());h.assertTrue(restored.evidence().count(hunt.id())==hunt.goal(),"Kill evidence survives native world-data serialization");
            counter();var q=HuntService.review(p,"hunt:daily");HuntService.confirm(p,q.nonce());long revision=store.revision();boolean replay=false;try{HuntService.confirm(p,q.nonce());}catch(IllegalStateException expected){replay=true;}
            h.assertTrue(replay&&revision==store.revision(),"Replayed claim cannot pay the completed contract again");
            h.assertTrue(store.balance(p.getUUID())==before,"Payment unlocks mail, never credits both wallet and mail");
            var coinReview=CommerceService.handle(p,new CommerceRequest(4,1,2,hunt.id().toString(),1,CommerceRequest.NONE));h.assertTrue(coinReview.reviewing(),"Ordinary Mailbox coin review opens at real resident");CommerceService.handle(p,new CommerceRequest(5,2,2,"",1,coinReview.review()));
            h.assertTrue(store.balance(p.getUUID())==before+hunt.reward(),"Exactly the reserved payment is claimed through normal interface");
            var reopened=EconomyStore.open(p.server.getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.balance(p.getUUID())==before+hunt.reward()&&reopened.hunt(p.getUUID(),hunt.day()).orElseThrow().paid(),"Payment and permanent daily entitlement reopen together");CommerceService.verifyReceipt(p);
            com.mojang.logging.LogUtils.getLogger().info("HUNT_ASSERTIONS_PASSED: actual Tier3 hall/counter, accepted locked reservation, real finalized natural-mob player kills, spawner/egg/command/indirect/foreign-claim/canceled-death refusals and owned-arrow credit, personal saved evidence, reviewed payment/mail, replay refusal and canonical reopen; controlled spawn fixture, not full-modpack certification");h.succeed();
        }
        void counter(){var npc=level.getEntity(project.npc());h.assertTrue(npc instanceof com.mohymods.kingdomsmp.npc.ReeveEntity,"Actual permanent Bounty Master remains loaded");for(var pos:BlockPos.betweenClosed(npc.blockPosition().offset(-2,0,-2),npc.blockPosition().offset(2,0,2))){if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;p.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),0,0);level.getChunkSource().move(p);if(p.hasLineOfSight(npc)&&p.distanceToSqr(npc)<25){CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)npc);return;}}throw new IllegalStateException("No safe nearby Bounty interaction position");}
    }
    @FunctionalInterface private interface Step { void run() throws Exception; }
    private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
