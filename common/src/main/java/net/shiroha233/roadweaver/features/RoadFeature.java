package net.shiroha233.roadweaver.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.features.config.RoadFeatureConfig;
import net.shiroha233.roadweaver.features.decoration.Decoration;
import net.shiroha233.roadweaver.features.decoration.system.SurfacePlacementUtil;
import net.shiroha233.roadweaver.features.decoration.system.ArtificialDecorationSystem;
import net.shiroha233.roadweaver.features.decoration.system.NaturalDecorationSystem;
import net.shiroha233.roadweaver.features.decoration.RoadStructures;
import net.shiroha233.roadweaver.features.bridge.BridgeBuilder;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.sharded.RoadShardStorage;

import java.util.*;

public class RoadFeature extends Feature<RoadFeatureConfig> {
    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RoadFeatureConfig> ctx) {
        WorldGenLevel world = ctx.level();
        Level lvl = world.getLevel();
        if (!(lvl instanceof ServerLevel server)) return false;

        ChunkPos currentChunk = new ChunkPos(ctx.origin());
        int minX = currentChunk.getMinBlockX();
        int minZ = currentChunk.getMinBlockZ();
        int maxX = currentChunk.getMaxBlockX();
        int maxZ = currentChunk.getMaxBlockZ();
        List<Records.RoadData> roadDataList = RoadShardStorage.queryRect(server, minX, minZ, maxX, maxZ);
        if (roadDataList == null || roadDataList.isEmpty()) return false;

        // currentChunk already defined
        Set<BlockPos> processedMiddle = new HashSet<>();
        RandomSource random = ctx.random();
        ModConfig cfg = ConfigService.get();
        int averagingRadius = Math.max(0, cfg.averagingRadius());

        Set<Decoration> decorations = new HashSet<>();
        for (Records.RoadData data : roadDataList) {
            int roadType = data.roadType();
            int roadWidth = Math.max(1, data.width());
            List<BlockState> materials = data.materials();
            List<Records.RoadSegmentPlacement> segments = data.roadSegmentList();
            if (segments == null || segments.size() < 5) continue;

            List<BlockPos> middlePositions = segments.stream().map(Records.RoadSegmentPlacement::middlePos).toList();
            // 预计算 BRIDGE 区间：将 span 映射为下标区间
            boolean[] isBridge = new boolean[middlePositions.size()];
            List<int[]> bridgeRanges = new ArrayList<>();
            {
                java.util.Map<Long, Integer> indexMap = new java.util.HashMap<>();
                for (int i = 0; i < middlePositions.size(); i++) indexMap.put(middlePositions.get(i).asLong(), i);
                List<Records.RoadSpan> spans = data.spans();
                if (spans != null) {
                    for (Records.RoadSpan sp : spans) {
                        if (sp.type() != Records.SpanType.BRIDGE) continue;
                        Integer si = indexMap.get(sp.start().asLong());
                        Integer ei = indexMap.get(sp.end().asLong());
                        if (si == null || ei == null) continue;
                        int a = Math.max(0, Math.min(si, ei));
                        int b = Math.min(middlePositions.size() - 1, Math.max(si, ei));
                        for (int k = a; k <= b; k++) isBridge[k] = true;
                        bridgeRanges.add(new int[]{a, b});
                    }
                }
                if (!bridgeRanges.isEmpty()) {
                    bridgeRanges.sort(java.util.Comparator.comparingInt(o -> o[0]));
                    List<int[]> merged = new ArrayList<>();
                    int[] cur = bridgeRanges.get(0);
                    for (int idx = 1; idx < bridgeRanges.size(); idx++) {
                        int[] nxt = bridgeRanges.get(idx);
                        if (nxt[0] <= cur[1] + 1) {
                            cur[1] = Math.max(cur[1], nxt[1]);
                        } else {
                            merged.add(cur);
                            cur = nxt;
                        }
                    }
                    merged.add(cur);
                    bridgeRanges = merged;
                }
            }
            {
                int n = middlePositions.size();
                java.util.List<Integer> targetY = data.targetY();
                boolean usePersisted = targetY != null && targetY.size() == n;
                int[] smoothedYArr = null;
                if (!usePersisted) {
                    int[] baseYArr = new int[n];
                    for (int ii = 0; ii < n; ii++) {
                        java.util.List<Integer> hs = new java.util.ArrayList<>();
                        for (int jj = ii - averagingRadius; jj <= ii + averagingRadius; jj++) {
                            if (jj >= 0 && jj < n) {
                                BlockPos sp = middlePositions.get(jj);
                                if (new ChunkPos(sp).equals(currentChunk)) {
                                    int yTop = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, sp.getX(), sp.getZ());
                                    hs.add(yTop);
                                }
                            }
                        }
                        if (hs.isEmpty()) {
                            BlockPos mid = middlePositions.get(ii);
                            if (new ChunkPos(mid).equals(currentChunk)) {
                                baseYArr[ii] = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, mid.getX(), mid.getZ());
                            } else {
                                baseYArr[ii] = middlePositions.get(ii).getY();
                            }
                        } else {
                            baseYArr[ii] = (int) Math.round(hs.stream().mapToInt(Integer::intValue).average().orElse(middlePositions.get(ii).getY()));
                        }
                    }
                    smoothedYArr = new int[n];
                    for (int ii = 0; ii < n; ii++) smoothedYArr[ii] = baseYArr[ii];
                    int step = Math.max(0, Math.min(8, cfg.maxSlopeStepPerTwoSegments()));
                    for (int ii = 2; ii < n; ii++) {
                        int py = smoothedYArr[ii - 2];
                        int y = smoothedYArr[ii];
                        if (y > py + step) y = py + step;
                        if (y < py - step) y = py - step;
                        smoothedYArr[ii] = y;
                    }
                    for (int ii = n - 3; ii >= 0; ii--) {
                        int ny = smoothedYArr[ii + 2];
                        int y = smoothedYArr[ii];
                        if (y > ny + step) y = ny + step;
                        if (y < ny - step) y = ny - step;
                        smoothedYArr[ii] = y;
                    }
                }

                int deckY = server.getSeaLevel() + cfg.bridgeDeckClearance();
                int segmentIndex = 0;
                boolean insideBridgeRange = false;
                int currentRangeEnd = -1;
                Integer lastBridgeDeckY = null;
                for (int i = 2; i < segments.size() - 2; i++) {
                    BlockPos middle = middlePositions.get(i);
                    if (!processedMiddle.add(middle)) continue;
                    segmentIndex++;
                    if (segmentIndex < 60 || segmentIndex > segments.size() - 60) continue;
                    ChunkPos middleChunk = new ChunkPos(middle);
                    if (!middleChunk.equals(currentChunk)) continue;

                    BlockPos prev = middlePositions.get(i - 2);
                    BlockPos next = middlePositions.get(i + 2);

                    int topYCenter = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, middle.getX(), middle.getZ());
                    BlockPos averaged = new BlockPos(middle.getX(), topYCenter, middle.getZ());
                    int baseYForThis = usePersisted ? targetY.get(i) : (smoothedYArr != null ? smoothedYArr[i] : topYCenter);

                    Records.RoadSegmentPlacement seg = segments.get(i);
                    if (cfg.bridgeEnabled() && isBridge[i]) {
                        int segDeckY = deckY;
                        boolean placePier = true;
                        boolean placeRail = true;
                        int rampN = Math.max(0, cfg.bridgeRampSegments());
                        if (!insideBridgeRange) {
                            for (int[] r : bridgeRanges) {
                                if (i >= r[0] && i <= r[1]) { insideBridgeRange = true; currentRangeEnd = r[1]; break; }
                            }
                            lastBridgeDeckY = null;
                        }
                        if (rampN > 0 && !bridgeRanges.isEmpty()) {
                            for (int[] r : bridgeRanges) {
                                if (i >= r[0] && i <= r[1]) {
                                    int dStart = i - r[0];
                                    int dEnd = r[1] - i;
                                    if (dStart < rampN || dEnd < rampN) {
                                        double f = dStart < rampN ? (dStart / (double) rampN) : (dEnd / (double) rampN);
                                        int rampBaseY = baseYForThis;
                                        segDeckY = (int) Math.round(rampBaseY + (deckY - rampBaseY) * Math.max(0.0, Math.min(1.0, f)));
                                        placePier = false;
                                        placeRail = false;
                                    }
                                    break;
                                }
                            }
                        }
                        if (placeRail && !bridgeRanges.isEmpty()) {
                            for (int[] r : bridgeRanges) {
                                if (i == r[0] || i == r[1]) { placeRail = false; break; }
                            }
                        }
                        if (lastBridgeDeckY != null) {
                            int stepDeck = Math.max(0, Math.min(8, cfg.maxSlopeStepPerTwoSegments()));
                            if (segDeckY > lastBridgeDeckY + stepDeck) segDeckY = lastBridgeDeckY + stepDeck;
                            if (segDeckY < lastBridgeDeckY - stepDeck) segDeckY = lastBridgeDeckY - stepDeck;
                        }
                        lastBridgeDeckY = segDeckY;
                        if (insideBridgeRange && i >= currentRangeEnd) {
                            insideBridgeRange = false;
                            currentRangeEnd = -1;
                            lastBridgeDeckY = null;
                        }
                        BridgeBuilder.placeSegment(world, seg, middle, prev, next, roadWidth, segDeckY, segmentIndex, random, cfg, placePier, placeRail);
                    } else {
                        int averageY = baseYForThis;
                        for (BlockPos widthBlock : seg.positions()) {
                            BlockPos pos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
                            if (roadType == 1) {
                                java.util.List<BlockState> biomeMats = net.shiroha233.roadweaver.features.decoration.util.BiomeRoadMaterialSelector.forBiome(world, pos);
                                SurfacePlacementUtil.placeOnSurface(world, pos, biomeMats, 0, random, cfg);
                            } else {
                                SurfacePlacementUtil.placeOnSurface(world, pos, materials, 0, random, cfg);
                            }
                        }
                    }

                    if (roadType == 0) {
                        if (!isBridge[i] || cfg.bridgeKeepLamps()) {
                            ArtificialDecorationSystem.addDecoration(world, decorations, averaged, segmentIndex, next, prev, middlePositions, roadWidth, random, cfg);
                        }
                    } else {
                        if (!isBridge[i] || cfg.bridgeKeepLamps()) {
                            NaturalDecorationSystem.addDecoration(world, decorations, averaged, segmentIndex, next, prev, middlePositions, roadWidth, random, cfg);
                        }
                    }
                }
            }
        }
        RoadStructures.tryPlaceDecorations(decorations);
        return true;
    }
    
}
