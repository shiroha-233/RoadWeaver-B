package net.shiroha233.roadweaver.structures.model;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import java.util.UUID;

public final class StructureInstance {
    private final UUID instanceId;
    private final ResourceLocation blueprintId;
    private final ResourceLocation variantTemplateId;
    private final ResourceLocation dimensionId;
    private final BlockPos anchorPos;
    private final AABB bounds;
    private final long placedAt;

    public StructureInstance(UUID instanceId, ResourceLocation blueprintId, ResourceLocation variantTemplateId,
                             ResourceLocation dimensionId, BlockPos anchorPos, AABB bounds, long placedAt) {
        this.instanceId = instanceId;
        this.blueprintId = blueprintId;
        this.variantTemplateId = variantTemplateId;
        this.dimensionId = dimensionId;
        this.anchorPos = anchorPos;
        this.bounds = bounds;
        this.placedAt = placedAt;
    }

    public UUID instanceId() { return instanceId; }
    public ResourceLocation blueprintId() { return blueprintId; }
    public ResourceLocation variantTemplateId() { return variantTemplateId; }
    public ResourceLocation dimensionId() { return dimensionId; }
    public BlockPos anchorPos() { return anchorPos; }
    public AABB bounds() { return bounds; }
    public long placedAt() { return placedAt; }
}
