package net.countered.settlementroads.decoration;

import net.countered.settlementroads.road.RoadTypeConfig;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;

/**
 * 装饰上下文
 * 包含装饰放置时的环境信息
 */
public class DecorationContext {
    
    private final BlockPos position;
    private final StructureWorldAccess world;
    private final int segmentIndex;
    private final RoadTypeConfig roadType;
    private final Vec3i direction;
    private final Random random;
    private final Map<String, Object> properties;
    
    public DecorationContext(BlockPos position, StructureWorldAccess world, 
                           int segmentIndex, RoadTypeConfig roadType, 
                           Vec3i direction, Random random) {
        this.position = position;
        this.world = world;
        this.segmentIndex = segmentIndex;
        this.roadType = roadType;
        this.direction = direction;
        this.random = random;
        this.properties = new HashMap<>();
    }
    
    /**
     * 获取装饰位置
     */
    public BlockPos getPosition() {
        return position;
    }
    
    /**
     * 获取世界访问器
     */
    public StructureWorldAccess getWorld() {
        return world;
    }
    
    /**
     * 获取服务器世界
     */
    public ServerWorld getServerWorld() {
        return world.toServerWorld();
    }
    
    /**
     * 获取路段索引
     */
    public int getSegmentIndex() {
        return segmentIndex;
    }
    
    /**
     * 获取道路类型
     */
    public RoadTypeConfig getRoadType() {
        return roadType;
    }
    
    /**
     * 获取方向向量
     */
    public Vec3i getDirection() {
        return direction;
    }
    
    /**
     * 获取随机数生成器
     */
    public Random getRandom() {
        return random;
    }
    
    /**
     * 获取属性映射
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
