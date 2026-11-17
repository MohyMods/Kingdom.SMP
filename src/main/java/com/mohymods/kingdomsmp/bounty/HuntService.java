package com.mohymods.kingdomsmp.bounty;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import com.mohymods.kingdomsmp.profession.ContractRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.io.IOException;
import java.util.*;

/** Accepted wilderness hunts: bounded server evidence, reserved payment, reviewed once-only handover. */
public final class HuntService {
    private static final String PROOF="kingdomsmp_hunt_origin";
    public record Quote(UUID nonce,long revision,long day,HuntRules terms,UUID accepted,long issued,String text) { }
    private static final Map<ServerPlayer,Quote> QUOTES=new WeakHashMap<>();
    private static final Map<MinecraftServer,ArrayDeque<Runnable>> PENDING=new WeakHashMap<>();
    private HuntService() { }
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST,false,FinalizeSpawnEvent.class,HuntService::spawn);
        bus.addListener(EventPriority.LOWEST,false,LivingDeathEvent.class,HuntService::death);
        bus.addListener(HuntService::tick);
    }
    public static void initialize(MinecraftServer server) { QUOTES.keySet().removeIf(p->p.server==server);PENDING.remove(server);HuntRules.clear(server);HuntSavedData.get(server); }
    public static void close(ServerPlayer p) { QUOTES.remove(p); }
    public static long today() { return ContractRules.period("daily"); }
    private static boolean real(ServerPlayer p) { return !(p instanceof FakePlayer)&&!p.isCreative()&&!p.isSpectator()&&p.isAlive()&&p.server.getPlayerList().getPlayer(p.getUUID())==p; }
    public static Optional<Quote> quote(ServerPlayer p) {
        var q=QUOTES.get(p);
        if(q!=null&&(q.day()!=today()||q.revision()!=CommerceService.store(p.server).revision()||System.nanoTime()-q.issued()>30_000_000_000L)){close(p);q=null;}
        return Optional.ofNullable(q);
    }
    private static void expire(MinecraftServer server) throws IOException {
        var store=CommerceService.store(server);long day=today();
        for(var hunt:store.hunts())if(!hunt.closed()&&hunt.day()<day)store.closeHunt(UUID.randomUUID(),hunt.player(),hunt.id(),day,false);
    }
    public static Quote review(ServerPlayer p,String selected) throws IOException {
        if(!selected.equals("hunt:daily"))throw new IllegalArgumentException("Choose today's hunting contract");
        CommunityContractService.requireCounter(p);if(!real(p))throw new IllegalStateException("Hunting contracts require a living Survival or Adventure player");
        CommerceService.verifyReceipt(p);expire(p.server);var store=CommerceService.store(p.server);long day=today();var hunt=store.hunt(p.getUUID(),day).orElse(null);var configured=HuntRules.get(p.server);
        HuntRules terms=hunt==null?new HuntRules(configured.goal(),configured.reward(),store.huntBudget(day,configured.pool())):new HuntRules(hunt.goal(),hunt.reward(),store.huntBudget(day,configured.pool()));String text;
        if(hunt==null){if(terms.reward()>terms.pool()-store.huntSpent(day))throw new IllegalStateException("Today's hunting reward pool is fully reserved");text="Accept today's wilderness hunt: personally defeat "+terms.goal()+" "+targetName(HuntState.target(day))+". The kingdom reserves "+terms.reward()+" coins for you. Return to Garrick and review payment before 00:00 UTC; unfinished or unclaimed hunts expire unpaid. One acceptance per player per UTC day. Only recent natural Overworld spawns (today or yesterday), killed directly by you or your projectile while online in Survival/Adventure, count. Spawners, eggs, commands, pets, machines, indirect deaths and recycled victims do not count. Claims remain protected. No items are consumed.";}
        else{if(hunt.closed())throw new IllegalStateException("Today's hunting contract is already closed");int count=HuntSavedData.get(p.server).evidence().count(hunt.id());if(count<hunt.goal())throw new IllegalStateException("Hunt progress "+count+" / "+hunt.goal()+"; return after personally defeating the remaining eligible targets");text="Complete today's verified hunt of "+hunt.goal()+" "+targetName(hunt.target())+" and unlock exactly "+hunt.reward()+" reserved coins in your private Mailbox. Claiming this payment can happen only once. No items are consumed.";}
        var q=new Quote(UUID.randomUUID(),store.revision(),day,terms,hunt==null?null:hunt.id(),System.nanoTime(),text);QUOTES.put(p,q);return q;
    }
    public static String confirm(ServerPlayer p,UUID nonce) throws IOException {
        var q=QUOTES.remove(p);var store=CommerceService.store(p.server);CommerceService.verifyReceipt(p);
        if(q==null||!q.nonce().equals(nonce)||q.revision()!=store.revision()||q.day()!=today()||System.nanoTime()-q.issued()>30_000_000_000L)throw new IllegalStateException("Hunt review expired or kingdom accounts changed; review again");
        CommunityContractService.requireCounter(p);if(!real(p))throw new IllegalStateException("Hunt player is not eligible");
        if(q.accepted()==null){store.acceptHunt(q.nonce(),p.getUUID(),q.day(),q.terms().goal(),q.terms().reward(),q.terms().pool());return "Hunt accepted. Your payment is reserved; personally defeat the listed natural targets and return to Garrick before 00:00 UTC.";}
        var hunt=store.hunt(p.getUUID(),q.day()).orElseThrow();if(!hunt.id().equals(q.accepted())||hunt.closed()||HuntSavedData.get(p.server).evidence().count(hunt.id())<hunt.goal())throw new IllegalStateException("Hunting proof is incomplete or its entitlement changed");
        store.closeHunt(q.nonce(),p.getUUID(),hunt.id(),q.day(),true);return "Hunt completed once. Your reserved coins are ready to claim from the Mailbox.";
    }
    public static List<CommerceView.Entry> entries(ServerPlayer p) {
        var store=CommerceService.store(p.server);long day=today();var hunt=store.hunt(p.getUUID(),day).orElse(null);var terms=HuntRules.get(p.server);String target=hunt==null?HuntState.target(day):hunt.target();int goal=hunt==null?terms.goal():hunt.goal(),count=hunt==null?0:HuntSavedData.get(p.server).evidence().count(hunt.id());long reward=hunt==null?terms.reward():hunt.reward();
        String state=!CommunityContractService.built(p)?"LOCKED":hunt==null?"HUNT_AVAILABLE":hunt.closed()?"CONTRACT_DONE":count>=goal?"HUNT_READY":"HUNT_TRACKING";
        return List.of(new CommerceView.Entry("hunt:daily","Wilderness patrol · "+targetName(target),"Personal "+count+" / "+goal+"; "+reward+" reserved coins. Pool "+store.huntSpent(day)+" / "+store.huntBudget(day,terms.pool())+". Accept and return to Garrick before 00:00 UTC. Recent natural Overworld targets; direct player/projectile kills. "+(hunt!=null&&hunt.paid()?"Payment in Mailbox.":"Review all eligibility rules."),reward,state));
    }
    private static String targetName(String target) { return switch(target){case "minecraft:zombie"->"Zombies";case "minecraft:skeleton"->"Skeletons";case "minecraft:spider"->"Spiders";default->"Creepers";}; }
    private static void spawn(FinalizeSpawnEvent event) {
        var mob=event.getEntity();mob.getPersistentData().remove(PROOF);
        if(event.isSpawnCancelled()||event.getSpawnType()!=MobSpawnType.NATURAL||!(mob.level() instanceof ServerLevel level)||level.dimension()!=net.minecraft.world.level.Level.OVERWORLD||CommerceService.initializedStore(level.getServer()).isEmpty())return;
        String type=BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();if(!HuntState.TARGETS.contains(type))return;
        try{mob.getPersistentData().put(PROOF,HuntSavedData.get(level.getServer()).evidence().spawn(mob.getUUID(),type,level.dimension().location().toString(),today()));}
        catch(RuntimeException failed){com.mojang.logging.LogUtils.getLogger().error("Optional natural-spawn evidence refused; native spawning continues",failed);}
    }
    private static void death(LivingDeathEvent event) {
        try{collectDeath(event);}catch(RuntimeException failure){com.mojang.logging.LogUtils.getLogger().error("Optional death evidence refused; native combat continues",failure);}
    }
    private static void collectDeath(LivingDeathEvent event) {
        if(!(event.getSource().getEntity() instanceof ServerPlayer p)||!real(p)||!(event.getEntity().level() instanceof ServerLevel level)||p.serverLevel()!=level||level.getEntity(event.getEntity().getUUID())!=event.getEntity()||p.distanceToSqr(event.getEntity())>1024)return;
        var direct=event.getSource().getDirectEntity();if(direct!=p&&!(direct instanceof Projectile projectile&&projectile.getOwner()==p))return;
        var store=CommerceService.initializedStore(p.server).orElse(null);if(store==null||store.quarantined())return;var hunt=store.hunt(p.getUUID(),today()).orElse(null);if((hunt==null||hunt.closed())&&!com.mohymods.kingdomsmp.profession.EncounterQuests.eligible(p))return;
        var entity=event.getEntity();var pos=entity.blockPosition().immutable();var proof=entity.getPersistentData().getCompound(PROOF).copy();var type=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();var id=entity.getUUID();long day=today();
        var pending=PENDING.computeIfAbsent(p.server,s->new ArrayDeque<>());if(pending.size()>=8192)return;
        pending.add(()->{
            if(event.isCanceled()||entity.isAlive()||entity.getPose()!=net.minecraft.world.entity.Pose.DYING||level.getEntity(id)!=entity||!real(p)||today()!=day||p.serverLevel()!=level||!level.hasChunkAt(pos)||PlacementProtection.check(level,p,pos,pos)!=null)return;
            var data=HuntSavedData.get(p.server);
            if(data.evidence().naturalOrigin(id,type,level.dimension().location().toString(),proof,day))try{com.mohymods.kingdomsmp.profession.EncounterQuests.record(p,id,type);}catch(RuntimeException failed){com.mojang.logging.LogUtils.getLogger().error("Optional encounter evidence refused; native death and hunt remain independent",failed);}
            if(hunt!=null&&store.hunt(p.getUUID(),day).filter(h->h.id().equals(hunt.id())&&!h.closed()).isPresent()&&data.evidence().record(hunt,id,type,level.dimension().location().toString(),proof,day)){data.setDirty();p.sendSystemMessage(Component.literal("Wilderness patrol: "+data.evidence().count(hunt.id())+" / "+hunt.goal()+". "+(data.evidence().count(hunt.id())>=hunt.goal()?"Return to Garrick before 00:00 UTC to collect payment.":"")));}
        });
    }
    private static void tick(ServerTickEvent.Post event) {
        var server=event.getServer();var pending=PENDING.remove(server);
        if(pending!=null)while(!pending.isEmpty())try{pending.removeFirst().run();}catch(RuntimeException e){com.mojang.logging.LogUtils.getLogger().error("Hunt evidence refused; no reward granted",e);}
        if(server.getTickCount()%1200==0&&CommerceService.initializedStore(server).isPresent())try{expire(server);var data=HuntSavedData.get(server);data.evidence().prune(today());data.setDirty();}catch(IOException|RuntimeException e){com.mojang.logging.LogUtils.getLogger().error("Hunt expiry deferred; reserved payments remain protected",e);}
    }
}
