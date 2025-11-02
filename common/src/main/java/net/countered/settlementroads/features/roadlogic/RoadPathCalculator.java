package net.countered.settlementroads.features.roadlogic;

import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.*;

public class RoadPathCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    private static final int NEIGHBOR_DISTANCE = 4;

    // Cache for height values, mapping hashed (x, z) to height (y)
    public static final Map<Long, Integer> heightCache = new ConcurrentHashMap<>();

    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // Backward-compatible overload using current config defaults
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(BlockPos start, BlockPos end, int width, ServerLevel serverWorld, int maxSteps) {
        IModConfig cfg = ConfigProvider.get();
        return calculateAStarRoadPath(start, end, width, serverWorld, maxSteps, cfg.maxHeightDifference(), cfg.maxTerrainStability(), false);
    }

    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(BlockPos start, BlockPos end, int width, ServerLevel serverWorld, int maxSteps, int maxHeightDifference, int maxTerrainStability) {
        return calculateAStarRoadPath(start, end, width, serverWorld, maxSteps, maxHeightDifference, maxTerrainStability, false);
    }

    /**
     * 使用A*算法计算路线路径
     *
     * @param start               起始坐标
     * @param end                 终点坐标
     * @param width               路线宽度
     * @param serverWorld         服务器
     * @param maxSteps            最大步长
     * @param maxHeightDifference 最大高度差
     * @param maxTerrainStability 最大地形稳定度
     * @param ignoreWater         是否忽略水域
     * @return //
     */
    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(BlockPos start, BlockPos end, int width, ServerLevel serverWorld, int maxSteps, int maxHeightDifference, int maxTerrainStability, boolean ignoreWater) {
        Objects.requireNonNull(serverWorld);
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, List<BlockPos>> interpolatedSegments = new HashMap<>();

        int startX = snapToGrid(start.getX(), NEIGHBOR_DISTANCE);
        int startZ = snapToGrid(start.getZ(), NEIGHBOR_DISTANCE);
        int endX = snapToGrid(end.getX(), NEIGHBOR_DISTANCE);
        int endZ = snapToGrid(end.getZ(), NEIGHBOR_DISTANCE);

        start = new BlockPos(startX, start.getY(), startZ);
        end = new BlockPos(endX, end.getY(), endZ);

        BlockPos startGround = new BlockPos(start.getX(), heightSampler(start.getX(), start.getZ(), serverWorld), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(end.getX(), end.getZ(), serverWorld), end.getZ());

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        int d = NEIGHBOR_DISTANCE;
        int[][] neighborOffsets = {{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};

        while (!openSet.isEmpty() && maxSteps-- > 0) {
            Node current = openSet.poll();

            if (current.pos.distManhattan(endGround) < NEIGHBOR_DISTANCE * 2) {
                LOGGER.debug("Found path! {}", current.pos);
                return reconstructPath(current, width, interpolatedSegments);
            }

            closedSet.add(current.pos);
            allNodes.remove(current.pos);

            for (int[] offset : neighborOffsets) {
                BlockPos neighborXZ = current.pos.offset(offset[0], 0, offset[1]);
                int y = heightSampler(neighborXZ.getX(), neighborXZ.getZ(), serverWorld);
                BlockPos neighborPos = new BlockPos(neighborXZ.getX(), y, neighborXZ.getZ());
                if (closedSet.contains(neighborPos)) continue;

                Holder<Biome> biomeHolder = biomeSampler(neighborPos, serverWorld);
                boolean isWater = biomeHolder.is(BiomeTags.IS_RIVER) || biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_DEEP_OCEAN);
                // 水域成本：50 * 8 = 400（与原项目一致）
                // 如果绕路成本更高（距离远、高度差大），仍会选择穿过水域
                // 手动模式且忽略水域时，水域成本为 0（用于跨海连接）
                int biomeCost = (isWater && !ignoreWater) ? 50 : 0;
                int elevation = abs(y - current.pos.getY());
                if (elevation > maxHeightDifference) continue;
                double stepCost = offset[0] * offset[0] + offset[1] * offset[1] == NEIGHBOR_DISTANCE * NEIGHBOR_DISTANCE ? 1.0 : 1.5;
                int terrainStabilityCost = calculateTerrainStability(neighborPos, y, serverWorld);
                if (terrainStabilityCost > maxTerrainStability) {
                    continue;
                }
                int yLevelCost = y == serverWorld.getSeaLevel() ? 20 : 0;
                double tentativeG = current.gScore + stepCost
                        + elevation * 40
                        + biomeCost * 8
                        + yLevelCost * 8
                        + terrainStabilityCost * 16;

                Node neighbor = allNodes.get(neighborPos);
                if (neighbor == null || tentativeG < neighbor.gScore) {
                    double h = heuristic(neighborPos, endGround);
                    neighbor = new Node(neighborPos, current, tentativeG, tentativeG + h);
                    allNodes.put(neighborPos, neighbor);
                    openSet.add(neighbor);

                    List<BlockPos> segmentPoints = new ArrayList<>();
                    for (int i = 1; i < NEIGHBOR_DISTANCE; i++) {
                        int interpX = current.pos.getX() + (offset[0] * i) / NEIGHBOR_DISTANCE;
                        int interpZ = current.pos.getZ() + (offset[1] * i) / NEIGHBOR_DISTANCE;
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
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        double dxzApprox = abs(dx) + abs(dz) - 0.6 * min(abs(dx), abs(dz));
        return dxzApprox * 30;
    }

    private static int calculateTerrainStability(BlockPos neighborPos, int y, ServerLevel serverWorld) {
        int cost = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos testPos = neighborPos.relative(direction);
            int testY = heightSampler(testPos.getX(), testPos.getZ(), serverWorld);
            int elevation = abs(y - testY);
            cost += elevation;
        }
        return cost;
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(Node endNode, int width, Map<BlockPos, List<BlockPos>> interpolatedPathMap) {
        Objects.requireNonNull(endNode);
        Objects.requireNonNull(interpolatedPathMap);
        if (width <= 0) {
            LOGGER.error("Width must be greater than 0.");
            return List.of();
        }

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
            List<BlockPos> interpolated = interpolatedPathMap.getOrDefault(pos, List.of());
            RoadDirection roadDirection = determineRoadDirection(interpolated, pos);
            if (!interpolated.isEmpty()) {
                for (var interp : interpolatedPathMap.getOrDefault(pos, List.of())) {
                    Set<BlockPos> widthSetInterp = generateWidth(interp, width / 2, widthCache, roadDirection);
                    roadSegments.put(interp, widthSetInterp);
                }
            }

            Set<BlockPos> widthSet = generateWidth(pos, width / 2, widthCache, roadDirection);
            roadSegments.put(pos, widthSet);
        }
        return roadSegments.entrySet().stream().map(entry -> new Records.RoadSegmentPlacement(entry.getKey(), new ArrayList<>(entry.getValue()))).toList();
    }

    private static RoadDirection determineRoadDirection(List<BlockPos> interpolated, BlockPos pos) {
        RoadDirection roadDirection = RoadDirection.X_AXIS;
        if (!interpolated.isEmpty()) {
            BlockPos firstInterpolated = interpolated.get(0);
            int dx = pos.getX() - firstInterpolated.getX();
            int dz = pos.getZ() - firstInterpolated.getZ();

            if ((dx < 0 && dz > 0) || (dx > 0 && dz < 0)) {
                roadDirection = RoadDirection.DIAGONAL_1;
            } else if ((dx < 0 && dz < 0) || (dx > 0 && dz > 0)) {
                roadDirection = RoadDirection.DIAGONAL_2;
            } else if (dx == 0 && dz != 0) {
                roadDirection = RoadDirection.Z_AXIS;
            }
        }
        return roadDirection;
    }

    // Height sampler method - improved with sea level handling
    private static int heightSampler(int x, int z, ServerLevel serverWorld) {
        long key = hashXZ(x, z);
        return heightCache.computeIfAbsent(key, k -> {
            int seaLevel = serverWorld.getSeaLevel();
            int oceanFloorHeight = serverWorld.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, serverWorld, serverWorld.getChunkSource().randomState());
            int worldSurfaceHeight = serverWorld.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, serverWorld, serverWorld.getChunkSource().randomState());

            if (worldSurfaceHeight <= seaLevel && oceanFloorHeight < seaLevel) {
                return seaLevel;
            }
            return worldSurfaceHeight;
        });
    }

    private static Holder<Biome> biomeSampler(BlockPos pos, ServerLevel serverWorld) {
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
        return floorDiv(value, gridSize) * gridSize;
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
