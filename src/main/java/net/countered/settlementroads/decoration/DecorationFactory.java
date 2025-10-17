package net.countered.settlementroads.decoration;

import net.countered.settlementroads.features.decoration.Decoration;

/**
 * 装饰工厂接口
 * 负责创建装饰实例
 */
public interface DecorationFactory {
    
    /**
     * 获取装饰ID
     * @return 装饰唯一标识符
     */
    String getDecorationId();
    
    /**
     * 创建装饰实例
     * @param context 装饰上下文
     * @return 装饰实例
     */
    Decoration createDecoration(DecorationContext context);
    
    /**
     * 检查是否应该放置装饰
     * @param context 装饰上下文
     * @return 是否应该放置
     */
    boolean shouldPlace(DecorationContext context);
    
    /**
     * 获取放置间隔
     * @return 放置间隔（每N个路段放置一次）
     */
    int getPlacementInterval();
}
