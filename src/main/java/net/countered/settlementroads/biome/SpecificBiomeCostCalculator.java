package net.countered.settlementroads.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Specific biome cost calculator
 * Calculates costs based on specific biome IDs
 */
public class SpecificBiomeCostCalculator implements BiomeCostCalculator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-SpecificBiomeCostCalculator");
    
    private final Map<RegistryKey<Biome>, Double> biomeCosts;
    
    public SpecificBiomeCostCalculator(Map<String, Double> biomeCostMap) {
        this.biomeCosts = new HashMap<>();
        compileBiomeCosts(biomeCostMap);
    }
    
    /**
     * Compile biome cost mapping
     * @param biomeCostMap Biome cost mapping (string ID -> cost)
     */
    private void compileBiomeCosts(Map<String, Double> biomeCostMap) {
        for (Map.Entry<String, Double> entry : biomeCostMap.entrySet()) {
            try {
                String biomeId = entry.getKey();
                double cost = entry.getValue();
                
                Identifier identifier = Identifier.tryParse(biomeId);
                if (identifier != null) {
                    RegistryKey<Biome> biomeKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, identifier);
                    biomeCosts.put(biomeKey, cost);
                    LOGGER.debug("Compiled biome cost: {} -> {}", biomeId, cost);
                } else {
                    LOGGER.warn("Invalid biome identifier: {}", biomeId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to compile biome cost for {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }
    }
    
    @Override
    public double calculateCost(RegistryEntry<Biome> biome, BlockPos position, ServerWorld world) {
        return biome.getKey()
            .map(biomeCosts::get)
            .orElse(0.0);
    }
    
    @Override
    public String getCalculatorId() {
        return "specific_biome_calculator";
    }
    
    @Override
    public int getPriority() {
        return 2; // Higher priority, executes after tag calculators
    }
    
    /**
     * Get biome cost mapping
     * @return Biome cost mapping
     */
    public Map<RegistryKey<Biome>, Double> getBiomeCosts() {
        return new HashMap<>(biomeCosts);
    }
    
    /**
     * Set biome cost
     * @param biomeKey Biome key
     * @param cost Cost value
     */
    public void setBiomeCost(RegistryKey<Biome> biomeKey, double cost) {
        biomeCosts.put(biomeKey, cost);
    }
    
    /**
     * Set biome cost (by ID)
     * @param biomeId Biome ID
     * @param cost Cost value
     * @return Whether setting was successful
     */
    public boolean setBiomeCost(String biomeId, double cost) {
        try {
            Identifier identifier = Identifier.tryParse(biomeId);
            if (identifier != null) {
                RegistryKey<Biome> biomeKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, identifier);
                biomeCosts.put(biomeKey, cost);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to set biome cost for {}: {}", biomeId, e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Remove biome cost
     * @param biomeKey Biome key
     * @return Whether removal was successful
     */
    public boolean removeBiomeCost(RegistryKey<Biome> biomeKey) {
        return biomeCosts.remove(biomeKey) != null;
    }
    
    /**
     * Remove biome cost (by ID)
     * @param biomeId Biome ID
     * @return Whether removal was successful
     */
    public boolean removeBiomeCost(String biomeId) {
        try {
            Identifier identifier = Identifier.tryParse(biomeId);
            if (identifier != null) {
                RegistryKey<Biome> biomeKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, identifier);
                return removeBiomeCost(biomeKey);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove biome cost for {}: {}", biomeId, e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Clear all biome costs
     */
    public void clearBiomeCosts() {
        biomeCosts.clear();
    }
    
    /**
     * Check if contains specific biome
     * @param biomeKey Biome key
     * @return Whether contains
     */
    public boolean containsBiome(RegistryKey<Biome> biomeKey) {
        return biomeCosts.containsKey(biomeKey);
    }
    
    /**
     * Check if contains specific biome (by ID)
     * @param biomeId Biome ID
     * @return Whether contains
     */
    public boolean containsBiome(String biomeId) {
        try {
            Identifier identifier = Identifier.tryParse(biomeId);
            if (identifier != null) {
                RegistryKey<Biome> biomeKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, identifier);
                return containsBiome(biomeKey);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check biome for {}: {}", biomeId, e.getMessage(), e);
        }
        return false;
    }
}
