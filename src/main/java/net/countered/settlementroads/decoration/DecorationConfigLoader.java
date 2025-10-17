package net.countered.settlementroads.decoration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decoration configuration loader
 * Responsible for loading decoration configurations from JSON config files
 */
public class DecorationConfigLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-DecorationConfigLoader");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Load decoration configurations from config file
     * @param configFile Config file path
     * @param registry Decoration registry
     * @return Number of decorations loaded
     */
    public static int loadDecorations(Path configFile, DecorationRegistry registry) {
        if (!Files.exists(configFile)) {
            LOGGER.warn("Decorations config file not found: {}", configFile);
            return 0;
        }
        
        try {
            String jsonContent = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            if (!root.has("decorations")) {
                LOGGER.warn("No 'decorations' section found in config file: {}", configFile);
                return 0;
            }
            
            JsonObject decorationsSection = root.getAsJsonObject("decorations");
            int loadedCount = 0;
            
            for (String decorationId : decorationsSection.keySet()) {
                try {
                    JsonObject decorationConfig = decorationsSection.getAsJsonObject(decorationId);
                    DecorationFactory factory = parseDecorationFactory(decorationId, decorationConfig);
                    
                    if (factory != null && registry.registerDecoration(factory)) {
                        loadedCount++;
                        LOGGER.info("Loaded decoration factory: {}", decorationId);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load decoration factory {}: {}", decorationId, e.getMessage(), e);
                }
            }
            
            LOGGER.info("Loaded {} decoration factories from {}", loadedCount, configFile);
            return loadedCount;
            
        } catch (IOException e) {
            LOGGER.error("Failed to read decorations config file: {}", configFile, e);
            return 0;
        }
    }
    
    /**
     * Parse decoration factory configuration
     * @param id Decoration ID
     * @param configJson Configuration JSON object
     * @return Decoration factory
     */
    private static DecorationFactory parseDecorationFactory(String id, JsonObject configJson) {
        try {
            String factoryClass = configJson.has("factory") ? 
                configJson.get("factory").getAsString() : null;
            
            if (factoryClass == null) {
                LOGGER.warn("No factory class specified for decoration: {}", id);
                return null;
            }
            Class<?> clazz = Class.forName(factoryClass);
            if (!DecorationFactory.class.isAssignableFrom(clazz)) {
                LOGGER.warn("Factory class {} does not implement DecorationFactory", factoryClass);
                return null;
            }
            
            DecorationFactory factory = (DecorationFactory) clazz.getDeclaredConstructor().newInstance();
            
            if (configJson.has("properties")) {
                configureFactory(factory, configJson.getAsJsonObject("properties"));
            }
            
            return factory;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create decoration factory for {}: {}", id, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Configure factory properties
     * @param factory Decoration factory
     * @param properties Property configuration
     */
    private static void configureFactory(DecorationFactory factory, JsonObject properties) {
        for (String key : properties.keySet()) {
            var value = properties.get(key);
            if (value.isJsonPrimitive()) {
                var primitive = value.getAsJsonPrimitive();
                if (primitive.isString()) {
                } else if (primitive.isNumber()) {
                } else if (primitive.isBoolean()) {
                }
            }
        }
    }
    
    /**
     * Load decoration configurations from JSON object
     * @param decorationsJson Decoration configuration JSON object
     * @param registry Decoration registry
     * @return Number of decorations loaded
     */
    public static int loadDecorationsFromJson(com.google.gson.JsonObject decorationsJson, DecorationRegistry registry) {
        int loadedCount = 0;
        
        for (String decorationId : decorationsJson.keySet()) {
            try {
                com.google.gson.JsonObject decorationConfig = decorationsJson.getAsJsonObject(decorationId);
                DecorationFactory factory = parseDecorationFactory(decorationId, decorationConfig);
                
                if (factory != null && registry.registerDecoration(factory)) {
                    loadedCount++;
                    LOGGER.info("Loaded decoration factory: {}", decorationId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load decoration factory {}: {}", decorationId, e.getMessage(), e);
            }
        }
        
        LOGGER.info("Loaded {} decoration factories from JSON", loadedCount);
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
            JsonObject decorations = new JsonObject();
            
            JsonObject lamppost = new JsonObject();
            lamppost.addProperty("factory", "net.countered.settlementroads.decoration.factory.LamppostDecorationFactory");
            lamppost.addProperty("placementInterval", 59);
            
            JsonObject lamppostConditions = new JsonObject();
            lamppostConditions.addProperty("minSegmentIndex", 60);
            lamppost.add("conditions", lamppostConditions);
            
            decorations.add("lamppost", lamppost);
            
            JsonObject waypoint = new JsonObject();
            waypoint.addProperty("factory", "net.countered.settlementroads.decoration.factory.FenceWaypointDecorationFactory");
            waypoint.addProperty("placementInterval", 25);
            
            JsonObject waypointConditions = new JsonObject();
            waypoint.add("conditions", waypointConditions);
            
            decorations.add("waypoint", waypoint);
            
            JsonObject distanceSign = new JsonObject();
            distanceSign.addProperty("factory", "net.countered.settlementroads.decoration.factory.DistanceSignDecorationFactory");
            distanceSign.addProperty("placementInterval", 1);
            
            JsonObject distanceSignConditions = new JsonObject();
            distanceSignConditions.addProperty("segmentIndex", "start|end");
            distanceSign.add("conditions", distanceSignConditions);
            
            decorations.add("distance_sign", distanceSign);
            
            root.add("decorations", decorations);
            
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(root));
            
            LOGGER.info("Created default decorations config: {}", configFile);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create default decorations config: {}", configFile, e);
            return false;
        }
    }
}
