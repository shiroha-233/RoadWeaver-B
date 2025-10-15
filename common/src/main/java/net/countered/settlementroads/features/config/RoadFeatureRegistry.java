package net.countered.settlementroads.features.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.countered.settlementroads.features.RoadFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class RoadFeatureRegistry {
    private static final String MOD_ID = "roadweaver";

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(MOD_ID, Registries.FEATURE);

    public static final RegistrySupplier<Feature<RoadFeatureConfig>> ROAD_FEATURE =
            FEATURES.register("road_feature", () -> RoadFeature.ROAD_FEATURE);

    public static void registerFeatures() {
        FEATURES.register();
    }

    private RoadFeatureRegistry() {}
}
