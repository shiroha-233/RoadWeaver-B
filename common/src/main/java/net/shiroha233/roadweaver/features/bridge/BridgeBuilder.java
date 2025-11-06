package net.shiroha233.roadweaver.features.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.helpers.Records;

 

public final class BridgeBuilder {
    private BridgeBuilder() {}

    private static final BlockState DECK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState RAIL = Blocks.STONE_BRICK_WALL.defaultBlockState();
    private static final BlockState PIER = Blocks.STONE_BRICKS.defaultBlockState();

    public static void placeSegment(WorldGenLevel world,
                                    Records.RoadSegmentPlacement seg,
                                    BlockPos middle,
                                    BlockPos prev,
                                    BlockPos next,
                                    int roadWidth,
                                    int deckY,
                                    int segmentIndex,
                                    RandomSource random,
                                    ModConfig cfg,
                                    boolean placePier,
                                    boolean placeRail) {
        int radius = Math.max(1, roadWidth / 2);
        // 使用 prev -> next 的方向，转角处更稳定
        int sx = Integer.compare(next.getX() - prev.getX(), 0);
        int sz = Integer.compare(next.getZ() - prev.getZ(), 0);
        int ox = -sz; // 正交方向分量
        int oz = sx;

        // 1) 在 prev → middle 之间连续铺设桥面（带宽度）
        plotLine(prev.getX(), prev.getZ(), middle.getX(), middle.getZ(), (x, z) -> {
            for (int off = -radius; off <= radius; off++) {
                int px = x + off * ox;
                int pz = z + off * oz;
                world.setBlock(new BlockPos(px, deckY, pz), DECK, 3);
                if (sx != 0 && sz != 0) {
                    world.setBlock(new BlockPos(px + sx, deckY, pz), DECK, 3);
                    world.setBlock(new BlockPos(px, deckY, pz + sz), DECK, 3);
                }
            }
        });

        // 1.1) 再用本段横向点集补齐一次，避免斜向或转角出现缺块
        for (BlockPos w : seg.positions()) {
            world.setBlock(new BlockPos(w.getX(), deckY, w.getZ()), DECK, 3);
        }

        // 2) 连续护栏
        if (placeRail && cfg.bridgeRailingEnabled()) {
            final int leftOutX = -ox, leftOutZ = -oz;
            plotLine(prev.getX() + (-radius) * ox, prev.getZ() + (-radius) * oz,
                    middle.getX() + (-radius) * ox, middle.getZ() + (-radius) * oz,
                    (x, z) -> {
                        if (!hasDeck(world, x, deckY, z)) return;
                        if (hasDeck(world, x + leftOutX, deckY, z + leftOutZ)) return;
                        if (!(hasDeck(world, x + sx, deckY, z + sz) || hasDeck(world, x - sx, deckY, z - sz))) return;
                        if (deckNeighborCount(world, x, deckY, z) <= 1) return;
                        world.setBlock(new BlockPos(x, deckY + 1, z), RAIL, 3);
                    });

            final int rightOutX = ox, rightOutZ = oz;
            plotLine(prev.getX() + (radius) * ox, prev.getZ() + (radius) * oz,
                    middle.getX() + (radius) * ox, middle.getZ() + (radius) * oz,
                    (x, z) -> {
                        if (!hasDeck(world, x, deckY, z)) return;
                        if (hasDeck(world, x + rightOutX, deckY, z + rightOutZ)) return;
                        if (!(hasDeck(world, x + sx, deckY, z + sz) || hasDeck(world, x - sx, deckY, z - sz))) return;
                        if (deckNeighborCount(world, x, deckY, z) <= 1) return;
                        world.setBlock(new BlockPos(x, deckY + 1, z), RAIL, 3);
                    });
        }

        // 3) 桥墩（按段间隔）
        if (placePier) {
            int interval = Math.max(3, cfg.bridgePierInterval());
            if (segmentIndex % interval == 0) {
                placePierUnder(world, middle.getX(), middle.getZ(), deckY - 1, cfg.bridgePierMaxHeight(), cfg.bridgePierWidth());
            }
        }
    }

    private static boolean hasDeck(WorldGenLevel world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock() == DECK.getBlock();
    }

    private static int deckNeighborCount(WorldGenLevel world, int x, int y, int z) {
        int c = 0;
        if (hasDeck(world, x + 1, y, z)) c++;
        if (hasDeck(world, x - 1, y, z)) c++;
        if (hasDeck(world, x, y, z + 1)) c++;
        if (hasDeck(world, x, y, z - 1)) c++;
        return c;
    }

    // 简单 Bresenham 画线器（含终点）
    private static void plotLine(int x0, int z0, int x1, int z1, LineConsumer consumer) {
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;
        int x = x0, z = z0;
        while (true) {
            consumer.accept(x, z);
            if (x == x1 && z == z1) break;
            int e2 = 2 * err;
            if (e2 > -dz) { err -= dz; x += sx; }
            if (e2 < dx) { err += dx; z += sz; }
        }
    }

    @FunctionalInterface
    private interface LineConsumer { void accept(int x, int z); }

    private static void placePierUnder(WorldGenLevel world, int x, int z, int fromY, int maxHeight, int pierWidth) {
        int minY = world.getMinBuildHeight();
        int half = Math.max(0, pierWidth - 1);
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int y = fromY;
                int h = 0;
                while (y >= minY && h < maxHeight) {
                    BlockPos cur = new BlockPos(x + dx, y, z + dz);
                    // 若当前方块可承重，则停止在其上方，不再继续向下
                    if (world.getBlockState(cur).isFaceSturdy(world, cur, Direction.UP)) {
                        break;
                    }
                    world.setBlock(cur, PIER, 3);
                    y--;
                    h++;
                }
            }
        }
    }
}
