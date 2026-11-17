package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Locale;

/** Registered, noncraftable denominations. Client data and display names carry no monetary authority. */
public final class CurrencyItems {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(KingdomSmp.MOD_ID);
    public static final DeferredItem<Item> COPPER=ITEMS.registerSimpleItem("copper_coin",new Item.Properties());
    public static final DeferredItem<Item> SILVER=ITEMS.registerSimpleItem("silver_coin",new Item.Properties());
    public static final DeferredItem<Item> GOLD=ITEMS.registerSimpleItem("gold_coin",new Item.Properties());
    public static final DeferredItem<Item> PLATINUM=ITEMS.registerSimpleItem("platinum_coin",new Item.Properties());
    public enum Denomination {
        COPPER(1),SILVER(10),GOLD(100),PLATINUM(1000);
        private final int value;
        Denomination(int value){this.value=value;}
        public int value(){return value;}
        public String id(){return name().toLowerCase(Locale.ROOT);}
        public String title(){return name().charAt(0)+name().substring(1).toLowerCase(Locale.ROOT)+" coin";}
        public Item item(){return switch(this){case COPPER->CurrencyItems.COPPER.get();case SILVER->CurrencyItems.SILVER.get();case GOLD->CurrencyItems.GOLD.get();case PLATINUM->CurrencyItems.PLATINUM.get();};}
    }
    private CurrencyItems() { }
    public static void register(IEventBus bus){ITEMS.register(bus);}
    public static Denomination denomination(String id){for(var d:Denomination.values())if(d.id().equals(id))return d;throw new IllegalArgumentException("Unknown coin denomination");}
    public static Denomination denomination(ItemStack stack){
        if(stack.isEmpty())return null;
        for(var d:Denomination.values())if(ItemStack.isSameItemSameComponents(stack,new ItemStack(d.item())))return d;
        return null;
    }
    public static long value(ItemStack stack){var d=denomination(stack);return d==null?0:Math.multiplyExact((long)stack.getCount(),d.value());}
}
