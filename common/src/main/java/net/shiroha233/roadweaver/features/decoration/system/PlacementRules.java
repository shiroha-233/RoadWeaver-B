package net.shiroha233.roadweaver.features.decoration.system;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.shiroha233.roadweaver.features.decoration.RoadFeatureCompat;

public final class PlacementRules {
    private PlacementRules() {}

    public static boolean placeAllowedCheck(Block block) {
        return !(RoadFeatureCompat.dontPlaceHere(block)
                || block.defaultBlockState().is(BlockTags.UNDERWATER_BONEMEALS)
                || block.defaultBlockState().is(BlockTags.WOODEN_FENCES)
                || block.defaultBlockState().is(BlockTags.PLANKS)
        );
    }
}
