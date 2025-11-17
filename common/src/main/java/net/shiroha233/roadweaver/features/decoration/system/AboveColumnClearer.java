package net.shiroha233.roadweaver.features.decoration.system;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.config.ModConfig;

public final class AboveColumnClearer {
    private AboveColumnClearer() {}

    public static void clearAboveColumn(WorldGenLevel world, BlockPos surfacePos, ModConfig cfg) {
        boolean tunnel = cfg != null && cfg.tunnelEnabled();
        int defaultClear = (cfg != null ? cfg.roadClearHeight() : 3);
        int maxH = tunnel
                ? Math.max(2, Math.min(16, cfg.tunnelClearHeight()))
                : Math.max(1, Math.min(16, defaultClear));
        boolean allowCutLogsInThisColumn = false;
        for (int i = 0; i < maxH; i++) {
            BlockPos up = surfacePos.above(i);
            BlockState st = world.getBlockState(up);
            if (st.isAir()) continue;
            boolean isLog = st.is(BlockTags.LOGS);
            boolean isFence = st.is(BlockTags.FENCES);
            boolean isTreeCoreCandidate = isLog
                    || st.is(Blocks.BAMBOO)
                    || TreeRemovalUtil.isVineLike(st)
                    || st.is(Blocks.COCOA)
                    || TreeRemovalUtil.isMushroomLike(st);
            if (cfg != null && cfg.removeWholeTreeOnPath() && isTreeCoreCandidate) {
                if (TreeRemovalUtil.fellTreeAt(world, up, cfg)) {
                    continue;
                } else {
                    if (isLog || TreeRemovalUtil.isMushroomLike(st)) {
                        allowCutLogsInThisColumn = true;
                    }
                }
            }
            if (tunnel) {
                // 隧道模式：允许挖掉除木头/栅栏以外的大部分方块（包括石头、冰等），高度由 tunnelClearHeight 控制
                if ((!isLog || allowCutLogsInThisColumn) && !isFence) {
                    world.setBlock(up, Blocks.AIR.defaultBlockState(), 3);
                } else {
                    break;
                }
            } else {
                if ((!isLog || allowCutLogsInThisColumn) && !isFence) {
                    world.setBlock(up, Blocks.AIR.defaultBlockState(), 3);
                } else {
                    break;
                }
            }
        }
    }
}
