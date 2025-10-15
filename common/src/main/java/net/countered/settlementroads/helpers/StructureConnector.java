 package net.countered.settlementroads.helpers;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import net.minecraft.core.BlockPos;
 import net.minecraft.server.level.ServerLevel;
 
 import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
 
import net.countered.settlementroads.helpers.async.ThrottledStructureLocator;
import net.countered.settlementroads.persistence.WorldDataProvider;
 
public class StructureConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    // 按世界维度区分的队列存储
    private static final ConcurrentHashMap<String, Queue<Records.StructureConnection>> worldQueues = new ConcurrentHashMap<>();
    
    /**
     * 获取指定世界的连接队列
     */
    public static Queue<Records.StructureConnection> getQueueForWorld(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        return worldQueues.computeIfAbsent(worldKey, k -> new ConcurrentLinkedQueue<>());
    }
    
    /**
     * 清理指定世界的队列
     */
    public static void clearQueueForWorld(ServerLevel level) {
        String worldKey = level.dimension().location().toString();
        Queue<Records.StructureConnection> queue = worldQueues.remove(worldKey);
        if (queue != null) {
            queue.clear();
            LOGGER.debug("Cleared queue for world: {}", worldKey);
        }
    }
 
    /**
     * 同步方式缓存新连接（已弃用 - 会阻塞主线程）
     * @deprecated 使用 {@link #cacheNewConnectionAsync(ServerLevel, boolean)} 代替
     */
    @Deprecated
    public static void cacheNewConnection(ServerLevel serverWorld, boolean locateAtPlayer) {
        LOGGER.warn("⚠️ Using deprecated synchronous structure search - this may cause lag!");
        LOGGER.warn("⚠️ Consider using cacheNewConnectionAsync() instead");
        
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        int beforeCount = dataProvider.getStructureLocations(serverWorld).structureLocations().size();
        
        StructureLocator.locateConfiguredStructure(serverWorld, 1, locateAtPlayer);
        
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
        if (structureLocationData == null) {
            LOGGER.warn(" structureLocationData is null");
            return;
        }
        
        List<BlockPos> locations = structureLocationData.structureLocations();
        int afterCount = locations.size();
        LOGGER.debug("Structure count: before={}, after={}", beforeCount, afterCount);
        
        if (locations == null || locations.size() < 2) {
            LOGGER.debug(" Not enough structures to create connection (need 2, have {})", locations.size());
            return;
        }
        
        createNewStructureConnection(serverWorld);
    }
    
    /**
     * 异步方式缓存新连接（推荐使用 - 分批处理，避免长时间阻塞）
     * 使用限流机制，每个tick只处理少量搜寻请求
     * 
     * @param serverWorld 服务器世界
     * @param locateAtPlayer 是否在玩家位置搜寻
     */
    public static void cacheNewConnectionAsync(ServerLevel serverWorld, boolean locateAtPlayer) {
        LOGGER.debug("🔍 Queuing structure search request, locateAtPlayer={}", locateAtPlayer);
        
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        int beforeCount = dataProvider.getStructureLocations(serverWorld).structureLocations().size();
        
        // 使用限流定位器（在主线程分批处理）
        ThrottledStructureLocator.locateAsync(serverWorld, 1, locateAtPlayer, results -> {
            // 这个回调在主线程中执行
            if (results.isEmpty()) {
                LOGGER.debug("Structure search found no new structures");
                return;
            }
            
            Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
            if (structureLocationData == null) {
                LOGGER.warn("structureLocationData is null after search");
                return;
            }
            
            List<BlockPos> locations = structureLocationData.structureLocations();
            int afterCount = locations.size();
            LOGGER.debug("✅ Structure search completed: before={}, after={}, found={}", 
                beforeCount, afterCount, results.size());
            
            if (locations.size() >= 2) {
                createNewStructureConnection(serverWorld);
            } else {
                LOGGER.debug("Not enough structures to create connection (need 2, have {})", locations.size());
            }
        });
    }
    
    private static void createNewStructureConnection(ServerLevel serverWorld) {
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        Records.StructureLocationData structureLocationData = dataProvider.getStructureLocations(serverWorld);
        if (structureLocationData == null) return;
        List<BlockPos> worldStructureLocations = structureLocationData.structureLocations();
        if (worldStructureLocations == null || worldStructureLocations.size() < 2) return;
 
        BlockPos latestVillagePos = worldStructureLocations.get(worldStructureLocations.size() - 1);
        BlockPos closestVillage = findClosestStructure(latestVillagePos, worldStructureLocations);
 
        if (closestVillage != null) {
            List<Records.StructureConnection> connections = new ArrayList<>(
                    Optional.ofNullable(dataProvider.getStructureConnections(serverWorld)).orElseGet(ArrayList::new)
            );
            if (!connectionExists(connections, latestVillagePos, closestVillage)) {
                Records.StructureConnection structureConnection = new Records.StructureConnection(latestVillagePos, closestVillage);
                connections.add(structureConnection);
                dataProvider.setStructureConnections(serverWorld, connections);
                Queue<Records.StructureConnection> queue = getQueueForWorld(serverWorld);
                queue.add(structureConnection);
                double distance = Math.sqrt(latestVillagePos.distSqr(closestVillage));
                LOGGER.info(" Created connection between {} and {} (distance: {} blocks, queue size: {})",
                        latestVillagePos, closestVillage, (int) Math.round(distance), queue.size());
            } else {
                LOGGER.debug("Connection already exists between {} and {}", latestVillagePos, closestVillage);
            }
        } else {
            LOGGER.warn(" Could not find closest structure for {}", latestVillagePos);
        }
    }
    
    private static boolean connectionExists(List<Records.StructureConnection> existingConnections, BlockPos a, BlockPos b) {
        for (Records.StructureConnection connection : existingConnections) {
            if ((connection.from().equals(a) && connection.to().equals(b)) ||
                (connection.from().equals(b) && connection.to().equals(a))) {
                return true;
            }
        }
        return false;
    }
    
    private static BlockPos findClosestStructure(BlockPos currentVillage, List<BlockPos> allVillages) {
        BlockPos closestVillage = null;
        double minDistance = Double.MAX_VALUE;
        for (BlockPos village : allVillages) {
            if (!village.equals(currentVillage)) {
                double distance = currentVillage.distSqr(village);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestVillage = village;
                }
            }
        }
        return closestVillage;
    }
}
