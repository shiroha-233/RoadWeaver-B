package net.countered.settlementroads.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 秋千装饰（优先使用 NBT，失败时不生成）
 */
public class SwingDecoration extends StructureDecoration {

    public SwingDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world) {
        super(pos, direction, world, "swing", new Vec3i(4, 10, 4));
    }

    protected boolean checkSwingPlacement() {
        if (!super.placeAllowed()) return false;
        BlockPos basePos = getPos();
        WorldGenLevel world = getWorld();
        for (int y = 1; y <= 10; y++) {
            BlockPos checkPos = basePos.above(y);
            BlockState state = world.getBlockState(checkPos);
            if (!state.isAir()
                    && !state.getBlock().equals(Blocks.GRASS)
                    && !state.getBlock().equals(Blocks.TALL_GRASS)
                    && !state.is(BlockTags.FLOWERS)
                    && !state.is(BlockTags.LEAVES)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void place() {
        if (!checkSwingPlacement()) return;
        super.place();
    }

    @Override
    protected void placeFallbackStructure() {
        // 不生成备用结构，只使用NBT文件；若缺失则跳过
    }
}
