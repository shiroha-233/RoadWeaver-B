package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.config.PresetService;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.sharded.RoadShardStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Road {
    private final ServerLevel level;
    private final Records.StructureConnection connection;
    private final RoadFeatureConfig config;

    public Road(ServerLevel level, Records.StructureConnection connection, RoadFeatureConfig config) {
        this.level = level;
        this.connection = connection;
        this.config = config;
    }

    public void generateRoad(int maxSteps) {
        RandomSource random = RandomSource.create();
        int width = ConfigService.get().roadWidth() > 0 ? ConfigService.get().roadWidth() : getRandomWidth(random, config);
        ModConfig cfg = ConfigService.get();
        boolean allowA = cfg.allowArtificial();
        boolean allowN = cfg.allowNatural();
        if (!allowA && !allowN) return;
        int type = allowA && allowN ? (random.nextBoolean() ? 0 : 1) : (allowA ? 0 : 1);
        List<BlockState> materials;
        if (type == 0) {
            // 人工道路始终从 JSON 预设系统中选择一套材质
            materials = PresetService.chooseMaterialsForArtificial(random, cfg);
        } else {
            materials = java.util.List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.GRAVEL.defaultBlockState());
        }

        BlockPos start = connection.from();
        BlockPos end = connection.to();
        TerrainSamplingCache cache = new TerrainSamplingCache();
        List<Records.RoadSegmentPlacement> segments = RoadPathCalculator.calculateAStarRoadPath(start, end, width, level, maxSteps, cache);
        if (segments == null || segments.size() < 5) return;
        List<Records.RoadSpan> spans = RoadPathCalculator.extractSpans(segments, level, cache);

        List<Integer> targetY = computeTargetY(level, segments, spans, cache);

        Records.RoadData rd = new Records.RoadData(width, type, materials, segments, spans, targetY);
        RoadShardStorage.addRoad(level, rd);
    }

    

    private static int getRandomWidth(RandomSource rnd, RoadFeatureConfig cfg) {
        return 3;
    }
    
    private static List<Integer> computeTargetY(ServerLevel level, List<Records.RoadSegmentPlacement> segments, List<Records.RoadSpan> spans, TerrainSamplingCache cache) {
        int n = segments.size();
        List<BlockPos> centers = new ArrayList<>(n);
        for (Records.RoadSegmentPlacement s : segments) centers.add(s.middlePos());

        // 将 spans 映射到索引范围，用于 BRIDGE
        boolean[] isBridge = new boolean[n];
        if (spans != null && !spans.isEmpty()) {
            Map<Long, Integer> indexMap = new HashMap<>();
            for (int i = 0; i < centers.size(); i++) indexMap.put(centers.get(i).asLong(), i);
            for (Records.RoadSpan sp : spans) {
                if (sp.type() != Records.SpanType.BRIDGE) continue;
                Integer si = indexMap.get(sp.start().asLong());
                Integer ei = indexMap.get(sp.end().asLong());
                if (si == null || ei == null) continue;
                int a = Math.max(0, Math.min(si, ei));
                int b = Math.min(n - 1, Math.max(si, ei));
                for (int k = a; k <= b; k++) isBridge[k] = true;
            }
        }

        int avg = Math.max(0, ConfigService.get().averagingRadius());
        int[] base = new int[n];
        for (int i = 0; i < n; i++) {
            int sum = 0, cnt = 0;
            int lo = Math.max(0, i - avg);
            int hi = Math.min(n - 1, i + avg);
            for (int j = lo; j <= hi; j++) {
                BlockPos sp = centers.get(j);
                int yTop = RoadPathCalculator.heightSampler(cache, sp.getX(), sp.getZ(), level);
                sum += yTop; cnt++;
            }
            base[i] = cnt > 0 ? (int) Math.round(sum / (double) cnt) : centers.get(i).getY();
        }

        // 如果关闭限坡平滑，则直接使用基础平均高度，不再进行每两段步进限制
        if (!ConfigService.get().slopeLimitEnabled()) {
            List<Integer> out = new ArrayList<>(n);
            for (int v : base) out.add(v);
            return out;
        }

        int[] smoothed = base.clone();
        // 对每个连续非桥梁段进行平滑，以避免奇偶振荡
        int i = 0;
        while (i < n) {
            // 跳过桥梁索引
            while (i < n && isBridge[i]) i++;
            int s = i;
            while (i < n && !isBridge[i]) i++;
            int e = i - 1; // inclusive
            if (s <= e) {
                int step2 = Math.max(0, Math.min(8, ConfigService.get().maxSlopeStepPerTwoSegments()));
                int halfLow = Math.max(0, step2 / 2);
                int halfHigh = Math.max(0, (step2 + 1) / 2);
                for (int ii = s + 1; ii <= e; ii++) {
                    int y = smoothed[ii];
                    if (ii == s + 1) {
                        int py = smoothed[ii - 1];
                        if (y > py + halfLow) y = py + halfLow;
                        if (y < py - halfLow) y = py - halfLow;
                    } else {
                        int py = smoothed[ii - 1];
                        if (y > py + halfHigh) y = py + halfHigh;
                        if (y < py - halfHigh) y = py - halfHigh;
                        int p2 = smoothed[ii - 2];
                        int hi = p2 + step2;
                        int lo = p2 - step2;
                        if (y > hi) y = hi;
                        if (y < lo) y = lo;
                    }
                    smoothed[ii] = y;
                }
                for (int ii = e - 1; ii >= s; ii--) {
                    int y = smoothed[ii];
                    if (ii == e - 1) {
                        int ny = smoothed[ii + 1];
                        if (y > ny + halfLow) y = ny + halfLow;
                        if (y < ny - halfLow) y = ny - halfLow;
                    } else {
                        int ny = smoothed[ii + 1];
                        if (y > ny + halfHigh) y = ny + halfHigh;
                        if (y < ny - halfHigh) y = ny - halfHigh;
                        int n2 = smoothed[ii + 2];
                        int hi = n2 + step2;
                        int lo = n2 - step2;
                        if (y > hi) y = hi;
                        if (y < lo) y = lo;
                    }
                    smoothed[ii] = y;
                }
            }
        }

        List<Integer> out = new ArrayList<>(n);
        for (int v : smoothed) out.add(v);
        return out;
    }
    
}
