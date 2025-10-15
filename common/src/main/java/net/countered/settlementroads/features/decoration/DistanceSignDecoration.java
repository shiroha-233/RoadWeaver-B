package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.BiomeWoodAware;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Objects;

public class DistanceSignDecoration extends OrientedDecoration implements BiomeWoodAware {
    private final boolean isStart;
    private final String signText;
    private Records.WoodAssets wood;

    public DistanceSignDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world, Boolean isStart, String distanceText) {
        super(pos, direction, world);
        this.isStart = isStart;
        this.signText = distanceText;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;

        int rotation = getCardinalRotationFromVector(getOrthogonalVector(), isStart);
        DirectionProperties props = getDirectionProperties(rotation);

        BlockPos basePos = this.getPos();
        WorldGenLevel world = this.getWorld();

        BlockPos signPos = basePos.above(2).relative(props.offsetDirection.getOpposite());
        world.setBlock(signPos, wood.hangingSign().defaultBlockState()
                .setValue(BlockStateProperties.ROTATION_16, rotation)
                .setValue(BlockStateProperties.ATTACHED, true), 3);
        updateSigns(world, signPos, signText);

        placeFenceStructure(basePos, props);
    }

    private void placeFenceStructure(BlockPos pos, DirectionProperties props) {
        WorldGenLevel world = this.getWorld();

        world.setBlock(pos.above(3).relative(props.offsetDirection.getOpposite()), wood.fence().defaultBlockState().setValue(props.directionProperty, true), 3);
        world.setBlock(pos.above(0), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(1), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(2), wood.fence().defaultBlockState(), 3);
        world.setBlock(pos.above(3), wood.fence().defaultBlockState().setValue(props.reverseDirectionProperty, true), 3);
    }

    private void updateSigns(WorldGenLevel worldGenLevel, BlockPos surfacePos, String text) {
        Objects.requireNonNull(worldGenLevel.getLevel().getServer()).execute(() -> {
            BlockEntity signEntity = worldGenLevel.getBlockEntity(surfacePos);
            if (signEntity instanceof HangingSignBlockEntity signBlockEntity) {
                signBlockEntity.setLevel(worldGenLevel.getLevel());
                SignText signText = signBlockEntity.getFrontText();
                signText = (signText.setMessage(0, Component.translatable("sign.roadweaver.distance.separator")));
                signText = (signText.setMessage(1, Component.translatable("sign.roadweaver.distance.next_location")));
                signText = (signText.setMessage(2, Component.literal(text + "m")));
                signText = (signText.setMessage(3, Component.translatable("sign.roadweaver.distance.separator")));
                signBlockEntity.setText(signText, true);

                SignText signTextBack = signBlockEntity.getBackText();
                signTextBack = signTextBack.setMessage(0, Component.translatable("sign.roadweaver.distance.separator"));
                signTextBack = signTextBack.setMessage(1, Component.translatable("sign.roadweaver.distance.welcome"));
                signTextBack = signTextBack.setMessage(2, Component.translatable("sign.roadweaver.distance.traveller"));
                signTextBack = signTextBack.setMessage(3, Component.translatable("sign.roadweaver.distance.separator"));
                signBlockEntity.setText(signTextBack, false);

                signBlockEntity.setChanged();
            }
        });
    }

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }
}
