package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.helpers.Records;


public final class BridgeRangeCalculator {
    private BridgeRangeCalculator() {}

    public static record RangeResult(boolean[] isBridge, java.util.List<int[]> mergedRanges) {}

    public static RangeResult compute(java.util.List<BlockPos> middlePositions, java.util.List<Records.RoadSpan> spans) {
        int n = middlePositions.size();
        boolean[] isBridge = new boolean[n];
        java.util.List<int[]> bridgeRanges = new java.util.ArrayList<>();
        if (spans != null) {
            java.util.Map<Long, Integer> indexMap = new java.util.HashMap<>();
            for (int i = 0; i < n; i++) indexMap.put(middlePositions.get(i).asLong(), i);
            for (Records.RoadSpan sp : spans) {
                if (sp.type() != Records.SpanType.BRIDGE) continue;
                Integer si = indexMap.get(sp.start().asLong());
                Integer ei = indexMap.get(sp.end().asLong());
                if (si == null || ei == null) continue;
                int a = Math.max(0, Math.min(si, ei));
                int b = Math.min(n - 1, Math.max(si, ei));
                for (int k = a; k <= b; k++) isBridge[k] = true;
                bridgeRanges.add(new int[]{a, b});
            }
        }
        if (!bridgeRanges.isEmpty()) {
            bridgeRanges.sort(java.util.Comparator.comparingInt(o -> o[0]));
            java.util.List<int[]> merged = new java.util.ArrayList<>();
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
        return new RangeResult(isBridge, bridgeRanges);
    }
}
