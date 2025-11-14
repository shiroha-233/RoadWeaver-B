package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.features.bridge.BridgeBuilder;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.List;

public final class BridgeSegmentPlanner {
    private BridgeSegmentPlanner() {}

    public static final class Context {
        public boolean insideBridgeRange = false;
        public int currentRangeEnd = -1;
        public Integer lastBridgeDeckY = null;
    }

    public static Context newContext() { return new Context(); }

    public static void processSegment(WorldGenLevel world,
                                      Records.RoadSegmentPlacement seg,
                                      BlockPos middle,
                                      BlockPos prev,
                                      BlockPos next,
                                      int roadWidth,
                                      int baseYForThis,
                                      int deckY,
                                      int segmentIndex,
                                      RandomSource random,
                                      ModConfig cfg,
                                      List<int[]> bridgeRanges,
                                      int i,
                                      Context ctx) {
        int segDeckY = deckY;
        boolean placePier = true;
        boolean placeRail = true;

        // 进入区间初始化
        if (!ctx.insideBridgeRange) {
            for (int[] r : bridgeRanges) {
                if (i >= r[0] && i <= r[1]) { ctx.insideBridgeRange = true; ctx.currentRangeEnd = r[1]; break; }
            }
            ctx.lastBridgeDeckY = null;
        }

        int rampN = Math.max(0, cfg.bridgeRampSegments());
        if (rampN > 0 && !bridgeRanges.isEmpty()) {
            for (int[] r : bridgeRanges) {
                if (i >= r[0] && i <= r[1]) {
                    int dStart = i - r[0];
                    int dEnd = r[1] - i;
                    if (dStart < rampN || dEnd < rampN) {
                        double f = (dStart < rampN) ? (dStart / (double) rampN) : (dEnd / (double) rampN);
                        f = Math.max(0.0, Math.min(1.0, f));
                        int rampBaseY = baseYForThis;
                        segDeckY = (int) Math.round(rampBaseY + (deckY - rampBaseY) * f);
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

        if (ctx.lastBridgeDeckY != null) {
            int stepDeck = Math.max(0, Math.min(8, cfg.maxSlopeStepPerTwoSegments()));
            if (segDeckY > ctx.lastBridgeDeckY + stepDeck) segDeckY = ctx.lastBridgeDeckY + stepDeck;
            if (segDeckY < ctx.lastBridgeDeckY - stepDeck) segDeckY = ctx.lastBridgeDeckY - stepDeck;
        }
        ctx.lastBridgeDeckY = segDeckY;

        if (ctx.insideBridgeRange && i >= ctx.currentRangeEnd) {
            ctx.insideBridgeRange = false;
            ctx.currentRangeEnd = -1;
            ctx.lastBridgeDeckY = null;
        }

        BridgeBuilder.placeSegment(world, seg, middle, prev, next, roadWidth, segDeckY, segmentIndex, random, cfg, placePier, placeRail);
    }
}
