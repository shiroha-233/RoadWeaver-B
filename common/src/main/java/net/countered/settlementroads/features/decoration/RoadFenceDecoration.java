package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.BiomeWoodAware;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

public class RoadFenceDecoration extends OrientedDecoration implements BiomeWoodAware {
    private final boolean leftRoadSide; // 未使用，占位对齐原始定义
    private final int fenceLength;
    private Records.WoodAssets wood;

    public RoadFenceDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world, boolean leftRoadSide, int fenceLength) {
        super(pos, direction, world);
        this.leftRoadSide = leftRoadSide;
        this.fenceLength = Math.min(3, Math.max(1, fenceLength));
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        BlockPos basePos = this.getPos();
        WorldGenLevel world = this.getWorld();
        Vec3i roadDirection = getOrthogonalVector();

        // 沿道路方向放置
        Vec3i fenceDirection = new Vec3i(roadDirection.getZ(), 0, -roadDirection.getX());

        for (int i = 0; i < fenceLength; i++) {
            BlockPos fencePos = basePos.offset(fenceDirection.multiply(i));
            BlockPos surfacePos = fencePos.atY(world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, fencePos.getX(), fencePos.getZ()));
            if (Math.abs(surfacePos.getY() - basePos.getY()) > 1) {
                continue;
            }
            world.setBlock(surfacePos, wood.fence().defaultBlockState(), 3);
        }
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }
}
