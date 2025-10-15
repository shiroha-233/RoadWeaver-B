package net.countered.settlementroads.network;

import io.netty.buffer.Unpooled;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试数据包 - 传输结构、连接和道路数据
 */
public class DebugDataPacket {
    
    private final List<Records.StructureInfo> structureInfos;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;
    
    public DebugDataPacket(
        List<Records.StructureInfo> structureInfos,
        List<Records.StructureConnection> connections,
        List<Records.RoadData> roads
    ) {
        this.structureInfos = structureInfos != null ? structureInfos : new ArrayList<>();
        this.connections = connections != null ? connections : new ArrayList<>();
        this.roads = roads != null ? roads : new ArrayList<>();
    }
    
    public List<Records.StructureInfo> getStructureInfos() {
        return structureInfos;
    }
    
    public List<Records.StructureConnection> getConnections() {
        return connections;
    }
    
    public List<Records.RoadData> getRoads() {
        return roads;
    }
    
    /**
     * 编码数据包
     */
    public FriendlyByteBuf encode() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        // 写入结构信息
        buf.writeInt(structureInfos.size());
        for (Records.StructureInfo info : structureInfos) {
            buf.writeBlockPos(info.pos());
            buf.writeUtf(info.structureId());
        }
        
        // 写入连接信息
        buf.writeInt(connections.size());
        for (Records.StructureConnection conn : connections) {
            buf.writeBlockPos(conn.from());
            buf.writeBlockPos(conn.to());
            buf.writeUtf(conn.status().name());
            buf.writeBoolean(conn.manual());
        }
        
        // 写入道路数据
        buf.writeInt(roads.size());
        for (Records.RoadData road : roads) {
            buf.writeInt(road.width());
            buf.writeInt(road.roadType());
            
            // 写入道路段列表
            List<Records.RoadSegmentPlacement> segments = road.roadSegmentList();
            buf.writeInt(segments.size());
            for (Records.RoadSegmentPlacement segment : segments) {
                // 只传输中点位置（用于绘制道路路径）
                buf.writeBlockPos(segment.middlePos());
            }
        }
        
        return buf;
    }
    
    /**
     * 解码数据包
     */
    public static DebugDataPacket decode(FriendlyByteBuf buf) {
        // 读取结构信息
        int structureCount = buf.readInt();
        List<Records.StructureInfo> structureInfos = new ArrayList<>();
        for (int i = 0; i < structureCount; i++) {
            BlockPos pos = buf.readBlockPos();
            String structureId = buf.readUtf();
            structureInfos.add(new Records.StructureInfo(pos, structureId));
        }
        
        // 读取连接信息
        int connectionCount = buf.readInt();
        List<Records.StructureConnection> connections = new ArrayList<>();
        for (int i = 0; i < connectionCount; i++) {
            BlockPos from = buf.readBlockPos();
            BlockPos to = buf.readBlockPos();
            String statusStr = buf.readUtf();
            boolean manual = buf.readBoolean();
            Records.ConnectionStatus status = Records.ConnectionStatus.valueOf(statusStr);
            connections.add(new Records.StructureConnection(from, to, status, manual));
        }
        
        // 读取道路数据
        int roadCount = buf.readInt();
        List<Records.RoadData> roads = new ArrayList<>();
        for (int i = 0; i < roadCount; i++) {
            int width = buf.readInt();
            int roadType = buf.readInt();
            
            // 读取道路段列表
            int segmentCount = buf.readInt();
            List<Records.RoadSegmentPlacement> segments = new ArrayList<>();
            for (int j = 0; j < segmentCount; j++) {
                BlockPos middlePos = buf.readBlockPos();
                // 创建简化的段数据（只有中点，positions为空列表）
                segments.add(new Records.RoadSegmentPlacement(middlePos, new ArrayList<>()));
            }
            
            // 创建道路数据（materials为空列表，因为只用于显示）
            roads.add(new Records.RoadData(width, roadType, new ArrayList<>(), segments));
        }
        
        return new DebugDataPacket(structureInfos, connections, roads);
    }
}
