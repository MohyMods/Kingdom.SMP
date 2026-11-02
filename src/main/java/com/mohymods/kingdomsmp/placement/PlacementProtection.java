package com.mohymods.kingdomsmp.placement;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Optional, exact-version claim APIs. No fake player, bypass grant or cached permission decision. */
public final class PlacementProtection {
    private static final Set<String> WARNED = new HashSet<>();
    private PlacementProtection() { }

    public static boolean installed() {
        return ModList.get().isLoaded("ftbchunks") || ModList.get().isLoaded("openpartiesandclaims");
    }

    public static String denial() {
        return installed() ? "Claim validation needs the actual online construction owner in this dimension." : null;
    }

    public static String check(ServerLevel level, UUID owner, BlockPos from, BlockPos to) {
        if (!installed()) return null;
        return check(level, level.getServer().getPlayerList().getPlayer(owner), from, to);
    }

    public static String check(ServerLevel level, ServerPlayer actor, BlockPos from, BlockPos to) {
        if (!installed()) return null;
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Claim checks require the server thread.");
        if (actor == null || actor.serverLevel() != level) return denial();
        if (!actor.isAlive() || actor.isSpectator() || actor instanceof net.neoforged.neoforge.common.util.FakePlayer)
            return "Claim validation requires the living, non-spectator player who owns this construction right.";
        long size = ((long) to.getX() - from.getX() + 1) * ((long) to.getY() - from.getY() + 1) * ((long) to.getZ() - from.getZ() + 1);
        if (from.getX() > to.getX() || from.getY() > to.getY() || from.getZ() > to.getZ() || size < 1 || size > 32768)
            return "Claim validation volume exceeds the supported blueprint bounds.";
        try {
            var checks = new ArrayList<Check>(2);
            if (ModList.get().isLoaded("ftbchunks")) {
                requireVersion("ftbchunks", "2101.1.22");
                Object api = Ftb.API.invoke(null);
                if (!Boolean.TRUE.equals(Ftb.READY.invoke(api))) return "FTB Chunks manager is not ready; construction paused.";
                Object manager = Ftb.MANAGER.invoke(api);
                checks.add(new Check("FTB Chunks", pos -> bool(Ftb.DENIED.invoke(manager, actor, InteractionHand.MAIN_HAND, pos, Ftb.EDIT, null))));
            }
            if (ModList.get().isLoaded("openpartiesandclaims")) {
                requireVersion("openpartiesandclaims", "0.33.0");
                Object protection = Opac.PROTECTION.invoke(Opac.API.invoke(null, level.getServer()));
                checks.add(new Check("Open Parties and Claims", pos -> bool(Opac.DENIED.invoke(protection, actor, level, pos))));
            }
            for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
                if (!level.hasChunkAt(pos)) return "Claim validation requires already-loaded chunks; construction paused.";
                if (!level.mayInteract(actor,pos)) return "Server spawn protection denies building at " + pos.toShortString() + ".";
                for (Check check : checks) if (check.test.denied(pos)) return check.name + " denies building at " + pos.toShortString() + ".";
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            String key = failure.getClass().getName() + ":" + failure.getMessage();
            if (WARNED.add(key)) LogUtils.getLogger().warn("Claim adapter unavailable; construction refused", failure);
            return "Installed claim API/version could not be verified; construction paused. See the server log.";
        }
    }

    /** Current entity interaction policy, including public-NPC exceptions. This does not require terrain-edit rights. */
    public static String entityInteraction(ServerPlayer actor, Entity target) {
        if (!actor.server.isSameThread()) throw new IllegalStateException("Claim checks require the server thread.");
        if (!actor.isAlive() || actor.isSpectator() || actor instanceof net.neoforged.neoforge.common.util.FakePlayer
                || actor.server.getPlayerList().getPlayer(actor.getUUID()) != actor || target == null || !target.isAlive()
                || target.level() != actor.serverLevel() || !actor.serverLevel().hasChunkAt(target.blockPosition()))
            return "Resident access requires the real living online player and a loaded resident in this dimension.";
        if (!installed()) return null;
        try {
            if (ModList.get().isLoaded("ftbchunks")) {
                requireVersion("ftbchunks", "2101.1.22"); Object api=Ftb.API.invoke(null);
                if (!Boolean.TRUE.equals(Ftb.READY.invoke(api))) return "FTB Chunks manager is not ready; resident access paused.";
                Object manager=Ftb.MANAGER.invoke(api);
                if (bool(Ftb.DENIED.invoke(manager,actor,InteractionHand.MAIN_HAND,target.blockPosition(),Ftb.INTERACT_ENTITY,target)))
                    return "FTB Chunks denies interaction with this resident.";
            }
            if (ModList.get().isLoaded("openpartiesandclaims")) {
                requireVersion("openpartiesandclaims", "0.33.0"); Object protection=Opac.PROTECTION.invoke(Opac.API.invoke(null,actor.server));
                if (bool(Opac.ENTITY_INTERACTION.invoke(protection,null,actor,target,actor.getMainHandItem(),InteractionHand.MAIN_HAND,false,false,true)))
                    return "Open Parties and Claims denies interaction with this resident.";
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            String key="resident:"+failure.getClass().getName()+":"+failure.getMessage();
            if (WARNED.add(key)) LogUtils.getLogger().warn("Claim entity adapter unavailable; resident transaction refused",failure);
            return "Installed claim entity API/version could not be verified; resident access paused. See the server log.";
        }
    }

    /** Permission events use the same actual player who authorized delegated construction. */
    public static Entity eventActor(ServerLevel level, UUID owner, Entity builder) {
        if (!installed()) return builder;
        ServerPlayer actor = level.getServer().getPlayerList().getPlayer(owner);
        if (actor == null || actor.serverLevel() != level) throw new IllegalStateException(denial());
        return actor;
    }

    private static void requireVersion(String id, String supported) {
        String version = ModList.get().getModContainerById(id).orElseThrow().getModInfo().getVersion().toString();
        if (!version.equals(supported)) throw new IllegalStateException("Unsupported " + id + " version " + version + "; verified " + supported);
    }
    private static boolean bool(Object value) {
        if (!(value instanceof Boolean answer)) throw new IllegalStateException("Claim API returned a non-boolean decision.");
        return answer;
    }
    @FunctionalInterface private interface Query { boolean denied(BlockPos pos) throws ReflectiveOperationException; }
    private record Check(String name, Query test) { }

    // Reflection keeps absent optional APIs off the core class path. Bind public exact-binary interfaces.
    private static final class Ftb {
        static final Method API, READY, MANAGER, DENIED;
        static final Object EDIT, INTERACT_ENTITY;
        static {
            try {
                Class<?> root = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
                Class<?> api = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
                Class<?> manager = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
                Class<?> protection = Class.forName("dev.ftb.mods.ftbchunks.api.Protection");
                API = root.getMethod("api"); READY = api.getMethod("isManagerLoaded"); MANAGER = api.getMethod("getManager");
                DENIED = manager.getMethod("shouldPreventInteraction", Entity.class, InteractionHand.class, BlockPos.class, protection, Entity.class);
                EDIT = protection.getField("EDIT_BLOCK").get(null);
                INTERACT_ENTITY = protection.getField("INTERACT_ENTITY").get(null);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
    private static final class Opac {
        static final Method API, PROTECTION, DENIED, ENTITY_INTERACTION;
        static {
            try {
                Class<?> api = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
                Class<?> protection = Class.forName("xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI");
                API = api.getMethod("get", MinecraftServer.class); PROTECTION = api.getMethod("getChunkProtection");
                DENIED = protection.getMethod("onEntityPlaceBlock", Entity.class, ServerLevel.class, BlockPos.class);
                ENTITY_INTERACTION = protection.getMethod("onEntityInteraction",Entity.class,Entity.class,Entity.class,net.minecraft.world.item.ItemStack.class,InteractionHand.class,boolean.class,boolean.class,boolean.class);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
    }
}
