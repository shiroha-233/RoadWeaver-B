package net.shiroha233.roadweaver.features.decoration.system;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.features.decoration.Decoration;
import net.shiroha233.roadweaver.features.decoration.DistanceSignDecoration;
import net.shiroha233.roadweaver.features.decoration.LamppostDecoration;
import net.shiroha233.roadweaver.features.decoration.LanternPostDecoration;

import java.util.List;
import java.util.Set;

public final class DecorationPlanner {
    private DecorationPlanner() {}

    public enum Mode { ARTIFICIAL, NATURAL }

    private static final int SIGN_INDEX_OFFSET = 65;
    private static final int SIDE_OFFSET = 2;

    public static void placeOnSurface(WorldGenLevel world, BlockPos placePos, List<BlockState> material, RandomSource random, ModConfig cfg, Mode mode) {
        int roadType = (mode == Mode.ARTIFICIAL) ? 0 : 1;
        SurfacePlacementUtil.placeOnSurface(world, placePos, material, roadType, random, cfg);
    }

    public static void addDecoration(WorldGenLevel world,
                                     Set<Decoration> out,
                                     BlockPos placePos,
                                     int segmentIndex,
                                     BlockPos nextPos,
                                     BlockPos prevPos,
                                     List<BlockPos> middlePositions,
                                     int roadWidth,
                                     RandomSource random,
                                     ModConfig cfg,
                                     Mode mode) {
        int dx = nextPos.getX() - prevPos.getX();
        int dz = nextPos.getZ() - prevPos.getZ();
        double len = Math.sqrt((double) dx * dx + (double) dz * dz);
        int nx = len != 0 ? (int) Math.round(dx / len) : 0;
        int nz = len != 0 ? (int) Math.round(dz / len) : 0;
        Vec3i dir = new Vec3i(nx, 0, nz);
        Vec3i ortho = new Vec3i(-dir.getZ(), 0, dir.getX());
        int halfWidth = Math.max(1, roadWidth / 2);
        int sideOffset = Math.max(SIDE_OFFSET, halfWidth + 1);

        boolean isStart = (segmentIndex == SIGN_INDEX_OFFSET);
        if (segmentIndex == SIGN_INDEX_OFFSET || segmentIndex == middlePositions.size() - SIGN_INDEX_OFFSET) {
            BlockPos shifted = isStart ? placePos.offset(ortho.getX() * sideOffset, 0, ortho.getZ() * sideOffset)
                    : placePos.offset(-ortho.getX() * sideOffset, 0, -ortho.getZ() * sideOffset);
            int dist = computeApproxDistanceMeters(world, shifted, isStart, middlePositions);
            out.add(new DistanceSignDecoration(shifted, ortho, world, isStart, String.valueOf(dist)));
            return;
        }

        int interval = Math.max(1, cfg.lampInterval());
        if (segmentIndex % interval == 0) {
            boolean left = random.nextBoolean();
            BlockPos shifted = left ? placePos.offset(ortho.getX() * sideOffset, 0, ortho.getZ() * sideOffset)
                    : placePos.offset(-ortho.getX() * sideOffset, 0, -ortho.getZ() * sideOffset);
            shifted = new BlockPos(shifted.getX(), world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, shifted.getX(), shifted.getZ()), shifted.getZ());
            if (Math.abs(shifted.getY() - placePos.getY()) > 1) return;
            if (mode == Mode.ARTIFICIAL) {
                out.add(new LamppostDecoration(shifted, ortho, world));
            } else {
                out.add(new LanternPostDecoration(shifted, ortho, world));
            }
        }
    }

    private static int computeApproxDistanceMeters(WorldGenLevel world, BlockPos fromPos, boolean isStart, List<BlockPos> middlePositions) {
        BlockPos target = isStart ? middlePositions.get(middlePositions.size() - 1) : middlePositions.get(0);
        long dx = (long) target.getX() - fromPos.getX();
        long dz = (long) target.getZ() - fromPos.getZ();
        double d = Math.sqrt((double) dx * dx + (double) dz * dz);
        return (int) Math.round(d);
    }
}
