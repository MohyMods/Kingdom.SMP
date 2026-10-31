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
    public static final DeferredItem<Item> FARMER=register("farmer_blueprint");
    public static final DeferredItem<Item> BUILDER=register("builder_blueprint");
    public static final DeferredItem<Item> LUMBERJACK=register("lumberjack_blueprint");
    public static final DeferredItem<Item> BANK=register("royal_bank_blueprint");
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
    public static Item item(net.minecraft.resources.ResourceLocation id){
        if(id.equals(BlueprintRegistry.HUT))return HUT.get();
        if(id.equals(TownHallBlueprint.ID))return HALL.get();
        if(id.equals(FoundersLodgeBlueprint.ID))return LODGE.get();
        if(id.equals(ProfessionBlueprints.FARMER))return FARMER.get();
        if(id.equals(ProfessionBlueprints.BUILDER))return BUILDER.get();
        if(id.equals(ProfessionBlueprints.LUMBERJACK))return LUMBERJACK.get();
        if(id.equals(RoyalBankBlueprint.ID))return BANK.get();
        throw new IllegalArgumentException("Unsupported blueprint item");
    }
    public static net.minecraft.resources.ResourceLocation definition(ItemStack stack){
        if(stack.is(HUT.get()))return BlueprintRegistry.HUT;
        if(stack.is(HALL.get()))return TownHallBlueprint.ID;
        if(stack.is(LODGE.get()))return FoundersLodgeBlueprint.ID;
        if(stack.is(FARMER.get()))return ProfessionBlueprints.FARMER;
        if(stack.is(BUILDER.get()))return ProfessionBlueprints.BUILDER;
        if(stack.is(LUMBERJACK.get()))return ProfessionBlueprints.LUMBERJACK;
        return stack.is(BANK.get())?RoyalBankBlueprint.ID:null;
    }
    private KingdomBlueprints() { }
    public static void register(IEventBus bus){ITEMS.register(bus);}
}
