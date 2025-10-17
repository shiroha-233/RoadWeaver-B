package net.countered.settlementroads.decoration;

import net.countered.settlementroads.features.decoration.Decoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 装饰注册表
 * 负责管理所有装饰工厂的注册和创建
 */
public class DecorationRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-DecorationRegistry");
    
    private final Map<String, DecorationFactory> factories = new ConcurrentHashMap<>();
    
    /**
     * 注册装饰工厂
     * @param factory 装饰工厂
     * @return 是否注册成功
     */
    public boolean registerDecoration(DecorationFactory factory) {
        if (factory == null) {
            LOGGER.warn("Cannot register null decoration factory");
            return false;
        }
        
        String id = factory.getDecorationId();
        if (id == null || id.trim().isEmpty()) {
            LOGGER.warn("Cannot register decoration factory with null or empty ID");
            return false;
        }
        
        if (factories.containsKey(id)) {
            LOGGER.warn("Decoration factory {} is already registered, overwriting", id);
        }
        
        factories.put(id, factory);
        LOGGER.info("Registered decoration factory: {}", id);
        return true;
    }
    
    /**
     * 获取装饰工厂
     * @param id 装饰ID
     * @return 装饰工厂，如果不存在返回null
     */
    public DecorationFactory getFactory(String id) {
        return factories.get(id);
    }
    
    /**
     * 获取所有装饰工厂
     * @return 装饰工厂映射
     */
    public Map<String, DecorationFactory> getAllFactories() {
        return new HashMap<>(factories);
    }
    
    /**
     * 创建装饰列表
     * @param context 装饰上下文
     * @return 装饰列表
     */
    public List<Decoration> createDecorations(DecorationContext context) {
        List<Decoration> decorations = new ArrayList<>();
        
        for (DecorationFactory factory : factories.values()) {
            if (factory.shouldPlace(context)) {
                if (context.getSegmentIndex() % factory.getPlacementInterval() == 0) {
                    try {
                        Decoration decoration = factory.createDecoration(context);
                        if (decoration != null) {
                            decorations.add(decoration);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to create decoration from factory {}: {}", 
                                   factory.getDecorationId(), e.getMessage(), e);
                    }
                }
            }
        }
        
        // 按优先级排序
        decorations.sort(Comparator.comparingInt(Decoration::getPlacementPriority));
        return decorations;
    }
    
    /**
     * 清空所有装饰工厂
     */
    public void clear() {
        factories.clear();
        LOGGER.info("Cleared all decoration factories");
    }
}
