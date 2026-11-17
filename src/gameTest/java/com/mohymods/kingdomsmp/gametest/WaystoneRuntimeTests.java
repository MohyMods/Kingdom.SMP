package com.mohymods.kingdomsmp.gametest;

import com.mojang.datafixers.util.Either;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.integration.waystones.WaystoneAccess;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Exact optional mod, native costs/teleports and actual JVM restart. Boundary tiers are explicitly seeded only here. */
@GameTestHolder("kingdom_smp_waystone_tests") @PrefixGameTestTemplate(false)
public final class WaystoneRuntimeTests {
    @GameTest(template="empty",timeoutTicks=1600)
    public static void beforeCostGateAndPersistentNativeWaystones(GameTestHelper h)throws Exception{
        var walk=new Walk(h);h.onEachTick(walk::tick);
    }
    private static final class Api {
        final Class<?> api=Class.forName("net.blay09.mods.waystones.api.WaystonesAPI"),waystone=Class.forName("net.blay09.mods.waystones.api.Waystone"),context=Class.forName("net.blay09.mods.waystones.api.WaystoneTeleportContext"),requirement=Class.forName("net.blay09.mods.waystones.api.requirement.WarpRequirement");
        Api()throws Exception { }
        @SuppressWarnings({"unchecked","rawtypes"})
        void competingOverride(ServerPlayer p,java.util.concurrent.atomic.AtomicBoolean enabled,java.util.concurrent.atomic.AtomicBoolean veto)throws Exception{
            var event=Class.forName("net.blay09.mods.waystones.api.event.WaystoneTeleportEntityEvent$Pre");var result=Class.forName("net.blay09.mods.waystones.api.EntityTeleportResult");var destination=Class.forName("net.blay09.mods.waystones.api.TeleportDestination");
            var actor=event.getMethod("getEntity");var original=event.getMethod("getOriginalDestination");var override=event.getMethod("overrideResult",result);var success=result.getMethod("success",Entity.class,destination,destination);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,false,(Class)event,(java.util.function.Consumer<net.neoforged.bus.api.Event>)e->{
                if(!enabled.get()&&!veto.get())return;try{if(actor.invoke(e)!=p)return;if(enabled.get()){var d=original.invoke(e);override.invoke(e,success.invoke(null,p,d,d));}((net.neoforged.bus.api.ICancellableEvent)e).setCanceled(true);}catch(Exception error){throw new IllegalStateException(error);}
            });
        }
        Object place(ServerLevel level,BlockPos pos)throws Exception{
            var style=Class.forName("net.blay09.mods.waystones.api.WaystoneStyle");var normal=Class.forName("net.blay09.mods.waystones.api.WaystoneStyles").getField("DEFAULT").get(null);
            var stone=((Optional<?>)api.getMethod("placeWaystone",Level.class,BlockPos.class,style).invoke(null,level,pos,normal)).orElseThrow();
            Class.forName("net.blay09.mods.waystones.api.MutableWaystone").getMethod("setName",net.minecraft.network.chat.Component.class).invoke(stone,net.minecraft.network.chat.Component.literal(level.dimension().equals(Level.END)?"Kingdom End QA":pos.getX()<30?"Kingdom Hearth QA":"Kingdom Road QA"));return stone;
        }
        UUID id(Object stone)throws Exception{return (UUID)waystone.getMethod("getWaystoneUid").invoke(stone);}
        Object saved(net.minecraft.server.MinecraftServer server,UUID id)throws Exception{return ((Optional<?>)api.getMethod("getWaystone",net.minecraft.server.MinecraftServer.class,UUID.class).invoke(null,server,id)).orElseThrow();}
        Object xp(ServerPlayer p,Object target,int cost)throws Exception{return context(p,target,Class.forName("net.blay09.mods.waystones.requirement.ExperienceLevelRequirement").getConstructor(int.class).newInstance(cost));}
        Object items(ServerPlayer p,Object target,int count)throws Exception{return context(p,target,Class.forName("net.blay09.mods.waystones.requirement.ItemRequirement").getConstructor(ItemStack.class,int.class).newInstance(new ItemStack(Items.IRON_INGOT),count));}
        Object context(ServerPlayer p,Object target,Object cost)throws Exception{
            var c=api.getMethod("createUnboundTeleportContext",Entity.class,waystone).invoke(null,p,target);context.getMethod("setRequirements",requirement).invoke(c,cost);return c;
        }
        Either<?,?> sync(String name,Object c)throws Exception{return (Either<?,?>)api.getMethod(name,context).invoke(null,c);}
        @SuppressWarnings("unchecked") CompletableFuture<Either<?,?>> async(String name,Object c)throws Exception{return (CompletableFuture<Either<?,?>>)api.getMethod(name,context).invoke(null,c);}
        boolean activated(ServerPlayer p,Object stone)throws Exception{return (boolean)api.getMethod("isWaystoneActivated",net.minecraft.world.entity.player.Player.class,waystone).invoke(null,p,stone);}
    }
    private static final class Walk {
        final GameTestHelper h;final Api api=new Api();final ServerLevel level,end;final net.minecraft.server.MinecraftServer server;final Path proofFile;final boolean restart;final java.util.concurrent.atomic.AtomicBoolean competing=new java.util.concurrent.atomic.AtomicBoolean(),veto=new java.util.concurrent.atomic.AtomicBoolean();
        final BlockPos aPos,bPos,endPos=new BlockPos(0,81,0);ServerPlayer p;Object a,b,endStone;int phase;long deadline;List<CompletableFuture<Either<?,?>>> waiting=List.of();CompoundTag proof;
        Walk(GameTestHelper h)throws Exception{
            this.h=h;level=h.getLevel();server=level.getServer();end=Objects.requireNonNull(server.getLevel(Level.END));proofFile=server.getWorldPath(LevelResource.ROOT).resolve("waystone-runtime-proof.nbt");restart=Files.exists(proofFile);
            h.assertTrue(WaystoneAccess.status().contains("pre-cost teleport gate registered"),"Exact mod versions and actual adapter registered: "+WaystoneAccess.status());
            aPos=h.absolutePos(new BlockPos(12,1,12));bPos=h.absolutePos(new BlockPos(44,1,12));
            if(restart){proof=AtomicNbt.read(proofFile);p=ClaimRuntimeTests.vanillaPlayer(level,proof.getUUID("player"),"WaystoneTraveler");level.getChunkAt(aPos);level.getChunkAt(bPos);end.getChunkAt(endPos);a=api.saved(server,proof.getUUID("a"));b=api.saved(server,proof.getUUID("b"));endStone=api.saved(server,proof.getUUID("end"));phase=4;deadline=h.getTick()+40;}
            else{
                DimensionTravelRuntimeTests.seed(server,8);p=ClaimRuntimeTests.vanillaPlayer(level,UUID.nameUUIDFromBytes("OfflinePlayer:WaystoneTraveler".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"WaystoneTraveler");
                for(var pos:BlockPos.betweenClosed(aPos.offset(-4,-1,-4),bPos.offset(4,-1,4)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
                for(var pos:BlockPos.betweenClosed(endPos.offset(-4,-1,-4),endPos.offset(4,-1,4)))end.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
                a=api.place(level,aPos);b=api.place(level,bPos);endStone=api.place(end,endPos);p.moveTo(aPos.getX()+.5,aPos.getY(),aPos.getZ()+2.5);p.giveExperienceLevels(20);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,16));phase=0;deadline=h.getTick()+40;
            }
            h.assertTrue(!p.hasPermissions(2),"Actual vanilla Survival actor, no operator bypass");
            competing.set(!restart);api.competingOverride(p,competing,veto);
        }
        void tick(){if(phase<0||h.getTick()<deadline)return;try{
            switch(phase){
                case 0->{
                    h.assertTrue(!api.activated(p,a),"New native node is not already personally activated");use(aPos);h.assertTrue(!api.activated(p,a),"Tier8 actual block use cannot activate a Waystone");
                    for(String name:List.of("tryTeleport","forceTeleport")){com.mojang.logging.LogUtils.getLogger().info("WAYSTONE_DENIAL_PATH {}",name);cancelled(api.sync(name,api.xp(p,b,3)));cancelled(api.sync(name,api.items(p,b,4)));}
                    waiting=List.of(api.async("tryTeleportAsync",api.xp(p,b,3)),api.async("forceTeleportAsync",api.items(p,b,4)));phase=1;deadline=h.getTick()+1;
                }
                case 1->{if(!completed())return;for(var f:waiting)cancelled(f.join());h.assertTrue(p.experienceLevel==20&&count()==16&&p.serverLevel()==level&&p.distanceToSqr(Vec3.atCenterOf(aPos))<16,"Every early sync/force/async attempt preserves exact XP/items/location");
                    competing.set(false);DimensionTravelRuntimeTests.seed(server,9);use(aPos);h.assertTrue(api.activated(p,a),"Tier9 actual interaction activates the native Waystone normally");
                    success(api.sync("tryTeleport",api.xp(p,b,3)));h.assertTrue(p.experienceLevel==17&&p.distanceToSqr(Vec3.atCenterOf(bPos))<36,"Tier9 normal native warp consumes exactly three XP levels");
                    success(api.sync("tryTeleport",api.items(p,a,4)));h.assertTrue(count()==12,"Native item requirement consumes exactly four iron once");
                    veto.set(true);cancelled(api.sync("forceTeleport",api.xp(p,b,1)));veto.set(false);h.assertTrue(p.distanceToSqr(Vec3.atCenterOf(aPos))<36&&p.experienceLevel==17,"Unlocked kingdom gate never clears another mod's entity veto");
                    for(String name:List.of("tryTeleport","forceTeleport"))cancelled(api.sync(name,api.xp(p,endStone,2)));
                    h.assertTrue(p.experienceLevel==17&&count()==12&&p.serverLevel()==level,"End remains locked at9 before any Waystone costs");
                    waiting=List.of(api.async("tryTeleportAsync",api.xp(p,b,1)));phase=2;deadline=h.getTick()+1;
                }
                case 2->{if(!completed())return;success(waiting.getFirst().join());h.assertTrue(p.experienceLevel==16,"Allowed async warp consumes native cost once");
                    DimensionTravelRuntimeTests.seed(server,12);success(api.sync("tryTeleport",api.xp(p,endStone,2)));h.assertTrue(p.serverLevel()==end&&p.experienceLevel==14,"Actual Waystone reaches real End only after12 with exact costs");
                    p.teleportTo(level,aPos.getX()+.5,aPos.getY(),aPos.getZ()+2.5,0,0);proof=new CompoundTag();proof.putUUID("player",p.getUUID());proof.putUUID("a",api.id(a));proof.putUUID("b",api.id(b));proof.putUUID("end",api.id(endStone));proof.putInt("xp",p.experienceLevel);proof.putInt("iron",count());AtomicNbt.write(proofFile,proof);finish("WAYSTONE_PREPARED: exact mod native interaction/sync/force/async Tier8 denials before XP/items, Tier9 activation/real costs/warp, End9denial and actual End12arrival; explicitly seeded boundary tiers");
                }
                case 4->{h.assertTrue(ContributionService.kingdom(server).attainedTier()==12&&p.experienceLevel==proof.getInt("xp")&&count()==proof.getInt("iron"),"Actual restart preserves seeded tier and exact native XP/inventory");
                    h.assertTrue(api.id(a).equals(proof.getUUID("a"))&&api.id(b).equals(proof.getUUID("b"))&&api.id(endStone).equals(proof.getUUID("end")),"All three actual Waystone database identities survive restart");
                    success(api.sync("tryTeleport",api.xp(p,b,1)));h.assertTrue(p.experienceLevel==13&&count()==12,"Restarted native warp still charges once and retains items");finish("WAYSTONE_RESTART_VERIFIED: same three native records, exact XP/items, seeded tier and actual usable server warp across a real JVM restart");}
            }
        }catch(Exception e){phase=-1;com.mojang.logging.LogUtils.getLogger().error("Actual Waystones integration test failed",e);h.fail(e.toString());}}
        void use(BlockPos pos){p.gameMode.useItemOn(p,level,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
        int count(){int total=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.IRON_INGOT))total+=p.getInventory().getItem(i).getCount();return total;}
        boolean completed(){if(waiting.stream().allMatch(CompletableFuture::isDone))return true;if(h.getTick()>1000)h.fail("Native async Waystone did not complete");return false;}
        void cancelled(Either<?,?> result){h.assertTrue(result.right().isPresent()&&result.right().orElseThrow().getClass().getSimpleName().equals("CancelledByEvent"),"Actual Waystones API reports Pre cancellation: "+result);}
        void success(Either<?,?> result){h.assertTrue(result.left().isPresent(),"Actual Waystones API completed native travel: "+result);}
        void finish(String marker){phase=-1;h.succeed();com.mojang.logging.LogUtils.getLogger().info(marker);}
    }
}
