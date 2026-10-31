package com.mohymods.kingdomsmp.npc;

import com.mohymods.kingdomsmp.building.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Mira Ashford: a meticulous, dryly humorous keeper of the community's record. */
public final class ReeveEntity extends Villager {
    private UUID building;
    private BlockPos home=BlockPos.ZERO;
    public ReeveEntity(EntityType<? extends Villager> type,Level level){
        super(type,level);setPersistenceRequired();setCanPickUpLoot(false);
        setVillagerData(new VillagerData(VillagerType.PLAINS,type==KingdomNpcs.STEWARD.get()?VillagerProfession.CARTOGRAPHER:VillagerProfession.LIBRARIAN,1));
        if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation)navigation.setCanOpenDoors(true);
    }
    public void configure(UUID building,BlockPos home,String name){
        this.building=building;this.home=home.immutable();restrictTo(home,4);setCustomName(Component.literal(name));setCustomNameVisible(true);
    }
    public UUID buildingId(){return building;}
    public BlockPos home(){return home;}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new OpenDoorGoal(this,true));goalSelector.addGoal(2,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(3,new RandomLookAroundGoal(this));}
    @Override protected void customServerAiStep(){
        if(tickCount%100!=0||!(level() instanceof ServerLevel level))return;
        var p=ConstructionService.lodge(level.getServer());
        if(p==null||!ConstructionService.registeredReeve(this)||!ConstructionService.structureIntact(level,p)){getNavigation().stop();return;}
        var b=BlueprintRegistry.get(p.blueprint(),p.blueprintVersion());
        BlockPos target=level.isNight()&&!ConstructionService.hall(p)?p.origin().offset(b.rotate(new BlockPos(3,1,5),p.rotation())):home;
        if(!level.hasChunkAt(target)||!level.hasChunkAt(blockPosition())){getNavigation().stop();return;}
        if(distanceToSqr(target.getX()+0.5,target.getY(),target.getZ()+0.5)<1){getNavigation().stop();return;}
        var path=getNavigation().createPath(target,0);
        if(path!=null&&path.canReach())getNavigation().moveTo(path,0.45);else getNavigation().stop();
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(hand==InteractionHand.MAIN_HAND&&player instanceof ServerPlayer server)ConstructionService.openReeve(server,this);
        return InteractionResult.CONSUME;
    }
    @Override public boolean canBreed(){return false;}
    @Override public boolean canTakeItem(ItemStack stack){return false;}
    @Override protected void pickUpItem(ItemEntity item){ }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean canBeLeashed(){return false;}
    @Override public void die(DamageSource source){if(!level().isClientSide)ConstructionService.reeveDied(this);super.die(source);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(building!=null)tag.putUUID("KingdomBuilding",building);tag.putLong("KingdomHome",home.asLong());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);building=tag.hasUUID("KingdomBuilding")?tag.getUUID("KingdomBuilding"):null;home=BlockPos.of(tag.getLong("KingdomHome"));restrictTo(home,4);setPersistenceRequired();setCanPickUpLoot(false);}
}
