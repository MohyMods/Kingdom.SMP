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
    public static final DeferredItem<Item> LODGE=register("founders_lodge_blueprint");
    public static final DeferredItem<Item> HUT=register("builders_hut_blueprint");
    public static final DeferredItem<Item> HALL=register("town_hall_blueprint");
    private static DeferredItem<Item> register(String id){return ITEMS.register(id,()->new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){
            if(player instanceof ServerPlayer p)ConstructionService.openPlan(p,null);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
        @Override public InteractionResult useOn(UseOnContext context){
            if(context.getPlayer() instanceof ServerPlayer p)ConstructionService.openPlan(p,context.getClickedPos().relative(context.getClickedFace()));
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
    });}
    public static Item item(net.minecraft.resources.ResourceLocation id){return id.equals(BlueprintRegistry.HUT)?HUT.get():id.equals(TownHallBlueprint.ID)?HALL.get():LODGE.get();}
    public static net.minecraft.resources.ResourceLocation definition(ItemStack stack){return stack.is(HUT.get())?BlueprintRegistry.HUT:stack.is(HALL.get())?TownHallBlueprint.ID:stack.is(LODGE.get())?FoundersLodgeBlueprint.ID:null;}
    private KingdomBlueprints() { }
    public static void register(IEventBus bus){ITEMS.register(bus);}
}
