package net.countered.settlementroads.road;

import net.minecraft.block.BlockState;
import java.util.List;
import java.util.Map;

/**
 * Road type configuration interface
 * Defines basic properties and behavior of road types
 */
public interface RoadTypeConfig {
    
    /**
     * Get unique identifier for road type
     * @return Road type ID
     */
    String getId();
    
    /**
     * Get road materials list
     * Each inner list represents a material combination
     * @return Materials list
     */
    List<List<BlockState>> getMaterials();
    
    /**
     * Get road generation weight
     * Higher weight means higher probability of being selected
     * @return Generation weight
     */
    double getGenerationWeight();
    
    /**
     * Check if road type is enabled
     * @return Whether enabled
     */
    boolean isEnabled();
    
    /**
     * Get road type extension properties
     * @return Extension properties map
     */
    Map<String, Object> getProperties();
    
    /**
     * Get specific property value
     * @param key Property key
     * @return Property value
     */
    default Object getProperty(String key) {
        return getProperties().get(key);
    }
    
    /**
     * Get specific property value with default
     * @param key Property key
     * @param defaultValue Default value
     * @return Property value
     */
    default Object getProperty(String key, Object defaultValue) {
        return getProperties().getOrDefault(key, defaultValue);
    }
    
    /**
     * Get road width range
     * @return Width list
     */
    default List<Integer> getWidths() {
        Object widths = getProperty("widths");
        if (widths instanceof List) {
            @SuppressWarnings("unchecked")
            List<Integer> widthList = (List<Integer>) widths;
            return widthList;
        }
        return List.of(3, 5, 7);
    }
    
    /**
     * Get road quality levels
     * @return Quality level list
     */
    default List<Integer> getQualities() {
        Object qualities = getProperty("qualities");
        if (qualities instanceof List) {
            @SuppressWarnings("unchecked")
            List<Integer> qualityList = (List<Integer>) qualities;
            return qualityList;
        }
        return List.of(1, 2, 3);
    }
    
    /**
     * Get averaging radius
     * @return Averaging radius
     */
    default int getAveragingRadius() {
        Object radius = getProperty("averagingRadius");
        if (radius instanceof Integer) {
            return (Integer) radius;
        }
        return 1;
    }
    
    /**
     * Check if supports specific material
     * @param material Material
     * @return Whether supports
     */
    default boolean supportsMaterial(BlockState material) {
        return getMaterials().stream()
                .anyMatch(materialList -> materialList.contains(material));
    }
    
    /**
     * Get random material
     * @param random Random number generator
     * @return Randomly selected material list
     */
    default List<BlockState> getRandomMaterials(java.util.Random random) {
        List<List<BlockState>> materials = getMaterials();
        if (materials.isEmpty()) {
            return List.of();
        }
        return materials.get(random.nextInt(materials.size()));
    }
    
    /**
     * Check if road type is suitable for specific conditions
     * @param context Context information
     * @return Whether suitable
     */
    default boolean isSuitableFor(RoadTypeContext context) {
        return isEnabled();
    }
    
    /**
     * Get preferred biome ID list
     * @return Biome ID list
     */
    default List<String> getPreferredBiomes() {
        Object biomes = getProperty("preferredBiomes");
        if (biomes instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> biomeList = (List<String>) biomes;
            return biomeList;
        }
        return List.of();
    }
    
    /**
     * Check if road type prefers specific biome
     * @param biomeId Biome ID
     * @return Whether prefers
     */
    default boolean prefersBiome(String biomeId) {
        return getPreferredBiomes().contains(biomeId);
    }
}
