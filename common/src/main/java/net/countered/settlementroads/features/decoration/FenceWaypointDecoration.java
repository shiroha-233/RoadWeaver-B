package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.BiomeWoodAware;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

public class FenceWaypointDecoration extends Decoration implements BiomeWoodAware {
    private Records.WoodAssets wood;

    public FenceWaypointDecoration(BlockPos placePos, WorldGenLevel world) {
        super(placePos, world);
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        BlockPos surfacePos = this.getPos();
        WorldGenLevel world = this.getWorld();

        world.setBlock(surfacePos, wood.fence().defaultBlockState(), 3);
        world.setBlock(surfacePos.above(), Blocks.TORCH.defaultBlockState(), 3);
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }
}
