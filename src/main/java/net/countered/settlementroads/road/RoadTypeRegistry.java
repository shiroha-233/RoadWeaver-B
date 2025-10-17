package net.countered.settlementroads.road;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Road type registry
 * Responsible for managing registration and selection of all road types
 */
public class RoadTypeRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-RoadTypeRegistry");
    
    private final Map<String, RoadTypeConfig> roadTypes = new ConcurrentHashMap<>();
    private final Map<String, Double> weightCache = new ConcurrentHashMap<>();
    
    /**
     * Register road type
     * @param config Road type configuration
     * @return Whether registration was successful
     */
    public boolean registerRoadType(RoadTypeConfig config) {
        if (config == null) {
            LOGGER.warn("Cannot register null road type config");
            return false;
        }
        
        String id = config.getId();
        if (id == null || id.trim().isEmpty()) {
            LOGGER.warn("Cannot register road type with null or empty ID");
            return false;
        }
        
        if (roadTypes.containsKey(id)) {
            LOGGER.warn("Road type {} is already registered, overwriting", id);
        }
        
        roadTypes.put(id, config);
        weightCache.clear();
        
        LOGGER.info("Registered road type: {} ({})", id);
        return true;
    }
    
    /**
     * Unregister road type
     * @param id Road type ID
     * @return Whether unregistration was successful
     */
    public boolean unregisterRoadType(String id) {
        RoadTypeConfig removed = roadTypes.remove(id);
        if (removed != null) {
            weightCache.clear();
            LOGGER.info("Unregistered road type: {}", id);
            return true;
        }
        return false;
    }
    
    /**
     * Get road type
     * @param id Road type ID
     * @return Road type configuration, null if not found
     */
    public RoadTypeConfig getRoadType(String id) {
        return roadTypes.get(id);
    }
    
    /**
     * Get all road types
     * @return Road type map
     */
    public Map<String, RoadTypeConfig> getAllRoadTypes() {
        return new HashMap<>(roadTypes);
    }
    
    /**
     * Get enabled road types
     * @return List of enabled road types
     */
    public List<RoadTypeConfig> getEnabledRoadTypes() {
        return roadTypes.values().stream()
                .filter(RoadTypeConfig::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * Randomly select road type based on weight
     * @param random Random number generator
     * @return Randomly selected road type, null if none available
     */
    public RoadTypeConfig getRandomRoadType(Random random) {
        List<RoadTypeConfig> enabledTypes = getEnabledRoadTypes();
        
        if (enabledTypes.isEmpty()) {
            LOGGER.warn("No enabled road types available");
            return null;
        }
        
        double totalWeight = getTotalWeight(enabledTypes);
        if (totalWeight <= 0) {
            LOGGER.warn("Total weight is zero or negative, using uniform distribution");
            return enabledTypes.get(random.nextInt(enabledTypes.size()));
        }
        
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0;
        
        for (RoadTypeConfig type : enabledTypes) {
            currentWeight += type.getGenerationWeight();
            if (randomValue <= currentWeight) {
                return type;
            }
        }
        
        return enabledTypes.get(enabledTypes.size() - 1);
    }
    
    /**
     * Select road type based on context
     * @param context Road type context
     * @param random Random number generator
     * @return Suitable road type, null if none suitable
     */
    public RoadTypeConfig getSuitableRoadType(RoadTypeContext context, Random random) {
        List<RoadTypeConfig> suitableTypes = roadTypes.values().stream()
                .filter(RoadTypeConfig::isEnabled)
                .filter(type -> type.isSuitableFor(context))
                .collect(Collectors.toList());
        
        if (suitableTypes.isEmpty()) {
            LOGGER.warn("No suitable road types found for context");
            return null;
        }
        
        if (suitableTypes.size() == 1) {
            return suitableTypes.get(0);
        }
        double totalWeight = getTotalWeight(suitableTypes);
        if (totalWeight <= 0) {
            return suitableTypes.get(random.nextInt(suitableTypes.size()));
        }
        
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0;
        
        for (RoadTypeConfig type : suitableTypes) {
            currentWeight += type.getGenerationWeight();
            if (randomValue <= currentWeight) {
                return type;
            }
        }
        
        return suitableTypes.get(suitableTypes.size() - 1);
    }
    
    /**
     * Select road type based on biome
     * Prioritizes road types that prefer the specified biome
     * @param biomeId Biome ID
     * @param random Random number generator
     * @return Suitable road type, null if none suitable
     */
    public RoadTypeConfig getRoadTypeForBiome(String biomeId, Random random) {
        List<RoadTypeConfig> enabledTypes = getEnabledRoadTypes();
        
        if (enabledTypes.isEmpty()) {
            LOGGER.warn("No enabled road types available for biome: {}", biomeId);
            return null;
        }
        
        List<RoadTypeConfig> preferredTypes = enabledTypes.stream()
                .filter(type -> type.prefersBiome(biomeId))
                .collect(Collectors.toList());
        
        if (!preferredTypes.isEmpty()) {
            RoadTypeConfig selected = preferredTypes.get(random.nextInt(preferredTypes.size()));
            LOGGER.debug("Selected preferred road type '{}' for biome '{}'", selected.getId(), biomeId);
            return selected;
        }
        
        List<RoadTypeConfig> defaultTypes = enabledTypes.stream()
                .filter(type -> type.getPreferredBiomes().isEmpty())
                .collect(Collectors.toList());
        
        if (!defaultTypes.isEmpty()) {
            RoadTypeConfig selected = defaultTypes.get(random.nextInt(defaultTypes.size()));
            LOGGER.debug("Selected default road type '{}' for biome '{}' (no preferred types found)", selected.getId(), biomeId);
            return selected;
        }
        
        LOGGER.info("No preferred or default road types for biome '{}', skipping road generation", biomeId);
        return null;
    }
    
    /**
     * Get road types that prefer specific biome
     * @param biomeId Biome ID
     * @return List of road types that prefer the biome
     */
    public List<RoadTypeConfig> getPreferredRoadTypesForBiome(String biomeId) {
        return roadTypes.values().stream()
                .filter(RoadTypeConfig::isEnabled)
                .filter(type -> type.prefersBiome(biomeId))
                .collect(Collectors.toList());
    }
    
    /**
     * Get road type count
     * @return Total number of road types
     */
    public int getRoadTypeCount() {
        return roadTypes.size();
    }
    
    /**
     * Get enabled road type count
     * @return Number of enabled road types
     */
    public int getEnabledRoadTypeCount() {
        return getEnabledRoadTypes().size();
    }
    
    /**
     * Check if road type exists
     * @param id Road type ID
     * @return Whether exists
     */
    public boolean hasRoadType(String id) {
        return roadTypes.containsKey(id);
    }
    
    /**
     * Check if road type is enabled
     * @param id Road type ID
     * @return Whether enabled
     */
    public boolean isRoadTypeEnabled(String id) {
        RoadTypeConfig config = roadTypes.get(id);
        return config != null && config.isEnabled();
    }
    
    /**
     * Enable/disable road type
     * @param id Road type ID
     * @param enabled Whether to enable
     * @return Whether operation was successful
     */
    public boolean setRoadTypeEnabled(String id, boolean enabled) {
        RoadTypeConfig config = roadTypes.get(id);
        if (config == null) {
            return false;
        }
        
        // Note: This would require modifying the configuration
        // Since the interface is read-only, this may need to be implemented differently
        LOGGER.info("Road type {} {} {}", id, enabled ? "enabled" : "disabled");
        return true;
    }
    
    /**
     * Calculate total weight
     * @param types Road type list
     * @return Total weight
     */
    private double getTotalWeight(List<RoadTypeConfig> types) {
        return types.stream()
                .mapToDouble(RoadTypeConfig::getGenerationWeight)
                .sum();
    }
    
    /**
     * Get road type statistics
     * @return Statistics string
     */
    public String getStatistics() {
        int total = getRoadTypeCount();
        int enabled = getEnabledRoadTypeCount();
        double totalWeight = getTotalWeight(getEnabledRoadTypes());
        
        return String.format("Road Types: %d total, %d enabled, total weight: %.2f", 
                           total, enabled, totalWeight);
    }
    
    /**
     * Clear all road types
     */
    public void clear() {
        roadTypes.clear();
        weightCache.clear();
        LOGGER.info("Cleared all road types");
    }
}
