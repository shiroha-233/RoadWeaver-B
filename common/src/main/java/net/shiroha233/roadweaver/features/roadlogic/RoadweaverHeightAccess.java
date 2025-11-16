package net.shiroha233.roadweaver.features.roadlogic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * RoadWeaver 统一高度访问接口。
 * <p>
 * 实现该接口的 ChunkGenerator（通常通过 Mixin 注入）需要提供一个带内部缓存的高度查询：
 * 对同一 ServerLevel + (x,z) + Heightmap.Types 的多次调用，应尽量避免重复的底层噪声计算。
 * <p>
 * 这样可以让道路寻路等高层逻辑在不关心具体地形模组实现的前提下，
 * 统一复用底层生成器中的高度缓存。
 */
public interface RoadweaverHeightAccess {
    int roadweaver$getCachedBaseHeight(ServerLevel level, int x, int z, Heightmap.Types type);
}
