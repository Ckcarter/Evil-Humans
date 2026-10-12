package com.spaz97.humanmob;

import com.spaz97.humanmob.entity.Human;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(HumanMobMod.MODID)
public class HumanMobMod {
    public static final String MODID = "humanmobmod";

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<Human>> HUMAN = ENTITIES.register("human",
            () -> EntityType.Builder.of(Human::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8)
                    .build(new ResourceLocation(MODID, "human").toString()));

    // Same egg colours as the original (black / white).
    public static final RegistryObject<Item> HUMAN_SPAWN_EGG = ITEMS.register("human_spawn_egg",
            () -> new ForgeSpawnEggItem(HUMAN, 0x000000, 0xFFFFFF, new Item.Properties()));

    public HumanMobMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTabs);
        modBus.addListener(this::onSpawnPlacements);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HumanMobConfig.SPEC);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HUMAN.get(), Human.createAttributes().build());
    }

    private void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(HUMAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Human::checkHumanSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(HUMAN_SPAWN_EGG.get());
        }
    }
}
