package com.mohymods.kingdomsmp.npc;

import com.mohymods.kingdomsmp.building.BuilderHutService;
import com.mohymods.kingdomsmp.persistence.BuilderHutSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

import java.util.UUID;

/** A named hut resident with a small, safe routine, never a vanilla merchant. */
public final class AldricEntity extends Villager {
    private UUID hutId;
    private BlockPos home = BlockPos.ZERO;
    private String routineStatus = "At home in the Builder's Hut.";

    public AldricEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.MASON, 1));
    }

    public void configure(UUID buildingId, BlockPos home, String name) {
        this.hutId = buildingId;
        this.home = home.immutable();
        restrictTo(this.home, 4);
        setCustomName(Component.literal(name));
        setCustomNameVisible(true);
    }

    public UUID hutId() { return hutId; }
    public BlockPos home() { return home; }
    public String routineStatus() { return routineStatus; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        // Deliberately replace the vanilla trading/POI brain. The tiny routine
        // paths only through already loaded hut context and never teleports.
        if (tickCount % 100 != 0 || !(level() instanceof ServerLevel level)) return;
        if (!BuilderHutService.isRegistered(this)) {
            getNavigation().stop();
            routineStatus = "Home association unavailable; routine paused.";
            return;
        }
        var hut = BuilderHutSavedData.get(level.getServer()).hut().orElseThrow();
        setCustomName(Component.literal(hut.displayName()));
        if (!BuilderHutService.structureIntact(level, hut)) {
            getNavigation().stop();
            routineStatus = "Hut damaged or unloaded; routine paused.";
            return;
        }
        BlockPos target = level.isNight() ? home : home.offset(0, 0, -1);
        if (!level.hasChunkAt(target) || !level.hasChunkAt(blockPosition())) {
            getNavigation().stop();
            routineStatus = "Home chunk unavailable; routine paused.";
            return;
        }
        if (distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) < 1.0) {
            getNavigation().stop();
            routineStatus = level.isNight() ? "Resting at the hut; no construction assigned."
                    : "At the workbench; no construction assigned.";
            return;
        }
        Path path = getNavigation().createPath(target, 0);
        if (path == null || !path.canReach()) {
            getNavigation().stop();
            routineStatus = "Path to home is blocked; routine paused.";
        } else {
            getNavigation().moveTo(path, 0.5);
            routineStatus = level.isNight() ? "Returning to the hut for the night."
                    : "Walking to the work area.";
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            BuilderHutService.openDialogue(serverPlayer, this);
        }
        return InteractionResult.CONSUME;
    }

    @Override public boolean canBreed() { return false; }
    @Override public boolean canTakeItem(ItemStack stack) { return false; }
    @Override protected void pickUpItem(ItemEntity item) { }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeLeashed() { return false; }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) BuilderHutService.npcDied(this);
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hutId != null) tag.putUUID("KingdomHut", hutId);
        tag.putLong("KingdomHome", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        hutId = tag.hasUUID("KingdomHut") ? tag.getUUID("KingdomHut") : null;
        home = BlockPos.of(tag.getLong("KingdomHome"));
        restrictTo(home, 4);
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }
}
