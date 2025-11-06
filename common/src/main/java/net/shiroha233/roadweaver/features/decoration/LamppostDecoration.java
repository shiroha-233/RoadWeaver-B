package net.shiroha233.roadweaver.features.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.shiroha233.roadweaver.features.decoration.util.BiomeWoodAware;
import net.shiroha233.roadweaver.helpers.Records;

public class LamppostDecoration extends OrientedDecoration implements BiomeWoodAware {
    private final boolean leftRoadSide;
    private Records.WoodAssets wood;

    public LamppostDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world, boolean leftRoadSide) {
        super(pos, direction, world);
        this.leftRoadSide = leftRoadSide;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;
        BlockPos basePos = this.getPos();
        WorldGenLevel world = this.getWorld();
        if (this.leftRoadSide) { }
        placeLampStructure(basePos, world);
    }

    private void placeLampStructure(BlockPos pos, WorldGenLevel world) {
        world.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        world.setBlock(pos.above(1), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
        world.setBlock(pos.above(2), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(3), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(4), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);

        BlockPos lampPos = pos.above(5);
        world.setBlock(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);

        world.setBlock(lampPos.above(), Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
                .setValue(BlockStateProperties.INVERTED, true), 3);

        Block trap = trapdoorForWood(wood);
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos tpos = lampPos.relative(dir);
            BlockState st = trap.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, dir)
                    .setValue(BlockStateProperties.OPEN, false)
                    .setValue(BlockStateProperties.HALF, Half.TOP);
            if (st.hasProperty(BlockStateProperties.WATERLOGGED)) {
                st = st.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            world.setBlock(tpos, st, 3);
        }
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }

    private static Block trapdoorForWood(Records.WoodAssets assets) {
        if (assets == null || assets.planks() == null) return Blocks.SPRUCE_TRAPDOOR;
        Block p = assets.planks();
        if (p == Blocks.SPRUCE_PLANKS) return Blocks.SPRUCE_TRAPDOOR;
        if (p == Blocks.OAK_PLANKS) return Blocks.OAK_TRAPDOOR;
        if (p == Blocks.BIRCH_PLANKS) return Blocks.BIRCH_TRAPDOOR;
        if (p == Blocks.JUNGLE_PLANKS) return Blocks.JUNGLE_TRAPDOOR;
        if (p == Blocks.ACACIA_PLANKS) return Blocks.ACACIA_TRAPDOOR;
        if (p == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_TRAPDOOR;
        if (p == Blocks.MANGROVE_PLANKS) return Blocks.MANGROVE_TRAPDOOR;
        if (p == Blocks.BAMBOO_PLANKS) return Blocks.BAMBOO_TRAPDOOR;
        if (p == Blocks.CHERRY_PLANKS) return Blocks.CHERRY_TRAPDOOR;
        if (p == Blocks.CRIMSON_PLANKS) return Blocks.CRIMSON_TRAPDOOR;
        if (p == Blocks.WARPED_PLANKS) return Blocks.WARPED_TRAPDOOR;
        return Blocks.SPRUCE_TRAPDOOR;
    }
}
