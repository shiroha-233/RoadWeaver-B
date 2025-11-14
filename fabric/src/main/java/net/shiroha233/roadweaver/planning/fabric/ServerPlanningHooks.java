package net.shiroha233.roadweaver.planning.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.planning.RoadPlanningService;
import net.shiroha233.roadweaver.generation.RoadGenerationService;
import net.shiroha233.roadweaver.generation.InitialGenManager;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.util.ComputeService;
import net.shiroha233.roadweaver.persistence.sharded.RoadShardStorage;
import net.shiroha233.roadweaver.structures.StructureSystem;
import net.shiroha233.roadweaver.structures.index.StructureIndexRestorer;

public final class ServerPlanningHooks {
    private ServerPlanningHooks() {}

    private static int tick;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            StructureSystem.clearAll();
            net.shiroha233.roadweaver.runtime.ThreadPoolManager.onServerStarted(server);
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level == null) return;
            // 从持久化数据恢复结构索引（例如出生点小屋等结构实例）
            StructureIndexRestorer.restore(level);
            boolean dedicated = server.isDedicatedServer();
            if (dedicated) {
                RoadGenerationService.onServerStarted();
                RoadPlanningService.initialPlanAsync(level);
                return;
            }
            java.util.List<net.shiroha233.roadweaver.helpers.Records.StructureConnection> conns = WorldDataProvider.getInstance().getStructureConnections(level);
            if (conns == null || conns.isEmpty()) {
                InitialGenManager.begin(level);
                InitialGenManager.blockUntilDone(level);
            } else {
                RoadGenerationService.onServerStarted();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if ((tick++ % 20) == 0) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    RoadPlanningService.planAroundPlayer(p);
                }
            }
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                RoadGenerationService.tick(level);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerLevel lvl : server.getAllLevels()) {
                RoadShardStorage.flushAll(lvl);
                RoadShardStorage.clearAll(lvl);
            }
            RoadGenerationService.onServerStopping();
            StructureSystem.clearAll();
            ComputeService.shutdownNow();
        });
    }
}
