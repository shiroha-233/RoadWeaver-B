package net.countered.settlementroads.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public abstract class Decoration {
    private BlockPos placePos;
    private final WorldGenLevel world;

    public Decoration(BlockPos placePos, WorldGenLevel world) {
        this.placePos = placePos;
        this.world = world;
    }

    public abstract void place();

    protected final boolean placeAllowed() {
        BlockPos placePos = getPos();
        BlockPos surfacePos = placePos.atY(world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ()));
        this.placePos = surfacePos;
        BlockState blockStateBelow = world.getBlockState(surfacePos.below());

        boolean belowInvalid = blockStateBelow.is(Blocks.WATER)
                || blockStateBelow.is(Blocks.LAVA)
                || blockStateBelow.is(BlockTags.LOGS)
                || RoadPlacementRules.dontPlaceHere.contains(blockStateBelow.getBlock());

        if (belowInvalid) {
            return false;
        }
        return true;
    }

    public BlockPos getPos() {
        return placePos;
    }

    public WorldGenLevel getWorld() {
        return world;
    }
}
