package net.countered.settlementroads.decoration.factory;

import net.countered.settlementroads.decoration.DecorationContext;
import net.countered.settlementroads.decoration.DecorationFactory;
import net.countered.settlementroads.features.decoration.Decoration;
import net.countered.settlementroads.features.decoration.DistanceSignDecoration;

/**
 * 距离标志装饰工厂
 * 负责创建距离标志装饰实例
 */
public class DistanceSignDecorationFactory implements DecorationFactory {
    
    private static final String DECORATION_ID = "distance_sign";
    private int placementInterval = 1;
    
    @Override
    public String getDecorationId() {
        return DECORATION_ID;
    }
    
    @Override
    public Decoration createDecoration(DecorationContext context) {
        // 计算偏移位置
        var direction = context.getDirection();
        var orthogonalVector = new net.minecraft.util.math.Vec3i(-direction.getZ(), 0, direction.getX());
        
        // 判断是否为终点
        int totalSegments = (Integer) context.getProperty("totalSegments", 0);
        boolean isEnd = context.getSegmentIndex() != totalSegments;
        
        // 计算偏移位置
        var shiftedPos = isEnd ? 
            context.getPosition().add(orthogonalVector.multiply(2)) : 
            context.getPosition().subtract(orthogonalVector.multiply(2));
        
        // 获取距离文本
        String distanceText = context.getProperty("distanceText", "0").toString();
        
        return new DistanceSignDecoration(shiftedPos, orthogonalVector, context.getWorld(), isEnd, distanceText);
    }
    
    @Override
    public boolean shouldPlace(DecorationContext context) {
        // 只在起点和终点放置距离标志
        int segmentIndex = context.getSegmentIndex();
        int totalSegments = (Integer) context.getProperty("totalSegments", 0);
        
        return segmentIndex == 65 || segmentIndex == totalSegments - 65;
    }
    
    @Override
    public int getPlacementInterval() {
        return placementInterval;
    }
    
    /**
     * 设置放置间隔
     * @param interval 放置间隔
     */
    public void setPlacementInterval(int interval) {
        this.placementInterval = interval;
    }
}
