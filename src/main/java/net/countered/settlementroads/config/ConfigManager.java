package net.countered.settlementroads.config;

import net.countered.settlementroads.road.RoadTypeRegistry;
import net.countered.settlementroads.decoration.DecorationRegistry;
import net.countered.settlementroads.biome.CompositeBiomeCostCalculator;
import net.countered.settlementroads.pathfinding.PathFindingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global configuration manager singleton
 * Provides unified entry point for global access to configuration system
 */
public class ConfigManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-ConfigManager");
    
    private static ConfigManager instance;
    private RoadWeaverConfig config;
    
    private ConfigManager() {
        // Private constructor to prevent external instantiation
    }
    
    /**
     * Get configuration manager singleton
     * @return Configuration manager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
            instance.initialize();
        }
        return instance;
    }
    
    /**
     * Initialize configuration manager
     */
    private void initialize() {
        try {
            config = new RoadWeaverConfig();
            LOGGER.info("ConfigManager initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ConfigManager", e);
            throw new RuntimeException("Failed to initialize ConfigManager", e);
        }
    }
    
    /**
     * Get road type registry
     * @return Road type registry
     */
    public RoadTypeRegistry getRoadTypeRegistry() {
        return config.getRoadTypeRegistry();
    }
    
    /**
     * Get decoration registry
     * @return Decoration registry
     */
    public DecorationRegistry getDecorationRegistry() {
        return config.getDecorationRegistry();
    }
    
    /**
     * Get biome cost calculator
     * @return Biome cost calculator
     */
    public CompositeBiomeCostCalculator getBiomeCostCalculator() {
        return config.getBiomeCostCalculator();
    }
    
    /**
     * Get pathfinding configuration
     * @return Pathfinding configuration
     */
    public PathFindingConfig getPathFindingConfig() {
        return config.getPathFindingConfig();
    }
    
    /**
     * Reload configuration
     */
    public void reloadConfig() {
        try {
            config.reloadConfiguration();
            LOGGER.info("Configuration reloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to reload configuration", e);
        }
    }
    
    /**
     * Reload JSON configuration files
     */
    public void reloadJsonConfigs() {
        try {
            config.reloadJsonConfigs();
            LOGGER.info("JSON configuration files reloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to reload JSON configuration files", e);
        }
    }
    
    /**
     * Get configuration statistics
     * @return Configuration statistics
     */
    public java.util.Map<String, Object> getConfigStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("roadTypes", config.getRoadTypeRegistry().getAllRoadTypes().size());
        stats.put("decorations", config.getDecorationRegistry().getAllFactories().size());
        stats.put("biomeCalculators", config.getBiomeCostCalculator().getCalculators().size());
        return stats;
    }
}
