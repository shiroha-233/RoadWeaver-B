package net.shiroha233.roadweaver.structures.api;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class StructureBlueprint {
    private final ResourceLocation id;
    private final List<StructureVariant> variants;
    private final List<StructureConnector> connectors;
    private final Vec3i sizeHint;
    private final BlendProfile blendProfile;
    private final SpawnRule spawnRule;

    public StructureBlueprint(ResourceLocation id, List<StructureVariant> variants, List<StructureConnector> connectors,
                              Vec3i sizeHint, BlendProfile blendProfile, SpawnRule spawnRule) {
        this.id = id;
        this.variants = List.copyOf(variants);
        this.connectors = List.copyOf(connectors);
        this.sizeHint = sizeHint;
        this.blendProfile = blendProfile;
        this.spawnRule = spawnRule;
    }

    public ResourceLocation id() { return id; }
    public List<StructureVariant> variants() { return variants; }
    public List<StructureConnector> connectors() { return connectors; }
    public Vec3i sizeHint() { return sizeHint; }
    public BlendProfile blendProfile() { return blendProfile; }
    public SpawnRule spawnRule() { return spawnRule; }
}
