package net.countered.settlementroads.persistence.forge;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge 端便捷访问工具。
 * 注意：客户端(CLIENT)世界为 ClientLevel，不可直接持久化访问，若传入非 ServerLevel 将返回空数据。
 */
public final class WorldDataHelper {
    private WorldDataHelper() {}

    public static Records.StructureLocationData getStructureLocations(Level level) {
        if (level instanceof ServerLevel server) {
            return WorldDataProvider.getInstance().getStructureLocations(server);
        }
        return new Records.StructureLocationData(new ArrayList<>());
    }

    public static List<Records.StructureConnection> getConnectedStructures(Level level) {
        if (level instanceof ServerLevel server) {
            return WorldDataProvider.getInstance().getStructureConnections(server);
        }
        return new ArrayList<>();
    }

    public static List<Records.RoadData> getRoadDataList(Level level) {
        if (level instanceof ServerLevel server) {
            return WorldDataProvider.getInstance().getRoadDataList(server);
        }
        return new ArrayList<>();
    }

    public static void setStructureLocations(Level level, Records.StructureLocationData data) {
        if (level instanceof ServerLevel server) {
            WorldDataProvider.getInstance().setStructureLocations(server, data);
        }
    }

    public static void setStructureConnections(Level level, List<Records.StructureConnection> connections) {
        if (level instanceof ServerLevel server) {
            WorldDataProvider.getInstance().setStructureConnections(server, connections);
        }
    }

    public static void setRoadDataList(Level level, List<Records.RoadData> roadDataList) {
        if (level instanceof ServerLevel server) {
            WorldDataProvider.getInstance().setRoadDataList(server, roadDataList);
        }
    }
}