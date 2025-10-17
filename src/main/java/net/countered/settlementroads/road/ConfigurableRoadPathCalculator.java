package net.countered.settlementroads.road;

import net.countered.settlementroads.config.RoadWeaverConfig;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.features.roadlogic.RoadDirection;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可配置的道路路径计算器
 * 使用配置化的生物群系成本系统
 */
public class ConfigurableRoadPathCalculator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-ConfigurableRoadPathCalculator");
    
    // Cache for height values, mapping hashed (x, z) to height (y)
    public static final Map<Long, Integer> heightCache = new ConcurrentHashMap<>();
    
    private final RoadWeaverConfig config;
    
    public ConfigurableRoadPathCalculator(RoadWeaverConfig config) {
        this.config = config;
    }
    
    // Helper method to hash coordinates into a single long
    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
    
    public List<Records.RoadSegmentPlacement> calculateAStarRoadPath(
            BlockPos start, BlockPos end, int width, ServerWorld serverWorld, int maxSteps
    ) {
        int neighborDistance = config.getPathFindingConfig().getNeighborDistance();
        
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, List<BlockPos>> interpolatedSegments = new HashMap<>();
        
        int startX = snapToGrid(start.getX(), neighborDistance);
        int startZ = snapToGrid(start.getZ(), neighborDistance);
        int endX = snapToGrid(end.getX(), neighborDistance);
        int endZ = snapToGrid(end.getZ(), neighborDistance);
        
        start = new BlockPos(startX, start.getY(), startZ);
        end = new BlockPos(endX, end.getY(), endZ);
        
        BlockPos startGround = new BlockPos(start.getX(), heightSampler(start.getX(), start.getZ(), serverWorld), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(end.getX(), end.getZ(), serverWorld), end.getZ());
        
        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);
        
        int d = neighborDistance;
        int[][] neighborOffsets = {
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };
        
        while (!openSet.isEmpty() && maxSteps-- > 0) {
            Node current = openSet.poll();
            
            if (current.pos.withY(0).getManhattanDistance(endGround.withY(0)) < neighborDistance * 2) {
                LOGGER.debug("Found path! {}", current.pos);
                return reconstructPath(current, width, interpolatedSegments);
            }
            
            closedSet.add(current.pos);
            allNodes.remove(current.pos);
            
            for (int[] offset : neighborOffsets) {
                BlockPos neighborXZ = current.pos.add(offset[0], 0, offset[1]);
                int y = heightSampler(neighborXZ.getX(), neighborXZ.getZ(), serverWorld);
                BlockPos neighborPos = new BlockPos(neighborXZ.getX(), y, neighborXZ.getZ());
                if (closedSet.contains(neighborPos)) continue;
                
                // 使用配置化的生物群系成本计算
                RegistryEntry<Biome> biomeRegistryEntry = biomeSampler(neighborPos, serverWorld);
                double biomeCost = config.getBiomeCostCalculator().calculateTotalCost(biomeRegistryEntry, neighborPos, serverWorld);
                
                int elevation = Math.abs(y - current.pos.getY());
                if (elevation > config.getPathFindingConfig().getMaxSteps()) { // 使用配置的最大高度差
                    continue;
                }
                
                int offsetSum = Math.abs(Math.abs(offset[0])) + Math.abs(offset[1]);
                double stepCost = (offsetSum == 2 * neighborDistance) ? 
                    config.getPathFindingConfig().getDiagonalStepCost() : 
                    config.getPathFindingConfig().getStraightStepCost();
                
                double terrainStabilityCost = calculateTerrainStability(neighborPos, y, serverWorld);
                if (terrainStabilityCost > config.getPathFindingConfig().getMaxSteps()) { // 使用配置的最大地形稳定性
                    continue;
                }
                
                // 使用配置化的成本乘数
                double tentativeG = current.gScore + stepCost
                        + elevation * config.getPathFindingConfig().getCostMultiplier("elevation")
                        + biomeCost * config.getPathFindingConfig().getCostMultiplier("biome")
                        + (y == 62 ? config.getPathFindingConfig().getCostMultiplier("seaLevel") : 0)
                        + terrainStabilityCost * config.getPathFindingConfig().getCostMultiplier("terrainStability");
                
                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround) * config.getPathFindingConfig().getHeuristicWeight();
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);
                    
                    List<BlockPos> segmentPoints = new ArrayList<>();
                    for (int i = 1; i < neighborDistance; i++) {
                        int interpX = current.pos.getX() + (offset[0] * i) / neighborDistance;
                        int interpZ = current.pos.getZ() + (offset[1] * i) / neighborDistance;
                        BlockPos interpolated = new BlockPos(interpX, current.pos.getY(), interpZ);
                        segmentPoints.add(interpolated);
                    }
                    interpolatedSegments.put(neighborPos, segmentPoints);
                }
            }
        }
        return Collections.emptyList();
    }
    
    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        double dxzApprox = Math.abs(dx) + Math.abs(dz) - 0.6 * Math.min(Math.abs(dx), Math.abs(dz));
        return dxzApprox * 30;
    }
    
    private static double calculateTerrainStability(BlockPos neighborPos, int y, ServerWorld serverWorld) {
        double cost = 0.0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos testPos = neighborPos.offset(direction);
            int testY = heightSampler(testPos.getX(), testPos.getZ(), serverWorld);
            int elevation = Math.abs(y - testY);
            cost += elevation;
            if (cost > 2) {
                return Double.MAX_VALUE;
            }
        }
        return cost;
    }
    
    private static List<Records.RoadSegmentPlacement> reconstructPath(
            Node endNode, int width, Map<BlockPos, List<BlockPos>> interpolatedPathMap
    ) {
        List<Node> pathNodes = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            pathNodes.add(current);
            current = current.parent;
        }
        Collections.reverse(pathNodes);
        
        Map<BlockPos, Set<BlockPos>> roadSegments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();
        
        for (Node node : pathNodes) {
            BlockPos pos = node.pos;
            List<BlockPos> interpolated = interpolatedPathMap.getOrDefault(pos, Collections.emptyList());
            RoadDirection roadDirection = RoadDirection.X_AXIS;
            if (!interpolated.isEmpty()) {
                BlockPos firstInterpolated = interpolated.get(0);
                int dx = pos.getX() - firstInterpolated.getX();
                int dz = pos.getZ() - firstInterpolated.getZ();
                
                if (dx < 0 && dz > 0 || dx > 0 && dz < 0){
                    roadDirection = RoadDirection.DIAGONAL_1;
                }
                else if (dx < 0 && dz < 0 || dx > 0 && dz > 0) {
                    roadDirection = RoadDirection.DIAGONAL_2;
                }
                else if (dx == 0 && dz != 0) {
                    roadDirection = RoadDirection.Z_AXIS;
                }
                
                for (BlockPos interp : interpolated) {
                    Set<BlockPos> widthSetInterp = generateWidth(interp, width / 2, widthCache, roadDirection);
                    roadSegments.put(interp, widthSetInterp);
                }
            }
            
            Set<BlockPos> widthSet = generateWidth(pos, width / 2, widthCache, roadDirection);
            roadSegments.put(pos, widthSet);
        }
        
        List<Records.RoadSegmentPlacement> result = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> entry : roadSegments.entrySet()) {
            result.add(new Records.RoadSegmentPlacement(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return result;
    }
    
    // Height sampler method
    private static int heightSampler(int x, int z, ServerWorld serverWorld) {
        long key = hashXZ(x, z);
        return heightCache.computeIfAbsent(key, k -> serverWorld.getChunkManager()
                .getChunkGenerator()
                .getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, serverWorld, serverWorld.getChunkManager().getNoiseConfig()));
    }
    
    // Biome sampler method
    private static RegistryEntry<Biome> biomeSampler(BlockPos pos, ServerWorld serverWorld) {
        return serverWorld.getBiome(pos);
    }
    
    private static class Node {
        BlockPos pos;
        Node parent;
        double gScore, fScore;
        
        Node(BlockPos pos, Node parent, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }
    
    private static int snapToGrid(int value, int gridSize) {
        return Math.floorDiv(value, gridSize) * gridSize;
    }
    
    private static Set<BlockPos> generateWidth(BlockPos center, int radius, Set<BlockPos> widthPositionsCache, RoadDirection direction) {
        Set<BlockPos> segmentWidthPositions = new HashSet<>();
        
        int centerX = center.getX();
        int centerZ = center.getZ();
        int y = 0;
        
        if (direction == RoadDirection.X_AXIS) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = new BlockPos(centerX, y, centerZ + dz);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        } else if (direction == RoadDirection.Z_AXIS) {
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos pos = new BlockPos(centerX + dx, y, centerZ);
                if (!widthPositionsCache.contains(pos)) {
                    widthPositionsCache.add(pos);
                    segmentWidthPositions.add(pos);
                }
            }
        } else {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (direction == RoadDirection.DIAGONAL_2) {
                        if ((dx == -radius && dz == -radius) || (dx == radius && dz == radius)) {
                            continue;
                        }
                    }
                    if (direction == RoadDirection.DIAGONAL_1) {
                        if ((dx == -radius && dz == radius) || (dx == radius && dz == -radius)) {
                            continue;
                        }
                    }
                    BlockPos pos = new BlockPos(centerX + dx, y, centerZ + dz);
                    if (!widthPositionsCache.contains(pos)) {
                        widthPositionsCache.add(pos);
                        segmentWidthPositions.add(pos);
                    }
                }
            }
        }
        return segmentWidthPositions;
    }
}
