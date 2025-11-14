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
        int maxH = tunnel ? Math.max(2, Math.min(16, cfg.tunnelClearHeight())) : 3;
        for (int i = 0; i < maxH; i++) {
            BlockPos up = surfacePos.above(i);
            BlockState st = world.getBlockState(up);
            if (st.isAir()) continue;
            if (cfg != null && cfg.removeWholeTreeOnPath() && (st.is(BlockTags.LOGS) || st.is(Blocks.BAMBOO) || TreeRemovalUtil.isVineLike(st) || st.is(Blocks.COCOA))) {
                if (TreeRemovalUtil.fellTreeAt(world, up, cfg)) {
                    continue;
                }
            }
            if (tunnel) {
                if (st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || !st.canOcclude()) {
                    world.setBlock(up, Blocks.AIR.defaultBlockState(), 3);
                }
            } else {
                if (!st.is(BlockTags.LOGS) && !st.is(BlockTags.FENCES)) {
                    world.setBlock(up, Blocks.AIR.defaultBlockState(), 3);
                } else {
                    break;
                }
            }
        }
    }
}
