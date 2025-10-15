package net.countered.settlementroads.features.config;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.features.RoadFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadFeatureRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void registerFeatures() {
        LOGGER.info("Registering road_feature");
        Registry.register(BuiltInRegistries.FEATURE, new ResourceLocation(SettlementRoads.MOD_ID, "road_feature"), RoadFeature.ROAD_FEATURE);
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                RoadFeature.ROAD_FEATURE_PLACED_KEY
        );
    }
}
