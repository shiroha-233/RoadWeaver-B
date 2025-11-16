package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.config.ConfigService;

import java.util.*;

public final class RoadPathCalculator {
    private RoadPathCalculator() {}

    static int getNeighborDistance() {
        try {
            int v = ConfigService.get().aStarStep();
            if (v < 4) return 16;
            if (v > 128) return 128;
            return v;
        } catch (Throwable ignore) {
            return 16;
        }
    }

    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(BlockPos startIn,
                                                                            BlockPos endIn,
                                                                            int width,
                                                                            ServerLevel level,
                                                                            int maxSteps,
                                                                            TerrainSamplingCache cache) {
        int dGrid = getNeighborDistance();
        int sx = snapToGrid(startIn.getX(), dGrid);
        int sz = snapToGrid(startIn.getZ(), dGrid);
        int ex = snapToGrid(endIn.getX(), dGrid);
        int ez = snapToGrid(endIn.getZ(), dGrid);

        BlockPos start = new BlockPos(sx, startIn.getY(), sz);
        BlockPos end = new BlockPos(ex, endIn.getY(), ez);

        BlockPos startGround = new BlockPos(start.getX(), heightSampler(cache, start.getX(), start.getZ(), level), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(cache, end.getX(), end.getZ(), level), end.getZ());

        List<Records.RoadSegmentPlacement> land = BasicAStarPathfinder.calculateLandPath(startGround, endGround, width, level, maxSteps, cache);
        return land;
    }

    static int calculateTerrainStability(TerrainSamplingCache cache, BlockPos pos, int y, ServerLevel level) {
        int cost = 0;
        if (Math.abs(heightSampler(cache, pos.getX() + 1, pos.getZ(), level) - y) > 0) cost++;
        if (Math.abs(heightSampler(cache, pos.getX() - 1, pos.getZ(), level) - y) > 0) cost++;
        if (Math.abs(heightSampler(cache, pos.getX(), pos.getZ() + 1, level) - y) > 0) cost++;
        if (Math.abs(heightSampler(cache, pos.getX(), pos.getZ() - 1, level) - y) > 0) cost++;
        return cost;
    }

    static int heightSampler(TerrainSamplingCache cache, int x, int z, ServerLevel level) {
        return cache.height(level, x, z);
    }

    static boolean isWaterLike(TerrainSamplingCache cache, int x, int z, ServerLevel level) {
        return cache.isWaterLike(level, x, z);
    }

    static int oceanFloorSampler(TerrainSamplingCache cache, int x, int z, ServerLevel level) {
        return cache.oceanFloor(level, x, z);
    }

    static boolean isNearWaterLike(TerrainSamplingCache cache, int x, int z, ServerLevel level) {
        int d = getNeighborDistance();
        return cache.isNearWaterLike(level, x, z, d);
    }

    static boolean isColumnWater(TerrainSamplingCache cache, int x, int z, ServerLevel level) {
        return cache.isColumnWater(level, x, z);
    }

    static int snapToGrid(int v, int gridSize) {
        return Math.floorDiv(v, gridSize) * gridSize;
    }

    static Set<BlockPos> generateWidth(BlockPos center, int radius, Set<BlockPos> cache, RoadDirection dir) {
        Set<BlockPos> set = new HashSet<>();
        int cx = center.getX();
        int cz = center.getZ();
        int y = 0;
        if (dir == RoadDirection.X_AXIS) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos p = new BlockPos(cx, y, cz + dz);
                if (cache.add(p)) set.add(p);
            }
        } else if (dir == RoadDirection.Z_AXIS) {
            for (int dx = -radius; dx <= radius; dx++) {
                BlockPos p = new BlockPos(cx + dx, y, cz);
                if (cache.add(p)) set.add(p);
            }
        } else {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dir == RoadDirection.DIAGONAL_2) {
                        if ((dx == -radius && dz == -radius) || (dx == radius && dz == radius)) continue;
                    }
                    if (dir == RoadDirection.DIAGONAL_1) {
                        if ((dx == -radius && dz == radius) || (dx == radius && dz == -radius)) continue;
                    }
                    BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                    if (cache.add(p)) set.add(p);
                }
            }
        }
        return set;
    }

    public static List<Records.RoadSpan> extractSpans(List<Records.RoadSegmentPlacement> segments, ServerLevel level, TerrainSamplingCache cache) {
        List<Records.RoadSpan> spans = new ArrayList<>();
        if (segments == null || segments.isEmpty()) return spans;

        List<BlockPos> centers = new ArrayList<>(segments.size());
        for (Records.RoadSegmentPlacement seg : segments) {
            centers.add(seg.middlePos());
        }

        boolean inWater = false;
        int waterStart = -1;
        for (int i = 0; i < centers.size(); i++) {
            BlockPos p = centers.get(i);
            boolean water = isColumnWater(cache, p.getX(), p.getZ(), level);
            if (water && !inWater) {
                inWater = true;
                waterStart = i;
            } else if (!water && inWater) {
                int startIdx = Math.max(0, waterStart - 1);
                int endIdx = i;
                BlockPos start = centers.get(startIdx);
                BlockPos end = centers.get(Math.min(endIdx, centers.size() - 1));
                spans.add(new Records.RoadSpan(start, end, Records.SpanType.BRIDGE));
                inWater = false;
                waterStart = -1;
            }
        }

        final int SLOPE_ABS_THRESHOLD = 4;
        final int RUN_MIN_LENGTH = 3;
        int runStart = -1;
        for (int i = 1; i < centers.size(); i++) {
            BlockPos a = centers.get(i - 1);
            BlockPos b = centers.get(i);
            int ya = heightSampler(cache, a.getX(), a.getZ(), level);
            int yb = heightSampler(cache, b.getX(), b.getZ(), level);
            int dy = Math.abs(yb - ya);
            boolean steep = dy >= SLOPE_ABS_THRESHOLD;
            if (steep) {
                if (runStart < 0) runStart = i - 1;
            } else if (runStart >= 0) {
                int len = i - runStart;
                if (len >= RUN_MIN_LENGTH) {
                    BlockPos s = centers.get(runStart);
                    BlockPos e = centers.get(i);
                    spans.add(new Records.RoadSpan(s, e, Records.SpanType.TUNNEL));
                }
                runStart = -1;
            }
        }
        if (runStart >= 0) {
            int len = centers.size() - runStart;
            if (len >= RUN_MIN_LENGTH) {
                BlockPos s = centers.get(runStart);
                BlockPos e = centers.get(centers.size() - 1);
                spans.add(new Records.RoadSpan(s, e, Records.SpanType.TUNNEL));
            }
        }

        return spans;
    }
}
