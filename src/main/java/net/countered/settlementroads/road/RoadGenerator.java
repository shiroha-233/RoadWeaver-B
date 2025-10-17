package net.countered.settlementroads.road;

import net.countered.settlementroads.config.RoadWeaverConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * New road generator
 * Uses configuration-driven road type system
 */
public class RoadGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-RoadGenerator");
    
    private final ServerWorld serverWorld;
    private final Records.StructureConnection structureConnection;
    private final RoadWeaverConfig config;
    
    public RoadGenerator(ServerWorld serverWorld, Records.StructureConnection structureConnection, RoadWeaverConfig config) {
        this.serverWorld = serverWorld;
        this.structureConnection = structureConnection;
        this.config = config;
    }
    
    /**
     * Generate road
     * @param maxSteps Maximum steps
     */
    public void generateRoad(int maxSteps) {
        updateConnectionStatus(Records.ConnectionStatus.GENERATING);
        
        try {
            Random random = Random.create();
            
            // Select road type
            RoadTypeConfig roadType = config.getRoadTypeRegistry().getRandomRoadType(new java.util.Random(random.nextLong()));
            if (roadType == null) {
                LOGGER.warn("No suitable road type found for connection from {} to {}", 
                           structureConnection.from(), structureConnection.to());
                updateConnectionStatus(Records.ConnectionStatus.FAILED);
                return;
            }
            
            // Get road parameters
            int width = getRandomWidth(random, roadType);
            List<BlockState> materials = roadType.getRandomMaterials(new java.util.Random(random.nextLong()));
            
            BlockPos start = structureConnection.from();
            BlockPos end = structureConnection.to();
            
            // Calculate path
            ConfigurableRoadPathCalculator pathCalculator = new ConfigurableRoadPathCalculator(config);
            List<Records.RoadSegmentPlacement> roadSegmentPlacementList = pathCalculator.calculateAStarRoadPath(
                start, end, width, serverWorld, maxSteps);
            
            // Check if generation failed (path is empty)
            if (roadSegmentPlacementList.isEmpty()) {
                updateConnectionStatus(Records.ConnectionStatus.FAILED);
                return;
            }
            
            // 保存道路数据
            List<Records.RoadData> roadDataList = new ArrayList<>(
                serverWorld.getAttachedOrCreate(WorldDataAttachment.ROAD_DATA_LIST, ArrayList::new));
            
            // 使用道路类型ID作为类型标识
            String roadTypeId = roadType.getId();
            roadDataList.add(new Records.RoadData(width, roadTypeId, materials, roadSegmentPlacementList));
            
            serverWorld.setAttached(WorldDataAttachment.ROAD_DATA_LIST, roadDataList);
            
            // 道路生成完成，更新状态为"已完成"
            updateConnectionStatus(Records.ConnectionStatus.COMPLETED);
            
            LOGGER.info("Generated road of type '{}' from {} to {}", 
                       roadType.getId(), start, end);
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate road from {} to {}: {}", 
                       structureConnection.from(), structureConnection.to(), e.getMessage(), e);
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
        }
    }
    
    /**
     * 更新连接状态
     * @param newStatus 新状态
     */
    private void updateConnectionStatus(Records.ConnectionStatus newStatus) {
        List<Records.StructureConnection> connections = new ArrayList<>(
            serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new)
        );
        
        // 查找并更新对应的连接
        for (int i = 0; i < connections.size(); i++) {
            Records.StructureConnection conn = connections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                // 更新状态
                connections.set(i, new Records.StructureConnection(conn.from(), conn.to(), newStatus));
                serverWorld.setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
                break;
            }
        }
    }
    
    /**
     * 获取随机宽度
     * @param random 随机数生成器
     * @param roadType 道路类型
     * @return 道路宽度
     */
    private int getRandomWidth(Random random, RoadTypeConfig roadType) {
        List<Integer> widths = roadType.getWidths();
        if (widths.isEmpty()) {
            return 3; // 默认宽度
        }
        return widths.get(random.nextInt(widths.size()));
    }
    
}
