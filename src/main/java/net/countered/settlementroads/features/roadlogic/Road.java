package net.countered.settlementroads.features.roadlogic;

import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ConfigManager;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.countered.settlementroads.road.RoadTypeConfig;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Road {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

    ServerWorld serverWorld;
    Records.StructureConnection structureConnection;
    RoadFeatureConfig context;

    public Road(ServerWorld serverWorld, Records.StructureConnection structureConnection, RoadFeatureConfig config) {
        this.serverWorld = serverWorld;
        this.structureConnection = structureConnection;
        this.context = config;
    }

    public void generateRoad(int maxSteps){
        updateConnectionStatus(Records.ConnectionStatus.GENERATING);
        
        Random random = Random.create();
        int width = getRandomWidth(random, context);
        String roadTypeId = getSelectedRoadTypeId(random, serverWorld, structureConnection.from());
        if (roadTypeId == null) {
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }
        
        RoadTypeConfig roadType = context.getRoadTypeById(roadTypeId);
        if (roadType == null) {
            LOGGER.error("Road type '{}' not found in configuration", roadTypeId);
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }
        
        List<BlockState> material = getRandomRoadMaterials(random, roadType);

        BlockPos start = structureConnection.from();
        BlockPos end = structureConnection.to();

        RoadPathCalculator.ProgressCallback progressCallback = (progress, currentSteps, totalSteps, status) -> {
            updateConnectionProgress(progress);
        };

        List<Records.RoadSegmentPlacement> roadSegmentPlacementList = RoadPathCalculator.calculateAStarRoadPath(start, end, width, serverWorld, progressCallback);

        if (roadSegmentPlacementList.isEmpty()) {
            updateConnectionStatus(Records.ConnectionStatus.FAILED);
            return;
        }

        List<Records.RoadData> roadDataList = new ArrayList<>(serverWorld.getAttachedOrCreate(WorldDataAttachment.ROAD_DATA_LIST, ArrayList::new));
        roadDataList.add(new Records.RoadData(width, roadTypeId, material, roadSegmentPlacementList));

        serverWorld.setAttached(WorldDataAttachment.ROAD_DATA_LIST, roadDataList);
        
        updateConnectionStatus(Records.ConnectionStatus.COMPLETED);
    }
    
    private void updateConnectionStatus(Records.ConnectionStatus newStatus) {
        updateConnectionStatus(newStatus, 0.0f);
    }
    
    private void updateConnectionStatus(Records.ConnectionStatus newStatus, float progress) {
        List<Records.StructureConnection> connections = new ArrayList<>(
                serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new)
        );
        
        for (int i = 0; i < connections.size(); i++) {
            Records.StructureConnection conn = connections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                connections.set(i, new Records.StructureConnection(conn.from(), conn.to(), newStatus, progress));
                serverWorld.setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
                break;
            }
        }
    }
    
    private void updateConnectionProgress(float progress) {
        List<Records.StructureConnection> connections = new ArrayList<>(
                serverWorld.getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new)
        );
        
        for (int i = 0; i < connections.size(); i++) {
            Records.StructureConnection conn = connections.get(i);
            if ((conn.from().equals(structureConnection.from()) && conn.to().equals(structureConnection.to())) ||
                (conn.from().equals(structureConnection.to()) && conn.to().equals(structureConnection.from()))) {
                connections.set(i, conn.withProgress(progress));
                serverWorld.setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
                break;
            }
        }
    }

    private static String getSelectedRoadTypeId(Random deterministicRandom, ServerWorld serverWorld, BlockPos startPos) {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            List<RoadTypeConfig> enabledTypes = configManager.getRoadTypeRegistry().getEnabledRoadTypes();
            
            if (enabledTypes.isEmpty()) {
                LOGGER.error("No enabled road types found in configuration - road generation will fail");
                return null;
            }
            
            String biomeId = getBiomeId(serverWorld, startPos);
            LOGGER.debug("Starting road generation in biome: {}", biomeId);
            RoadTypeConfig selectedType = configManager.getRoadTypeRegistry().getRoadTypeForBiome(biomeId, new java.util.Random(deterministicRandom.nextLong()));
            if (selectedType == null) {
                LOGGER.info("No road type configured for biome '{}', skipping road generation", biomeId);
                return null;
            }
            
            LOGGER.info("Selected road type: {} for biome: {}", selectedType.getId(), biomeId);
            return selectedType.getId();
            
        } catch (Exception e) {
            LOGGER.error("Failed to get enabled road types from config - road generation will fail", e);
            return null;
        }
    }
    
    /**
     * Get biome ID at specified position
     * @param serverWorld Server world
     * @param pos Position
     * @return Biome ID
     */
    private static String getBiomeId(ServerWorld serverWorld, BlockPos pos) {
        try {
            var biome = serverWorld.getBiome(pos);
            var biomeKey = serverWorld.getRegistryManager().get(RegistryKeys.BIOME).getId(biome.value());
            return biomeKey.toString();
        } catch (Exception e) {
            LOGGER.warn("Failed to get biome ID for position {}, using default", pos, e);
            return "minecraft:plains";
        }
    }

    private static List<BlockState> getRandomRoadMaterials(Random random, RoadTypeConfig roadType) {
        List<List<BlockState>> materialsList = roadType.getMaterials();
        if (materialsList.isEmpty()) {
            LOGGER.warn("No materials available for road type: {}", roadType.getId());
            return List.of();
        }
        return materialsList.get(random.nextInt(materialsList.size()));
    }

    private static int getRandomWidth(Random random, RoadFeatureConfig config) {
        List<Integer> widthList = config.getWidths();
        return widthList.get(random.nextInt(widthList.size()));
    }
}
