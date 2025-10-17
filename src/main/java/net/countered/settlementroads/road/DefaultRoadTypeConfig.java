package net.countered.settlementroads.road;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Default road type configuration implementation
 * Provides basic road type configuration functionality
 */
public class DefaultRoadTypeConfig implements RoadTypeConfig {
    
    private final String id;
    private final List<List<BlockState>> materials;
    private final double generationWeight;
    private final boolean enabled;
    private final Map<String, Object> properties;
    
    public DefaultRoadTypeConfig(String id, 
                                List<List<BlockState>> materials, 
                                double generationWeight, 
                                boolean enabled) {
        this.id = id;
        this.materials = materials;
        this.generationWeight = generationWeight;
        this.enabled = enabled;
        this.properties = new HashMap<>();
    }
    
    public DefaultRoadTypeConfig(String id, 
                                List<List<BlockState>> materials, 
                                double generationWeight, 
                                boolean enabled,
                                Map<String, Object> properties) {
        this.id = id;
        this.materials = materials;
        this.generationWeight = generationWeight;
        this.enabled = enabled;
        this.properties = new HashMap<>(properties);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    
    @Override
    public List<List<BlockState>> getMaterials() {
        return materials;
    }
    
    @Override
    public double getGenerationWeight() {
        return generationWeight;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    
    
    @Override
    public String toString() {
        return String.format("RoadTypeConfig{id='%s', enabled=%s, weight=%.2f}", 
                           id, enabled, generationWeight);
    }
}
