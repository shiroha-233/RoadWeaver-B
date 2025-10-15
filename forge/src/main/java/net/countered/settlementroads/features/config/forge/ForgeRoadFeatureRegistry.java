package net.countered.settlementroads.features.config.forge;

import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge 平台的特性注册器
 * 使用 Forge 原生的 DeferredRegister 而不是 Architectury 的版本
 */
public final class ForgeRoadFeatureRegistry {
    private static final String MOD_ID = "roadweaver";

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MOD_ID);

    public static final RegistryObject<Feature<RoadFeatureConfig>> ROAD_FEATURE =
            FEATURES.register("road_feature", () -> RoadFeature.ROAD_FEATURE);

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }

    private ForgeRoadFeatureRegistry() {}
}
