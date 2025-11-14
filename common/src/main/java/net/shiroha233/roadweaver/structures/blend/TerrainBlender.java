package net.shiroha233.roadweaver.structures.blend;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaver.structures.api.BlendProfile;

import java.util.HashMap;
import java.util.Map;

public final class TerrainBlender {
    private TerrainBlender() {}

    public static BlendPlan plan(WorldGenLevel world, AABB box, BlendProfile profile) {
        if (box == null || profile == null) return BlendPlan.EMPTY;
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.ceil(box.maxX);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.ceil(box.maxZ);

        int cx = (minX + maxX) / 2;
        int cz = (minZ + maxZ) / 2;
        int inner = Math.max(0, profile.ringInner());
        int outer = Math.max(inner, profile.ringOuter());

        // 估算目标高度：取内环区域表面高度平均
        long sum = 0;
        int cnt = 0;
        for (int x = cx - inner; x <= cx + inner; x++) {
            for (int z = cz - inner; z <= cz + inner; z++) {
                int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                sum += top;
                cnt++;
            }
        }
        int targetY = (cnt == 0) ? world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz) : (int) Math.round(sum / (double) cnt);

        Map<Long, Integer> desired = new HashMap<>();
        int cutFillBudget = Math.max(0, profile.cutFillBudget());
        int used = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int dx = Math.abs(x - cx);
                int dz = Math.abs(z - cz);
                int d = Math.max(dx, dz); // Chebyshev，矩形环
                int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int desiredY;
                if (d <= inner) {
                    desiredY = targetY;
                } else if (d <= outer) {
                    double t = (outer == inner) ? 1.0 : (outer - d) / (double) (outer - inner);
                    // kernel: quadratic / linear
                    double k;
                    if ("quadratic".equalsIgnoreCase(profile.kernel())) {
                        k = t * t;
                    } else {
                        k = t; // linear fallback
                    }
                    desiredY = (int) Math.round(top + (targetY - top) * k);
                } else {
                    desiredY = top;
                }
                int delta = Math.abs(desiredY - top);
                if (delta > 0) {
                    if (used + delta > cutFillBudget) {
                        continue; // 预算限制
                    }
                    used += delta;
                }
                desired.put(BlendPlan.key2D(x, z), desiredY);
            }
        }
        return new BlendPlan(box, targetY, inner, outer, profile.fellTree(), desired);
    }

    public static void apply(WorldGenLevel world, BlendPlan plan) {
        if (plan == null || plan == BlendPlan.EMPTY) return;
        for (Map.Entry<Long, Integer> e : plan.desiredHeights().entrySet()) {
            long key = e.getKey();
            int desiredY = e.getValue();
            int x = (int) (key >> 32);
            int z = (int) key;
            int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            // 清障：砍高于目标的方块（含树/叶）
            if (top > desiredY) {
                for (int y = top; y > desiredY; y--) {
                    world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            // 填筑：把地面抬到目标高度（用泥土）
            if (top < desiredY) {
                for (int y = top; y <= desiredY; y++) {
                    world.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }
    }
}
