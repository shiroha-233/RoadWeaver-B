package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.BiomeWoodAware;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

public class LamppostDecoration extends OrientedDecoration implements BiomeWoodAware {
    private final boolean leftRoadSide;
    private Records.WoodAssets wood;

    public LamppostDecoration(BlockPos pos, Vec3i direction, StructureWorldAccess world, boolean leftRoadSide) {
        super(pos, direction, world);
        this.leftRoadSide = leftRoadSide;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        int rotation = getCardinalRotationFromVector(getOrthogonalVector(), leftRoadSide);
        DirectionProperties props = getDirectionProperties(rotation);

        BlockPos basePos = this.getPos();
        StructureWorldAccess world = this.getWorld();

        world.setBlockState(basePos.up(2).offset(props.offsetDirection.getOpposite()), Blocks.LANTERN.getDefaultState().with(Properties.HANGING, true), 3);
        placeFenceStructure(basePos, props);
    }

    private void placeFenceStructure(BlockPos pos, DirectionProperties props) {
        StructureWorldAccess world = this.getWorld();

        // Hanging Lantern oder Sign bereits vorher setzen!
        world.setBlockState(pos.up(3).offset(props.offsetDirection.getOpposite()), wood.fence().getDefaultState().with(props.directionProperty, true), 3);
        world.setBlockState(pos.up(0), wood.fence().getDefaultState(), 3);
        world.setBlockState(pos.up(1), wood.fence().getDefaultState(), 3);
        world.setBlockState(pos.up(2), wood.fence().getDefaultState(), 3);
        world.setBlockState(pos.up(3), wood.fence().getDefaultState().with(props.reverseDirectionProperty, true), 3);
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }
}
