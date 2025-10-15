package net.countered.settlementroads.features.config.forge;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModConfiguredFeatures {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfiguredFeatures.class);
    
    public static final ResourceKey<ConfiguredFeature<?, ?>> ROAD_FEATURE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, 
            new ResourceLocation(SettlementRoads.MOD_ID, "road_feature")
    );
    
    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        LOGGER.info("Bootstrapping configured features for Forge...");
        
        // 人工道路材料（不同质量等级）
        List<List<BlockState>> artificialMaterials = List.of(
                List.of(Blocks.COBBLESTONE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.STONE.defaultBlockState(), Blocks.SMOOTH_STONE.defaultBlockState())
        );
        
        // 自然道路材料（不同质量等级）
        List<List<BlockState>> naturalMaterials = List.of(
                List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.GRAVEL.defaultBlockState()),
                List.of(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState())
        );
        
        // 道路宽度选项
        List<Integer> widths = List.of(3, 5, 7);
        
        // 道路质量等级
        List<Integer> qualities = List.of(1, 2, 3);
        
        context.register(ROAD_FEATURE, new ConfiguredFeature<>(
                RoadFeature.ROAD_FEATURE,
                new RoadFeatureConfig(artificialMaterials, naturalMaterials, widths, qualities)
        ));
        
        LOGGER.info("Configured features bootstrapped successfully.");
    }
}
