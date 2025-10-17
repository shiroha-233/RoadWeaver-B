package net.countered.settlementroads.decoration.factory;

import net.countered.settlementroads.decoration.DecorationContext;
import net.countered.settlementroads.decoration.DecorationFactory;
import net.countered.settlementroads.features.decoration.Decoration;
import net.countered.settlementroads.features.decoration.FenceWaypointDecoration;
import net.countered.settlementroads.road.RoadTypeConfig;

/**
 * 围栏路标装饰工厂
 * 负责创建围栏路标装饰实例
 */
public class FenceWaypointDecorationFactory implements DecorationFactory {
    
    private static final String DECORATION_ID = "fence_waypoint";
    private int placementInterval = 25;
    
    @Override
    public String getDecorationId() {
        return DECORATION_ID;
    }
    
    @Override
    public Decoration createDecoration(DecorationContext context) {
        var surfacePos = context.getPosition().withY(
            context.getWorld().getTopY(
                net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, 
                context.getPosition().getX(), 
                context.getPosition().getZ()));
        
        return new FenceWaypointDecoration(surfacePos, context.getWorld());
    }
    
    @Override
    public boolean shouldPlace(DecorationContext context) {
        // 根据道路类型配置决定是否放置围栏路标
        RoadTypeConfig roadType = context.getRoadType();
        return roadType != null && roadType.getProperties().containsKey("allowsFenceWaypoints") && 
               Boolean.TRUE.equals(roadType.getProperties().get("allowsFenceWaypoints"));
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
