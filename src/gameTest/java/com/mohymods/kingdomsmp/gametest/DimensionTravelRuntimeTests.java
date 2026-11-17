package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.kingdom.KingdomTravelRules;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;

/** Actual optional dimensions, actual normal dedicated JVMs; explicitly seeded tiers test gate boundaries, not earned progression. */
@GameTestHolder("kingdom_smp_travel_tests") @PrefixGameTestTemplate(false)
public final class DimensionTravelRuntimeTests {
    /** Test-only native saved-data fixture; deliberately no shared package or production tier-grant API. */
    static void seed(net.minecraft.server.MinecraftServer server,int tier)throws Exception{
        var tag=KingdomSavedData.get(server).save(new CompoundTag(),server.registryAccess());tag.putInt("tier",tier);tag.putLong("revision",tier);
        var load=KingdomSavedData.class.getDeclaredMethod("load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);load.setAccessible(true);
        var data=(KingdomSavedData)load.invoke(null,tag,server.registryAccess());data.setDirty();server.overworld().getDataStorage().set(KingdomSavedData.DATA_NAME,data);
    }
    @GameTest(template="empty",timeoutTicks=2400)
    public static void realRealmBoundariesAndRestart(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var path=server.getWorldPath(LevelResource.ROOT).resolve("travel-runtime-proof.nbt");
        boolean restart=Files.exists(path);var player=ClaimRuntimeTests.player(h.getLevel(),"RealmTraveler");
        player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,23));
        h.assertTrue(!player.hasPermissions(2),"Ordinary Survival traveler, no operator or bypass");
        h.assertTrue(server.getCommands().getDispatcher().execute("kingdom admin travel inspect",server.createCommandSourceStack())==1,"Actual administrator diagnostics");
        try{server.getCommands().getDispatcher().execute("kingdom admin travel inspect",player.createCommandSourceStack().withPermission(0));h.fail("Ordinary travel diagnostics accepted");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){}
        if(restart){var proof=AtomicNbt.read(path);h.assertTrue(proof.getInt("tier")==12&&ContributionService.kingdom(server).attainedTier()==12,"Native saved baseline persists across actual JVM restart, no progression awards invented");}
        var home=player.serverLevel();var original=new Vec3(player.getX(),player.getY(),player.getZ());int[] step={0};
        // Register once: adding callbacks while GameTest iterates its tick map is unsafe.
        h.onEachTick(()->{
            if(h.getTick()<20+step[0]*20L)return;
            try{
                if(step[0]>=KingdomTravelRules.realms().size()){
                    if(!restart){seed(server,12);var proof=new CompoundTag();proof.putInt("tier",12);proof.putInt("realms",step[0]);AtomicNbt.write(path,proof);}
                    h.assertTrue(player.getInventory().getItem(0).is(Items.IRON_INGOT)&&player.getInventory().getItem(0).getCount()==23,"All transitions preserve actual player inventory");
                    server.getPlayerList().remove(player);h.succeed();
                    com.mojang.logging.LogUtils.getLogger().info(restart?"TRAVEL_RESTART_VERIFIED: real registered realms, native saved Tier12, actual player transitions and return":"TRAVEL_PREPARED: exact Aether/Twilight/Otherside and vanilla gates, premature native/teleport/item denial, unlocked transitions, inventory, no operator bypass; explicitly seeded boundary tiers");return;
                }
                var realm=KingdomTravelRules.realms().get(step[0]);var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(realm.dimension()));var target=server.getLevel(key);
                h.assertTrue(target!=null,"Exact installed dimension is registered: "+realm.dimension());
                var transition=new DimensionTransition(target,new Vec3(0,target.getMaxBuildHeight()-8,0),Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING);
                if(!restart){
                    seed(server,realm.tier()-1);
                    h.assertTrue(ContributionService.kingdom(server).attainedTier()==realm.tier()-1,"Explicit isolated boundary tier loaded");
                    h.assertTrue(NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(player,key)).isCanceled(),"Actual entry event refuses premature "+realm.name());
                    h.assertTrue(player.changeDimension(transition)==null&&player.serverLevel()==home,"Native transition canceled before moving: "+realm.name());
                    player.teleportTo(target,0,transition.pos().y,0,0,0);h.assertTrue(player.serverLevel()==home,"Direct mod-style teleport cannot bypass "+realm.name());
                    var item=new ItemEntity(home,original.x,original.y,original.z,new ItemStack(Items.DIAMOND,7));
                    h.assertTrue(item.changeDimension(transition)==null&&item.level()==home&&item.getItem().getCount()==7,"Premature item transfer retains exact item stack: "+realm.name());
                    seed(server,realm.tier());
                }
                h.assertTrue(!NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(player,key)).isCanceled(),"Entry unlocks at approved boundary: "+realm.name());
                h.assertTrue(player.changeDimension(transition)==player&&player.serverLevel()==target,"Actual unlocked ServerPlayer transition: "+realm.name());
                h.assertTrue(player.changeDimension(new DimensionTransition(home,original,Vec3.ZERO,0,0,DimensionTransition.DO_NOTHING))==player&&player.serverLevel()==home,"Overworld return is never locked: "+realm.name());
                // The other public cross-dimension API must work after unlocking too.
                player.teleportTo(target,0,transition.pos().y,0,0,0);h.assertTrue(player.serverLevel()==target,"Unlocked teleportTo works: "+realm.name());
                player.teleportTo(home,original.x,original.y,original.z,0,0);h.assertTrue(player.serverLevel()==home,"Direct Overworld return works");
                step[0]++;
            }catch(Exception failure){h.fail("Real dimension boundary failed: "+failure);}
        });
    }
}
