package net.countered.settlementroads.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

/**
 * Configuration loader - supports JSON file loading and validation
 * Follows hybrid configuration architecture: basic config uses MidnightConfig, complex config uses external JSON files
 */
public class ConfigLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-ConfigLoader");
    // Gson instance for JSON serialization and deserialization
    // private static final Gson GSON = new GsonBuilder()
    //         .setPrettyPrinting()
    //         .create();
    
    private final Map<String, JsonObject> loadedConfigs = new HashMap<>();
    
    /**
     * Load configuration file
     * @param configPath Configuration file path
     * @return Loaded JSON object, null if loading fails
     */
    public JsonObject loadConfigFile(String configPath) {
        LOGGER.info("Attempting to load config file: {}", configPath);
        try {
            Path path = Paths.get(configPath);
            LOGGER.info("Config file path resolved to: {}", path.toAbsolutePath());
            
            if (!Files.exists(path)) {
                LOGGER.warn("Config file does not exist: {}, attempting to create default", configPath);
                // Try to create default configuration file
                if (createDefaultConfigFile(configPath)) {
                    LOGGER.info("Default config file created successfully: {}", configPath);
                } else {
                    LOGGER.error("Failed to create default config file: {}", configPath);
                    return null;
                }
            } else {
                LOGGER.info("Config file already exists: {}", configPath);
            }
            
            // Check if file exists again
            if (!Files.exists(path)) {
                LOGGER.error("Config file still does not exist after creation attempt: {}", configPath);
                return null;
            }
            
            String content = Files.readString(path);
            LOGGER.info("Config file content length: {} characters", content.length());
            
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();
            
            loadedConfigs.put(configPath, config);
            LOGGER.info("Successfully loaded and parsed config file: {}", configPath);
            
            return config;
            
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: {}", configPath, e);
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to parse config file: {}", configPath, e);
            return null;
        }
    }
    
    /**
     * Validate configuration file
     * @param configPath Configuration file path
     * @param requiredFields Required fields list
     * @return Whether validation passed
     */
    public boolean validateConfigFile(String configPath, String[] requiredFields) {
        JsonObject config = loadedConfigs.get(configPath);
        if (config == null) {
            LOGGER.error("Config not loaded: {}", configPath);
            return false;
        }
        
        for (String field : requiredFields) {
            if (!config.has(field)) {
                LOGGER.error("Required field '{}' missing in config: {}", field, configPath);
                return false;
            }
        }
        
        LOGGER.info("Config validation passed: {}", configPath);
        return true;
    }
    
    /**
     * Get loaded configuration
     * @param configPath Configuration file path
     * @return Configuration JSON object
     */
    public JsonObject getConfig(String configPath) {
        return loadedConfigs.get(configPath);
    }
    
    /**
     * Reload configuration file
     * @param configPath Configuration file path
     * @return Whether reload was successful
     */
    public boolean reloadConfigFile(String configPath) {
        loadedConfigs.remove(configPath);
        return loadConfigFile(configPath) != null;
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadAllConfigs() {
        LOGGER.info("Reloading all config files...");
        for (String configPath : loadedConfigs.keySet()) {
            reloadConfigFile(configPath);
        }
    }
    
    /**
     * Get configuration statistics
     * @return Configuration statistics
     */
    public Map<String, Object> getConfigStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("loadedConfigs", loadedConfigs.size());
        stats.put("configPaths", loadedConfigs.keySet());
        return stats;
    }
    
    /**
     * Create default configuration file
     * @param configPath Configuration file path
     * @return Whether creation was successful
     */
    private boolean createDefaultConfigFile(String configPath) {
        LOGGER.info("Creating default config file: {}", configPath);
        Path path = Paths.get(configPath);
        Path parentDir = path.getParent();
        
        // Create directory
        if (parentDir != null && !Files.exists(parentDir)) {
            try {
                Files.createDirectories(parentDir);
                LOGGER.info("Created directory: {}", parentDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create directory {}: {}", parentDir, e.getMessage());
                return false;
            }
        }
        
        try {
            // Create different default content based on file type
            if (configPath.endsWith("road_types.json")) {
                LOGGER.info("Creating road_types.json default config");
                boolean result = net.countered.settlementroads.road.RoadTypeConfigLoader.createDefaultConfig(path);
                LOGGER.info("Road types config creation result: {}", result);
                return result;
            } else if (configPath.endsWith("decorations.json")) {
                LOGGER.info("Creating decorations.json default config");
                return net.countered.settlementroads.decoration.DecorationConfigLoader.createDefaultConfig(path);
            } else if (configPath.endsWith("biome_costs.json")) {
                LOGGER.info("Creating biome_costs.json default config");
                return net.countered.settlementroads.biome.BiomeCostConfigLoader.createDefaultConfig(path);
            } else if (configPath.endsWith("pathfinding.json")) {
                LOGGER.info("Creating pathfinding.json default config");
                return createDefaultPathfindingConfig(path);
            } else if (configPath.endsWith("replaceable_blocks.json")) {
                LOGGER.info("Creating replaceable_blocks.json default config");
                return net.countered.settlementroads.config.ForbiddenBlocksConfigLoader.createDefaultConfig(path);
            } else {
                // If no matching default creation logic, create an empty JSON object
                LOGGER.info("Creating empty JSON config for: {}", configPath);
                JsonObject emptyConfig = new JsonObject();
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(emptyConfig));
                return true;
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to create default config file {}: {}", configPath, e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.error("Unexpected error creating default config file {}: {}", configPath, e.getMessage());
            return false;
        }
    }
    
    /**
     * Create default pathfinding configuration file
     * @param configFile Configuration file path
     * @return Whether creation was successful
     */
    private boolean createDefaultPathfindingConfig(Path configFile) {
        try {
            JsonObject root = new JsonObject();
            JsonObject pathfinding = new JsonObject();
            
            // Use optimized parameters from current configuration file
            pathfinding.addProperty("neighborDistance", 8);
            pathfinding.addProperty("diagonalStepCost", 1.4);
            pathfinding.addProperty("straightStepCost", 1.0);
            pathfinding.addProperty("maxSteps", 50000);
            pathfinding.addProperty("heuristicWeight", 1.0);
            
            JsonObject costMultipliers = new JsonObject();
            costMultipliers.addProperty("elevation", 40.0);
            costMultipliers.addProperty("biome", 8.0);
            costMultipliers.addProperty("seaLevel", 8.0);
            costMultipliers.addProperty("terrainStability", 16.0);
            pathfinding.add("costMultipliers", costMultipliers);
            
            root.add("pathfinding", pathfinding);
            
            Files.writeString(configFile, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            LOGGER.info("Created default pathfinding config: {}", configFile);
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to create default pathfinding config: {}", configFile, e);
            return false;
        }
    }
    
}
