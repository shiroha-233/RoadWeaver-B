package net.countered.settlementroads.network;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据包处理器 - 处理网络数据包的业务逻辑
 */
public class PacketHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    
    // 存储接收到的调试数据（客户端）
    private static DebugDataPacket cachedDebugData = null;
    
    /**
     * 服务器端：处理客户端的调试数据请求
     */
    public static void handleDebugDataRequest(ServerPlayer player) {
        try {
            ServerLevel world = player.serverLevel();
            WorldDataProvider dataProvider = WorldDataProvider.getInstance();
            
            // 获取世界数据
            Records.StructureLocationData structureData = dataProvider.getStructureLocations(world);
            List<Records.StructureConnection> connections = dataProvider.getStructureConnections(world);
            List<Records.RoadData> roads = dataProvider.getRoadDataList(world);
            
            List<Records.StructureInfo> structureInfos = structureData != null ? 
                new ArrayList<>(structureData.structureInfos()) : new ArrayList<>();
            
            // 创建数据包并发送
            DebugDataPacket packet = new DebugDataPacket(structureInfos, connections, roads);
            RoadWeaverNetworkManager.sendDebugData(player, packet);
            
            LOGGER.info("Sent debug data to player {}: {} structures, {} connections, {} roads",
                player.getName().getString(), structureInfos.size(), connections.size(), roads.size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to send debug data to player {}", player.getName().getString(), e);
            // 发送空数据
            DebugDataPacket emptyPacket = new DebugDataPacket(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            RoadWeaverNetworkManager.sendDebugData(player, emptyPacket);
        }
    }
    
    /**
     * 客户端：处理服务器发送的调试数据
     */
    public static void handleDebugDataResponse(DebugDataPacket packet) {
        cachedDebugData = packet;
        LOGGER.info("Received debug data: {} structures, {} connections, {} roads",
            packet.getStructureInfos().size(), 
            packet.getConnections().size(), 
            packet.getRoads().size());
    }
    
    /**
     * 客户端：获取缓存的调试数据
     */
    public static DebugDataPacket getCachedDebugData() {
        return cachedDebugData;
    }
    
    /**
     * 客户端：清除缓存的调试数据
     */
    public static void clearCachedDebugData() {
        cachedDebugData = null;
    }
}
