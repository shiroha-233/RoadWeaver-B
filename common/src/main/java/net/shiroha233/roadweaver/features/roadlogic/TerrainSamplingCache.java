package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.HashMap;
import java.util.Map;

final class TerrainSamplingCache {
    private final Map<Long, Boolean> waterCache = new HashMap<>();
    private final Map<Long, Boolean> nearWaterCache = new HashMap<>();
    private final Map<Long, Boolean> columnWaterCache = new HashMap<>();
    private final Map<Long, Integer> heightCache = new HashMap<>();
    private final Map<Long, Integer> oceanFloorCache = new HashMap<>();

    private static long hashXZ(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    int height(ServerLevel level, int x, int z) {
        long key = hashXZ(x, z);
        Integer cached = heightCache.get(key);
        if (cached != null) {
            return cached;
        }
        var generator = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().getGeneratorState().randomState();
        int h = generator.getBaseHeight(x, z, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, level, rs);
        heightCache.put(key, h);
        return h;
    }

    boolean isWaterLike(ServerLevel level, int x, int z) {
        long key = hashXZ(x, z);
        Boolean cached = waterCache.get(key);
        if (cached != null) return cached;
        Holder<Biome> biome = level.getBiome(new BlockPos(x, 0, z));
        boolean res = biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN);
        waterCache.put(key, res);
        return res;
    }

    int oceanFloor(ServerLevel level, int x, int z) {
        long key = hashXZ(x, z);
        Integer cached = oceanFloorCache.get(key);
        if (cached != null) {
            return cached;
        }
        var generator = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().getGeneratorState().randomState();
        int h = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
        oceanFloorCache.put(key, h);
        return h;
    }

    boolean isNearWaterLike(ServerLevel level, int x, int z, int neighborDistance) {
        long key = hashXZ(x, z);
        Boolean cached = nearWaterCache.get(key);
        if (cached != null) return cached;
        int d = neighborDistance;
        int[][] neighborOffsets = new int[][]{
                {d, 0}, {-d, 0}, {0, d}, {0, -d},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };
        for (int[] off : neighborOffsets) {
            int nx = x + off[0];
            int nz = z + off[1];
            if (isWaterLike(level, nx, nz)) {
                nearWaterCache.put(key, true);
                return true;
            }
        }
        nearWaterCache.put(key, false);
        return false;
    }

    boolean isColumnWater(ServerLevel level, int x, int z) {
        long key = hashXZ(x, z);
        Boolean cached = columnWaterCache.get(key);
        if (cached != null) return cached;
        int ws = height(level, x, z);
        int of = oceanFloor(level, x, z);
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
}
