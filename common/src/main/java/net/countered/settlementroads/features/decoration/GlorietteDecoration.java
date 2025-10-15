package net.countered.settlementroads.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 凉亭装饰（优先使用 NBT，失败时不生成）
 */
public class GlorietteDecoration extends StructureDecoration {

    public GlorietteDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world) {
        super(pos, direction, world, "gloriette", new Vec3i(5, 6, 5));
    }

    protected boolean checkGloriettePlacement() {
        if (!super.placeAllowed()) return false;
        BlockPos basePos = getPos();
        WorldGenLevel world = getWorld();
        
        // 检查凉亭占用空间（5x6x5）是否可用
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 1; y <= 6; y++) {
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
        if (!checkGloriettePlacement()) return;
        super.place();
    }

    @Override
    protected void placeFallbackStructure() {
        // 不生成备用结构，只使用NBT文件；若缺失则跳过
    }
}
