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
import net.shiroha233.roadweaver.features.decoration.RoadStructures;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.sharded.RoadShardStorage;
import net.shiroha233.roadweaver.features.decoration.system.DecorationPlanner;
import net.shiroha233.roadweaver.features.roadlogic.BridgeRangeCalculator;
import net.shiroha233.roadweaver.features.roadlogic.HeightProfileService;
import net.shiroha233.roadweaver.features.roadlogic.SegmentPaver;
import net.shiroha233.roadweaver.features.roadlogic.BridgeSegmentPlanner;
import net.shiroha233.roadweaver.features.roadlogic.BridgeTransitionAdjuster;

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
            boolean[] isBridge = new boolean[middlePositions.size()];
            List<int[]> bridgeRanges = new ArrayList<>();
            {
                BridgeRangeCalculator.RangeResult res = BridgeRangeCalculator.compute(middlePositions, data.spans());
                isBridge = res.isBridge();
                bridgeRanges = res.mergedRanges();
            }
            {
                java.util.List<Integer> targetY = data.targetY();
                HeightProfileService.HeightProfile hp = HeightProfileService.build(world, middlePositions, currentChunk, averagingRadius, cfg, targetY);
                boolean usePersisted = hp.usePersisted();
                int[] smoothedYArr = hp.smoothedY();
                int[] baseYArr;
                if (usePersisted && targetY != null && targetY.size() == middlePositions.size()) {
                    baseYArr = targetY.stream().mapToInt(Integer::intValue).toArray();
                } else {
                    baseYArr = smoothedYArr;
                }

                if (baseYArr != null && cfg.bridgeEnabled() && !bridgeRanges.isEmpty()) {
                    baseYArr = BridgeTransitionAdjuster.adjust(baseYArr, bridgeRanges, cfg);
                }

                int deckY = server.getSeaLevel() + cfg.bridgeDeckClearance();
                int segmentIndex = 0;
                BridgeSegmentPlanner.Context bridgeCtx = BridgeSegmentPlanner.newContext();
                for (int i = 2; i < segments.size() - 2; i++) {
                    BlockPos middle = middlePositions.get(i);
                    if (!processedMiddle.add(middle)) continue;
                    segmentIndex++;
                    if (segmentIndex < 60 || segmentIndex > segments.size() - 60) continue;
                    ChunkPos middleChunk = new ChunkPos(middle);
                    if (!middleChunk.equals(currentChunk)) continue;

                    BlockPos prev = middlePositions.get(i - 2);
                    BlockPos next = middlePositions.get(i + 2);

                    int topYCenter = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, middle.getX(), middle.getZ());
                    BlockPos averaged = new BlockPos(middle.getX(), topYCenter, middle.getZ());
                    int baseYForThis = (baseYArr != null ? baseYArr[i] : topYCenter);

                    Records.RoadSegmentPlacement seg = segments.get(i);
                    if (cfg.bridgeEnabled() && isBridge[i]) {
                        BridgeSegmentPlanner.processSegment(world, seg, middle, prev, next, roadWidth, baseYForThis, deckY, segmentIndex, random, cfg, bridgeRanges, i, bridgeCtx);
                    } else {
                        SegmentPaver.paveSegment(world, seg, baseYForThis, roadType, materials, random, cfg);
                    }

                    if (!isBridge[i] || cfg.bridgeKeepLamps()) {
                        DecorationPlanner.addDecoration(
                                world,
                                decorations,
                                averaged,
                                segmentIndex,
                                next,
                                prev,
                                middlePositions,
                                roadWidth,
                                random,
                                cfg,
                                (roadType == 0 ? DecorationPlanner.Mode.ARTIFICIAL : DecorationPlanner.Mode.NATURAL)
                        );
                    }
                }
            }
        }
        RoadStructures.tryPlaceDecorations(decorations);
        return true;
    }
    
}
