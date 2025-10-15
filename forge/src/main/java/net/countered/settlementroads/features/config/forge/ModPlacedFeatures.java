package net.countered.settlementroads.features.config.forge;

import net.countered.settlementroads.SettlementRoads;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ModPlacedFeatures {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModPlacedFeatures.class);
    
    public static final ResourceKey<PlacedFeature> ROAD_FEATURE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, 
            new ResourceLocation(SettlementRoads.MOD_ID, "road_feature_placed")
    );
    
    public static void bootstrap(BootstapContext<PlacedFeature> context) {
        LOGGER.info("Bootstrapping placed features for Forge...");
        
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        
        Holder<ConfiguredFeature<?, ?>> roadFeature = configuredFeatures.getOrThrow(ModConfiguredFeatures.ROAD_FEATURE);
        
        List<PlacementModifier> roadPlacements = List.of(
                HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG)
        );
        
        context.register(ROAD_FEATURE_PLACED, new PlacedFeature(roadFeature, roadPlacements));
        
        LOGGER.info("Placed features bootstrapped successfully.");
    }
}
