package net.countered.settlementroads.features.config;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ConfigManager;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.road.RoadTypeConfig;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registerable;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class ModConfiguredFeatures {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    public static void bootstrap(Registerable<ConfiguredFeature<?,?>> context){
        LOGGER.info("Bootstrap ConfiguredFeature");
        
        // 获取所有启用的道路类型
        List<RoadTypeConfig> enabledRoadTypes = getEnabledRoadTypes();
        
        if (enabledRoadTypes.isEmpty()) {
            LOGGER.error("No enabled road types found in configuration! Road generation will fail.");
            throw new RuntimeException("No enabled road types found in configuration. Please check your road_types.json configuration.");
        }
        
        // 从启用的道路类型中获取材料
        List<List<BlockState>> allMaterials = new ArrayList<>();
        for (RoadTypeConfig roadType : enabledRoadTypes) {
            allMaterials.addAll(roadType.getMaterials());
        }
        
        if (allMaterials.isEmpty()) {
            LOGGER.error("No materials found in enabled road types!");
            throw new RuntimeException("No materials found in enabled road types. Please check your road_types.json configuration.");
        }
        
        // 使用新的配置驱动构造函数
        context.register(RoadFeature.ROAD_FEATURE_KEY,
                new ConfiguredFeature<>(RoadFeature.ROAD_FEATURE,
                        new RoadFeatureConfig(
                                List.of(3), // width
                                List.of(1,2,3,4,5,6,7,8,9) // quality (not used)
                        )
                )
        );
    }
    
    /**
     * 获取所有启用的道路类型
     */
    private static List<RoadTypeConfig> getEnabledRoadTypes() {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            return configManager.getRoadTypeRegistry().getEnabledRoadTypes();
        } catch (Exception e) {
            LOGGER.error("Failed to get enabled road types from config: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
}