package net.countered.settlementroads.features;

import com.mojang.serialization.Codec;
import net.countered.settlementroads.SettlementRoads;
import net.countered.settlementroads.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.countered.settlementroads.config.ConfigManager;
import net.countered.settlementroads.features.config.RoadFeatureConfig;
import net.countered.settlementroads.features.decoration.*;
import net.countered.settlementroads.features.roadlogic.RoadPathCalculator;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.countered.settlementroads.road.RoadTypeConfig;
import net.countered.settlementroads.decoration.DecorationContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoadFeature extends Feature<RoadFeatureConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);
    

    public static int chunksForLocatingCounter = 1;

    public static final RegistryKey<PlacedFeature> ROAD_FEATURE_PLACED_KEY =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(SettlementRoads.MOD_ID, "road_feature_placed"));
    public static final RegistryKey<ConfiguredFeature<?,?>> ROAD_FEATURE_KEY =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(SettlementRoads.MOD_ID, "road_feature"));
    public static final Feature<RoadFeatureConfig> ROAD_FEATURE = new RoadFeature(RoadFeatureConfig.CODEC);
    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> context) {
        int heightCacheLimit = ModConfig.heightCacheLimit;
        if (RoadPathCalculator.heightCache.size() > heightCacheLimit){
            RoadPathCalculator.heightCache.clear();
        }
        ServerWorld serverWorld = context.getWorld().toServerWorld();
        StructureWorldAccess structureWorldAccess = context.getWorld();
        Records.StructureLocationData structureLocationData = serverWorld.getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS);
        if (structureLocationData == null) {
            return false;
        }
        List<BlockPos> villageLocations = structureLocationData.structureLocations();;
        tryFindNewStructureConnection(villageLocations, serverWorld);
        Set<Decoration> roadDecorationCache = new HashSet<>();
        runRoadLogic(structureWorldAccess, context, roadDecorationCache);
        RoadStructures.tryPlaceDecorations(roadDecorationCache);
        return true;
    }

    private void tryFindNewStructureConnection(List<BlockPos> villageLocations, ServerWorld serverWorld) {
        if (villageLocations == null || villageLocations.size() < ModConfig.maxLocatingCount) {
            chunksForLocatingCounter++;
            if (chunksForLocatingCounter > ModConfig.maxChunksForLocating) {
                serverWorld.getServer().execute(() -> {
                    StructureConnector.cacheNewConnection(serverWorld, true);
                });
                chunksForLocatingCounter = 1;
            }
        }
    }

    private void runRoadLogic(StructureWorldAccess structureWorldAccess, FeatureContext<RoadFeatureConfig> context, Set<Decoration> roadDecorationPlacementPositions) {
        int averagingRadius = ModConfig.averagingRadius;
        List<Records.RoadData> roadDataList = structureWorldAccess.toServerWorld().getAttached(WorldDataAttachment.ROAD_DATA_LIST);
        if (roadDataList == null) return;
        ChunkPos currentChunkPos = new ChunkPos(context.getOrigin());

        Set<BlockPos> posAlreadyContainsSegment = new HashSet<>();
        for (Records.RoadData data : roadDataList) {
            String roadTypeId = data.roadTypeId();
            List<BlockState> materials = data.materials();
            List<Records.RoadSegmentPlacement> segmentList = data.roadSegmentList();

            List<BlockPos> middlePositions = segmentList.stream().map(Records.RoadSegmentPlacement::middlePos).toList();
            int segmentIndex = 0;
            for (int i = 2; i < segmentList.size() - 2; i++) {
                if (posAlreadyContainsSegment.contains(middlePositions.get(i))) continue;
                segmentIndex++;
                Records.RoadSegmentPlacement segment = segmentList.get(i);
                BlockPos segmentMiddlePos = segment.middlePos();
                // offset to structure
                if (segmentIndex < ModConfig.segmentStartOffset || segmentIndex > segmentList.size() - ModConfig.segmentEndOffset) continue;
                ChunkPos middleChunkPos = new ChunkPos(segmentMiddlePos);
                if (!middleChunkPos.equals(currentChunkPos)) continue;

                BlockPos prevPos = middlePositions.get(i - 2);
                BlockPos nextPos = middlePositions.get(i + 2);
                List<Double> heights = new ArrayList<>();
                for (int j = i - averagingRadius; j <= i + averagingRadius; j++) {
                    if (j >= 0 && j < middlePositions.size()) {
                        BlockPos samplePos = middlePositions.get(j);
                        double y = structureWorldAccess.getTopY(Heightmap.Type.WORLD_SURFACE_WG, samplePos.getX(), samplePos.getZ());
                        heights.add(y);
                    }
                }

                int averageY = (int) Math.round(heights.stream().mapToDouble(Double::doubleValue).average().orElse(segmentMiddlePos.getY()));
                BlockPos averagedPos = new BlockPos(segmentMiddlePos.getX(), averageY, segmentMiddlePos.getZ());

                Random random = context.getRandom();
                
                for (BlockPos widthBlock : segment.positions()) {
                    BlockPos correctedYPos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                    placeOnSurface(structureWorldAccess, correctedYPos, materials, roadTypeId, random);
                }
                
                addDecorationWithConfigSystem(structureWorldAccess, roadDecorationPlacementPositions, averagedPos, segmentIndex, nextPos, prevPos, middlePositions, roadTypeId, random);
                posAlreadyContainsSegment.add(segmentMiddlePos);
            }
        }
    }

    private void addDecorationWithConfigSystem(StructureWorldAccess structureWorldAccess, Set<Decoration> roadDecorationPlacementPositions,
                                             BlockPos placePos, int segmentIndex, BlockPos nextPos, BlockPos prevPos, 
                                             List<BlockPos> middleBlockPositions, String roadTypeId, Random random) {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            RoadTypeConfig roadTypeConfig = getRoadTypeConfigById(roadTypeId);
            if (roadTypeConfig == null) {
                LOGGER.error("Failed to get road type config for roadTypeId: {}, skipping decoration generation", roadTypeId);
                return;
            }
            
            int dx = nextPos.getX() - prevPos.getX();
            int dz = nextPos.getZ() - prevPos.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            int normDx = length != 0 ? (int) Math.round(dx / length) : 0;
            int normDz = length != 0 ? (int) Math.round(dz / length) : 0;
            Vec3i directionVector = new Vec3i(normDx, 0, normDz);
            Vec3i orthogonalVector = new Vec3i(-directionVector.getZ(), 0, directionVector.getX());
            
            DecorationContext context = new DecorationContext(
                placePos, structureWorldAccess, segmentIndex, roadTypeConfig, 
                directionVector, new java.util.Random(random.nextLong())
            );
            
            String distanceText = String.valueOf(middleBlockPositions.size());
            
            context.setProperty("orthogonalVector", orthogonalVector);
            int decorationStartOffset = ModConfig.decorationStartOffset;
            context.setProperty("isEnd", segmentIndex == decorationStartOffset || segmentIndex == middleBlockPositions.size() - decorationStartOffset);
            context.setProperty("totalSegments", middleBlockPositions.size());
            context.setProperty("leftRoadSide", random.nextBoolean());
            context.setProperty("distanceText", distanceText);
            
            List<Decoration> decorations = configManager.getDecorationRegistry().createDecorations(context);
            
            roadDecorationPlacementPositions.addAll(decorations);
            
        } catch (Exception e) {
            LOGGER.error("Configuration-based decoration system failed, skipping decoration generation", e);
        }
    }
    
    private RoadTypeConfig getRoadTypeConfigById(String roadTypeId) {
        try {
            ConfigManager configManager = ConfigManager.getInstance();
            return configManager.getRoadTypeRegistry().getRoadType(roadTypeId);
        } catch (Exception e) {
            LOGGER.error("Failed to get road type '{}' from config: {}", roadTypeId, e.getMessage());
            return null;
        }
    }
    

    private void placeOnSurface(StructureWorldAccess structureWorldAccess, BlockPos placePos, List<BlockState> material, String roadTypeId, Random random) {
        RoadTypeConfig roadTypeConfig = getRoadTypeConfigById(roadTypeId);
        boolean needsAveraging = roadTypeConfig != null && roadTypeConfig.getAveragingRadius() > 0;
        
        BlockPos surfacePos = placePos;
        if (!needsAveraging || ModConfig.averagingRadius == 0) {
            surfacePos = structureWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, placePos);
        }
        BlockPos topPos = structureWorldAccess.getTopPosition(Heightmap.Type.WORLD_SURFACE_WG, surfacePos);
        BlockState blockStateAtPos = structureWorldAccess.getBlockState(topPos.down());
        
        if (blockStateAtPos.equals(Blocks.WATER.getDefaultState())) {
            return;
        }
        
        placeRoadBlock(structureWorldAccess, blockStateAtPos, surfacePos, material, random);
    }

    private void placeRoadBlock(StructureWorldAccess structureWorldAccess, BlockState blockStateAtPos, BlockPos surfacePos, List<BlockState> materials, Random deterministicRandom) {
        // If not water, just place the road
        if (!placeAllowedCheck(blockStateAtPos.getBlock())
                || (!structureWorldAccess.getBlockState(surfacePos.down()).isOpaque())
                && !structureWorldAccess.getBlockState(surfacePos.down(2)).isOpaque()
                //&& !structureWorldAccess.getBlockState(surfacePos.down(3)).isOpaque())
                //|| structureWorldAccess.getBlockState(surfacePos.up(3)).isOpaque()
        ) {
            return;
        }
        BlockState material = materials.get(deterministicRandom.nextInt(materials.size()));
        setBlockState(structureWorldAccess, surfacePos.down(), material);

        for (int i = 0; i < ModConfig.maxClearBlocksAbove; i++) {
            BlockState blockStateUp = structureWorldAccess.getBlockState(surfacePos.up(i));
            if (!blockStateUp.getBlock().equals(Blocks.AIR) && !blockStateUp.isIn(BlockTags.LOGS) && !blockStateUp.isIn(BlockTags.FENCES)) {
                setBlockState(structureWorldAccess, surfacePos.up(i), Blocks.AIR.getDefaultState());
            }
            else {
                break;
            }
        }

        BlockPos belowPos1 = surfacePos.down(2);
        BlockState belowState1 = structureWorldAccess.getBlockState(belowPos1);

        if (belowState1.getBlock().equals(Blocks.GRASS_BLOCK)) {
            setBlockState(structureWorldAccess, belowPos1, Blocks.DIRT.getDefaultState());
        }
    }

    private boolean placeAllowedCheck(Block blockToCheck) {
        if (net.countered.settlementroads.config.ForbiddenBlocksConfigLoader.isBlockReplaceable(blockToCheck)) {
            return true;
        }
        
        var blockState = blockToCheck.getDefaultState();
        for (String tagId : net.countered.settlementroads.config.ForbiddenBlocksConfigLoader.getReplaceableBlockTags()) {
            try {
                var tagKey = net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BLOCK, 
                    net.minecraft.util.Identifier.tryParse(tagId));
                if (tagKey != null && blockState.isIn(tagKey)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.warn("Invalid block tag in replaceable list: {}", tagId);
            }
        }
        
        return false;
    }
}

