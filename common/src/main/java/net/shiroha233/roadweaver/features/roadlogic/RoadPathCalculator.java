package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.config.ConfigService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public static final Map<Long, Integer> heightCache = new ConcurrentHashMap<>();
    public static final Map<Long, Boolean> waterCache = new ConcurrentHashMap<>();
    public static final Map<Long, Integer> oceanFloorCache = new ConcurrentHashMap<>();
    public static final Map<Long, Boolean> nearWaterCache = new ConcurrentHashMap<>();
    public static final Map<Long, Boolean> columnWaterCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_ENTRIES = 200_000;

    private static void pruneCachesIfTooLarge() {
        if (heightCache.size() > MAX_CACHE_ENTRIES) heightCache.clear();
        if (waterCache.size() > MAX_CACHE_ENTRIES) waterCache.clear();
        if (oceanFloorCache.size() > MAX_CACHE_ENTRIES) oceanFloorCache.clear();
        if (nearWaterCache.size() > MAX_CACHE_ENTRIES) nearWaterCache.clear();
        if (columnWaterCache.size() > MAX_CACHE_ENTRIES) columnWaterCache.clear();
    }

    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    public static List<Records.RoadSegmentPlacement> calculateAStarRoadPath(BlockPos startIn, BlockPos endIn, int width, ServerLevel level, int maxSteps) {
        int dGrid = getNeighborDistance();
        int sx = snapToGrid(startIn.getX(), dGrid);
        int sz = snapToGrid(startIn.getZ(), dGrid);
        int ex = snapToGrid(endIn.getX(), dGrid);
        int ez = snapToGrid(endIn.getZ(), dGrid);

        BlockPos start = new BlockPos(sx, startIn.getY(), sz);
        BlockPos end = new BlockPos(ex, endIn.getY(), ez);

        BlockPos startGround = new BlockPos(start.getX(), heightSampler(start.getX(), start.getZ(), level), start.getZ());
        BlockPos endGround = new BlockPos(end.getX(), heightSampler(end.getX(), end.getZ(), level), end.getZ());

        List<Records.RoadSegmentPlacement> land = BasicAStarPathfinder.calculateLandPath(startGround, endGround, width, level, maxSteps);
        return land;
    }

    static int calculateTerrainStability(BlockPos pos, int y, ServerLevel level) {
        int cost = 0;
        if (Math.abs(heightSampler(pos.getX() + 1, pos.getZ(), level) - y) > 0) cost++;
        if (Math.abs(heightSampler(pos.getX() - 1, pos.getZ(), level) - y) > 0) cost++;
        if (Math.abs(heightSampler(pos.getX(), pos.getZ() + 1, level) - y) > 0) cost++;
        if (Math.abs(heightSampler(pos.getX(), pos.getZ() - 1, level) - y) > 0) cost++;
        return cost;
    }

    static int heightSampler(int x, int z, ServerLevel level) {
        pruneCachesIfTooLarge();
        long key = hashXZ(x, z);
        return heightCache.computeIfAbsent(key, k -> {
            RandomState rs = level.getChunkSource().getGeneratorState().randomState();
            return level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, rs);
        });
    }

    static boolean isWaterLike(int x, int z, ServerLevel level) {
        pruneCachesIfTooLarge();
        long key = hashXZ(x, z);
        Boolean v = waterCache.get(key);
        if (v != null) return v;
        Holder<Biome> biome = level.getBiome(new BlockPos(x, 0, z));
        boolean res = biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
        waterCache.put(key, res);
        return res;
    }

    static int oceanFloorSampler(int x, int z, ServerLevel level) {
        pruneCachesIfTooLarge();
        long key = hashXZ(x, z);
        return oceanFloorCache.computeIfAbsent(key, k -> {
            RandomState rs = level.getChunkSource().getGeneratorState().randomState();
            return level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
        });
    }

    static boolean isNearWaterLike(int x, int z, ServerLevel level) {
        pruneCachesIfTooLarge();
        long key = hashXZ(x, z);
        Boolean cached = nearWaterCache.get(key);
        if (cached != null) return cached;
        int d = getNeighborDistance();
        int[][] neighborOffsets = new int[][]{
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };
        for (int[] off : neighborOffsets) {
            int nx = x + off[0];
            int nz = z + off[1];
            if (isWaterLike(nx, nz, level)) {
                nearWaterCache.put(key, true);
                return true;
            }
        }
        nearWaterCache.put(key, false);
        return false;
    }

    static boolean isColumnWater(int x, int z, ServerLevel level) {
        pruneCachesIfTooLarge();
        long key = hashXZ(x, z);
        Boolean cached = columnWaterCache.get(key);
        if (cached != null) return cached;
        int ws = heightSampler(x, z, level);
        int of = oceanFloorSampler(x, z, level);
        int sea = level.getSeaLevel();
        boolean res = (ws > of) && (ws >= sea);
        if (res) {
            BlockPos check = new BlockPos(x, Math.max(sea - 1, ws - 1), z);
            if (level.isLoaded(check)) {
                res = level.getFluidState(check).is(net.minecraft.tags.FluidTags.WATER);
            }
        }
        columnWaterCache.put(key, res);
        return res;
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

    public static List<Records.RoadSpan> extractSpans(List<Records.RoadSegmentPlacement> segments, ServerLevel level) {
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
            boolean water = isColumnWater(p.getX(), p.getZ(), level);
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
            int ya = heightSampler(a.getX(), a.getZ(), level);
            int yb = heightSampler(b.getX(), b.getZ(), level);
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
    public static void clearCaches() {
        heightCache.clear();
        waterCache.clear();
        oceanFloorCache.clear();
        nearWaterCache.clear();
        columnWaterCache.clear();
    }
}
