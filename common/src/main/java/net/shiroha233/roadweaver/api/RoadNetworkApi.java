package net.shiroha233.roadweaver.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.generation.RoadGenerationService;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.helpers.StructureConnector;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.planning.RoadPlanningService;

import java.util.ArrayList;
import java.util.List;

public final class RoadNetworkApi {
    private RoadNetworkApi() {}

    public static void registerStructureEndpoint(ServerLevel level, BlockPos pos) {
        registerStructureEndpoint(level, pos, null, false);
    }

    public static void registerStructureEndpoint(ServerLevel level, BlockPos pos, boolean autoConnect) {
        registerStructureEndpoint(level, pos, null, autoConnect);
    }

    public static void registerStructureEndpoint(ServerLevel level, BlockPos pos, String structureId, boolean autoConnect) {
        if (level == null || pos == null) return;
        WorldDataProvider provider = WorldDataProvider.getInstance();
        Records.StructureLocationData existing = provider.getStructureLocations(level);
        List<BlockPos> locations = existing != null ? new ArrayList<>(existing.structureLocations()) : new ArrayList<>();
        List<Records.StructureInfo> infos = existing != null ? new ArrayList<>(existing.structureInfos()) : new ArrayList<>();

        if (structureId != null && !structureId.isEmpty()) {
            Records.StructureInfo info = new Records.StructureInfo(pos, structureId);
            infos.add(info);
            if (!locations.contains(pos)) {
                locations.add(pos);
            }
        } else {
            if (!locations.contains(pos)) {
                locations.add(pos);
            }
        }

        Records.StructureLocationData updated = new Records.StructureLocationData(locations, infos);
        provider.setStructureLocations(level, updated);

        if (autoConnect) {
            StructureConnector.cacheNewConnection(level, true);
        }
    }

    public static void ensureConnection(ServerLevel level, BlockPos from, BlockPos to) {
        ensureConnection(level, from, to, false);
    }

    public static void ensureConnection(ServerLevel level, BlockPos from, BlockPos to, boolean generateImmediately) {
        if (level == null || from == null || to == null) return;
        if (from.equals(to)) return;

        WorldDataProvider provider = WorldDataProvider.getInstance();
        Records.StructureLocationData existing = provider.getStructureLocations(level);
        List<BlockPos> locations = existing != null ? new ArrayList<>(existing.structureLocations()) : new ArrayList<>();
        List<Records.StructureInfo> infos = existing != null ? new ArrayList<>(existing.structureInfos()) : new ArrayList<>();

        boolean changed = false;
        if (!locations.contains(from)) {
            locations.add(from);
            changed = true;
        }
        if (!locations.contains(to)) {
            locations.add(to);
            changed = true;
        }
        if (changed) {
            Records.StructureLocationData updated = new Records.StructureLocationData(locations, infos);
            provider.setStructureLocations(level, updated);
        }

        List<Records.StructureConnection> existingConns = provider.getStructureConnections(level);
        List<Records.StructureConnection> list = existingConns != null ? new ArrayList<>(existingConns) : new ArrayList<>();
        boolean exists = false;
        for (Records.StructureConnection c : list) {
            if (sameEdge(c, from, to)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            list.add(new Records.StructureConnection(from, to, Records.ConnectionStatus.PLANNED));
            provider.setStructureConnections(level, list);
        }

        if (generateImmediately) {
            Records.StructureConnection conn = new Records.StructureConnection(from, to, Records.ConnectionStatus.PLANNED);
            RoadGenerationService.generateInline(level, conn);
        }
    }

    public static void planRegion(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        if (level == null) return;
        RoadPlanningService.planRectAsync(level, minBlockX, minBlockZ, maxBlockX, maxBlockZ);
    }

    private static boolean sameEdge(Records.StructureConnection c, BlockPos a, BlockPos b) {
        BlockPos af = c.from();
        BlockPos at = c.to();
        return (af.equals(a) && at.equals(b)) || (af.equals(b) && at.equals(a));
    }
}
