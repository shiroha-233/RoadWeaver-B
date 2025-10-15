package net.countered.settlementroads.features.config;

import net.countered.settlementroads.features.RoadFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class FabricBiomeInjection {
    private FabricBiomeInjection() {}

    public static void inject() {
        BiomeModifications.addFeature(
                BiomeSelectors.all(),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS,
                RoadFeature.ROAD_FEATURE_PLACED_KEY
        );
    }
}
