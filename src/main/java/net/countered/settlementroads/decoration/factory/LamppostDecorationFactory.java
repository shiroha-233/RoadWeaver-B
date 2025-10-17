package net.countered.settlementroads.decoration.factory;

import net.countered.settlementroads.decoration.DecorationContext;
import net.countered.settlementroads.decoration.DecorationFactory;
import net.countered.settlementroads.features.decoration.Decoration;
import net.countered.settlementroads.features.decoration.LamppostDecoration;
import net.countered.settlementroads.road.RoadTypeConfig;

/**
 * 路灯装饰工厂
 * 负责创建路灯装饰实例
 */
public class LamppostDecorationFactory implements DecorationFactory {
    
    private static final String DECORATION_ID = "lamppost";
    private int placementInterval = 59;
    
    @Override
    public String getDecorationId() {
        return DECORATION_ID;
    }
    
    @Override
    public Decoration createDecoration(DecorationContext context) {
        // 计算偏移位置
        var direction = context.getDirection();
        var orthogonalVector = new net.minecraft.util.math.Vec3i(-direction.getZ(), 0, direction.getX());
        
        // 随机选择左右侧
        boolean leftRoadSide = context.getRandom().nextBoolean();
        var shiftedPos = leftRoadSide ? 
            context.getPosition().add(orthogonalVector.multiply(2)) : 
            context.getPosition().subtract(orthogonalVector.multiply(2));
        
        // 调整到地表
        shiftedPos = shiftedPos.withY(context.getWorld().getTopY(
            net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, 
            shiftedPos.getX(), shiftedPos.getZ()));
        
        // 检查高度差
        if (Math.abs(shiftedPos.getY() - context.getPosition().getY()) > 1) {
            return null;
        }
        
        return new LamppostDecoration(shiftedPos, orthogonalVector, context.getWorld(), leftRoadSide);
    }
    
    @Override
    public boolean shouldPlace(DecorationContext context) {
        // 根据道路类型配置决定是否放置路灯
        RoadTypeConfig roadType = context.getRoadType();
        return roadType != null && roadType.getProperties().containsKey("allowsLampposts") && 
               Boolean.TRUE.equals(roadType.getProperties().get("allowsLampposts"));
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
