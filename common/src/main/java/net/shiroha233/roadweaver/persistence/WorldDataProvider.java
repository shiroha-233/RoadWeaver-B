package net.shiroha233.roadweaver.persistence;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.shiroha233.roadweaver.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 跨平台世界数据访问抽象（Common）。
 * 使用 @ExpectPlatform 提供平台端实现提供者。
 */
public abstract class WorldDataProvider {

    @ExpectPlatform
    public static WorldDataProvider getInstance() {
        throw new AssertionError();
    }

    // 结构位置
    public abstract Records.StructureLocationData getStructureLocations(ServerLevel level);
    public abstract void setStructureLocations(ServerLevel level, Records.StructureLocationData data);

    // 结构连接
    public abstract List<Records.StructureConnection> getStructureConnections(ServerLevel level);
    public abstract void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections);
    
    // 规划覆盖：tile 键集合与中心点映射
    public abstract Set<Long> getPlannedTileKeys(ServerLevel level);
    public abstract void setPlannedTileKeys(ServerLevel level, Set<Long> keys);
    public abstract Map<Long, Long> getPlannedTileCenters(ServerLevel level);
    public abstract void setPlannedTileCenters(ServerLevel level, Map<Long, Long> centers);
    
    // 便捷方法：添加单个结构位置
    public void addStructureLocation(ServerLevel level, BlockPos pos) {
        Records.StructureLocationData data = getStructureLocations(level);
        List<BlockPos> locations = new ArrayList<>(data.structureLocations());
        if (!locations.contains(pos)) {
            locations.add(pos);
            setStructureLocations(level, new Records.StructureLocationData(locations));
        }
    }
}
