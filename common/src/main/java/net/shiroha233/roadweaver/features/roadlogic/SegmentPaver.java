package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.shiroha233.roadweaver.config.ModConfig;
import net.shiroha233.roadweaver.features.decoration.system.SurfacePlacementUtil;
import net.shiroha233.roadweaver.helpers.Records;

import java.util.List;

public final class SegmentPaver {
    private SegmentPaver() {}

    public static void paveSegment(WorldGenLevel world,
                                   Records.RoadSegmentPlacement seg,
                                   int averageY,
                                   int roadType,
                                   List<BlockState> materials,
                                   RandomSource random,
                                   ModConfig cfg) {
        for (BlockPos widthBlock : seg.positions()) {
            BlockPos pos = new BlockPos(widthBlock.getX(), averageY, widthBlock.getZ());
            List<BlockState> mats;
            if (roadType == 1) {
                mats = net.shiroha233.roadweaver.features.decoration.util.BiomeRoadMaterialSelector.forBiome(world, pos);
            } else {
                mats = materials;
            }
            SurfacePlacementUtil.placeOnSurface(world, pos, mats, 0, random, cfg);
        }
    }
}
