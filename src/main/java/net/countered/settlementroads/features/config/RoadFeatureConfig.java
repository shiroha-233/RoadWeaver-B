package net.countered.settlementroads.features.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.countered.settlementroads.road.RoadTypeConfig;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 道路特征配置
 */
public class RoadFeatureConfig implements FeatureConfig {

    // 存储所有启用的道路类型
    public final Map<String, RoadTypeConfig> enabledRoadTypes;
    public final List<Integer> width;
    public final List<Integer> quality;

    public static final Codec<RoadFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            Codec.INT.listOf().fieldOf("width").forGetter(RoadFeatureConfig::getWidths),
                            Codec.INT.listOf().fieldOf("quality").forGetter(RoadFeatureConfig::getQualities)
                    )
                    .apply(instance, RoadFeatureConfig::new)
    );

    /**
     * 构造函数 - 完全基于配置系统
     * @param width 道路宽度列表
     * @param quality 道路质量列表
     */
    public RoadFeatureConfig(List<Integer> width, List<Integer> quality) {
        this.enabledRoadTypes = new HashMap<>();
        this.width = width;
        this.quality = quality;
        
        // 从配置系统获取启用的道路类型
        try {
            net.countered.settlementroads.config.ConfigManager configManager = net.countered.settlementroads.config.ConfigManager.getInstance();
            List<RoadTypeConfig> enabledTypes = configManager.getRoadTypeRegistry().getEnabledRoadTypes();
            for (RoadTypeConfig roadType : enabledTypes) {
                this.enabledRoadTypes.put(roadType.getId(), roadType);
            }
        } catch (Exception e) {
            // 配置加载失败时抛出异常，遵循"无回退原则"
            throw new RuntimeException("Failed to load road types from configuration", e);
        }
    }
    
    public List<Integer> getWidths() {
        return width;
    }
    
    public List<Integer> getQualities() {
        return quality;
    }
    
    /**
     * 根据ID获取道路类型
     * @param roadTypeId 道路类型ID
     * @return 道路类型配置，如果不存在返回null
     */
    public RoadTypeConfig getRoadTypeById(String roadTypeId) {
        return enabledRoadTypes.get(roadTypeId);
    }
    
    /**
     * 获取所有启用的道路类型
     * @return 启用的道路类型列表
     */
    public List<RoadTypeConfig> getAllEnabledRoadTypes() {
        return List.copyOf(enabledRoadTypes.values());
    }
    
    /**
     * 随机选择一个道路类型
     * @param random 随机数生成器
     * @return 随机选择的道路类型，如果没有可用类型返回null
     */
    public RoadTypeConfig getRandomRoadType(java.util.Random random) {
        List<RoadTypeConfig> types = getAllEnabledRoadTypes();
        if (types.isEmpty()) {
            return null;
        }
        return types.get(random.nextInt(types.size()));
    }
}
