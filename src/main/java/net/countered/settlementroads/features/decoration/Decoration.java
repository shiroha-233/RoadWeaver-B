package net.countered.settlementroads.features.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;

public abstract class Decoration {
    private BlockPos placePos;
    private final StructureWorldAccess world;

    public Decoration(BlockPos placePos, StructureWorldAccess world) {
        this.placePos = placePos;
        this.world = world;
    }

    public abstract void place();
    
    /**
     * 获取装饰放置优先级
     * 优先级越高，越先放置
     * @return 放置优先级
     */
    public int getPlacementPriority() {
        return 0; // 默认优先级
    }

    protected final boolean placeAllowed() {
        BlockPos placePos = getPos();
        BlockPos surfacePos = placePos.withY(world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ()));
        this.placePos = surfacePos;
        BlockState blockStateBelow = world.getBlockState(surfacePos.down());

        boolean belowInvalid = blockStateBelow.isOf(Blocks.WATER)
                || blockStateBelow.isOf(Blocks.LAVA)
                || !net.countered.settlementroads.config.ForbiddenBlocksConfigLoader.isBlockReplaceable(blockStateBelow.getBlock());

        if (belowInvalid) {
            return false;
        }
        return true;
    }

    public BlockPos getPos() {
        return placePos;
    }

    public StructureWorldAccess getWorld() {
        return world;
    }
}
