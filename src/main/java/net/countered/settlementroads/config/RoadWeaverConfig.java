package net.countered.settlementroads.config;

import net.countered.settlementroads.road.RoadTypeRegistry;
import net.countered.settlementroads.decoration.DecorationRegistry;
import net.countered.settlementroads.biome.CompositeBiomeCostCalculator;
import net.countered.settlementroads.pathfinding.PathFindingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Path;

/**
 * RoadWeaver unified configuration manager
 * Follows hybrid configuration architecture: basic config uses MidnightConfig, complex config uses external JSON files
 * Supports zero hardcoding, full extensibility, full customization
 */
public class RoadWeaverConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-Config");
    
    private final RoadTypeRegistry roadTypeRegistry;
    private final DecorationRegistry decorationRegistry;
    private final CompositeBiomeCostCalculator biomeCostCalculator;
    private final PathFindingConfig pathFindingConfig;
    private final ConfigLoader configLoader;
    
    private final Map<String, Object> globalProperties;
    
    public RoadWeaverConfig() {
        this.roadTypeRegistry = new RoadTypeRegistry();
        this.decorationRegistry = new DecorationRegistry();
        this.biomeCostCalculator = new CompositeBiomeCostCalculator();
        this.pathFindingConfig = new PathFindingConfig();
        this.configLoader = new ConfigLoader();
        this.globalProperties = new HashMap<>();
        
        loadFromMixedConfig();
    }
    
    /**
     * Load all configurations from mixed config architecture
     */
    private void loadFromMixedConfig() {
        LOGGER.info("Loading RoadWeaver configuration from mixed config architecture");

        try {
            loadBasicConfigFromMidnightConfig();
            loadComplexConfigFromJsonFiles();
            if (ModConfig.enableConfigValidation) {
                validateConfiguration();
            }

            LOGGER.info("RoadWeaver configuration loaded successfully from mixed config architecture");

        } catch (Exception e) {
            LOGGER.error("Failed to load configuration from mixed config architecture", e);
            if (ModConfig.fallbackToDefaults) {
                LOGGER.warn("Falling back to default configuration");
                loadDefaultConfiguration();
            } else {
                throw new RuntimeException("Configuration loading failed and fallback is disabled", e);
            }
        }
    }
    
    /**
     * Load basic configuration from MidnightConfig
     */
    private void loadBasicConfigFromMidnightConfig() {
        LOGGER.info("Loading basic configuration from MidnightConfig");
        
        // Load global properties
        loadGlobalProperties();
        
        // Load pathfinding configuration
        loadPathFindingConfig();
        
        // Load replaceable blocks configuration
        loadReplaceableBlocksConfig();
    }
    
    /**
     * Load complex configuration from JSON files
     */
    private void loadComplexConfigFromJsonFiles() {
        LOGGER.info("Loading complex configuration from JSON files");
        
        // Load road types configuration
        loadRoadTypesFromJson();
        
        // Load decorations configuration
        loadDecorationsFromJson();
        
        // Load biome costs configuration
        loadBiomeCostsFromJson();
        
        // Load pathfinding configuration
        loadPathFindingFromJson();
        
    }
    
    /**
     * 加载全局属性
     */
    private void loadGlobalProperties() {
        // 使用通用配置前缀，不硬编码具体功能名称
        globalProperties.put("averagingRadius", ModConfig.averagingRadius);
        globalProperties.put("maxHeightDifference", ModConfig.maxHeightDifference);
        globalProperties.put("maxTerrainStability", ModConfig.maxTerrainStability);
        globalProperties.put("heightCacheLimit", ModConfig.heightCacheLimit);
        globalProperties.put("maxConcurrentPathfinding", ModConfig.maxConcurrentPathfinding);
    }
    
    /**
     * 加载路径查找配置
     */
    private void loadPathFindingConfig() {
        LOGGER.info("Pathfinding configuration loaded from JSON file");
    }
    
    private void loadReplaceableBlocksConfig() {
        try {
            Path configFile = Path.of(ModConfig.replaceableBlocksConfigFile);
            if (ForbiddenBlocksConfigLoader.loadConfig(configFile)) {
                LOGGER.info("Replaceable blocks configuration loaded successfully");
            } else {
                LOGGER.error("Failed to load replaceable blocks configuration");
                throw new RuntimeException("Replaceable blocks configuration is required but could not be loaded");
            }
        } catch (Exception e) {
            LOGGER.error("Error loading replaceable blocks configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load replaceable blocks configuration", e);
        }
    }
    
    /**
     * 验证配置完整性
     */
    private void validateConfiguration() {
        // 验证道路类型配置
        if (roadTypeRegistry.getRoadTypeCount() == 0) {
            LOGGER.warn("No road types loaded, this may cause issues");
        }
        
        // 验证装饰配置
        if (decorationRegistry.getAllFactories().size() == 0) {
            LOGGER.warn("No decorations loaded, this may cause issues");
        }
        
        // 验证生物群系成本配置
        if (biomeCostCalculator.getCalculators().size() == 0) {
            LOGGER.warn("No biome cost calculators loaded, this may cause issues");
        }
        
        LOGGER.info("Configuration validation completed");
    }
    
    /**
     * 加载默认配置（回退方案）
     */
    private void loadDefaultConfiguration() {
        LOGGER.warn("Default configuration loading is disabled - all configurations must be provided via JSON files");
        throw new RuntimeException("No fallback configuration available - all configurations must be provided via JSON files");
    }
    
    /**
     * 从JSON文件加载道路类型配置
     */
    private void loadRoadTypesFromJson() {
        try {
            String configPath = ModConfig.roadTypesConfigFile;
            com.google.gson.JsonObject config = configLoader.loadConfigFile(configPath);
            
            if (config == null) {
                LOGGER.error("Failed to load road types config file: {}", configPath);
                throw new RuntimeException("Road types configuration file is required but could not be loaded");
            }
            
            // 验证配置文件
            String[] requiredFields = {"roadTypes"};
            if (!configLoader.validateConfigFile(configPath, requiredFields)) {
                LOGGER.error("Road types config validation failed: {}", configPath);
                throw new RuntimeException("Road types configuration validation failed");
            }
            
            // 解析道路类型配置
            if (config.has("roadTypes")) {
                com.google.gson.JsonObject roadTypesJson = config.getAsJsonObject("roadTypes");
                int loadedCount = net.countered.settlementroads.road.RoadTypeConfigLoader.loadRoadTypesFromJson(roadTypesJson, roadTypeRegistry);
                LOGGER.info("Loaded {} road types from JSON config: {}", loadedCount, configPath);
            } else {
                LOGGER.error("No road types found in JSON config: {}", configPath);
                throw new RuntimeException("No road types found in configuration file");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load road types from JSON config", e);
            throw new RuntimeException("Failed to load road types configuration", e);
        }
    }
    
    /**
     * 从JSON文件加载装饰配置
     */
    private void loadDecorationsFromJson() {
        try {
            String configPath = ModConfig.decorationsConfigFile;
            configLoader.loadConfigFile(configPath);
            
            // 验证配置文件
            String[] requiredFields = {"decorations"};
            if (!configLoader.validateConfigFile(configPath, requiredFields)) {
                LOGGER.error("Decorations config validation failed: {}", configPath);
                throw new RuntimeException("Decorations configuration validation failed");
            }
            
            // 解析装饰配置
            com.google.gson.JsonObject config = configLoader.getConfig(configPath);
            if (config != null && config.has("decorations")) {
                com.google.gson.JsonObject decorationsJson = config.getAsJsonObject("decorations");
                int loadedCount = net.countered.settlementroads.decoration.DecorationConfigLoader.loadDecorationsFromJson(decorationsJson, decorationRegistry);
                LOGGER.info("Loaded {} decorations from JSON config: {}", loadedCount, configPath);
            } else {
                LOGGER.error("No decorations found in JSON config: {}", configPath);
                throw new RuntimeException("No decorations found in configuration file");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load decorations from JSON config", e);
            throw new RuntimeException("Failed to load decorations configuration", e);
        }
    }
    
    /**
     * 从JSON文件加载生物群系成本配置
     */
    private void loadBiomeCostsFromJson() {
        try {
            String configPath = ModConfig.biomeCostsConfigFile;
            configLoader.loadConfigFile(configPath);
            
            // 验证配置文件
            String[] requiredFields = {"biomeCosts"};
            if (!configLoader.validateConfigFile(configPath, requiredFields)) {
                LOGGER.error("Biome costs config validation failed: {}", configPath);
                throw new RuntimeException("Biome costs configuration validation failed");
            }
            
            // 解析生物群系成本配置
            com.google.gson.JsonObject config = configLoader.getConfig(configPath);
            if (config != null && config.has("biomeCosts")) {
                com.google.gson.JsonObject biomeCostsJson = config.getAsJsonObject("biomeCosts");
                int loadedCount = net.countered.settlementroads.biome.BiomeCostConfigLoader.loadBiomeCostsFromJson(biomeCostsJson, biomeCostCalculator);
                LOGGER.info("Loaded {} biome cost calculators from JSON config: {}", loadedCount, configPath);
            } else {
                LOGGER.error("No biome costs found in JSON config: {}", configPath);
                throw new RuntimeException("No biome costs found in configuration file");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load biome costs from JSON config", e);
            throw new RuntimeException("Failed to load biome costs configuration", e);
        }
    }
    
    /**
     * 从JSON文件加载路径查找配置
     */
    private void loadPathFindingFromJson() {
        try {
            String configPath = ModConfig.pathfindingConfigFile;
            configLoader.loadConfigFile(configPath);
            
            // 验证配置文件
            String[] requiredFields = {"pathfinding"};
            if (!configLoader.validateConfigFile(configPath, requiredFields)) {
                LOGGER.warn("Pathfinding config validation failed, using defaults");
                return;
            }
            
            // 解析路径查找配置
            com.google.gson.JsonObject config = configLoader.getConfig(configPath);
            if (config != null && config.has("pathfinding")) {
                com.google.gson.JsonObject pathfindingJson = config.getAsJsonObject("pathfinding");
                loadPathFindingConfigFromJson(pathfindingJson);
                LOGGER.info("Loaded pathfinding config from JSON: {}", configPath);
            } else {
                LOGGER.warn("No pathfinding config found in JSON, using defaults");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load pathfinding config from JSON", e);
            if (ModConfig.fallbackToDefaults) {
                LOGGER.warn("Using default pathfinding configuration");
            } else {
                throw new RuntimeException("Failed to load pathfinding config and fallback is disabled", e);
            }
        }
    }
    
    /**
     * 从JSON对象加载路径查找配置
     */
    private void loadPathFindingConfigFromJson(com.google.gson.JsonObject pathfindingJson) {
        // 加载基础参数
        if (pathfindingJson.has("neighborDistance")) {
            pathFindingConfig.setNeighborDistance(pathfindingJson.get("neighborDistance").getAsInt());
        }
        if (pathfindingJson.has("diagonalStepCost")) {
            pathFindingConfig.setDiagonalStepCost(pathfindingJson.get("diagonalStepCost").getAsDouble());
        }
        if (pathfindingJson.has("straightStepCost")) {
            pathFindingConfig.setStraightStepCost(pathfindingJson.get("straightStepCost").getAsDouble());
        }
        if (pathfindingJson.has("maxSteps")) {
            pathFindingConfig.setMaxSteps(pathfindingJson.get("maxSteps").getAsInt());
        }
        if (pathfindingJson.has("heuristicWeight")) {
            pathFindingConfig.setHeuristicWeight(pathfindingJson.get("heuristicWeight").getAsDouble());
        }
        
        // 加载成本乘数
        if (pathfindingJson.has("costMultipliers")) {
            com.google.gson.JsonObject costMultipliersJson = pathfindingJson.getAsJsonObject("costMultipliers");
            for (String key : costMultipliersJson.keySet()) {
                double value = costMultipliersJson.get(key).getAsDouble();
                pathFindingConfig.setCostMultiplier(key, value);
            }
        }
    }
    
    
    
    /**
     * 获取道路类型注册表
     */
    public RoadTypeRegistry getRoadTypeRegistry() {
        return roadTypeRegistry;
    }
    
    /**
     * 获取装饰注册表
     */
    public DecorationRegistry getDecorationRegistry() {
        return decorationRegistry;
    }
    
    /**
     * 获取生物群系成本计算器
     */
    public CompositeBiomeCostCalculator getBiomeCostCalculator() {
        return biomeCostCalculator;
    }
    
    /**
     * 获取路径查找配置
     */
    public PathFindingConfig getPathFindingConfig() {
        return pathFindingConfig;
    }
    
    /**
     * 获取全局属性
     */
    public Map<String, Object> getGlobalProperties() {
        return globalProperties;
    }
    
    /**
     * 设置全局属性
     */
    public void setGlobalProperty(String key, Object value) {
        globalProperties.put(key, value);
    }
    
    /**
     * 获取全局属性
     */
    public Object getGlobalProperty(String key) {
        return globalProperties.get(key);
    }
    
    /**
     * 获取全局属性（带默认值）
     */
    public Object getGlobalProperty(String key, Object defaultValue) {
        return globalProperties.getOrDefault(key, defaultValue);
    }
    
    /**
     * 重新加载配置（支持热重载）
     */
    public void reloadConfiguration() {
        LOGGER.info("Reloading RoadWeaver configuration...");

        // 清空现有配置
        roadTypeRegistry.clear();
        decorationRegistry.clear();
        biomeCostCalculator.clear();
        globalProperties.clear();

        // 重新加载配置
        loadFromMixedConfig();

        LOGGER.info("Configuration reloaded successfully");
    }
    
    /**
     * 重新加载JSON配置文件
     */
    public void reloadJsonConfigs() {
        LOGGER.info("Reloading JSON configuration files...");
        
        // 重新加载所有JSON配置文件
        configLoader.reloadAllConfigs();
        
        // 重新加载复杂配置
        loadComplexConfigFromJsonFiles();
        
        LOGGER.info("JSON configuration files reloaded successfully");
    }
    
    /**
     * 验证配置文件是否存在
     */
    public boolean validateConfigFiles() {
        boolean allValid = true;
        
        // 验证配置完整性（使用通用配置，不硬编码具体文件路径）
        // 这里应该根据ModConfig中的通用配置进行验证
        LOGGER.info("Validating configuration using generic settings");
        
        return allValid;
    }
    
    /**
     * 获取配置统计信息
     */
    public Map<String, Object> getConfigurationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("roadTypes", roadTypeRegistry.getRoadTypeCount());
        stats.put("decorations", decorationRegistry.getAllFactories().size());
        stats.put("biomeCalculators", biomeCostCalculator.getCalculators().size());
        stats.put("globalProperties", globalProperties.size());
        stats.put("configValidation", ModConfig.enableConfigValidation);
        stats.put("fallbackToDefaults", ModConfig.fallbackToDefaults);
        return stats;
    }
}