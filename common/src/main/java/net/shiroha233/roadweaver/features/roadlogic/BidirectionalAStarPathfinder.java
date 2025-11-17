package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.*;

/**
 * 双向 A* 寻路：从起点和终点同时扩展搜索，
 * 在中间相遇后重建完整路径，以减少节点展开数量。
 */
final class BidirectionalAStarPathfinder {
    private BidirectionalAStarPathfinder() {}

    
    private static final double ORTHO_STEP_COST = 1.0;//正交步进基础成本，数值越大越不倾向采用正交步进
    private static final double DIAG_STEP_COST = 1.0;//对角步进基础成本，数值越大越不倾向采用对角步进
    private static final int ELEVATION_WEIGHT = 80;//高度成本权重，数值越大越偏好平坦区域，防止道路贴近悬崖边或坑洼
    private static final int BIOME_BASE_COST = 40;// 特定生物群系基础成本（河流/海洋/深海），已参与计算
    private static final int BIOME_WEIGHT = 2;// 生物群系成本权重，实际代价为 BIOME_BASE_COST * BIOME_WEIGHT
    private static final int STABILITY_WEIGHT = 15;// 稳定性权重，数值越大越偏好平坦区域，防止道路贴近悬崖边或坑洼
    private static final int WATER_DEPTH_WEIGHT = 40;// 水深权重，数值越大越偏好远离水域
    private static final int NEAR_WATER_COST = 40;// 水边成本，数值越大越偏好远离水域
    private static final double HEURISTIC_WEIGHT = 15.0;//启发式权重，积极朝终点方向推进，路径更直，但也更可能忽视局部最优绕路
    private static final double HEURISTIC_EPSILON = 0.2;//启发式epsilon
    private static final double DEVIATION_WEIGHT = 0.5;//偏差权重，数值越大越偏好直线路径

    static List<Records.RoadSegmentPlacement> calculateLandPath(BlockPos startGround,
                                                                BlockPos endGround,
                                                                int width,
                                                                ServerLevel level,
                                                                int maxSteps,
                                                                TerrainSamplingCache cache) {
        // 特殊情况：起终点非常接近时无需复杂寻路
        if (startGround.equals(endGround)) {
            return Collections.emptyList();
        }

        int d = RoadPathCalculator.getNeighborDistance();
        int[][] neighborOffsets = new int[][]{
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };

        PriorityQueue<Node> openF = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        PriorityQueue<Node> openB = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, Node> nodesF = new HashMap<>();
        Map<BlockPos, Node> nodesB = new HashMap<>();
        Set<BlockPos> closedF = new HashSet<>();
        Set<BlockPos> closedB = new HashSet<>();

        Node startNode = new Node(startGround, null, 0.0, heuristic(startGround, endGround));
        Node endNode = new Node(endGround, null, 0.0, heuristic(endGround, startGround));
        openF.add(startNode);
        nodesF.put(startGround, startNode);
        openB.add(endNode);
        nodesB.put(endGround, endNode);

        int stepsBudget = Math.max(1, maxSteps);
        while (!openF.isEmpty() && !openB.isEmpty() && stepsBudget-- > 0) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            Node peekF = openF.peek();
            Node peekB = openB.peek();
            boolean expandForward;
            if (peekF == null) {
                expandForward = false;
            } else if (peekB == null) {
                expandForward = true;
            } else {
                expandForward = peekF.f <= peekB.f;
            }

            Meet meet;
            if (expandForward) {
                meet = expandOneSide(openF, nodesF, closedF, nodesB,
                        startGround, endGround, level, cache, neighborOffsets, d);
            } else {
                meet = expandOneSide(openB, nodesB, closedB, nodesF,
                        endGround, startGround, level, cache, neighborOffsets, d);
            }

            if (meet != null) {
                return reconstructPath(meet.forward, meet.backward, width);
            }
        }

        return null;
    }

    private static Meet expandOneSide(PriorityQueue<Node> open,
                                      Map<BlockPos, Node> nodesThis,
                                      Set<BlockPos> closedThis,
                                      Map<BlockPos, Node> nodesOther,
                                      BlockPos from,
                                      BlockPos to,
                                      ServerLevel level,
                                      TerrainSamplingCache cache,
                                      int[][] neighborOffsets,
                                      int d) {
        if (open.isEmpty()) return null;
        Node current = open.poll();
        if (current == null) return null;

        closedThis.add(current.pos);
        nodesThis.remove(current.pos);

        for (int[] off : neighborOffsets) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            BlockPos nxz = current.pos.offset(off[0], 0, off[1]);
            int y = RoadPathCalculator.heightSampler(cache, nxz.getX(), nxz.getZ(), level);
            BlockPos np = new BlockPos(nxz.getX(), y, nxz.getZ());
            if (closedThis.contains(np)) continue;

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

            double deviation = deviation2d(np, from, to);
            double deviationCost = deviation * DEVIATION_WEIGHT / Math.max(1.0, d);

            double tentativeG = current.g
                    + stepCost
                    + elevation * ELEVATION_WEIGHT
                    + biomeCost * BIOME_WEIGHT
                    + stabilityCost * STABILITY_WEIGHT
                    + waterDepthCost
                    + nearWaterCost
                    + deviationCost;

            Node existing = nodesThis.get(np);
            if (existing != null && tentativeG >= existing.g) {
                continue;
            }

            double h = heuristic(np, to);
            double fWeighted = tentativeG + (1.0 + HEURISTIC_EPSILON) * h;
            Node next = new Node(np, current, tentativeG, fWeighted);
            nodesThis.put(np, next);
            open.add(next);

            Node other = nodesOther.get(np);
            if (other != null) {
                // 在该位置两侧搜索相遇
                if (isFromForward(from, to, next, other)) {
                    return new Meet(next, other);
                } else {
                    return new Meet(other, next);
                }
            }
        }

        return null;
    }

    private static boolean isFromForward(BlockPos from, BlockPos to, Node a, Node b) {
        // 通过与起点/终点的距离判断哪一侧更接近 from，
        // 用于决定哪一个视作“前向”节点，哪一个视作“反向”节点。
        int da = manhattan2d(a.pos, from);
        int db = manhattan2d(b.pos, from);
        if (da != db) return da <= db;
        // 若距离相同，则用到终点的距离作为次要判断，保持稳定性
        int ea = manhattan2d(a.pos, to);
        int eb = manhattan2d(b.pos, to);
        return ea <= eb;
    }

    private static List<Records.RoadSegmentPlacement> reconstructPath(Node meetForward,
                                                                      Node meetBackward,
                                                                      int width) {
        // 从前向搜索链表回溯到起点
        List<Node> forwardNodes = new ArrayList<>();
        Node cur = meetForward;
        while (cur != null) {
            forwardNodes.add(cur);
            cur = cur.parent;
        }
        Collections.reverse(forwardNodes);

        // 从反向搜索链表回溯到终点，注意跳过重复的汇合节点
        List<Node> backwardNodes = new ArrayList<>();
        cur = (meetBackward != null && meetBackward.pos.equals(meetForward.pos)) ? meetBackward.parent : meetBackward;
        while (cur != null) {
            backwardNodes.add(cur);
            cur = cur.parent;
        }
        Collections.reverse(backwardNodes);

        List<Node> allNodes = new ArrayList<>(forwardNodes.size() + backwardNodes.size());
        allNodes.addAll(forwardNodes);
        allNodes.addAll(backwardNodes);

        Map<BlockPos, Set<BlockPos>> segments = new LinkedHashMap<>();
        Set<BlockPos> widthCache = new HashSet<>();

        for (int i = 0; i < allNodes.size(); i++) {
            Node curNode = allNodes.get(i);
            BlockPos p = curNode.pos;
            RoadDirection dir = RoadDirection.X_AXIS;

            if (i > 0) {
                Node prevNode = allNodes.get(i - 1);
                BlockPos prev = prevNode.pos;
                int dx = p.getX() - prev.getX();
                int dz = p.getZ() - prev.getZ();
                int stepCount = Math.max(Math.abs(dx), Math.abs(dz));
                if ((dx < 0 && dz > 0) || (dx > 0 && dz < 0)) dir = RoadDirection.DIAGONAL_1;
                else if ((dx < 0 && dz < 0) || (dx > 0 && dz > 0)) dir = RoadDirection.DIAGONAL_2;
                else if (dx == 0 && dz != 0) dir = RoadDirection.Z_AXIS;

                if (stepCount > 1) {
                    for (int s = 1; s < stepCount; s++) {
                        int ix = prev.getX() + dx * s / stepCount;
                        int iz = prev.getZ() + dz * s / stepCount;
                        BlockPos ip = new BlockPos(ix, prev.getY(), iz);
                        Set<BlockPos> ws = RoadPathCalculator.generateWidth(ip, width / 2, widthCache, dir);
                        segments.put(ip, ws);
                    }
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
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }
    }

    private static final class Meet {
        final Node forward;
        final Node backward;

        Meet(Node forward, Node backward) {
            this.forward = forward;
            this.backward = backward;
        }
    }
}
