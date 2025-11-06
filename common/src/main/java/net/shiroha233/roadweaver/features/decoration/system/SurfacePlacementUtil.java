package net.shiroha233.roadweaver.features.decoration.system;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.features.decoration.RoadFeatureCompat;

import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class SurfacePlacementUtil {
    private SurfacePlacementUtil() {}

    public static void placeOnSurface(WorldGenLevel world, BlockPos placePos, List<BlockState> material, int roadType, RandomSource random, ModConfig cfg) {
        double naturalBlockChance = 1.0;
        BlockPos surfacePos = placePos;
        BlockPos belowTop = surfacePos.below();
        BlockState blockStateAtPos = world.getBlockState(belowTop);
        boolean doPlace = (roadType == 0) || random.nextDouble() < naturalBlockChance;
        if (doPlace) {
            placeRoadBlock(world, blockStateAtPos, surfacePos, material, random, cfg);
        } else {
            clearAboveColumn(world, surfacePos, cfg);
        }
    }

    public static void placeRoadBlock(WorldGenLevel world, BlockState blockBelow, BlockPos surfacePos, List<BlockState> materials, RandomSource random, ModConfig cfg) {
        if (!placeAllowedCheck(blockBelow.getBlock())) return;
        BlockState chosen = materials.get(random.nextInt(materials.size()));

        final int MAX_CAUSEWAY_DEPTH = Math.max(0, Math.min(12, (cfg == null ? 1 : cfg.causewayMaxDepth())));//回填深度
        BlockPos below1 = surfacePos.below();
        BlockPos below2 = surfacePos.below(2);
        boolean sturdy1 = world.getBlockState(below1).isFaceSturdy(world, below1, Direction.UP);
        boolean sturdy2 = world.getBlockState(below2).isFaceSturdy(world, below2, Direction.UP);

        if (!sturdy1 && !sturdy2) {
            BlockPos cursor = below2;
            int depth = 0;
            BlockPos base = null;
            while (cursor.getY() > world.getMinBuildHeight() && depth < MAX_CAUSEWAY_DEPTH) {
                if (world.getBlockState(cursor).isFaceSturdy(world, cursor, Direction.UP)) {
                    base = cursor;
                    break;
                }
                cursor = cursor.below();
                depth++;
            }

            BlockPos fillStart = (base != null) ? base.above() : below1.below(Math.min(MAX_CAUSEWAY_DEPTH - 1, Math.max(0, below1.getY() - world.getMinBuildHeight())));
            if (fillStart.getY() < world.getMinBuildHeight()) {
                fillStart = new BlockPos(fillStart.getX(), world.getMinBuildHeight(), fillStart.getZ());
            }
            BlockPos pos = fillStart;
            while (pos.getY() <= below1.getY()) {
                world.setBlock(pos, chosen, 3);
                pos = pos.above();
            }
        } else {
            world.setBlock(below1, chosen, 3);
        }

        clearAboveColumn(world, surfacePos, cfg);

        BlockPos belowPos1 = surfacePos.below(2);
        BlockState belowState1 = world.getBlockState(belowPos1);
        if (belowState1.is(Blocks.GRASS_BLOCK)) {
            world.setBlock(belowPos1, Blocks.DIRT.defaultBlockState(), 3);
        }
    }

    public static void clearAboveColumn(WorldGenLevel world, BlockPos surfacePos, ModConfig cfg) {
        boolean tunnel = cfg != null && cfg.tunnelEnabled();
        int maxH = tunnel ? Math.max(2, Math.min(16, cfg.tunnelClearHeight())) : 3;
        for (int i = 0; i < maxH; i++) {
            BlockPos up = surfacePos.above(i);
            BlockState st = world.getBlockState(up);
            if (st.isAir()) continue;
            if (cfg != null && cfg.removeWholeTreeOnPath() && (st.is(BlockTags.LOGS) || st.is(Blocks.BAMBOO) || isVineLike(st) || st.is(Blocks.COCOA))) {
                if (fellTreeAt(world, up, cfg)) {
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

    private static boolean fellTreeAt(WorldGenLevel world, BlockPos logStart, ModConfig cfg) {
        if (cfg == null) return false;
        int radius = Math.max(2, Math.min(12, cfg.treeRemovalMaxRadius()));
        int maxH = Math.max(8, Math.min(64, cfg.treeRemovalMaxHeight()));
        int maxBlocks = Math.max(64, Math.min(8192, cfg.treeRemovalMaxBlocks()));
        int leavesConfirm = Math.max(0, Math.min(128, cfg.treeLeavesConfirm()));

        BlockState startState = world.getBlockState(logStart);
        if (!(startState.is(BlockTags.LOGS) || startState.is(Blocks.BAMBOO) || isVineLike(startState) || startState.is(Blocks.COCOA) || startState.is(Blocks.HANGING_ROOTS))) return false;
        BlockPos base = logStart;
        int steps = 0;
        while (steps < maxH) {
            BlockPos down = base.below();
            BlockState downSt = world.getBlockState(down);
            if (downSt.is(BlockTags.LOGS) || downSt.is(Blocks.BAMBOO)) {
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
            if (!(isLog || isLeaves || isBamboo || isVine || isCocoa || isHangingRoots)) continue;
            toRemove.add(p);
            if (isLeaves) leavesCount++;
            if (isBamboo) hasBamboo = true;
            if (isVine) hasVineLike = true;
            if (isCocoa) hasCocoa = true;

            // 6邻接
            BlockPos[] neigh = new BlockPos[]{p.above(), p.below(), p.north(), p.south(), p.east(), p.west()};
            for (BlockPos n : neigh) {
                long key = n.asLong();
                if (seen.add(key)) {
                    q.addLast(n);
                }
            }
        }

        if (toRemove.isEmpty()) return false;
        if (!hasBamboo && !hasVineLike && !hasCocoa && leavesCount < leavesConfirm) return false;
        for (BlockPos p : toRemove) {
            world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        return true;
    }

    private static boolean isVineLike(BlockState st) {
        return st.is(Blocks.VINE)
                || st.is(Blocks.CAVE_VINES)
                || st.is(Blocks.CAVE_VINES_PLANT)
                || st.is(Blocks.WEEPING_VINES)
                || st.is(Blocks.WEEPING_VINES_PLANT)
                || st.is(Blocks.TWISTING_VINES)
                || st.is(Blocks.TWISTING_VINES_PLANT);
    }

    private static boolean placeAllowedCheck(Block block) {
        return !(RoadFeatureCompat.dontPlaceHere(block)
                || block.defaultBlockState().is(BlockTags.LEAVES)
                || block.defaultBlockState().is(BlockTags.LOGS)
                || block.defaultBlockState().is(BlockTags.UNDERWATER_BONEMEALS)
                || block.defaultBlockState().is(BlockTags.WOODEN_FENCES)
                || block.defaultBlockState().is(BlockTags.PLANKS)
        );
    }
}
