package net.countered.settlementroads.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Biome tag cost calculator
 * Calculates costs based on biome tags
 */
public class BiomeTagCostCalculator implements BiomeCostCalculator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-BiomeTagCostCalculator");
    
    private final Map<String, Double> tagCosts;
    private final Map<TagKey<Biome>, Double> compiledTagCosts;
    
    public BiomeTagCostCalculator(Map<String, Double> tagCosts) {
        this.tagCosts = new HashMap<>(tagCosts);
        this.compiledTagCosts = new HashMap<>();
        compileTagCosts();
    }
    
    /**
     * Compile tag cost mapping
     */
    private void compileTagCosts() {
        for (Map.Entry<String, Double> entry : tagCosts.entrySet()) {
            try {
                String tagName = entry.getKey();
                double cost = entry.getValue();
                
                // Handle common biome tags
                TagKey<Biome> tagKey = getBiomeTag(tagName);
                if (tagKey != null) {
                    compiledTagCosts.put(tagKey, cost);
                    LOGGER.debug("Compiled tag cost: {} -> {}", tagName, cost);
                } else {
                    LOGGER.warn("Unknown biome tag: {}", tagName);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to compile tag cost for {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Get biome tag
     * 
     * Supported tags based on Minecraft 1.21 official data
     * Source: https://github.com/misode/mcmeta/tree/data/data/minecraft/tags/worldgen/biome
     * 
     * @param tagName Tag name
     * @return Biome tag key, null if tag does not exist
     */
    private TagKey<Biome> getBiomeTag(String tagName) {
        try {
            // Handle available biome tags in Minecraft 1.21
            switch (tagName) {
                // Core biome type tags
                case "minecraft:is_river":
                    return BiomeTags.IS_RIVER;
                case "minecraft:is_ocean":
                    return BiomeTags.IS_OCEAN;
                case "minecraft:is_deep_ocean":
                    return BiomeTags.IS_DEEP_OCEAN;
                case "minecraft:is_forest":
                    return BiomeTags.IS_FOREST;
                case "minecraft:is_taiga":
                    return BiomeTags.IS_TAIGA;
                case "minecraft:is_savanna":
                    return BiomeTags.IS_SAVANNA;
                case "minecraft:is_badlands":
                    return BiomeTags.IS_BADLANDS;
                case "minecraft:is_mountain":
                    return BiomeTags.IS_MOUNTAIN;
                case "minecraft:is_hill":
                    return BiomeTags.IS_HILL;
                case "minecraft:is_jungle":
                    return BiomeTags.IS_JUNGLE;
                case "minecraft:is_beach":
                    return BiomeTags.IS_BEACH;
                case "minecraft:is_nether":
                    return BiomeTags.IS_NETHER;
                case "minecraft:is_end":
                    return BiomeTags.IS_END;
                case "minecraft:is_overworld":
                    return BiomeTags.IS_OVERWORLD;
                
                default:
                    // Try to parse custom tag
                    Identifier identifier = Identifier.tryParse(tagName);
                    if (identifier != null) {
                        return TagKey.of(net.minecraft.registry.RegistryKeys.BIOME, identifier);
                    }
                    LOGGER.warn("Unknown biome tag: {}", tagName);
                    return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get biome tag for {}: {}", tagName, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public double calculateCost(RegistryEntry<Biome> biome, BlockPos position, ServerWorld world) {
        double totalCost = 0.0;
        
        for (Map.Entry<TagKey<Biome>, Double> entry : compiledTagCosts.entrySet()) {
            TagKey<Biome> tagKey = entry.getKey();
            double cost = entry.getValue();
            
            if (biome.isIn(tagKey)) {
                totalCost += cost;
                LOGGER.debug("Biome {} matches tag {}, adding cost {}", 
                           biome.getKey().map(RegistryKey::getValue).orElse(null), 
                           tagKey.id(), cost);
            }
        }
        
        return totalCost;
    }
    
    @Override
    public String getCalculatorId() {
        return "biome_tag_calculator";
    }
    
    @Override
    public int getPriority() {
        return 1; // Lower priority, executes before specific biome calculators
    }
    
    /**
     * Get tag cost mapping
     * @return Tag cost mapping
     */
    public Map<String, Double> getTagCosts() {
        return new HashMap<>(tagCosts);
    }
    
    /**
     * Set tag cost
     * @param tagName Tag name
     * @param cost Cost value
     */
    public void setTagCost(String tagName, double cost) {
        tagCosts.put(tagName, cost);
        compileTagCosts(); // Recompile
    }
    
    /**
     * Remove tag cost
     * @param tagName Tag name
     * @return Whether removal was successful
     */
    public boolean removeTagCost(String tagName) {
        boolean removed = tagCosts.remove(tagName) != null;
        if (removed) {
            compileTagCosts(); // Recompile
        }
        return removed;
    }
    
    /**
     * Clear all tag costs
     */
    public void clearTagCosts() {
        tagCosts.clear();
        compiledTagCosts.clear();
    }
}
