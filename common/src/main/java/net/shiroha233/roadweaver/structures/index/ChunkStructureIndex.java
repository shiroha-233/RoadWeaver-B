package net.shiroha233.roadweaver.structures.index;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaver.structures.model.StructureInstance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkStructureIndex implements StructureIndex {
    private final Map<Long, Set<StructureInstance>> byChunk = new ConcurrentHashMap<>();

    private static long key(ChunkPos cp) { return (((long)cp.x) << 32) ^ (cp.z & 0xffffffffL); }

    @Override
    public void add(StructureInstance inst) {
        AABB b = inst.bounds();
        if (b == null) return;
        int minCX = (int)Math.floor(b.minX / 16.0);
        int maxCX = (int)Math.floor(b.maxX / 16.0);
        int minCZ = (int)Math.floor(b.minZ / 16.0);
        int maxCZ = (int)Math.floor(b.maxZ / 16.0);
        for (int cx=minCX; cx<=maxCX; cx++) {
            for (int cz=minCZ; cz<=maxCZ; cz++) {
                byChunk.computeIfAbsent(key(new ChunkPos(cx, cz)), k -> ConcurrentHashMap.newKeySet()).add(inst);
            }
        }
    }

    @Override
    public void remove(StructureInstance inst) {
        for (Set<StructureInstance> set : byChunk.values()) {
            set.remove(inst);
        }
    }

    @Override
    public java.util.Collection<StructureInstance> query(AABB box) {
        if (box == null) return java.util.Collections.emptyList();
        int minCX = (int)Math.floor(box.minX / 16.0);
        int maxCX = (int)Math.floor(box.maxX / 16.0);
        int minCZ = (int)Math.floor(box.minZ / 16.0);
        int maxCZ = (int)Math.floor(box.maxZ / 16.0);
        java.util.Set<StructureInstance> result = new java.util.HashSet<>();
        for (int cx=minCX; cx<=maxCX; cx++) {
            for (int cz=minCZ; cz<=maxCZ; cz++) {
                java.util.Set<StructureInstance> set = byChunk.get(key(new ChunkPos(cx, cz)));
                if (set != null) {
                    for (StructureInstance s : set) {
                        if (s.bounds() != null && s.bounds().intersects(box)) {
                            result.add(s);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean existsNear(BlockPos pos, int radius) {
        AABB box = new AABB(pos.getX()-radius, pos.getY()-radius, pos.getZ()-radius,
                            pos.getX()+radius, pos.getY()+radius, pos.getZ()+radius);
        return !query(box).isEmpty();
    }

    @Override
    public void clear() {
        byChunk.clear();
    }
}
