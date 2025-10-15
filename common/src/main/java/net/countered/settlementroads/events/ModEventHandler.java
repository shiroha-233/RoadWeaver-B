package net.countered.settlementroads.events;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.features.RoadFeature;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.roadlogic.Road;
import net.countered.settlementroads.features.roadlogic.RoadPathCalculator;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.helpers.async.ThrottledStructureLocator;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;


/**
 * 使用 Architectury 事件的通用事件处理器（Common）。
 * 平台端无需各自实现，主类直接调用 ModEventHandler.register() 即可。
 */
public class ModEventHandler {

    private static final int THREAD_COUNT = 128;
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    private static final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    // 添加初始化延迟机制
    private static final ConcurrentHashMap<String, Integer> worldInitDelay = new ConcurrentHashMap<>();
    private static final int INIT_DELAY_TICKS = 100; // 5秒延迟，确保注册表完全加载

    public static void register() {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        // 世界加载
        LifecycleEvent.SERVER_LEVEL_LOAD.register(ModEventHandler::onWorldLoad);

        // 世界卸载
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(level -> {
            if (!level.dimension().equals(Level.OVERWORLD)) return;
            String worldKey = level.dimension().location().toString();
            Future<?> task = runningTasks.remove(worldKey);
            if (task != null && !task.isDone()) {
                task.cancel(true);
                LOGGER.debug("Aborted running road task for world: {}", level.dimension().location());
            }
            // 清理延迟计数器和队列
            worldInitDelay.remove(worldKey);
            StructureConnector.clearQueueForWorld(level);
            
            // 🧹 清理区块道路状态
            net.countered.settlementroads.chunk.ChunkRoadStateManager.clearWorld(level);
        });

        // 服务器 Tick（遍历所有世界）
        TickEvent.SERVER_PRE.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension().equals(Level.OVERWORLD)) {
                    // 处理限流结构搜寻队列
                    ThrottledStructureLocator.tickProcess(level);
                    // 尝试生成新道路
                    tryGenerateNewRoads(level, true, 5000);
                }
            }
        });

        // 服务器停止
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            LOGGER.info("RoadWeaver: Shutting down...");
            
            // 关闭限流结构定位器
            ThrottledStructureLocator.shutdown();
            
            // 清理道路生成相关资源
            RoadPathCalculator.heightCache.clear();
            runningTasks.values().forEach(future -> future.cancel(true));
            runningTasks.clear();
            executor.shutdownNow();
            
            LOGGER.info("RoadWeaver: Shutdown completed");
        });
    }

    private static void onWorldLoad(ServerLevel level) {
        restartExecutorIfNeeded();
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        // 初始化世界延迟计数器，确保注册表完全加载后再开始生成
        String worldKey = level.dimension().location().toString();
        worldInitDelay.put(worldKey, INIT_DELAY_TICKS);
        LOGGER.info("RoadWeaver: 世界 {} 已加载，将在 {} ticks 后开始道路生成", worldKey, INIT_DELAY_TICKS);

        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(level);

        // 恢复未完成的道路生成任务
        restoreUnfinishedRoads(level);

        IModConfig config = ConfigProvider.get();
        int currentCount = structureLocationData.structureLocations().size();
        int targetCount = config.initialLocatingCount();
        
        if (currentCount < targetCount) {
            int toLocate = targetCount - currentCount;
            LOGGER.info("🌍 Initializing world with {} structures (current: {}, target: {})", 
                toLocate, currentCount, targetCount);
            LOGGER.info("Using throttled search (max 1 per tick) to avoid server lag");
            
            // 使用异步方式搜寻结构，避免阻塞主线程
            for (int i = 0; i < toLocate; i++) {
                StructureConnector.cacheNewConnectionAsync(level, false);
            }
            
            LOGGER.info("✅ Initial structure search requests submitted (async)");
        }
    }

    private static void tryGenerateNewRoads(ServerLevel level, Boolean async, int steps) {
        String worldKey = level.dimension().location().toString();
        
        // 检查初始化延迟
        Integer delayTicks = worldInitDelay.get(worldKey);
        if (delayTicks != null) {
            if (delayTicks > 0) {
                worldInitDelay.put(worldKey, delayTicks - 1);
                return; // 还在延迟期内，跳过本次生成
            } else {
                // 延迟结束，移除计数器
                worldInitDelay.remove(worldKey);
                LOGGER.info("RoadWeaver: 世界 {} 初始化延迟结束，开始道路生成", worldKey);
            }
        }
        
        IModConfig config = ConfigProvider.get();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        // 清理已完成的任务（包括异常终止的）
        runningTasks.entrySet().removeIf(entry -> {
            Future<?> future = entry.getValue();
            if (future.isDone()) {
                try {
                    future.get(); // 检查是否有异常
                } catch (Exception e) {
                    LOGGER.warn("Task {} completed with error: {}", entry.getKey(), e.getMessage());
                }
                return true;
            }
            return false;
        });

        // 并发上限检查
        int currentRunning = runningTasks.size();
        if (currentRunning >= config.maxConcurrentRoadGeneration()) {
            return;
        }

        Queue<Records.StructureConnection> queue = StructureConnector.getQueueForWorld(level);
        if (!queue.isEmpty()) {
            // 仅窥视队列，确保在资源未就绪时不丢弃任务
            Records.StructureConnection structureConnection = queue.peek();
            if (structureConnection == null) {
                return; // 并发情况下可能为 null
            }
            
            // 增强的注册表检查
            final RoadFeatureConfig roadConfig = getRoadFeatureConfig(level);
            if (roadConfig == null) {
                // 注册表未就绪，等待下一个 tick
                LOGGER.debug("RoadWeaver: 注册表未就绪，等待下一个 tick（队列大小: {}）", 
                    queue.size());
                return;
            }

            // 现在确认资源可用，再真正弹出队列并开始任务
            queue.poll();
            LOGGER.info("🚧 Starting road generation: {} -> {} (running: {}/{}, queue: {})", 
                structureConnection.from(), structureConnection.to(), 
                currentRunning + 1, config.maxConcurrentRoadGeneration(),
                queue.size());
            if (async) {
                String taskId = level.dimension().location().toString() + "_" + System.nanoTime();
                Future<?> future = executor.submit(() -> {
                    try {
                        LOGGER.debug("🔨 Generating road: {} -> {}", 
                            structureConnection.from(), structureConnection.to());
                        new Road(level, structureConnection, roadConfig).generateRoad(steps);
                        LOGGER.info("✅ Road generation completed: {} -> {}", 
                            structureConnection.from(), structureConnection.to());
                    } catch (Exception e) {
                        LOGGER.error("❌ Error generating road {} -> {}: {}", 
                            structureConnection.from(), structureConnection.to(), 
                            e.getMessage(), e);
                        
                        // 异常时标记为 FAILED，避免重试
                        try {
                            markConnectionAsFailed(level, structureConnection);
                        } catch (Exception ex) {
                            LOGGER.error("Failed to mark connection as failed", ex);
                        }
                    } finally {
                        runningTasks.remove(taskId);
                    }
                });
                runningTasks.put(taskId, future);
            } else {
                try {
                    new Road(level, structureConnection, roadConfig).generateRoad(steps);
                } catch (Exception e) {
                    LOGGER.error("❌ Error generating road: {}", e.getMessage(), e);
                    markConnectionAsFailed(level, structureConnection);
                }
            }
        }
    }

    /**
     * 获取道路特性配置，包含健壮的注册表检查
     * @param level 服务器世界
     * @return 配置对象，如果注册表未就绪则返回 null
     */
    private static RoadFeatureConfig getRoadFeatureConfig(ServerLevel level) {
        try {
            // 检查注册表是否可用
            if (level.registryAccess() == null) {
                LOGGER.debug("RoadWeaver: RegistryAccess is null");
                return null;
            }
            
            var registry = level.registryAccess().registry(Registries.CONFIGURED_FEATURE);
            if (registry.isEmpty()) {
                LOGGER.debug("RoadWeaver: ConfiguredFeature registry is not available");
                return null;
            }
            
            ConfiguredFeature<?, ?> feature = registry.get().get(RoadFeature.ROAD_FEATURE_KEY);
            if (feature != null && feature.config() instanceof RoadFeatureConfig cfg) {
                LOGGER.debug("RoadWeaver: Using registered RoadFeatureConfig");
                return cfg;
            } else {
                // 使用 fallback 配置
                LOGGER.debug("RoadWeaver: ConfiguredFeature {} missing or invalid, using fallback", 
                    RoadFeature.ROAD_FEATURE_KEY.location());
                return defaultRoadConfig();
            }
        } catch (Exception e) {
            LOGGER.debug("RoadWeaver: Exception while getting RoadFeatureConfig: {}", e.getMessage());
            return null; // 注册表未就绪，返回 null 等待下一个 tick
        }
    }

    private static RoadFeatureConfig defaultRoadConfig() {
        // 与 datagen 中的默认配置保持一致
        List<List<BlockState>> artificialMaterials = List.of(
                List.of(Blocks.MUD_BRICKS.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                List.of(Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState())
        );

        List<List<BlockState>> naturalMaterials = List.of(
                List.of(Blocks.COARSE_DIRT.defaultBlockState(), Blocks.ROOTED_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState()),
                List.of(Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.CRACKED_STONE_BRICKS.defaultBlockState()),
                List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.COARSE_DIRT.defaultBlockState(), Blocks.PACKED_MUD.defaultBlockState())
        );

        List<Integer> widths = List.of(3);
        List<Integer> qualities = List.of(1,2,3,4,5,6,7,8,9);
        return new RoadFeatureConfig(artificialMaterials, naturalMaterials, widths, qualities);
    }

    /**
     * 标记连接为失败状态
     */
    private static void markConnectionAsFailed(ServerLevel level, Records.StructureConnection structureConnection) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = dataProvider.getStructureConnections(level);
        List<Records.StructureConnection> mutableConnections = new ArrayList<>(connections != null ? connections : new ArrayList<>());
        
        for (int i = 0; i < mutableConnections.size(); i++) {
            Records.StructureConnection conn = mutableConnections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                mutableConnections.set(i, new Records.StructureConnection(
                    conn.from(), conn.to(), Records.ConnectionStatus.FAILED, conn.manual()));
                dataProvider.setStructureConnections(level, mutableConnections);
                LOGGER.info("Marked connection as FAILED: {} -> {}", conn.from(), conn.to());
                break;
            }
        }
    }

    private static void restartExecutorIfNeeded() {
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newFixedThreadPool(THREAD_COUNT);
            LOGGER.debug("RoadWeaver: ExecutorService restarted.");
        }
    }

    /**
     * 恢复未完成的道路生成任务
     * 在世界加载时调用，将所有 PLANNED 和 GENERATING 状态的连接重新加入队列
     * FAILED 和 COMPLETED 状态不处理
     */
    private static void restoreUnfinishedRoads(ServerLevel level) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> connections = dataProvider.getStructureConnections(level);

        int restoredCount = 0;
        List<Records.StructureConnection> updatedConnections = new ArrayList<>(connections);
        boolean needsUpdate = false;
        
        for (int i = 0; i < updatedConnections.size(); i++) {
            Records.StructureConnection connection = updatedConnections.get(i);
            
            // 恢复 PLANNED 和 GENERATING 状态的连接
            if (connection.status() == Records.ConnectionStatus.PLANNED ||
                connection.status() == Records.ConnectionStatus.GENERATING) {

                // 将 GENERATING 状态重置为 PLANNED（意外中断的任务）
                if (connection.status() == Records.ConnectionStatus.GENERATING) {
                    Records.StructureConnection resetConnection = new Records.StructureConnection(
                            connection.from(),
                            connection.to(),
                            Records.ConnectionStatus.PLANNED,
                            connection.manual()
                    );
                    updatedConnections.set(i, resetConnection);
                    StructureConnector.getQueueForWorld(level).add(resetConnection);
                    needsUpdate = true;
                } else {
                    // PLANNED 状态直接加入队列
                    StructureConnector.getQueueForWorld(level).add(connection);
                }
                restoredCount++;
            }
            // COMPLETED 和 FAILED 状态不处理
        }

        // 批量更新连接状态
        if (needsUpdate) {
            dataProvider.setStructureConnections(level, updatedConnections);
        }

        if (restoredCount > 0) {
            LOGGER.info("RoadWeaver: 恢复了 {} 个未完成的道路生成任务（队列大小: {}）", 
                restoredCount, StructureConnector.getQueueForWorld(level).size());
        }
    }
}
