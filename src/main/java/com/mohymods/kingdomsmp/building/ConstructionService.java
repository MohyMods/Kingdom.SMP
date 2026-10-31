package com.mohymods.kingdomsmp.building;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.persistence.ConstructionStore.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.io.IOException;
import com.mohymods.kingdomsmp.progression.WorldProgression;
import java.util.*;

/** Main-thread authority for one-use plans, approved reservations and bounded NPC work. */
public final class ConstructionService {
    public static final String FIRST_HEARTH="first_hearth", STARTING_HUT="starting_hut";
    private static final Map<ServerPlayer,UUID> SELECTION=new WeakHashMap<>();
    private static final long REVIEW_NANOS=300_000_000_000L;
    private static final UUID NONE=ConstructionRequest.NONE;
    private static final Map<MinecraftServer,ConstructionStore> STORES=new WeakHashMap<>();
    private static final Map<ServerPlayer,Review> REVIEWS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Preview> PREVIEWS=new WeakHashMap<>();
    private static final Map<UUID,String> ACTIVITY=new HashMap<>();
    private static final Map<UUID,Integer> VERIFIED=new HashMap<>();
    private static boolean placing;
    public record Review(UUID nonce,int action,UUID project,UUID proposal,long revision,long issued) { }
    public record Preview(BlockPos origin,Rotation rotation,String denial,long issued) { }
    private ConstructionService() { }

    public static void initialize(MinecraftServer server) {
        STORES.remove(server);SELECTION.clear();REVIEWS.clear();PREVIEWS.clear();ACTIVITY.clear();VERIFIED.clear();store(server);
    }
    public static ConstructionStore store(MinecraftServer server) {
        var store=STORES.get(server);
        if(store==null){
            try{store=ConstructionStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));STORES.put(server,store);}
            catch(IOException|RuntimeException e){throw new IllegalStateException("Construction journal requires recovery; restore matching world data",e);}
        }
        var profile=WorldProgression.get(server);
        if(profile.mode()==WorldProgression.Mode.ROYAL_HALL&&profile.owner()!=null&&!store.quarantined())try{
            var online=server.getPlayerList().getPlayer(profile.owner());
            store.award(STARTING_HUT,BlueprintRegistry.HUT,1,profile.owner(),online==null?(server.getSingleplayerProfile()==null?"Starting owner":server.getSingleplayerProfile().getName()):online.getGameProfile().getName());
        }catch(IOException e){throw storageFailure(e);}
        var ledger=ContributionService.ledger(server);
        if(ledger.progress().complete()&&!store.quarantined())try{
            UUID owner=ledger.progress().winner().orElseThrow();
            store.award(FIRST_HEARTH,profile.mode()==WorldProgression.Mode.ROYAL_HALL?TownHallBlueprint.ID:FoundersLodgeBlueprint.ID,1,owner,ContributionService.name(ledger,owner));
        }catch(IOException e){throw storageFailure(e);}
        for(var p:store.projects())BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        return store;
    }
    public static Project lodge(MinecraftServer server){return store(server).reward(FIRST_HEARTH).orElse(null);}
    public static boolean royal(MinecraftServer server){return WorldProgression.get(server).mode()==WorldProgression.Mode.ROYAL_HALL;}
    public static String rewardTitle(MinecraftServer server){return royal(server)?"Royal Town Hall":"Founders' Lodge";}
    public static boolean hut(Project p){return p!=null&&p.blueprint().equals(BlueprintRegistry.HUT);}
    public static boolean hall(Project p){return p!=null&&p.blueprint().equals(TownHallBlueprint.ID);}
    public static boolean purchased(Project p){return p!=null&&p.reward().startsWith("market:");}
    public static String resident(MinecraftServer server,Project p){
        return hall(p)||purchased(p)?WorldProgression.get(server).resident(p.id(),BlueprintRegistry.defaultResident(p.blueprint())):hut(p)?"Aldric Stonehand":"Mira Ashford";
    }
    private static void requireTier(MinecraftServer server,Project p){
        if(purchased(p)&&ContributionService.kingdom(server).attainedTier()<BlueprintRegistry.requiredTier(p.blueprint()))
            throw new IllegalStateException(BlueprintRegistry.title(p.blueprint())+" requires Kingdom Tier "+BlueprintRegistry.requiredTier(p.blueprint())+".");
    }
    public static Project current(ServerPlayer player){
        var store=store(player.server);
        var selected=SELECTION.get(player);if(selected!=null)for(var p:store.projects())if(p.id().equals(selected)&&!(hut(p)&&p.phase()==Phase.COMPLETE&&store.reward(FIRST_HEARTH).isPresent()))return p;
        var starter=store.reward(STARTING_HUT).orElse(null);
        if(starter!=null&&starter.phase()!=Phase.COMPLETE)return starter;
        var first=lodge(player.server);
        if(first!=null&&first.owner().equals(player.getUUID())&&first.phase().ordinal()<Phase.BUILDING.ordinal())return first;
        var active=store.projects().stream().filter(p->purchased(p)&&p.owner().equals(player.getUUID())&&p.phase()==Phase.BUILDING).findFirst().orElse(null);
        if(active!=null)return active;
        return store.projects().stream().filter(p->purchased(p)&&p.owner().equals(player.getUUID())&&p.phase().ordinal()<Phase.BUILDING.ordinal())
                .sorted(Comparator.comparing(Project::reward)).findFirst().orElse(first);
    }
    /** Held placement is resolved separately from an explicitly inspected mailbox/board project. */
    public static Project heldProject(ServerPlayer player){
        var store=store(player.server);
        for(var stack:List.of(player.getMainHandItem(),player.getOffhandItem())){
            var type=KingdomBlueprints.definition(stack);if(type==null)continue;
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(tag.hasUUID("store")&&tag.hasUUID("project")&&tag.hasUUID("token")&&tag.getUUID("store").equals(store.identity())){
                for(var p:store.projects())if(p.id().equals(tag.getUUID("project"))&&p.blueprint().equals(type)&&p.owner().equals(player.getUUID())&&Objects.equals(p.token(),tag.getUUID("token"))){
                    return p;
                }
            }
            throw new IllegalStateException("The held plan is stale or copied. Recover its current plan through your mailbox.");
        }
        throw new IllegalStateException("Hold your authenticated physical blueprint.");
    }
    public static Project selectHeld(ServerPlayer player){var p=heldProject(player);if(!p.id().equals(SELECTION.get(player)))select(player,p.id());return p;}
    public static void select(ServerPlayer player,UUID id){store(player.server).get(id);SELECTION.put(player,id);REVIEWS.remove(player);PREVIEWS.remove(player);}
    public static void closeReview(ServerPlayer player){REVIEWS.remove(player);PREVIEWS.remove(player);SELECTION.remove(player);}

    public static String request(ServerPlayer player,ConstructionRequest request){
        int action=request.action();
        if(action==0){REVIEWS.remove(player);return "";}
        if(store(player.server).quarantined())throw new IllegalStateException("Construction storage is quarantined. Administrator recovery is required.");
        if(action%2==0)return confirm(player,request.review(),action);
        if(action==3){var preview=preview(player,request.origin(),Rotation.values()[request.rotation()]);return preview.denial().isBlank()?"Review the outlined footprint and exterior work post, then submit for administrator approval.":preview.denial();}
        review(player,action);return switch(action){
            case 1->"Recovering a plan invalidates all old copies and cancels any previous site approval. Materials are included in construction.";
            case 5->"Review this exact footprint. No terrain changes are authorized. Confirm only after inspecting the site.";
            case 7->"Begin this approved project once. The plan is consumed; the building belongs to the kingdom.";
            case 9->"Resume only the existing project. No missing blocks or NPCs are recreated.";
            default->throw new IllegalArgumentException("Unsupported construction action");
        };
    }
    public static void openPlan(ServerPlayer player,BlockPos origin){
        try{
            selectHeld(player);Project p=owned(player);if(!hasPlan(player,p))throw new IllegalStateException("This plan is stale, copied, or belongs to another player. The winner can recover it in Construction.");
            if(origin!=null)preview(player,origin,Rotation.NONE);
            ConstructionNetworking.open(player,BlueprintRegistry.title(p.blueprint())+" — supplied materials, one kingdom building.");
        }catch(IllegalArgumentException|IllegalStateException e){player.sendSystemMessage(Component.literal(e.getMessage()));}
    }
    public static Review review(ServerPlayer player,int action){
        if(store(player.server).quarantined())throw new IllegalStateException("Construction storage is quarantined. Administrator recovery is required.");
        Project p=current(player);if(p==null)throw new IllegalStateException("Complete the First Hearth first, or have the server owner initialize the starter entitlement.");
        requireTier(player.server,p);
        if(action==1){
            ownerAlive(player,p);if(p.phase().ordinal()>=Phase.BUILDING.ordinal())throw new IllegalStateException("The blueprint was already redeemed.");
            if(claimSlot(player,p)<0)throw new IllegalStateException("Make a free main-inventory slot before claiming or recovering the plan.");
        }else if(action==5){
            administrator(player);if(p.phase()!=Phase.PROPOSED)throw new IllegalStateException("There is no current proposal to approve.");
            nearby(player,p.origin());String denied=validateSite(player.serverLevel(),p,false);if(denied!=null)throw new IllegalStateException(denied);
            PREVIEWS.put(player,new Preview(p.origin(),p.rotation(),"",System.nanoTime()));
        }else if(action==7){
            ownerAlive(player,p);if(!hasPlan(player,p)||p.phase()!=Phase.APPROVED)throw new IllegalStateException("A current plan and administrator-approved proposal are required.");
            if(!hut(p))requireBuilder(player.server);
            nearby(player,p.origin());String denied=validateSite(player.serverLevel(),p,false);if(denied!=null)throw new IllegalStateException(denied);
            if(store(player.server).projects().stream().anyMatch(q->q.phase()==Phase.BUILDING))throw new IllegalStateException("Aldric already has a reserved construction project.");
        }else if(action==9){
            administrator(player);if(p.phase()!=Phase.BUILDING)throw new IllegalStateException("There is no paused construction to resume.");
            nearby(player,p.origin());String denied=validateSite(player.serverLevel(),p,true);if(denied==null)denied=verifyPrefix(player.serverLevel(),p);
            if(denied!=null)throw new IllegalStateException(denied);
        }else throw new IllegalArgumentException("Invalid construction review");
        Review review=new Review(UUID.randomUUID(),action+1,p.id(),p.proposal(),store(player.server).revision(),System.nanoTime());REVIEWS.put(player,review);return review;
    }
    public static Preview preview(ServerPlayer player,BlockPos origin,Rotation rotation){
        Preview inspected=inspectCandidate(player,origin,rotation);
        Project p=owned(player);
        PREVIEWS.put(player,inspected);REVIEWS.remove(player);
        if(inspected.denial().isBlank())REVIEWS.put(player,new Review(UUID.randomUUID(),4,p.id(),p.proposal(),store(player.server).revision(),System.nanoTime()));
        return inspected;
    }
    /** Read-only candidate inspection; hologram probes never invalidate/authorize a GUI review. */
    public static Preview inspectCandidate(ServerPlayer player,BlockPos origin,Rotation rotation){return inspectCandidate(player,owned(player),origin,rotation);}
    public static Preview inspectCandidate(ServerPlayer player,Project p,BlockPos origin,Rotation rotation){
        ownerAlive(player,p);requireTier(player.server,p);
        if(!hasPlan(player,p)||p.phase().ordinal()>=Phase.BUILDING.ordinal())throw new IllegalStateException("Claim a current unused blueprint first.");
        nearby(player,origin);
        Project candidate=p.propose(origin,rotation);String denied=validateSite(player.serverLevel(),candidate,false);
        return new Preview(origin.immutable(),rotation,denied==null?"":denied,System.nanoTime());
    }
    public static String confirm(ServerPlayer player,UUID nonce,int action){
        Review review=REVIEWS.remove(player);
        var store=store(player.server);
        if(store.quarantined())throw new IllegalStateException("Construction storage is quarantined. Administrator recovery is required.");
        if(review==null||review.action()!=action||!review.nonce().equals(nonce)||System.nanoTime()-review.issued()>=REVIEW_NANOS
                ||review.revision()!=store.revision())throw new IllegalStateException("Review expired or state changed. Review the exact action again.");
        Project p=store.get(review.project());requireTier(player.server,p);
        if(!Objects.equals(review.proposal(),p.proposal()))throw new IllegalStateException("The proposal changed; review again.");
        try{
            switch(action){
                case 2->{
                    ownerAlive(player,p);if(p.phase().ordinal()>=Phase.BUILDING.ordinal())throw new IllegalStateException("Entitlement already redeemed.");
                    int slot=claimSlot(player,p);if(slot<0)throw new IllegalStateException("Make a free main-inventory slot first.");
                    Project claimed=p.claim();store.save(claimed,player.getUUID(),"CLAIM_OR_RECOVER");
                    // Durable generation rotation precedes delivery. Old/restored copies have no authority.
                    for(int i=0;i<player.getInventory().getContainerSize();i++)if(isProjectPlan(player.getInventory().getItem(i),p))player.getInventory().setItem(i,ItemStack.EMPTY);
                    player.getInventory().setItem(slot,plan(store,claimed));syncInventory(player);PREVIEWS.remove(player);
                    audit(player,p,"CLAIM_OR_RECOVER");return "Current blueprint delivered. Earlier copies and site approvals are invalid.";
                }
                case 4->{
                    ownerAlive(player,p);if(!hasPlan(player,p))throw new IllegalStateException("Current blueprint required.");
                    Preview preview=PREVIEWS.get(player);
                    if(preview==null||System.nanoTime()-preview.issued()>=REVIEW_NANOS)throw new IllegalStateException("Placement preview expired.");
                    nearby(player,preview.origin());Project proposed=p.propose(preview.origin(),preview.rotation());
                    String denied=validateSite(player.serverLevel(),proposed,false);if(denied!=null)throw new IllegalStateException(denied);
                    if(royal(player.server))proposed=proposed.approve(player.getUUID());
                    store.save(proposed,player.getUUID(),royal(player.server)?"SERVER_VALIDATED_SITE":"PROPOSE_SITE");audit(player,proposed,"PROPOSE_SITE");
                    return royal(player.server)?"Complete site validated. Review construction in the Project tab.":"Site proposed. An administrator must inspect and approve this exact footprint.";
                }
                case 6->{
                    administrator(player);nearby(player,p.origin());String denied=validateSite(player.serverLevel(),p,false);
                    if(denied!=null)throw new IllegalStateException(denied);
                    store.save(p.approve(player.getUUID()),player.getUUID(),"APPROVE_EXACT_SITE_NO_TERRAIN_CHANGES");audit(player,p,"APPROVE_SITE");
                    return "Exact site approved. The winner may review and begin construction.";
                }
                case 8->{
                    ownerAlive(player,p);if(!hasPlan(player,p)||p.phase()!=Phase.APPROVED)throw new IllegalStateException("Current plan and site approval required.");
                    if(!hut(p))requireBuilder(player.server);
                    nearby(player,p.origin());String denied=validateSite(player.serverLevel(),p,false);if(denied!=null)throw new IllegalStateException(denied);
                    if(store.projects().stream().anyMatch(q->q.phase()==Phase.BUILDING))throw new IllegalStateException("Aldric already has a project.");
                    store.save(p.begin(),player.getUUID(),"REDEEM_AND_RESERVE");
                    for(int i=0;i<player.getInventory().getContainerSize();i++)if(isProjectPlan(player.getInventory().getItem(i),p))player.getInventory().setItem(i,ItemStack.EMPTY);
                    syncInventory(player);VERIFIED.remove(p.id());PREVIEWS.remove(player);audit(player,p,"REDEEM_AND_RESERVE");
                    if(hut(p))bootstrap(player.server,p.id());
                    return "Construction reserved. Aldric will walk to the site; supplied materials cannot be claimed separately.";
                }
                case 10->{
                    administrator(player);nearby(player,p.origin());if(p.phase()!=Phase.BUILDING)throw new IllegalStateException("Project changed.");
                    String denied=validateSite(player.serverLevel(),p,true);if(denied==null)denied=verifyPrefix(player.serverLevel(),p);
                    if(denied!=null)throw new IllegalStateException(denied);
                    store.save(p,player.getUUID(),"INSPECT_AND_RESUME");VERIFIED.put(p.id(),p.placed());ACTIVITY.remove(p.id());audit(player,p,"RESUME");
                    return "Existing project inspected and resumed. No missing blocks or NPCs were regenerated.";
                }
                default->throw new IllegalArgumentException("Invalid confirmation");
            }
        }catch(IOException e){throw storageFailure(e);}
        finally{ConstructionNetworking.push(player.server);}
    }
    public static ItemStack plan(ConstructionStore store,Project p){
        ItemStack stack=new ItemStack(KingdomBlueprints.item(p.blueprint()));CompoundTag t=new CompoundTag();
        t.putUUID("store",store.identity());t.putUUID("project",p.id());t.putUUID("token",Objects.requireNonNull(p.token()));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(t));return stack;
    }
    private static boolean isProjectPlan(ItemStack stack,Project p){
        if(!stack.is(KingdomBlueprints.item(p.blueprint())))return false;
        CompoundTag tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return tag.hasUUID("project")&&tag.getUUID("project").equals(p.id());
    }
    public static boolean hasPlan(ServerPlayer player,Project p){
        if(p==null||p.token()==null||!p.owner().equals(player.getUUID()))return false;
        var store=store(player.server);
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            ItemStack stack=player.getInventory().getItem(i);if(!isProjectPlan(stack,p))continue;
            CompoundTag tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(tag.hasUUID("store")&&tag.hasUUID("token")&&tag.getUUID("store").equals(store.identity())&&tag.getUUID("token").equals(p.token()))return true;
        }return false;
    }
    public static boolean heldPlan(ServerPlayer player,Project p){
        if(p==null||p.token()==null||!p.owner().equals(player.getUUID()))return false;
        var store=store(player.server);
        for(var stack:List.of(player.getMainHandItem(),player.getOffhandItem())){
            if(!isProjectPlan(stack,p))continue;
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(tag.hasUUID("store")&&tag.hasUUID("token")&&tag.getUUID("store").equals(store.identity())&&tag.getUUID("token").equals(p.token()))return true;
        }return false;
    }
    private static int claimSlot(ServerPlayer player,Project p){for(int i=0;i<36;i++)if(player.getInventory().getItem(i).isEmpty()||isProjectPlan(player.getInventory().getItem(i),p))return i;return -1;}
    private static void syncInventory(ServerPlayer player){player.getInventory().setChanged();player.inventoryMenu.sendAllDataToRemote();}
    private static Project owned(ServerPlayer player){
        Project p=current(player);if(p==null)throw new IllegalStateException("Complete the First Hearth to earn this reward, or initialize the owner Hut plan.");
        if(!p.owner().equals(player.getUUID()))throw new IllegalStateException("Only this registered blueprint holder can claim or place the plan.");return p;
    }
    private static void ownerAlive(ServerPlayer player,Project p){
        GameType type=player.gameMode.getGameModeForPlayer();
        if(!p.owner().equals(player.getUUID())||!player.isAlive()||type!=GameType.SURVIVAL&&type!=GameType.ADVENTURE||player.containerMenu!=player.inventoryMenu)
            throw new IllegalStateException("The blueprint holder must be alive in Survival/Adventure with other containers closed.");
    }
    private static void administrator(ServerPlayer player){if(!player.hasPermissions(2)||!player.isAlive()||player.isSpectator())throw new IllegalStateException("Administrator permission level 2 is required.");}
    private static void requireBuilder(MinecraftServer server){
        var hut=BuilderHutSavedData.get(server).hut().orElse(null);
        if(hut==null||hut.npcDead()||!BuilderHutService.structureIntact(server.overworld(),hut)
                ||!(server.overworld().getEntity(hut.npcId()) instanceof AldricEntity builder)||!builder.isAlive()||!BuilderHutService.isRegistered(builder))
            throw new IllegalStateException("Verify the loaded Builder's Hut and living registered Aldric before redeeming the plan.");
    }
    private static void nearby(ServerPlayer player,BlockPos origin){
        if(player.serverLevel().dimension()!=Level.OVERWORLD||player.distanceToSqr(origin.getX(),origin.getY(),origin.getZ())>64*64)
            throw new IllegalStateException("Stand in the Overworld within 64 blocks of the exact site.");
    }
    private static IllegalStateException storageFailure(Exception e){LogUtils.getLogger().error("Construction storage failed; all mutations paused",e);return new IllegalStateException("Construction storage requires recovery. Contact the administrator.",e);}
    private static void audit(ServerPlayer player,Project p,String action){LogUtils.getLogger().info("Construction action={} actor={} project={} proposal={} origin={} rotation={}",action,player.getUUID(),p.id(),p.proposal(),p.origin(),p.rotation());}

    public static AABB bounds(Project p){var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());return bounds(p.origin(),b,p.rotation());}
    private static AABB bounds(BlockPos o,ConstructionBlueprint b,Rotation r){return new AABB(o.getX(),o.getY(),o.getZ(),o.getX()+b.width(r),o.getY()+b.height(),o.getZ()+b.depth(r));}
    public static boolean loaded(ServerLevel level,Project p){
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        for(int x=p.origin().getX()>>4;x<=(p.origin().getX()+b.width(p.rotation())-1)>>4;x++)
            for(int z=p.origin().getZ()>>4;z<=(p.origin().getZ()+b.depth(p.rotation())-1)>>4;z++)if(!level.hasChunk(x,z))return false;
        return level.hasChunkAt(b.workPost(p.origin(),p.rotation()));
    }
    private static String claimDenial(ServerLevel level,Project p){
        if(!com.mohymods.kingdomsmp.placement.PlacementProtection.installed())return null;
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        BlockPos last=p.origin().offset(b.width(p.rotation())-1,b.height()-1,b.depth(p.rotation())-1);
        String denial=com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p.owner(),p.origin().below(),last);
        if(denial!=null)return denial;
        BlockPos post=b.workPost(p.origin(),p.rotation());
        return com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p.owner(),post.below(),post.above());
    }
    /** Validate the complete air volume and its support, without acquiring chunk tickets. */
    public static String validateSite(ServerLevel level,Project p,boolean resuming){
        if(level.dimension()!=Level.OVERWORLD)return "Construction is restricted to the Overworld.";
        try{requireTier(level.getServer(),p);}catch(IllegalStateException denied){return denied.getMessage();}
        if(hut(p)){
            if(p.origin()==null)return "No site selected.";
            for(var other:store(level.getServer()).projects())if(other.origin()!=null&&!other.id().equals(p.id())&&bounds(p).intersects(bounds(other)))return "Site overlaps another kingdom reservation.";
            var actor=level.getServer().getPlayerList().getPlayer(p.owner());
            return BuilderHutService.validateSite(level,p.origin(),p.rotation(),resuming,actor);
        }
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());BlockPos o=p.origin();
        if(o==null)return "No site has been selected.";
        BlockPos last=o.offset(b.width(p.rotation())-1,b.height()-1,b.depth(p.rotation())-1),post=b.workPost(o,p.rotation());
        if(level.isOutsideBuildHeight(o.below())||level.isOutsideBuildHeight(last)||!level.getWorldBorder().isWithinBounds(o)
                ||!level.getWorldBorder().isWithinBounds(last)||!level.getWorldBorder().isWithinBounds(post))return "The whole site must fit the world border and build height.";
        if(!loaded(level,p))return "Every site and work-post chunk must already be loaded. No chunks are force-loaded.";
        String protection=claimDenial(level,p);if(protection!=null)return protection;
        for(var other:store(level.getServer()).projects())if(other.origin()!=null&&!other.id().equals(p.id())&&bounds(p).intersects(bounds(other)))return "Site overlaps another kingdom reservation or building.";
        var hut=BuilderHutSavedData.get(level.getServer()).hut().orElse(null);
        if(hut!=null){var h=BuilderHutBlueprint.create();if(bounds(p).intersects(new AABB(hut.origin().getX(),hut.origin().getY(),hut.origin().getZ(),hut.origin().getX()+h.width(hut.rotation()),hut.origin().getY()+BuilderHutBlueprint.HEIGHT,hut.origin().getZ()+h.depth(hut.rotation()))))return "Site overlaps the Builder's Hut.";}
        for(int x=0;x<b.width(p.rotation());x++)for(int z=0;z<b.depth(p.rotation());z++){
            BlockPos support=o.offset(x,-1,z);if(!level.getBlockState(support).isFaceSturdy(level,support,Direction.UP))return "Prepare solid level support under the complete footprint.";
        }
        boolean weatherPost=resuming&&p.phase()==Phase.BUILDING&&level.getBlockState(post).is(Blocks.SNOW);
        if(!level.getBlockState(post.below()).isFaceSturdy(level,post.below(),Direction.UP)||!level.getBlockState(post).isAir()&&!weatherPost||!level.getBlockState(post.above()).isAir())return "Aldric needs a clear supported work post immediately outside the entrance.";
        Map<BlockPos,net.minecraft.world.level.block.state.BlockState> expected=new HashMap<>();
        if(resuming)for(var block:b.blocks(p.rotation()))expected.put(o.offset(block.offset()),block.state());
        for(BlockPos pos:BlockPos.betweenClosed(o,last)){
            var state=level.getBlockState(pos);var required=expected.get(pos);
            boolean weatherSnow=resuming&&p.phase()==Phase.BUILDING&&state.is(Blocks.SNOW);
            if(!state.isAir()&&!weatherSnow&&!(resuming&&required!=null&&ConstructionBlueprint.matches(state,required)))return "Site obstructed at "+pos.toShortString()+"; terrain and property are never replaced.";
        }
        if(!level.getEntities((Entity)null,bounds(p),e->!(e instanceof AldricEntity a&&BuilderHutService.isRegistered(a))).isEmpty())return "Move players, mobs and dropped items outside the footprint.";
        return null;
    }
    public static String verifyPrefix(ServerLevel level,Project p){
        if(!loaded(level,p))return "Site chunks are unloaded; construction paused.";
        var blocks=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion()).blocks(p.rotation());
        if(p.placed()>blocks.size())return "Saved blueprint version/progress mismatch.";
        for(int i=0;i<p.placed();i++){
            var b=blocks.get(i);BlockPos pos=p.origin().offset(b.offset());
            // Only completed purchased farm crop/field cells are renewable gameplay space.
            // Harvesting/trampling does not suspend the resident, and never regenerates crops.
            // Foundations, walls, roof, workstation and every in-progress prefix remain required.
            if(p.phase()==Phase.COMPLETE&&purchased(p)&&p.blueprint().equals(ProfessionBlueprints.FARMER)
                    &&(b.state().getBlock() instanceof CropBlock||b.state().getBlock() instanceof FarmBlock))continue;
            if(!ConstructionBlueprint.matches(level.getBlockState(pos),b.state()))return "Saved progress differs at "+pos.toShortString()+". Administrator inspection required; missing materials are not regenerated.";
        }return null;
    }
    public static boolean structureIntact(ServerLevel level,Project p){
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        if(p.origin()==null||p.placed()!=b.blocks().size()||!loaded(level,p))return false;
        return verifyPrefix(level,p)==null;
    }
    public static void tick(ServerTickEvent.Post event){
        MinecraftServer server=event.getServer();if(server.getTickCount()%10!=0)return;
        for(Project p:List.copyOf(store(server).projects()))if(p.phase()==Phase.BUILDING)step(server,p.id());
        if(server.getTickCount()%20==0)ConstructionNetworking.push(server);
    }
    /** One bounded physical placement; also exercised directly by native-server tests. */
    public static void step(MinecraftServer server,UUID project){
        ConstructionStore store=store(server);Project p=store.get(project);if(p.phase()!=Phase.BUILDING||store.quarantined())return;
        ServerLevel level=server.overworld();var blueprint=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        try{requireTier(server,p);}catch(IllegalStateException denied){pause(p,denied.getMessage());return;}
        if(!loaded(level,p)){pause(p,"Site unloaded; waiting without chunk tickets.");return;}
        if(hut(p)){bootstrap(server,p.id());return;}
        String protection=claimDenial(level,p);if(protection!=null){pause(p,protection);return;}
        var hut=BuilderHutSavedData.get(server).hut().orElse(null);
        if(hut==null||hut.npcDead()||!BuilderHutService.structureIntact(level,hut)){pause(p,"Builder's Hut damaged/unloaded or builder unavailable.");return;}
        Entity entity=level.getEntity(hut.npcId());
        if(!(entity instanceof AldricEntity builder)||!builder.isAlive()||!BuilderHutService.isRegistered(builder)){pause(p,"Registered Aldric is unloaded or missing; no replacement is spawned.");return;}
        BlockPos post=blueprint.workPost(p.origin(),p.rotation());
        if(!level.getBlockState(post.below()).isFaceSturdy(level,post.below(),Direction.UP)||!level.getBlockState(post).isAir()&&!level.getBlockState(post).is(Blocks.SNOW)||!level.getBlockState(post.above()).isAir()){
            pause(p,"Aldric's exterior work post is obstructed or unsupported; work paused.");return;
        }
        if(builder.distanceToSqr(post.getX()+0.5,post.getY(),post.getZ()+0.5)>4){pause(p,"Aldric is walking to the work post; blocked/unloaded paths pause work.");return;}
        if(!Objects.equals(VERIFIED.get(p.id()),p.placed())||p.placed()%40==0){
            String denied=verifyPrefix(level,p);if(denied!=null){pause(p,denied);return;}VERIFIED.put(p.id(),p.placed());
        }
        var blocks=blueprint.blocks(p.rotation());
        if(p.placed()==blocks.size()){finish(level,p);return;}
        var block=blocks.get(p.placed());BlockPos pos=p.origin().offset(block.offset());
        var before=level.getBlockState(pos);
        // The approved reservation was entirely air, and player placement is protected.
        // Subsequent weather snow may be brushed away without drops; no terrain/block is cleared.
        boolean replace=before.isAir()||before.is(Blocks.SNOW);
        if(!replace&&!ConstructionBlueprint.matches(before,block.state())){pause(p,"Site changed at "+pos.toShortString()+". No terrain or player blocks were replaced.");return;}
        if(replace&&!level.getEntities((Entity)null,new AABB(pos),e->e!=builder).isEmpty()){pause(p,"Waiting for the next block space to be clear.");return;}
        try{
            if(replace){
                BlockSnapshot snapshot=BlockSnapshot.create(level.dimension(),level,pos);
                placing=true;
                try{
                    if(!level.setBlock(pos,block.state(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)){pause(p,"Block placement failed; reservation retained.");return;}
                    if(NeoForge.EVENT_BUS.post(new BlockEvent.EntityMultiPlaceEvent(List.of(snapshot),level.getBlockState(pos.below()),com.mohymods.kingdomsmp.placement.PlacementProtection.eventActor(level,p.owner(),builder))).isCanceled()){
                        if(level.getBlockState(pos).equals(block.state())&&(!(level.getBlockEntity(pos) instanceof Container c)||c.isEmpty()))level.setBlock(pos,before,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                        pause(p,"A protection listener denied construction. Administrator inspection required.");return;
                    }
                }finally{placing=false;}
            }
            store.save(p.advance(p.placed()+1),p.owner(),"PROGRESS");VERIFIED.put(p.id(),p.placed()+1);
            builder.swing(InteractionHand.MAIN_HAND);builder.getLookControl().setLookAt(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
            level.playSound(null,builder.blockPosition(),SoundEvents.WOOD_PLACE,SoundSource.NEUTRAL,0.4f,0.8f);
            pause(p,"Aldric is constructing the "+BlueprintRegistry.title(p.blueprint())+" with supplied materials.");
        }catch(IOException|RuntimeException failure){pause(p,"Storage/placement failed; project retained for administrator recovery.");LogUtils.getLogger().error("Construction interrupted project={}",p.id(),failure);}
    }
    private static void bootstrap(MinecraftServer server,UUID id){
        var store=store(server);var p=store.get(id);var player=server.getPlayerList().getPlayer(p.owner());
        if(player==null||player.serverLevel()!=server.overworld()){pause(p,"Starting owner must be online in the Overworld to finish bootstrap.");return;}
        try{
            ownerAlive(player,p);placing=true;
            if(!BuilderHutService.bootstrap(player,p)){pause(p,"Hut installation is reserved; inspect the native Hut journal before resuming.");return;}
            var done=p.advance(BlueprintRegistry.get(p.blueprint(),1).blocks().size());if(!done.npcIssued())done=done.issueNpc();
            store.save(done.complete(),p.owner(),"COMPLETE_OWNER_HUT");
            pause(p,"Builder's Hut complete. Aldric Stonehand is ready; open Contribute to begin First Hearth.");
        }catch(IOException|RuntimeException e){pause(p,"Bootstrap paused: "+Objects.toString(e.getMessage(),"inspection required"));LogUtils.getLogger().warn("Hut bootstrap retained {}",id,e);}
        finally{placing=false;}
    }
    private static void finish(ServerLevel level,Project p){
        if(!structureIntact(level,p)){pause(p,"Final required-block verification failed; no completion or NPC reward granted.");return;}
        var store=store(level.getServer());
        try{
            // The reservation was empty and protected throughout work. Clear only weather
            // layers before handover, including spaces not occupied by blueprint blocks.
            // Never run this on a completed building where players may have placed snow.
            var definition=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
            BlockPos last=p.origin().offset(definition.width(p.rotation())-1,definition.height()-1,definition.depth(p.rotation())-1);
            for(BlockPos space:BlockPos.betweenClosed(p.origin(),last))
                if(level.getBlockState(space).is(Blocks.SNOW))level.setBlock(space,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            Entity existing=level.getEntity(p.npc());
            if(!(existing instanceof ReeveEntity)){
                if(p.npcIssued()){pause(p,"Reeve issuance is already committed; missing entity requires inspection, never an automatic duplicate.");return;}
                BlockPos home=p.origin().offset(definition.rotate(definition.home(),p.rotation()));
                for(BlockPos space:List.of(home,home.above())){
                    if(level.getBlockState(space).is(Blocks.SNOW))level.setBlock(space,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                    if(!level.getBlockState(space).isAir()){pause(p,"Reeve's verified home is obstructed; no NPC issuance committed.");return;}
                }
                if(!level.getEntities((Entity)null,new AABB(home).expandTowards(0,1,0),entity->true).isEmpty()){
                    pause(p,"Move occupants away from the Reeve's home before her arrival.");return;
                }
                store.save(p.issueNpc(),p.owner(),"RESERVE_REEVE_IDENTITY");p=store.get(p.id());
                ReeveEntity reeve=(purchased(p)?KingdomNpcs.RESIDENT:hall(p)?KingdomNpcs.STEWARD:KingdomNpcs.REEVE).get().create(level);if(reeve==null){pause(p,"Reeve creation failed; issuance retained.");return;}
                reeve.setUUID(p.npc());
                reeve.configure(p.id(),home,resident(level.getServer(),p));reeve.moveTo(home.getX()+0.5,home.getY(),home.getZ()+0.5,180,0);
                if(!level.addFreshEntity(reeve)){pause(p,"Reeve spawn rejected; identity remains reserved.");return;}
            }else if(existing.getType()!=(purchased(p)?KingdomNpcs.RESIDENT:hall(p)?KingdomNpcs.STEWARD:KingdomNpcs.REEVE).get()
                    ||!p.id().equals(((ReeveEntity)existing).buildingId())){pause(p,"Resident identity association mismatch.");return;}
            store.save(p.complete(),p.owner(),purchased(p)?"COMPLETE_PURCHASED_BUILDING":"COMPLETE_LODGE");
            for(var b:BlueprintRegistry.get(p.blueprint(),p.blueprintVersion()).blocks(p.rotation()))level.updateNeighborsAt(p.origin().offset(b.offset()),b.state().getBlock());
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(BlueprintRegistry.title(p.blueprint())+" is complete! "+resident(level.getServer(),p)+", the "+BlueprintRegistry.occupation(p.blueprint())+", welcomes the kingdom."),false);
            pause(p,BlueprintRegistry.title(p.blueprint())+" complete. Kingdom and milestone services available; future services remain locked.");
        }catch(IOException|RuntimeException error){pause(p,"Completion interrupted; identity/reservation retained for recovery.");LogUtils.getLogger().error("Building completion interrupted",error);}
    }
    public static BlockPos workTarget(AldricEntity builder){
        if(!(builder.level() instanceof ServerLevel level)||!BuilderHutService.isRegistered(builder))return null;
        for(Project p:store(level.getServer()).projects())if(p.phase()==Phase.BUILDING)return BlueprintRegistry.get(p.blueprint(),p.blueprintVersion()).workPost(p.origin(),p.rotation());
        return null;
    }
    /** Resolve by immutable project association, never by name or the First Hearth singleton. */
    public static Project projectForResident(ReeveEntity reeve){
        if(!(reeve.level() instanceof ServerLevel level)||level.dimension()!=Level.OVERWORLD||reeve.buildingId()==null)return null;
        return store(level.getServer()).projects().stream().filter(p->p.id().equals(reeve.buildingId())).findFirst().orElse(null);
    }
    public static boolean registeredReeve(ReeveEntity reeve){
        Project p=projectForResident(reeve);
        if(p==null||reeve.getType()!=(purchased(p)?KingdomNpcs.RESIDENT.get():hall(p)?KingdomNpcs.STEWARD.get():KingdomNpcs.REEVE.get())
                ||p.phase()!=Phase.COMPLETE||!p.npcIssued()||p.npcDead()||!p.npc().equals(reeve.getUUID()))return false;
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());return p.origin().offset(b.rotate(b.home(),p.rotation())).equals(reeve.home());
    }
    public static void openReeve(ServerPlayer player,ReeveEntity reeve){
        Project p=projectForResident(reeve);
        if(p==null||!registeredReeve(reeve)||!player.isAlive()||player.isSpectator()||!reeve.isAlive()||player.level()!=reeve.level()
                ||player.distanceToSqr(reeve)>64||!player.hasLineOfSight(reeve)||!structureIntact(player.serverLevel(),p)){
            player.sendSystemMessage(Component.literal("The resident is unavailable: check the completed building, proximity and registered identity."));return;
        }
        if(purchased(p)){
            try{requireTier(player.server,p);}catch(IllegalStateException denied){player.sendSystemMessage(Component.literal(denied.getMessage()));return;}
            com.mohymods.kingdomsmp.economy.CommerceService.openResident(player,reeve);return;
        }
        select(player,p.id());
        ConstructionNetworking.open(player,resident(player.server,p)+(hall(p)?" — Royal Steward: A kingdom is a promise kept together. I will keep our accounts of progress; you decide our future.":" — Reeve: Every beam has a builder, every gift a name. I keep the record; you write our history.")+" Choose Community milestone or Kingdom overview below.");
    }
    public static void reeveDied(ReeveEntity reeve){
        if(!registeredReeve(reeve))return;
        var level=(ServerLevel)reeve.level();var p=projectForResident(reeve);
        try{store(level.getServer()).save(p.markDead(),p.owner(),"RESIDENT_DIED_NO_AUTOMATIC_REPLACEMENT");}
        catch(IOException e){storageFailure(e);}
    }
    public static String activity(UUID project){return ACTIVITY.getOrDefault(project,"Waiting for loaded context and verification.");}
    private static void pause(Project p,String reason){ACTIVITY.put(p.id(),reason);}
    public static boolean protectedAt(ServerLevel level,BlockPos pos){
        if(level.dimension()!=Level.OVERWORLD)return false;
        return store(level.getServer()).projects().stream().anyMatch(p->{
            if(p.phase()!=Phase.BUILDING)return false;
            if(bounds(p).expandTowards(0,-1,0).contains(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5))return true;
            BlockPos post=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion()).workPost(p.origin(),p.rotation());
            return pos.getX()==post.getX()&&pos.getZ()==post.getZ()&&pos.getY()>=post.getY()-1&&pos.getY()<=post.getY()+1;
        });
    }
    public static boolean placing(){return placing;}
    public static ConstructionView view(ServerPlayer player,long requestId,boolean open,String notice){
        var store=store(player.server);Project p=current(player);var b=p==null?BlueprintRegistry.get(royal(player.server)?TownHallBlueprint.ID:FoundersLodgeBlueprint.ID,1):BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());Preview preview=PREVIEWS.get(player);Review review=REVIEWS.get(player);
        if(preview!=null&&System.nanoTime()-preview.issued()>=REVIEW_NANOS){PREVIEWS.remove(player);preview=null;}
        if(review!=null&&(System.nanoTime()-review.issued()>=REVIEW_NANOS||review.revision()!=store.revision())){REVIEWS.remove(player);review=null;}
        BlockPos origin=preview!=null?preview.origin():p!=null&&p.origin()!=null?p.origin():BlockPos.ZERO;
        Rotation rotation=preview!=null?preview.rotation():p!=null?p.rotation():Rotation.NONE;
        String status=p==null?"Finish all four First Hearth resource targets to earn the Lodge.":switch(p.phase()){
            case AVAILABLE->"The permanent highest contributor can claim the unique Lodge blueprint. Construction materials are included.";
            case CLAIMED->"Select a clear supported site. Preview its footprint, then submit it for administrator approval.";
            case PROPOSED->"Waiting for an administrator to inspect and approve the exact proposed footprint and exterior work post.";
            case APPROVED->"Site approved. The winner can review construction in the Project tab and redeem the current plan once.";
            case BUILDING->activity(p.id());
            case COMPLETE->p.npcDead()?"The Reeve has died. Services are unavailable; no automatic replacement is issued.":
                    structureIntact(player.server.overworld(),p)?"The furnished Lodge is complete. Mira Ashford is the kingdom's Reeve.":"Lodge damaged or unloaded; Reeve services are suspended.";
        };
        if(p==null&&royal(player.server))status=WorldProgression.get(player.server).owner()==null?"Server owner initialization required: the host can review /kingdom admin owner <player>, then confirm.":"Finish the four First Hearth targets to earn the Royal Town Hall. The owner's Hut comes first.";
        if(p!=null&&(royal(player.server)||purchased(p))){
            status=switch(p.phase()){
                case AVAILABLE->"Claim or recover your unique "+BlueprintRegistry.title(p.blueprint())+" plan. Materials are included.";
                case CLAIMED->"Hold your plan and aim at a clear, loaded, supported site. Server validation authorizes normal placement.";
                case PROPOSED->"Review the selected site.";
                case APPROVED->"Complete site validated. Open Project to review and begin construction.";
                case BUILDING->activity(p.id());
                case COMPLETE->hut(p)?"Builder's Hut complete. Aldric is ready; contribute to First Hearth.":p.npcDead()?"Resident unavailable; identity retained for recovery.":purchased(p)?BlueprintRegistry.title(p.blueprint())+" complete. Visit "+resident(player.server,p)+", "+BlueprintRegistry.occupation(p.blueprint())+", for building services.":"Town Hall complete. "+resident(player.server,p)+" serves the kingdom.";
            };
        }
        if(preview!=null&&!preview.denial().isBlank())status=preview.denial();
        if(store.quarantined())status="Construction storage is quarantined. All claims, approvals and work are paused until restart/reconciliation.";
        boolean admin=player.hasPermissions(2);StringBuilder audit=new StringBuilder();
        if(admin)for(var a:store.audit().stream().skip(Math.max(0,store.audit().size()-6)).toList())audit.append(a.action()).append(" · ").append(a.actor().toString(),0,8).append('\n');
        return new ConstructionView(requestId,store.revision(),open,p==null?NONE:p.id(),p==null?NONE:p.npc(),p==null?"Unawarded":p.ownerName(),p==null?"LOCKED":p.phase().name(),
                origin,rotation.ordinal(),preview!=null||p!=null&&p.origin()!=null,preview==null||preview.denial().isBlank(),b.width(rotation),b.depth(rotation),b.height(),p==null?0:p.placed(),b.blocks().size(),
                p!=null&&p.owner().equals(player.getUUID()),admin,p!=null&&hasPlan(player,p),review==null?0:review.action(),review==null?NONE:review.nonce(),
                limit(status,512),limit(notice,512),limit(audit.toString(),2048),b.id(),p==null?"Elara Vale":resident(player.server,p));
    }
    private static String limit(String s,int n){return s==null?"":s.substring(0,Math.min(n,s.length()));}
    public static LiteralArgumentBuilder<CommandSourceStack> commands(){
        return Commands.literal("lodge")
                .then(Commands.literal("name").then(Commands.argument("name",StringArgumentType.greedyString()).executes(c->{
                    var player=c.getSource().getPlayerOrException();
                    try{
                        var p=owned(player);ownerAlive(player,p);
                        if((!hall(p)&&!purchased(p))||p.phase().ordinal()>=Phase.BUILDING.ordinal())throw new IllegalStateException("Only the unused building-plan holder can name its new resident before construction.");
                        String name=StringArgumentType.getString(c,"name").trim();WorldProgression.get(player.server).name(p.id(),name);
                        audit(player,p,"NAME_NEW_RESIDENT");c.getSource().sendSuccess(()->Component.literal("New resident display name: "+name+". Persistent identity unchanged."),false);return 1;
                    }catch(IOException|IllegalArgumentException|IllegalStateException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
                })))
                .then(Commands.literal("open").executes(c->{ConstructionNetworking.open(c.getSource().getPlayerOrException(),"");return 1;}))
                .then(Commands.literal("preview").requires(s->s.hasPermission(2)).then(Commands.argument("origin",BlockPosArgument.blockPos()).then(Commands.argument("rotation",StringArgumentType.word()).executes(c->{
                    ServerPlayer player=c.getSource().getPlayerOrException();
                    try{String r=StringArgumentType.getString(c,"rotation");Rotation rotation=switch(r){case "north"->Rotation.NONE;case "east"->Rotation.CLOCKWISE_90;case "south"->Rotation.CLOCKWISE_180;case "west"->Rotation.COUNTERCLOCKWISE_90;default->throw new IllegalArgumentException("Use north, east, south or west.");};
                        preview(player,BlockPosArgument.getBlockPos(c,"origin"),rotation);ConstructionNetworking.open(player,"Review the outlined site before submitting it.");return 1;
                    }catch(IllegalArgumentException|IllegalStateException error){c.getSource().sendFailure(Component.literal(error.getMessage()));return 0;}
                }))));
    }
}
