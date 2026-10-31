package com.mohymods.kingdomsmp.npc;

import com.mohymods.kingdomsmp.KingdomSmp;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** No spawn egg or natural spawn: the registered hut owns the single builder. */
public final class KingdomNpcs {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(
            Registries.ENTITY_TYPE, KingdomSmp.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<AldricEntity>> ALDRIC = TYPES.register(
            "aldric", () -> EntityType.Builder.of(AldricEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).noSummon()
                    .build(KingdomSmp.MOD_ID + ":aldric"));
    public static final DeferredHolder<EntityType<?>, EntityType<ReeveEntity>> REEVE = TYPES.register(
            "reeve", () -> EntityType.Builder.of(ReeveEntity::new, MobCategory.MISC)
                    .sized(0.6F,1.95F).clientTrackingRange(8).noSummon().build(KingdomSmp.MOD_ID+":reeve"));

    private KingdomNpcs() {}

    public static final DeferredHolder<EntityType<?>, EntityType<ReeveEntity>> STEWARD=TYPES.register("steward",()->EntityType.Builder.of(ReeveEntity::new,MobCategory.MISC).sized(0.6F,1.95F).clientTrackingRange(8).noSummon().build(KingdomSmp.MOD_ID+":steward"));
    public static final DeferredHolder<EntityType<?>, EntityType<ReeveEntity>> RESIDENT=TYPES.register("resident",()->EntityType.Builder.of(ReeveEntity::new,MobCategory.MISC).sized(0.6F,1.95F).clientTrackingRange(8).noSummon().build(KingdomSmp.MOD_ID+":resident"));
    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(KingdomNpcs::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ALDRIC.get(), Villager.createAttributes().build());
        event.put(REEVE.get(), Villager.createAttributes().build());
        event.put(STEWARD.get(), Villager.createAttributes().build());
        event.put(RESIDENT.get(), Villager.createAttributes().build());
    }
}
