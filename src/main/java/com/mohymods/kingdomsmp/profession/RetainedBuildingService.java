package com.mohymods.kingdomsmp.profession;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Loaded-world stability checks over native provenance; cached UI facts are never completion authority. */
public final class RetainedBuildingService {
    public record Snapshot(int count,String hash) { }
    private record Review(UUID nonce,UUID admin,UUID player,String quest,String dimension,BlockPos from,BlockPos to,String reason,long revision,long issued) { }
    private record Display(long tick,long revision,Snapshot snapshot,String denial) { }
    private static final Map<ServerPlayer,Review> REVIEWS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Map<String,Display>> DISPLAYS=new WeakHashMap<>();
    private RetainedBuildingService() { }
    public static void initialize(net.minecraft.server.MinecraftServer server){RetainedBuildingRules.clear(server);REVIEWS.keySet().removeIf(p->p.server==server);DISPLAYS.keySet().removeIf(p->p.server==server);}
    public static void changed(UUID player){DISPLAYS.keySet().removeIf(p->p.getUUID().equals(player));}
    public static boolean real(ServerPlayer p){return !(p instanceof FakePlayer)&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.server.getPlayerList().getPlayer(p.getUUID())==p;}
    private static void eligible(ServerPlayer p,String id){eligible(p,id,false);}
    private static void eligible(ServerPlayer p,String id,boolean completedAllowed){
        var d=RetainedBuildingQuests.get(id);if(!real(p))throw new IllegalStateException("Use the real living enrolled survival player");
        var roster=CommerceService.store(p.server).professions(p.getUUID());
        if(!roster.isActive(d.profession())||!completedAllowed&&roster.track(d.profession()).completedQuestIds().contains(id)||!RetainedBuildingQuests.catalog().mayAttempt(d.profession(),id,roster.track(d.profession()).completedQuestIds())||ContributionService.kingdom(p.server).attainedTier()<d.minimumTier()||!ProfessionService.available(p,d.profession()))throw new IllegalStateException("Active profession, maintained premises, tier and objective prerequisites required");
    }
    private static void siteSafe(ServerPlayer p,BuilderWorksite site){
        var level=p.serverLevel();if(!site.dimension().equals(level.dimension().location().toString()))throw new IllegalStateException("Visit the worksite dimension; no remote chunk loading");
        var from=site.from().below();var to=site.to().above(2);
        if(level.isOutsideBuildHeight(from)||level.isOutsideBuildHeight(to)||!level.getWorldBorder().isWithinBounds(from)||!level.getWorldBorder().isWithinBounds(to))throw new IllegalStateException("Worksite, support and headroom must remain inside world limits");
        for(var pos:BlockPos.betweenClosed(from,to))if(!level.hasChunkAt(pos))throw new IllegalStateException("All worksite chunks must already be loaded; no chunk tickets are created");
        String denial=PlacementProtection.check(level,p,from,to);if(denial!=null)throw new IllegalStateException("Worksite permission: "+denial);
        for(var project:ConstructionService.store(p.server).projects())if(project.origin()!=null&&site.bounds().intersects(ConstructionService.bounds(project)))throw new IllegalStateException("Manual worksite overlaps an existing kingdom construction reservation");
        var hut=BuilderHutSavedData.get(p.server).hut().orElse(null);
        if(hut!=null){var b=BuilderHutBlueprint.create();var origin=hut.origin();var bounds=new AABB(origin.getX(),origin.getY()-4,origin.getZ(),origin.getX()+b.width(hut.rotation()),origin.getY()+BuilderHutBlueprint.HEIGHT,origin.getZ()+b.depth(hut.rotation()));if(site.bounds().intersects(bounds))throw new IllegalStateException("Manual worksite cannot count the starting Builder's Hut");}
    }
    static boolean foundation(BlockState state){return state.is(BlockTags.BASE_STONE_OVERWORLD)||state.is(Blocks.COBBLESTONE)||state.is(Blocks.STONE_BRICKS)||state.is(Blocks.POLISHED_ANDESITE)||state.is(Blocks.POLISHED_DIORITE)||state.is(Blocks.POLISHED_GRANITE);}
    public static boolean candidate(BuilderWorksite site,BlockPos pos,BlockState state){
        if(!site.contains(pos))return false;
        return switch(site.quest()){
            case "kingdom_smp:builder/a01"->pos.getY()==site.from().getY()&&foundation(state);
            case "kingdom_smp:builder/a02"->pos.getY()>site.from().getY()&&state.is(BlockTags.LOGS);
            case "kingdom_smp:builder/a03"->pos.getY()>=site.from().getY()+3&&(state.is(BlockTags.PLANKS)||state.is(BlockTags.WOODEN_STAIRS)||state.is(BlockTags.WOODEN_SLABS));
            case "kingdom_smp:builder/a06"->state.is(Blocks.DIRT_PATH)||foundation(state)||state.is(BlockTags.PLANKS)||state.getBlock() instanceof StairBlock||state.getBlock() instanceof SlabBlock;
            default->false;
        };
    }
    private static boolean loadBearing(ServerLevel level,BlockPos pos){var state=level.getBlockState(pos);return state.isCollisionShapeFullBlock(level,pos)||state.is(BlockTags.LOGS)||state.getBlock() instanceof StairBlock||state.getBlock() instanceof SlabBlock;}
    private static Set<BlockPos> supported(ServerLevel level,BuilderWorksite site){
        Set<BlockPos> result=new HashSet<>();var queue=new ArrayDeque<BlockPos>();
        for(int x=site.from().getX();x<=site.to().getX();x++)for(int z=site.from().getZ();z<=site.to().getZ();z++){
            var pos=new BlockPos(x,site.from().getY(),z);
            if(loadBearing(level,pos)&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)){result.add(pos);queue.add(pos);}
        }
        while(!queue.isEmpty()){
            var current=queue.removeFirst();
            for(var direction:Direction.values()){
                var next=current.relative(direction);
                if(site.contains(next)&&!result.contains(next)&&loadBearing(level,next)){result.add(next);queue.addLast(next);}
            }
        }
        return result;
    }
    public static Snapshot scan(ServerPlayer p,String id){return scan(p,id,false);}
    public static Snapshot inspectCompletedProject(ServerPlayer p,String id){return scan(p,id,true);}
    private static Snapshot scan(ServerPlayer p,String id,boolean completedAllowed){
        eligible(p,id,completedAllowed);var site=CommerceService.store(p.server).builderWorksite(p.getUUID(),id).orElseThrow(()->new IllegalStateException("Ask an administrator to authorize your personal worksite before placing materials"));siteSafe(p,site);
        var level=p.serverLevel();Set<BlockPos> supported=id.endsWith("a02")||id.endsWith("a03")?supported(level,site):Set.of();
        Set<BlockPos> retained=new HashSet<>();var states=new TreeMap<Long,String>();
        for(var e:RetainedBuildingSavedData.get(p.server).evidence().positions(site.id()).entrySet()){
            var pos=BlockPos.of(e.getKey());if(!site.contains(pos))continue;var state=level.getBlockState(pos);
            if(!state.toString().equals(e.getValue())||!candidate(site,pos,state))continue;
            boolean safe=switch(id){
                case "kingdom_smp:builder/a01"->level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);
                case "kingdom_smp:builder/a02","kingdom_smp:builder/a03"->supported.contains(pos);
                case "kingdom_smp:builder/a06"->level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()&&level.getBlockState(pos.above(2)).getCollisionShape(level,pos.above(2)).isEmpty();
                default->false;
            };
            if(safe){retained.add(pos);states.put(pos.asLong(),state.toString());}
        }
        int count=retained.size();
        if(id.endsWith("a06"))count=connectedRoute(level,retained);
        StringBuilder text=new StringBuilder(site.id().toString()).append('|').append(site.dimension()).append('|').append(count);
        states.forEach((pos,state)->text.append('|').append(pos).append('=').append(state));
        try{return new Snapshot(count,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8))));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException("SHA-256 unavailable",impossible);}
    }
    private static int connectedRoute(ServerLevel level,Set<BlockPos> cells){
        var remaining=new HashSet<>(cells);int largest=0;
        Set<BlockPos> approaches=new HashSet<>();
        for(var project:ConstructionService.store(level.getServer()).projects()){
            if(project.phase()!=ConstructionStore.Phase.COMPLETE||project.npcDead()||!ConstructionService.structureIntact(level,project))continue;
            for(var block:BlueprintRegistry.get(project.blueprint(),project.blueprintVersion()).blocks(project.rotation())){
                var state=block.state();if(!(state.getBlock() instanceof DoorBlock)||state.getValue(DoorBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)continue;
                var door=project.origin().offset(block.offset());var facing=state.getValue(DoorBlock.FACING);
                for(var side:List.of(facing,facing.getOpposite())){
                    var approach=door.relative(side);if(!level.hasChunkAt(approach))continue;
                    if((loadBearing(level,approach.below())||level.getBlockState(approach.below()).is(Blocks.DIRT_PATH))&&level.getBlockState(approach).getCollisionShape(level,approach).isEmpty()&&level.getBlockState(approach.above()).getCollisionShape(level,approach.above()).isEmpty())approaches.add(approach);
                }
            }
        }
        while(!remaining.isEmpty()){
            var first=remaining.iterator().next();remaining.remove(first);var queue=new ArrayDeque<BlockPos>();queue.add(first);int count=0;boolean reaches=false;
            while(!queue.isEmpty()){
                var pos=queue.removeFirst();count++;if(approaches.contains(pos.above()))reaches=true;
                for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){var next=pos.relative(direction).offset(0,dy,0);if(remaining.remove(next))queue.addLast(next);}
            }
            if(reaches)largest=Math.max(largest,count);
        }
        return largest;
    }
    private static Display display(ServerPlayer p,String id){
        long tick=p.server.getTickCount(),revision=CommerceService.store(p.server).revision();var rows=DISPLAYS.computeIfAbsent(p,v->new HashMap<>());var old=rows.get(id);
        if(old!=null&&tick-old.tick()<10&&old.revision()==revision)return old;
        Display current;try{current=new Display(tick,revision,scan(p,id),"");}catch(IllegalStateException unavailable){String reason=Objects.toString(unavailable.getMessage(),"Worksite unavailable");current=new Display(tick,revision,null,reason.substring(0,Math.min(180,reason.length())));}
        rows.put(id,current);return current;
    }
    public static String status(ServerPlayer p,String id){
        var roster=CommerceService.store(p.server).professions(p.getUUID());if(roster.track(Profession.BUILDER).completedQuestIds().contains(id))return "Complete. Native retained placements and earned XP recorded.";
        var view=display(p,id);if(view.snapshot()==null)return view.denial();
        String hint=switch(id){
            case "kingdom_smp:builder/a01"->"Masonry at the site's lowest layer.";
            case "kingdom_smp:builder/a02"->"Logs/wood above the foundation, linked to supported framing.";
            case "kingdom_smp:builder/a03"->"Wooden roof at least3blocks above the foundation, linked to its frame.";
            default->"Clear connected road reaching an actual service doorway.";
        };
        return view.snapshot().count()+" / "+RetainedBuildingRules.get(p.server).goal(id)+". "+hint+" Only native placements after approval and supported final positions count.";
    }
    public static boolean ready(ServerPlayer p,String id){var view=display(p,id);return view.snapshot()!=null&&view.snapshot().count()>=RetainedBuildingRules.get(p.server).goal(id);}
    public static void complete(ServerPlayer p,UUID operation,String id)throws IOException{
        ContractService.requireCounter(p,Profession.BUILDER);var snapshot=scan(p,id);int goal=RetainedBuildingRules.get(p.server).goal(id);
        if(snapshot.count()<goal)throw new IllegalStateException("Retained materials, support or access changed; inspect the current worksite");
        var site=CommerceService.store(p.server).builderWorksite(p.getUUID(),id).orElseThrow();CommerceService.store(p.server).completeRetainedBuilding(operation,p.getUUID(),id,site.id(),goal,snapshot.count(),snapshot.hash());changed(p.getUUID());
    }
    private static void inspectAuthorization(ServerPlayer admin,ServerPlayer player,BuilderWorksite site){
        if(!admin.hasPermissions(2)||admin instanceof FakePlayer||!admin.isAlive()||admin.server.getPlayerList().getPlayer(admin.getUUID())!=admin)throw new IllegalStateException("A living on-site operator must authorize the project");
        eligible(player,site.quest());
        if(admin.serverLevel()!=player.serverLevel()||!site.dimension().equals(admin.serverLevel().dimension().location().toString())||site.bounds().getCenter().distanceToSqr(admin.position())>32*32||site.bounds().getCenter().distanceToSqr(player.position())>48*48)throw new IllegalStateException("Operator and player must attend the proposed worksite in its dimension");
        siteSafe(player,site);
    }
    public static UUID reviewSite(ServerPlayer admin,ServerPlayer player,String id,BlockPos from,BlockPos to,String reason){
        UUID nonce=UUID.randomUUID();var site=new BuilderWorksite(nonce,admin.getUUID(),player.getUUID(),id,player.serverLevel().dimension().location().toString(),from,to,reason);inspectAuthorization(admin,player,site);
        REVIEWS.put(admin,new Review(nonce,admin.getUUID(),player.getUUID(),id,site.dimension(),from.immutable(),to.immutable(),reason,CommerceService.store(admin.server).revision(),System.nanoTime()));return nonce;
    }
    public static Optional<UUID> currentReview(ServerPlayer admin){var r=REVIEWS.get(admin);return admin.hasPermissions(2)&&r!=null?Optional.of(r.nonce()):Optional.empty();}
    public static void confirmSite(ServerPlayer admin,UUID nonce)throws IOException{
        var r=REVIEWS.remove(admin);var store=CommerceService.store(admin.server);
        if(r==null||!r.admin().equals(admin.getUUID())||!r.nonce().equals(nonce)||r.revision()!=store.revision()||System.nanoTime()-r.issued()>30_000_000_000L)throw new IllegalStateException("Site authorization expired or canonical state changed; review again");
        var player=admin.server.getPlayerList().getPlayer(r.player());if(player==null)throw new IllegalStateException("The worksite player must remain online");
        var site=new BuilderWorksite(r.nonce(),r.admin(),r.player(),r.quest(),r.dimension(),r.from(),r.to(),r.reason());inspectAuthorization(admin,player,site);
        store.authorizeBuilderWorksite(r.nonce(),r.admin(),r.player(),r.quest(),r.dimension(),r.from(),r.to(),r.reason());changed(player.getUUID());
        com.mojang.logging.LogUtils.getLogger().info("BUILDER_WORKSITE actor={} player={} quest={} site={} from={} to={} reason={}",admin.getUUID(),player.getUUID(),r.quest(),r.nonce(),r.from(),r.to(),r.reason());
    }
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> commands(){
        return Commands.literal("worksite").requires(s->s.hasPermission(2))
            .then(Commands.literal("status").then(Commands.argument("player",EntityArgument.player()).executes(c->{
                var player=EntityArgument.getPlayer(c,"player");var records=CommerceService.store(player.server).builderWorksites().stream().filter(v->v.player().equals(player.getUUID())).toList();
                c.getSource().sendSuccess(()->Component.literal(player.getGameProfile().getName()+" | current authorized worksites="+records.size()+"; inspection is read-only and never loads chunks"),false);
                for(var site:records){String evidence;try{var snapshot=inspectCompletedProject(player,site.quest());evidence=snapshot.count()+"/"+RetainedBuildingRules.get(player.server).goal(site.quest())+" | snapshot "+snapshot.hash();}catch(RuntimeException unavailable){evidence="Paused: "+Objects.toString(unavailable.getMessage(),"evidence unavailable");}
                    String line=site.quest()+" | site "+site.id()+" | operator "+site.admin()+" | "+site.dimension()+" "+site.from().toShortString()+" .. "+site.to().toShortString()+" | "+evidence+" | "+site.reason();c.getSource().sendSuccess(()->Component.literal(line),false);
                }return 1;
            })))
            .then(Commands.literal("inspect").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("quest",ResourceLocationArgument.id()).suggests((c,b)->{RetainedBuildingQuests.catalog().definitions().keySet().stream().sorted().forEach(b::suggest);return b.buildFuture();})
                .then(Commands.argument("from",BlockPosArgument.blockPos()).then(Commands.argument("to",BlockPosArgument.blockPos()).then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->{try{
                    var admin=c.getSource().getPlayerOrException();var nonce=reviewSite(admin,EntityArgument.getPlayer(c,"player"),ResourceLocationArgument.getId(c,"quest").toString(),BlockPosArgument.getLoadedBlockPos(c,"from"),BlockPosArgument.getLoadedBlockPos(c,"to"),StringArgumentType.getString(c,"reason"));
                    c.getSource().sendSuccess(()->Component.literal("Authorize only future personal native placements. Existing structures and earlier/replaced-site evidence are not transferred; no blocks/items change. ").append(Component.literal("[Confirm worksite]").withStyle(s->s.withColor(0xE8C66A).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/kingdom admin worksite confirm "+nonce)))),false);return 1;
                }catch(Exception e){c.getSource().sendFailure(Component.literal(Objects.toString(e.getMessage(),e.toString())));return 0;}})))))))
            .then(Commands.literal("confirm").then(Commands.argument("review",UuidArgument.uuid()).executes(c->{try{confirmSite(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"review"));c.getSource().sendSuccess(()->Component.literal("Worksite authorization saved; only subsequent real player placements can earn evidence"),false);return 1;}catch(Exception e){c.getSource().sendFailure(Component.literal(Objects.toString(e.getMessage(),e.toString())));return 0;}})));
    }
}
