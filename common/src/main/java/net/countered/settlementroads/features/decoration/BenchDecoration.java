package net.countered.settlementroads.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 长椅装饰（优先使用 NBT，失败时不生成）
 */
public class BenchDecoration extends StructureDecoration {

    public BenchDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world) {
        super(pos, direction, world, "bench", new Vec3i(3, 3, 2));
    }

    protected boolean checkBenchPlacement() {
        if (!super.placeAllowed()) return false;
        BlockPos basePos = getPos();
        WorldGenLevel world = getWorld();
        
        // 检查长椅占用空间（3x3x2）是否可用
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 1; y <= 3; y++) {
                    BlockPos checkPos = basePos.offset(x, y, z);
                    BlockState state = world.getBlockState(checkPos);
                    if (!state.isAir()
                            && !state.getBlock().equals(Blocks.GRASS)
                            && !state.getBlock().equals(Blocks.TALL_GRASS)
                            && !state.is(BlockTags.FLOWERS)
                            && !state.is(BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void place() {
        if (!checkBenchPlacement()) return;
        super.place();
    }

    @Override
    protected void placeFallbackStructure() {
        // 不生成备用结构，只使用NBT文件；若缺失则跳过
    }
}
