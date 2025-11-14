package net.shiroha233.roadweaver.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;

public abstract class Decoration {
    private BlockPos placePos;
    private final WorldGenLevel world;

    public Decoration(BlockPos placePos, WorldGenLevel world) {
        this.placePos = placePos;
        this.world = world;
    }

    public abstract void place();

    public BlockPos computeSurfacePos() {
        return new BlockPos(placePos.getX(), world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, placePos.getX(), placePos.getZ()), placePos.getZ());
    }

    public boolean isPlaceAllowedAt(BlockPos surfacePos) {
        BlockState below = world.getBlockState(surfacePos.below());
        boolean belowInvalid = below.is(Blocks.WATER) || below.is(Blocks.LAVA) || below.is(BlockTags.LOGS) || RoadFeatureCompat.dontPlaceHere(below.getBlock());
        return !belowInvalid;
    }

    protected final boolean placeAllowed() {
        BlockPos surfacePos = computeSurfacePos();
        this.placePos = surfacePos;
        return isPlaceAllowedAt(surfacePos);
    }

    public BlockPos getPos() { return placePos; }

    public WorldGenLevel getWorld() { return world; }
}
