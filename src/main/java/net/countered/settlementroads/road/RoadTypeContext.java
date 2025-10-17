package net.countered.settlementroads.road;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.Map;
import java.util.HashMap;

/**
 * 道路类型上下文
 * 包含道路生成时的环境信息
 */
public class RoadTypeContext {
    
    private final ServerWorld world;
    private final BlockPos startPos;
    private final BlockPos endPos;
    private final RegistryEntry<Biome> startBiome;
    private final RegistryEntry<Biome> endBiome;
    private final Map<String, Object> properties;
    
    public RoadTypeContext(ServerWorld world, BlockPos startPos, BlockPos endPos) {
        this.world = world;
        this.startPos = startPos;
        this.endPos = endPos;
        this.startBiome = world.getBiome(startPos);
        this.endBiome = world.getBiome(endPos);
        this.properties = new HashMap<>();
    }
    
    /**
     * 获取世界实例
     */
    public ServerWorld getWorld() {
        return world;
    }
    
    /**
     * 获取起始位置
     */
    public BlockPos getStartPos() {
        return startPos;
    }
    
    /**
     * 获取结束位置
     */
    public BlockPos getEndPos() {
        return endPos;
    }
    
    /**
     * 获取起始位置生物群系
     */
    public RegistryEntry<Biome> getStartBiome() {
        return startBiome;
    }
    
    /**
     * 获取结束位置生物群系
     */
    public RegistryEntry<Biome> getEndBiome() {
        return endBiome;
    }
    
    /**
     * 获取道路距离
     */
    public double getDistance() {
        return startPos.getManhattanDistance(endPos);
    }
    
    /**
     * 获取属性
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * 设置属性
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * 获取属性值
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * 获取属性值（带默认值）
     */
    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}
