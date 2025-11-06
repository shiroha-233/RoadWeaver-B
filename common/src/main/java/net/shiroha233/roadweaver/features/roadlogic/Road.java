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
        List<BlockState> materials = type == 0
                ? PresetService.chooseMaterialsForArtificial(random, cfg)
                : java.util.List.of(Blocks.DIRT_PATH.defaultBlockState(), Blocks.GRAVEL.defaultBlockState());

        BlockPos start = connection.from();
        BlockPos end = connection.to();
        List<Records.RoadSegmentPlacement> segments = RoadPathCalculator.calculateAStarRoadPath(start, end, width, level, maxSteps);
        if (segments == null || segments.size() < 5) return;
        List<Records.RoadSpan> spans = RoadPathCalculator.extractSpans(segments, level);

        List<Integer> targetY = computeTargetY(level, segments, spans);

        Records.RoadData rd = new Records.RoadData(width, type, materials, segments, spans, targetY);
        RoadShardStorage.addRoad(level, rd);
    }

    

    private static int getRandomWidth(RandomSource rnd, RoadFeatureConfig cfg) {
        return 3;
    }
    
    private static List<Integer> computeTargetY(ServerLevel level, List<Records.RoadSegmentPlacement> segments, List<Records.RoadSpan> spans) {
        int n = segments.size();
        List<BlockPos> centers = new ArrayList<>(n);
        for (Records.RoadSegmentPlacement s : segments) centers.add(s.middlePos());

        // Map spans to index ranges for BRIDGE
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
                int yTop = RoadPathCalculator.heightSampler(sp.getX(), sp.getZ(), level);
                sum += yTop; cnt++;
            }
            base[i] = cnt > 0 ? (int) Math.round(sum / (double) cnt) : centers.get(i).getY();
        }

        int[] smoothed = base.clone();
        // Smooth each contiguous non-bridge run with ±step per two segments (configurable)
        int i = 0;
        while (i < n) {
            // skip bridge indices
            while (i < n && isBridge[i]) i++;
            int s = i;
            while (i < n && !isBridge[i]) i++;
            int e = i - 1; // inclusive
            if (s <= e) {
                int step = Math.max(0, Math.min(8, ConfigService.get().maxSlopeStepPerTwoSegments()));
                for (int ii = s + 2; ii <= e; ii++) {
                    int py = smoothed[ii - 2];
                    int y = smoothed[ii];
                    if (y > py + step) y = py + step;
                    if (y < py - step) y = py - step;
                    smoothed[ii] = y;
                }
                for (int ii = e - 2; ii >= s; ii--) {
                    int ny = smoothed[ii + 2];
                    int y = smoothed[ii];
                    if (y > ny + step) y = ny + step;
                    if (y < ny - step) y = ny - step;
                    smoothed[ii] = y;
                }
            }
        }

        List<Integer> out = new ArrayList<>(n);
        for (int v : smoothed) out.add(v);
        return out;
    }
    
}
