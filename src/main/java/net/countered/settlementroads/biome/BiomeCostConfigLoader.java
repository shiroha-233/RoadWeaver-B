package net.countered.settlementroads.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Biome cost configuration loader
 * Responsible for loading biome cost configurations from JSON config files
 */
public class BiomeCostConfigLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-BiomeCostConfigLoader");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Load biome cost configurations from config file
     * @param configFile Config file path
     * @param calculator Composite cost calculator
     * @return Number of calculators loaded
     */
    public static int loadBiomeCosts(Path configFile, CompositeBiomeCostCalculator calculator) {
        if (!Files.exists(configFile)) {
            LOGGER.warn("Biome costs config file not found: {}", configFile);
            return 0;
        }
        
        try {
            String jsonContent = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            int loadedCount = 0;
            
            if (root.has("tagCosts")) {
                JsonObject tagCostsSection = root.getAsJsonObject("tagCosts");
                Map<String, Double> tagCosts = parseTagCosts(tagCostsSection);
                
                if (!tagCosts.isEmpty()) {
                    BiomeTagCostCalculator tagCalculator = new BiomeTagCostCalculator(tagCosts);
                    calculator.addCalculator(tagCalculator);
                    loadedCount++;
                    LOGGER.info("Loaded biome tag cost calculator with {} tags", tagCosts.size());
                }
            }
            
            if (root.has("specificBiomes")) {
                JsonObject specificBiomesSection = root.getAsJsonObject("specificBiomes");
                Map<String, Double> biomeCosts = parseBiomeCosts(specificBiomesSection);
                
                if (!biomeCosts.isEmpty()) {
                    SpecificBiomeCostCalculator biomeCalculator = new SpecificBiomeCostCalculator(biomeCosts);
                    calculator.addCalculator(biomeCalculator);
                    loadedCount++;
                    LOGGER.info("Loaded specific biome cost calculator with {} biomes", biomeCosts.size());
                }
            }
            
            // Load terrain cost calculator
            if (root.has("terrainCosts")) {
                JsonObject terrainCostsSection = root.getAsJsonObject("terrainCosts");
                TerrainCostCalculator terrainCalculator = parseTerrainCosts(terrainCostsSection);
                
                if (terrainCalculator != null) {
                    calculator.addCalculator(terrainCalculator);
                    loadedCount++;
                    LOGGER.info("Loaded terrain cost calculator");
                }
            }
            
            LOGGER.info("Loaded {} biome cost calculators from {}", loadedCount, configFile);
            return loadedCount;
            
        } catch (IOException e) {
            LOGGER.error("Failed to read biome costs config file: {}", configFile, e);
            return 0;
        }
    }
    
    /**
     * Parse tag costs
     * @param tagCostsSection Tag cost configuration
     * @return Tag cost mapping
     */
    private static Map<String, Double> parseTagCosts(JsonObject tagCostsSection) {
        Map<String, Double> tagCosts = new HashMap<>();
        
        for (String tagName : tagCostsSection.keySet()) {
            try {
                double cost = tagCostsSection.get(tagName).getAsDouble();
                tagCosts.put(tagName, cost);
            } catch (Exception e) {
                LOGGER.error("Failed to parse tag cost for {}: {}", tagName, e.getMessage(), e);
            }
        }
        
        return tagCosts;
    }
    
    /**
     * Parse biome costs
     * @param biomeCostsSection Biome cost configuration
     * @return Biome cost mapping
     */
    private static Map<String, Double> parseBiomeCosts(JsonObject biomeCostsSection) {
        Map<String, Double> biomeCosts = new HashMap<>();
        
        for (String biomeId : biomeCostsSection.keySet()) {
            try {
                double cost = biomeCostsSection.get(biomeId).getAsDouble();
                biomeCosts.put(biomeId, cost);
            } catch (Exception e) {
                LOGGER.error("Failed to parse biome cost for {}: {}", biomeId, e.getMessage(), e);
            }
        }
        
        return biomeCosts;
    }
    
    /**
     * Parse terrain cost configuration
     * @param terrainCostsSection Terrain cost configuration
     * @return Terrain cost calculator
     */
    private static TerrainCostCalculator parseTerrainCosts(JsonObject terrainCostsSection) {
        try {
            double elevationCostMultiplier = terrainCostsSection.has("elevationCostMultiplier") ? 
                terrainCostsSection.get("elevationCostMultiplier").getAsDouble() : 40.0;
            double stabilityCostMultiplier = terrainCostsSection.has("stabilityCostMultiplier") ? 
                terrainCostsSection.get("stabilityCostMultiplier").getAsDouble() : 16.0;
            double seaLevelCost = terrainCostsSection.has("seaLevelCost") ? 
                terrainCostsSection.get("seaLevelCost").getAsDouble() : 8.0;
            int seaLevel = terrainCostsSection.has("seaLevel") ? 
                terrainCostsSection.get("seaLevel").getAsInt() : 62;
            int maxHeightDifference = terrainCostsSection.has("maxHeightDifference") ? 
                terrainCostsSection.get("maxHeightDifference").getAsInt() : 5;
            int maxTerrainStability = terrainCostsSection.has("maxTerrainStability") ? 
                terrainCostsSection.get("maxTerrainStability").getAsInt() : 4;
            
            return new TerrainCostCalculator(
                elevationCostMultiplier,
                stabilityCostMultiplier,
                seaLevelCost,
                seaLevel,
                maxHeightDifference,
                maxTerrainStability
            );
        } catch (Exception e) {
            LOGGER.error("Failed to parse terrain costs: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Load biome cost configurations from JSON object
     * @param biomeCostsJson Biome cost configuration JSON object
     * @param calculator Composite cost calculator
     * @return Number of calculators loaded
     */
    public static int loadBiomeCostsFromJson(com.google.gson.JsonObject biomeCostsJson, CompositeBiomeCostCalculator calculator) {
        int loadedCount = 0;
        
        // Load tag cost calculator
        if (biomeCostsJson.has("tagCosts")) {
            com.google.gson.JsonObject tagCostsSection = biomeCostsJson.getAsJsonObject("tagCosts");
            Map<String, Double> tagCosts = parseTagCosts(tagCostsSection);
            
            if (!tagCosts.isEmpty()) {
                BiomeTagCostCalculator tagCalculator = new BiomeTagCostCalculator(tagCosts);
                calculator.addCalculator(tagCalculator);
                loadedCount++;
                LOGGER.info("Loaded biome tag cost calculator with {} tags", tagCosts.size());
            }
        }
        
        // Load specific biome cost calculator
        if (biomeCostsJson.has("specificBiomes")) {
            com.google.gson.JsonObject specificBiomesSection = biomeCostsJson.getAsJsonObject("specificBiomes");
            Map<String, Double> biomeCosts = parseBiomeCosts(specificBiomesSection);
            
            if (!biomeCosts.isEmpty()) {
                SpecificBiomeCostCalculator biomeCalculator = new SpecificBiomeCostCalculator(biomeCosts);
                calculator.addCalculator(biomeCalculator);
                loadedCount++;
                LOGGER.info("Loaded specific biome cost calculator with {} biomes", biomeCosts.size());
            }
        }
        
        // Load terrain cost calculator
        if (biomeCostsJson.has("terrainCosts")) {
            com.google.gson.JsonObject terrainCostsSection = biomeCostsJson.getAsJsonObject("terrainCosts");
            TerrainCostCalculator terrainCalculator = parseTerrainCosts(terrainCostsSection);
            
            if (terrainCalculator != null) {
                calculator.addCalculator(terrainCalculator);
                loadedCount++;
                LOGGER.info("Loaded terrain cost calculator");
            }
        }
        
        LOGGER.info("Loaded {} biome cost calculators from JSON", loadedCount);
        return loadedCount;
    }
    
    /**
     * Create default configuration file
     * @param configFile Configuration file path
     * @return Whether creation was successful
     */
    public static boolean createDefaultConfig(Path configFile) {
        try {
            JsonObject root = new JsonObject();
            JsonObject biomeCosts = new JsonObject();
            
            // Tag cost configuration
            JsonObject tagCosts = new JsonObject();
            tagCosts.addProperty("minecraft:is_river", 50.0);
            tagCosts.addProperty("minecraft:is_ocean", 50.0);
            tagCosts.addProperty("minecraft:is_deep_ocean", 50.0);
            tagCosts.addProperty("minecraft:is_nether", 100.0);
            tagCosts.addProperty("minecraft:is_end", 100.0);
            biomeCosts.add("tagCosts", tagCosts);
            
            // Specific biome cost configuration - using current config file values
            JsonObject specificBiomes = new JsonObject();
            specificBiomes.addProperty("minecraft:plains", 5.0);
            specificBiomes.addProperty("minecraft:forest", 10.0);
            specificBiomes.addProperty("minecraft:desert", 15.0);
            specificBiomes.addProperty("minecraft:mountains", 20.0);
            biomeCosts.add("specificBiomes", specificBiomes);
            
            // Terrain cost configuration
            JsonObject terrainCosts = new JsonObject();
            terrainCosts.addProperty("elevationCostMultiplier", 40.0);
            terrainCosts.addProperty("stabilityCostMultiplier", 16.0);
            terrainCosts.addProperty("seaLevelCost", 8.0);
            terrainCosts.addProperty("seaLevel", 62);
            terrainCosts.addProperty("maxHeightDifference", 5);
            terrainCosts.addProperty("maxTerrainStability", 4);
            biomeCosts.add("terrainCosts", terrainCosts);
            
            // Add biomeCosts to root object
            root.add("biomeCosts", biomeCosts);
            
            // Ensure directory exists
            Files.createDirectories(configFile.getParent());
            
            // Write to file
            Files.writeString(configFile, GSON.toJson(root));
            
            LOGGER.info("Created default biome costs config: {}", configFile);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create default biome costs config: {}", configFile, e);
            return false;
        }
    }
}
