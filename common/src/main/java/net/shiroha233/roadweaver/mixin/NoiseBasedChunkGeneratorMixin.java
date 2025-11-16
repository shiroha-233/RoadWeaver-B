package net.shiroha233.roadweaver.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.shiroha233.roadweaver.features.roadlogic.RoadweaverHeightAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin implements RoadweaverHeightAccess {
    @Unique
    private final Map<Heightmap.Types, Map<Long, Integer>> roadweaver$heightCache = new ConcurrentHashMap<>();

    @Override
    public int roadweaver$getCachedBaseHeight(ServerLevel level, int x, int z, Heightmap.Types type) {
        if (level == null || type == null) {
            return 0;
        }
        Map<Long, Integer> perType = roadweaver$heightCache.computeIfAbsent(type, t -> new ConcurrentHashMap<>());
        long key = (((long) x) << 32) | (z & 0xffffffffL);
        Integer cached = perType.get(key);
        if (cached != null) {
            return cached;
        }
        RandomState rs = level.getChunkSource().getGeneratorState().randomState();
        int h = ((NoiseBasedChunkGenerator) (Object) this).getBaseHeight(x, z, type, level, rs);
        perType.put(key, h);
        return h;
    }
}
