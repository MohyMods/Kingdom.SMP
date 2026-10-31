package com.mohymods.kingdomsmp.building;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Noncraftable authenticated construction plan; copying an item copies no authority. */
public final class KingdomBlueprints {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(KingdomSmp.MOD_ID);
    public static final DeferredItem<Item> LODGE=ITEMS.register("founders_lodge_blueprint",()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){
            if(player instanceof ServerPlayer serverPlayer)ConstructionService.openPlan(serverPlayer,null);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        @Override public InteractionResult useOn(UseOnContext context){
            if(context.getPlayer() instanceof ServerPlayer player)ConstructionService.openPlan(player,context.getClickedPos().relative(context.getClickedFace()));
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
    });
    private KingdomBlueprints() { }
    public static void register(IEventBus bus){ITEMS.register(bus);}
}
