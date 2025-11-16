package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaver.helpers.Records;

import net.shiroha233.roadweaver.config.ConfigService;
import java.util.*;

final class BasicAStarPathfinder {
    private BasicAStarPathfinder() {}
    
    private static final double ORTHO_STEP_COST = 1.0;//正交步进基础成本，数值越大越不倾向采用正交步进
    private static final double DIAG_STEP_COST = 1.0;//对角步进基础成本，数值越大越不倾向采用对角步进
    private static final int ELEVATION_WEIGHT = 80;//高度成本权重，数值越大越偏好平坦区域，防止道路贴近悬崖边或坑洼
    private static final int BIOME_BASE_COST = 12;// 特定生物群系基础成本（河流/海洋/深海），已参与计算
    private static final int BIOME_WEIGHT = 2;// 生物群系成本权重，实际代价为 BIOME_BASE_COST * BIOME_WEIGHT
    private static final int STABILITY_WEIGHT = 15;// 稳定性权重，数值越大越偏好平坦区域，防止道路贴近悬崖边或坑洼
    private static final int WATER_DEPTH_WEIGHT = 40;// 水深权重，数值越大越偏好远离水域
    private static final int NEAR_WATER_COST = 40;// 水边成本，数值越大越偏好远离水域
    private static final double HEURISTIC_WEIGHT = 15.0;//启发式权重，积极朝终点方向推进，路径更直，但也更可能忽视局部最优绕路
    private static final double HEURISTIC_EPSILON = 0.2;//启发式epsilon
    private static final double DEVIATION_WEIGHT = 0.5;//偏差权重，数值越大越偏好直线路径

    public static List<Records.RoadSegmentPlacement> calculateLandPath(BlockPos startGround,
                                                                       BlockPos endGround,
                                                                       int width,
                                                                       ServerLevel level,
                                                                       int maxSteps,
                                                                       TerrainSamplingCache cache) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        Map<BlockPos, List<BlockPos>> interpolatedSegments = new HashMap<>();

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        openSet.add(startNode);
        allNodes.put(startGround, startNode);

        int d = getNeighborDistance();
        int[][] neighborOffsets = new int[][]{
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };

        int stepsBudget = Math.max(1, maxSteps);
        while (!openSet.isEmpty() && stepsBudget-- > 0) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            Node current = openSet.poll();
            if (current == null) break;

            if (manhattan2d(current.pos, endGround) < d * 2) {
                return reconstructPath(current, width, interpolatedSegments);
            }

            closed.add(current.pos);
            allNodes.remove(current.pos);

            for (int[] off : neighborOffsets) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                BlockPos nxz = current.pos.offset(off[0], 0, off[1]);
                int y = RoadPathCalculator.heightSampler(cache, nxz.getX(), nxz.getZ(), level);
                BlockPos np = new BlockPos(nxz.getX(), y, nxz.getZ());
                if (closed.contains(np)) continue;

                Holder<Biome> biome = level.getBiome(np);
                int biomeCost = (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) ? BIOME_BASE_COST : 0;
                int elevation = Math.abs(y - current.pos.getY());
                int offsetSum = Math.abs(Math.abs(off[0])) + Math.abs(off[1]);
                double stepCost = (offsetSum == 2 * d) ? DIAG_STEP_COST : ORTHO_STEP_COST;
                int stabilityCost = RoadPathCalculator.calculateTerrainStability(cache, np, y, level);
                int sea = level.getSeaLevel();
                boolean waterColumn = RoadPathCalculator.isColumnWater(cache, nxz.getX(), nxz.getZ(), level);
                boolean nearWater = RoadPathCalculator.isNearWaterLike(cache, nxz.getX(), nxz.getZ(), level);
                int oceanFloor = RoadPathCalculator.oceanFloorSampler(cache, nxz.getX(), nxz.getZ(), level);
                int waterDepth = Math.max(0, sea - oceanFloor);
                int waterDepthCost = waterColumn ? waterDepth * WATER_DEPTH_WEIGHT : 0;
                int nearWaterCost = nearWater ? NEAR_WATER_COST : 0;

                double deviation = deviation2d(np, startGround, endGround);
                double deviationCost = deviation * DEVIATION_WEIGHT / Math.max(1.0, d);

                double tentativeG = current.g
                        + stepCost
                        + elevation * ELEVATION_WEIGHT
                        + biomeCost * BIOME_WEIGHT
                        + stabilityCost * STABILITY_WEIGHT
                        + waterDepthCost
                        + nearWaterCost
                        + deviationCost;

                Node n = allNodes.get(np);
                if (n == null || tentativeG < n.g) {
                    double h = heuristic(np, endGround);
                    double fWeighted = tentativeG + (1.0 + HEURISTIC_EPSILON) * h;
                    n = new Node(np, current, tentativeG, fWeighted);
                    allNodes.put(np, n);
                    openSet.add(n);

                    List<BlockPos> seg = new ArrayList<>();
                    for (int i = 1; i < d; i++) {
                        int ix = current.pos.getX() + (off[0] * i) / d;
                        int iz = current.pos.getZ() + (off[1] * i) / d;
                        seg.add(new BlockPos(ix, current.pos.getY(), iz));
                    }
                    interpolatedSegments.put(np, seg);
                }
            }
        }
        return null;
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(Node endNode,
                                                                      int width,
                                                                      Map<BlockPos, List<BlockPos>> interpolatedPathMap) {
        List<Node> nodes = new ArrayList<>();
        Node cur = endNode;
        while (cur != null) {
            nodes.add(cur);
            cur = cur.parent;
        }
        Collections.reverse(nodes);

        Map<BlockPos, Set<BlockPos>> segments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();

        for (Node n : nodes) {
            BlockPos p = n.pos;
            List<BlockPos> interp = interpolatedPathMap.getOrDefault(p, Collections.emptyList());
            RoadDirection dir = RoadDirection.X_AXIS;
            if (!interp.isEmpty()) {
                BlockPos f = interp.get(0);
                int dx = p.getX() - f.getX();
                int dz = p.getZ() - f.getZ();
                if ((dx < 0 && dz > 0) || (dx > 0 && dz < 0)) dir = RoadDirection.DIAGONAL_1;
                else if ((dx < 0 && dz < 0) || (dx > 0 && dz > 0)) dir = RoadDirection.DIAGONAL_2;
                else if (dx == 0 && dz != 0) dir = RoadDirection.Z_AXIS;
                for (BlockPos ip : interp) {
                    Set<BlockPos> ws = RoadPathCalculator.generateWidth(ip, width / 2, widthCache, dir);
                    segments.put(ip, ws);
                }
            }
            Set<BlockPos> ws = RoadPathCalculator.generateWidth(p, width / 2, widthCache, dir);
            segments.put(p, ws);
        }

        List<Records.RoadSegmentPlacement> out = new ArrayList<>();
        for (Map.Entry<BlockPos, Set<BlockPos>> e : segments.entrySet()) {
            out.add(new Records.RoadSegmentPlacement(e.getKey(), new ArrayList<>(e.getValue())));
        }
        return out;
    }
    
    private static int getNeighborDistance() {
        try {
            int v = ConfigService.get().aStarStep();
            if (v < 4) return 16;
            if (v > 128) return 128;
            return v;
        } catch (Throwable ignore) {
            return 16;
        }
    }

    private static int manhattan2d(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        double dxzApprox = Math.abs(dx) + Math.abs(dz) - 0.6 * Math.min(Math.abs(dx), Math.abs(dz));
        return dxzApprox * HEURISTIC_WEIGHT;
    }

    private static double deviation2d(BlockPos p, BlockPos a, BlockPos b) {
        double ax = a.getX();
        double az = a.getZ();
        double bx = b.getX();
        double bz = b.getZ();
        double px = p.getX();
        double pz = p.getZ();
        double num = Math.abs((bz - az) * px - (bx - ax) * pz + bx * az - bz * ax);
        double den = Math.hypot(bx - ax, bz - az);
        if (den <= 0.0) return 0.0;
        return num / den;
    }

    private static final class Node {
        final BlockPos pos;
        final Node parent;
        final double g;
        final double f;
        Node(BlockPos pos, Node parent, double g, double f) {
            this.pos = pos; this.parent = parent; this.g = g; this.f = f;
        }
    }
}
