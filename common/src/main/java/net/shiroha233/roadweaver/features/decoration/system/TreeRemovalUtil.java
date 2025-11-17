package net.shiroha233.roadweaver.features.decoration.system;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.config.ModConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TreeRemovalUtil {
    private TreeRemovalUtil() {}

    public static boolean fellTreeAt(WorldGenLevel world, BlockPos logStart, ModConfig cfg) {
        if (cfg == null) return false;
        int radius = Math.max(2, Math.min(12, cfg.treeRemovalMaxRadius()));
        int maxH = Math.max(8, Math.min(64, cfg.treeRemovalMaxHeight()));
        int maxBlocks = Math.max(64, Math.min(8192, cfg.treeRemovalMaxBlocks()));
        int leavesConfirm = Math.max(0, Math.min(128, cfg.treeLeavesConfirm()));

        BlockState startState = world.getBlockState(logStart);
        boolean startIsTreeCore = startState.is(BlockTags.LOGS)
                || startState.is(Blocks.BAMBOO)
                || isVineLike(startState)
                || startState.is(Blocks.COCOA)
                || startState.is(Blocks.HANGING_ROOTS)
                || isMushroomLike(startState);
        if (!startIsTreeCore) return false;
        BlockPos base = logStart;
        int steps = 0;
        while (steps < maxH) {
            BlockPos down = base.below();
            BlockState downSt = world.getBlockState(down);
            if (downSt.is(BlockTags.LOGS) || downSt.is(Blocks.BAMBOO) || isMushroomLike(downSt)) {
                base = down;
                steps++;
            } else {
                break;
            }
        }

        int minX = base.getX() - radius;
        int maxX = base.getX() + radius;
        int minZ = base.getZ() - radius;
        int maxZ = base.getZ() + radius;
        int minY = Math.max(world.getMinBuildHeight(), base.getY() - 1);
        int maxY = Math.min(world.getMaxBuildHeight() - 1, base.getY() + maxH);

        Deque<BlockPos> q = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        List<BlockPos> toRemove = new java.util.ArrayList<>();
        int leavesCount = 0;
        boolean hasBamboo = false;
        boolean hasVineLike = false;
        boolean hasCocoa = false;
        boolean hasMushroom = false;

        q.add(base);
        seen.add(base.asLong());

        while (!q.isEmpty() && toRemove.size() < maxBlocks) {
            BlockPos p = q.pollFirst();
            if (p.getX() < minX || p.getX() > maxX || p.getZ() < minZ || p.getZ() > maxZ || p.getY() < minY || p.getY() > maxY) continue;
            BlockState st = world.getBlockState(p);
            boolean isLog = st.is(BlockTags.LOGS);
            boolean isLeaves = st.is(BlockTags.LEAVES);
            boolean isBamboo = st.is(Blocks.BAMBOO);
            boolean isVine = isVineLike(st);
            boolean isCocoa = st.is(Blocks.COCOA);
            boolean isHangingRoots = st.is(Blocks.HANGING_ROOTS);
            boolean isSnowLayer = st.is(Blocks.SNOW);

            boolean isMushroomStem = st.is(Blocks.MUSHROOM_STEM);
            boolean isMushroomCap = st.is(Blocks.RED_MUSHROOM_BLOCK) || st.is(Blocks.BROWN_MUSHROOM_BLOCK);
            boolean isMushroom = isMushroomStem || isMushroomCap;

            boolean coreTreeBlock = isLog || isLeaves || isBamboo || isVine || isCocoa || isHangingRoots || isMushroom;
            if (!coreTreeBlock) {
                if (isSnowLayer) {
                    toRemove.add(p);
                }
                continue;
            }

            toRemove.add(p);
            if (isLeaves) leavesCount++;
            if (isBamboo) hasBamboo = true;
            if (isVine) hasVineLike = true;
            if (isCocoa) hasCocoa = true;
            if (isMushroom) hasMushroom = true;

            BlockPos[] neigh = new BlockPos[]{p.above(), p.below(), p.north(), p.south(), p.east(), p.west()};
            for (BlockPos n : neigh) {
                long key = n.asLong();
                if (seen.add(key)) {
                    q.addLast(n);
                }
            }
        }

        if (toRemove.isEmpty()) return false;
        if (!hasBamboo && !hasVineLike && !hasCocoa && !hasMushroom && leavesCount < leavesConfirm) return false;
        for (BlockPos p : toRemove) {
            world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        return true;
    }

    public static boolean isVineLike(BlockState st) {
        return st.is(Blocks.VINE)
                || st.is(Blocks.CAVE_VINES)
                || st.is(Blocks.CAVE_VINES_PLANT)
                || st.is(Blocks.WEEPING_VINES)
                || st.is(Blocks.WEEPING_VINES_PLANT)
                || st.is(Blocks.TWISTING_VINES)
                || st.is(Blocks.TWISTING_VINES_PLANT);
    }

    public static boolean isMushroomLike(BlockState st) {
        return st.is(Blocks.MUSHROOM_STEM)
                || st.is(Blocks.RED_MUSHROOM_BLOCK)
                || st.is(Blocks.BROWN_MUSHROOM_BLOCK);
    }
}
