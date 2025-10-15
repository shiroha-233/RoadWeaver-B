package net.countered.settlementroads.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 管理区块的道路生成状态
 * 控制区块发送给玩家的时机，确保道路生成完成后再发送
 */
public class ChunkRoadStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    
    // 超时时间（秒）- 避免某些区块永远不发送
    private static final int TIMEOUT_SECONDS = 30;
    
    // 按世界维度存储待处理区块
    // Key: 世界维度 ID, Value: ChunkPos -> ChunkRoadState
    private static final Map<String, Map<ChunkPos, ChunkRoadState>> worldChunkStates = new ConcurrentHashMap<>();
    
    /**
     * 区块道路状态
     */
    public static class ChunkRoadState {
        private boolean roadProcessed = false;
        private final List<Runnable> releaseCallbacks = new ArrayList<>();
        private final long createdTime = System.currentTimeMillis();
        
        public synchronized void addReleaseCallback(Runnable callback) {
            if (roadProcessed) {
                callback.run(); // 已经完成，直接执行
            } else {
                releaseCallbacks.add(callback);
            }
        }
        
        public synchronized void markProcessed() {
            if (roadProcessed) return; // 防止重复执行
            
            this.roadProcessed = true;
            for (Runnable callback : releaseCallbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    LOGGER.error("Error executing release callback", e);
                }
            }
            releaseCallbacks.clear();
        }
        
        public boolean isProcessed() {
            return roadProcessed;
        }
        
        public boolean isTimedOut() {
            return System.currentTimeMillis() - createdTime > TIMEOUT_SECONDS * 1000L;
        }
    }
    
    /**
     * 标记区块需要等待道路生成
     * @param level 服务器世界
     * @param pos 区块位置
     */
    public static void markChunkPendingRoad(ServerLevel level, ChunkPos pos) {
        String worldKey = level.dimension().location().toString();
        Map<ChunkPos, ChunkRoadState> chunkMap = worldChunkStates.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>());
        
        chunkMap.computeIfAbsent(pos, k -> {
            LOGGER.debug("🔒 Marking chunk {} as pending road generation", pos);
            ChunkRoadState state = new ChunkRoadState();
            
            // 设置超时保护
            CompletableFuture.delayedExecutor(TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
                if (!state.isProcessed()) {
                    LOGGER.warn("⏰ Chunk {} timed out waiting for road generation, releasing anyway", pos);
                    markChunkRoadProcessed(level, pos);
                }
            });
            
            return state;
        });
    }
    
    /**
     * 批量标记多个区块需要等待道路生成
     * @param level 服务器世界
     * @param positions 区块位置列表
     */
    public static void markChunksPendingRoad(ServerLevel level, Collection<ChunkPos> positions) {
        for (ChunkPos pos : positions) {
            markChunkPendingRoad(level, pos);
        }
    }
    
    /**
     * 检查区块是否需要等待道路生成
     * @param level 服务器世界
     * @param pos 区块位置
     * @return true 如果区块正在等待道路生成
     */
    public static boolean isChunkPendingRoad(ServerLevel level, ChunkPos pos) {
        if (level.getDayTime() < 1000)
            return true;
        else
            return false;
//        String worldKey = level.dimension().location().toString();
//        Map<ChunkPos, ChunkRoadState> chunkMap = worldChunkStates.get(worldKey);
//        if (chunkMap == null) return false;
//
//        ChunkRoadState state = chunkMap.get(pos);
//        if (state == null) return false;
//
//        // 检查是否超时
//        if (state.isTimedOut() && !state.isProcessed()) {
//            LOGGER.warn("⏰ Chunk {} timed out in check, marking as processed", pos);
//            markChunkRoadProcessed(level, pos);
//            return false;
//        }
//
//        return !state.isProcessed();
    }
    
    /**
     * 标记区块的道路已处理完成，触发释放回调
     * @param level 服务器世界
     * @param pos 区块位置
     */
    public static void markChunkRoadProcessed(ServerLevel level, ChunkPos pos) {
        String worldKey = level.dimension().location().toString();
        Map<ChunkPos, ChunkRoadState> chunkMap = worldChunkStates.get(worldKey);
        if (chunkMap == null) return;
        
        ChunkRoadState state = chunkMap.get(pos);
        if (state != null) {
            LOGGER.debug("✅ Marking chunk {} road as processed", pos);
            state.markProcessed();
            
            // 延迟移除，避免并发问题
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
                .execute(() -> chunkMap.remove(pos));
        }
    }
    
    /**
     * 批量标记多个区块的道路已处理完成
     * @param level 服务器世界
     * @param positions 区块位置列表
     */
    public static void markChunksRoadProcessed(ServerLevel level, Collection<ChunkPos> positions) {
        LOGGER.info("✅ Releasing {} chunks after road generation", positions.size());
        for (ChunkPos pos : positions) {
            markChunkRoadProcessed(level, pos);
        }
    }
    
    /**
     * 注册区块释放回调（当道路生成完成时调用）
     * @param level 服务器世界
     * @param pos 区块位置
     * @param callback 回调函数
     */
    public static void registerReleaseCallback(ServerLevel level, ChunkPos pos, Runnable callback) {
        String worldKey = level.dimension().location().toString();
        Map<ChunkPos, ChunkRoadState> chunkMap = worldChunkStates.get(worldKey);
        if (chunkMap == null) {
            callback.run(); // 没有待处理状态，直接执行
            return;
        }
        
        ChunkRoadState state = chunkMap.get(pos);
        if (state == null) {
            callback.run(); // 没有待处理状态，直接执行
        } else {
            state.addReleaseCallback(callback);
        }
    }
    
    /**
     * 从道路段数据中提取所有受影响的区块
     * @param roadSegments 道路段列表
     * @return 受影响的区块位置集合
     */
    public static Set<ChunkPos> extractAffectedChunks(List<net.countered.settlementroads.helpers.Records.RoadSegmentPlacement> roadSegments) {
        Set<ChunkPos> affectedChunks = new HashSet<>();
        for (net.countered.settlementroads.helpers.Records.RoadSegmentPlacement segment : roadSegments) {
            // 中心点
            ChunkPos centerChunk = new ChunkPos(segment.middlePos());
            affectedChunks.add(centerChunk);
            
            // 宽度点
            for (BlockPos widthPos : segment.positions()) {
                ChunkPos widthChunk = new ChunkPos(widthPos);
                affectedChunks.add(widthChunk);
            }
        }
        return affectedChunks;
    }
    
    /**
     * 清理世界的所有状态（世界卸载时调用）
     * @param level 服务器世界
     */
    public static void clearWorld(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        Map<ChunkPos, ChunkRoadState> removed = worldChunkStates.remove(worldKey);
        if (removed != null && !removed.isEmpty()) {
            // 强制释放所有待处理的区块
            for (Map.Entry<ChunkPos, ChunkRoadState> entry : removed.entrySet()) {
                entry.getValue().markProcessed();
            }
            LOGGER.info("🧹 Cleared {} pending chunks for world {}", removed.size(), worldKey);
        }
    }
    
    /**
     * 获取待处理区块数量（调试用）
     * @param level 服务器世界
     * @return 待处理区块数量
     */
    public static int getPendingCount(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        Map<ChunkPos, ChunkRoadState> chunkMap = worldChunkStates.get(worldKey);
        return chunkMap != null ? (int) chunkMap.values().stream().filter(s -> !s.isProcessed()).count() : 0;
    }
    
    /**
     * 获取所有待处理区块（调试用）
     * @param level 服务器世界
     * @return 待处理区块位置列表
     */
    public static List<ChunkPos> getPendingChunks(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        Map<ChunkPos, ChunkRoadState> chunkMap = worldChunkStates.get(worldKey);
        if (chunkMap == null) return Collections.emptyList();
        
        List<ChunkPos> pending = new ArrayList<>();
        for (Map.Entry<ChunkPos, ChunkRoadState> entry : chunkMap.entrySet()) {
            if (!entry.getValue().isProcessed()) {
                pending.add(entry.getKey());
            }
        }
        return pending;
    }
}

