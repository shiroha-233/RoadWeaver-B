package net.shiroha233.roadweaver.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.shiroha233.roadweaver.features.decoration.util.BiomeWoodAware;
import net.shiroha233.roadweaver.helpers.Records;

public class LanternPostDecoration extends OrientedDecoration implements BiomeWoodAware {
    private Records.WoodAssets wood;

    public LanternPostDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world) {
        super(pos, direction, world);
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;
        BlockPos base = this.getPos();
        WorldGenLevel world = this.getWorld();
        world.setBlock(base, wood.fence().defaultBlockState(), 3);
        world.setBlock(base.above(1), wood.fence().defaultBlockState(), 3);
        world.setBlock(base.above(2), wood.fence().defaultBlockState(), 3);
        world.setBlock(base.above(3), Blocks.LANTERN.defaultBlockState(), 3);
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }
}
