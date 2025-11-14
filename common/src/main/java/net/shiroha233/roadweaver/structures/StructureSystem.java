package net.shiroha233.roadweaver.structures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.structures.api.StructureBlueprint;
import net.shiroha233.roadweaver.structures.index.ChunkStructureIndex;
import net.shiroha233.roadweaver.structures.index.StructureIndex;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureSystem {
    private StructureSystem() {}

    private static final Map<ResourceLocation, StructureBlueprint> BLUEPRINTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, StructureIndex> INDICES = new ConcurrentHashMap<>();

    public static void registerBlueprint(StructureBlueprint bp) {
        BLUEPRINTS.put(Objects.requireNonNull(bp).id(), bp);
    }

    public static StructureBlueprint getBlueprint(ResourceLocation id) {
        return BLUEPRINTS.get(id);
    }

    public static StructureIndex index(ServerLevel level) {
        return INDICES.computeIfAbsent(level.dimension().location(), k -> new ChunkStructureIndex());
    }

    public static void clearAll() {
        BLUEPRINTS.clear();
        INDICES.clear();
    }
}
