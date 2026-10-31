package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Opt-in real claim mods; isolated world only, never included in the playable JAR. */
@GameTestHolder("kingdom_smp_claim_tests")
@PrefixGameTestTemplate(false)
public final class ClaimRuntimeTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void actualClaimPermissions(GameTestHelper helper) throws Exception {
        ServerLevel level=helper.getLevel();var server=level.getServer();
        helper.assertTrue(PlacementProtection.installed(),"Claim test profile actually loads protection mods");
        var owner=player(level,"ClaimOwner");var stranger=player(level,"ClaimOther");
        BlockPos origin=helper.absolutePos(new BlockPos(2,1,2));
        BlockPos site=origin.offset(20,0,0);
        // Explicit isolated fixture preparation; the engine itself never loads these chunks.
        for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-4,-1,-4),site.offset(20,-1,20)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
        owner.moveTo(origin.getX()-2,origin.getY()+1,origin.getZ());stranger.moveTo(origin.getX()-3,origin.getY()+1,origin.getZ());
        helper.assertTrue(!owner.hasPermissions(2)&&!stranger.hasPermissions(2),"Actual registered actors have no administrator bypass");
        BlockPos point=new BlockPos((site.getX()>>4)*16+15,site.getY(),site.getZ());
        BlockPos across=point.east();
        helper.assertTrue(PlacementProtection.check(level,owner,point,across)==null,"Both APIs allow unclaimed loaded cross-chunk space");
        owner.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(PlacementProtection.check(level,owner,point,point)!=null,"Spectator cannot exploit permissive claim spectator behavior");
        owner.setGameMode(GameType.SURVIVAL);
        var fake=new net.neoforged.neoforge.common.util.FakePlayer(level,new GameProfile(owner.getUUID(),"ClaimOwner"));
        helper.assertTrue(PlacementProtection.check(level,fake,point,point)!=null,"Same-UUID fake player cannot act as the actual owner");
        helper.assertTrue(PlacementProtection.check(level,UUID.randomUUID(),point,point)!=null,"Missing/offline actor fails closed");
        var far=new BlockPos(24_000_000,80,24_000_000);
        helper.assertTrue(PlacementProtection.check(level,owner,far,far)!=null&&!level.hasChunkAt(far),"Permission query never loads distant chunks");

        ftbClaim(owner,point);ftbClaim(owner,across);
        helper.assertTrue(PlacementProtection.check(level,owner,point,across)==null,"FTB owner allowed across two claimed chunks");
        helper.assertTrue(PlacementProtection.check(level,stranger,point,point).startsWith("FTB Chunks"),"FTB denies unrelated actual player");
        opacClaim(level,stranger.getUUID(),across);
        helper.assertTrue(PlacementProtection.check(level,owner,point,across).startsWith("Open Parties"),"OPAC refusal overrides FTB ownership across boundary");
        opacUnclaim(level,across);opacClaim(level,owner.getUUID(),across);
        helper.assertTrue(PlacementProtection.check(level,owner,point,across)==null,"Both systems permit same actual owner");
        ftbClear(server);
        helper.assertTrue(PlacementProtection.check(level,stranger,across,across).startsWith("Open Parties"),"OPAC independently denies without FTB claim");
        opacUnclaim(level,across);

        // Real protected bootstrap command, exact player-bound chat token and native blocks/NPC.
        String preview="kingdom hut preview "+origin.getX()+" "+origin.getY()+" "+origin.getZ()+" north claim-fixture";
        var commands=server.getCommands().getDispatcher();
        helper.assertTrue(commands.execute(preview,owner.createCommandSourceStack().withPermission(4))==1,"Admin test source reviews clear claimed-aware Hut");
        helper.assertTrue(commands.execute(owner.confirmation(),owner.createCommandSourceStack().withPermission(4))==1,"Actual Hut confirmation installs through both claim APIs");
        var hut=BuilderHutSavedData.get(server).hut().orElseThrow();
        var builder=level.getEntity(hut.npcId());helper.assertTrue(builder!=null,"One native Aldric issued");
        var definition=BlueprintRegistry.lodge();
        var store=ConstructionService.store(server);
        var project=store.award("claim-fixture-only",FoundersLodgeBlueprint.ID,1,owner.getUUID(),"ClaimOwner")
                .claim().propose(site,Rotation.NONE).approve(owner.getUUID()).begin();
        store.save(project,owner.getUUID(),"ISOLATED_CLAIM_TEST");
        helper.assertTrue(ConstructionService.validateSite(level,project,false)==null,"Full support/volume/work-post site accepted");
        BlockPos post=definition.workPost(site,Rotation.NONE);builder.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);
        ConstructionService.step(server,project.id());
        helper.assertTrue(store.get(project.id()).placed()==1,"Real progressive write accepted by both loaded event listeners");
        opacClaim(level,stranger.getUUID(),post);
        ConstructionService.step(server,project.id());
        helper.assertTrue(store.get(project.id()).placed()==1,"Changed OPAC work-area permission pauses before another write");
        opacUnclaim(level,post);opacClaim(level,owner.getUUID(),post);
        ConstructionService.step(server,project.id());
        helper.assertTrue(store.get(project.id()).placed()==2,"Restored OPAC authority resumes without duplicate progress");
        ftbClaim(stranger,site);
        ConstructionService.step(server,project.id());
        helper.assertTrue(store.get(project.id()).placed()==2,"Changed FTB footprint permission pauses before another write");
        ftbClear(server);ftbClaim(owner,site);
        ConstructionService.step(server,project.id());
        helper.assertTrue(store.get(project.id()).placed()==3,"Restored FTB authority resumes physical construction");
        helper.assertTrue(PlacementProtection.eventActor(level,owner.getUUID(),builder)==owner,"Protection events use actual authoritative owner, never invented NPC permission");
        server.getPlayerList().remove(owner);
        ConstructionService.step(server,project.id());
        helper.assertTrue(store.get(project.id()).placed()==3,"Offline owner pauses under claims without item/progress loss");
        var directory=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data");
        helper.assertTrue(com.mohymods.kingdomsmp.persistence.ConstructionStore.open(directory).get(project.id()).placed()==3,"Native durable project reload retains exact accepted claim-aware prefix");
        server.getPlayerList().remove(stranger);
        helper.succeed();
        com.mojang.logging.LogUtils.getLogger().info("CLAIM_TEST_ASSERTIONS_PASSED: both APIs, independent denial, cross-chunk validation, native Hut, progressive revocation/resume, offline pause and journal reload");
    }

    @SuppressWarnings("unchecked")
    static ClaimPlayer player(ServerLevel level,String name)throws Exception{
        var p=new ClaimPlayer(level,name);var connection=new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // Embedded peers have no real configuration handshake. Declare the actual registered play
        // channels only in this test fixture, so login packets do not mask claim-engine assertions.
        var registrations=net.neoforged.neoforge.network.registration.NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
        registrations.setAccessible(true);
        var payloads=(Map<net.minecraft.network.ConnectionProtocol,Map<ResourceLocation,?>>)registrations.get(null);
        net.neoforged.neoforge.network.registration.ChannelAttributes.getOrCreateAdHocChannels(connection)
                .addAll(payloads.get(net.minecraft.network.ConnectionProtocol.PLAY).keySet());
        level.getServer().getPlayerList().placeNewPlayer(connection,p,CommonListenerCookie.createInitial(p.getGameProfile(),false));
        p.setGameMode(GameType.SURVIVAL);return p;
    }
    static void ftbClaim(ServerPlayer actor,BlockPos position)throws Exception{
        var root=Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");var type=Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
        var result=type.getMethod("claimAsPlayer",ServerPlayer.class,ResourceKey.class,ChunkPos.class,boolean.class)
                .invoke(root.getMethod("api").invoke(null),actor,actor.serverLevel().dimension(),new ChunkPos(position),false);
        if(!Boolean.TRUE.equals(Class.forName("dev.ftb.mods.ftbchunks.api.ClaimResult").getMethod("isSuccess").invoke(result)))throw new IllegalStateException("FTB fixture claim failed: "+result);
    }
    private static void ftbClear(net.minecraft.server.MinecraftServer server)throws Exception{
        var root=Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");var api=Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
        var manager=api.getMethod("getManager").invoke(root.getMethod("api").invoke(null));
        var chunks=(Collection<?>)Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager").getMethod("getAllClaimedChunks").invoke(manager);
        var unclaim=Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunk").getMethod("unclaim",net.minecraft.commands.CommandSourceStack.class,boolean.class);
        for(var chunk:List.copyOf(chunks))unclaim.invoke(chunk,server.createCommandSourceStack().withPermission(4),false);
    }
    private static Object opac(ServerLevel level)throws Exception{
        var api=Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
        return api.getMethod("getServerClaimsManager").invoke(api.getMethod("get",net.minecraft.server.MinecraftServer.class).invoke(null,level.getServer()));
    }
    static void opacClaim(ServerLevel level,UUID owner,BlockPos pos)throws Exception{
        Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI").getMethod("claim",ResourceLocation.class,UUID.class,int.class,int.class,int.class,boolean.class)
                .invoke(opac(level),level.dimension().location(),owner,0,pos.getX()>>4,pos.getZ()>>4,false);
    }
    private static void opacUnclaim(ServerLevel level,BlockPos pos)throws Exception{
        Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI").getMethod("unclaim",ResourceLocation.class,int.class,int.class)
                .invoke(opac(level),level.dimension().location(),pos.getX()>>4,pos.getZ()>>4);
    }
    static final class ClaimPlayer extends ServerPlayer{
        final List<Component> messages=new ArrayList<>();
        ClaimPlayer(ServerLevel level,String name){super(level.getServer(),level,new GameProfile(UUID.randomUUID(),name),ClientInformation.createDefault());}
        @Override public void sendSystemMessage(Component c){messages.add(c.copy());}
        String confirmation(){
            for(int i=messages.size()-1;i>=0;i--){
                var command=messages.get(i).visit((style,text)->{
                    var click=style.getClickEvent();return click!=null&&click.getAction()==ClickEvent.Action.RUN_COMMAND&&click.getValue().startsWith("/kingdom hut confirm ")?Optional.of(click.getValue().substring(1)):Optional.<String>empty();
                },Style.EMPTY);
                if(command.isPresent())return command.get();
            }throw new IllegalStateException("No authenticated Hut confirmation delivered");
        }
    }
}
