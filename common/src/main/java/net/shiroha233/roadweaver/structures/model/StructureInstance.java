package net.shiroha233.roadweaver.structures.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import java.util.UUID;

public final class StructureInstance {
    public static final Codec<AABB> AABB_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("min_x").forGetter(box -> box.minX),
                    Codec.DOUBLE.fieldOf("min_y").forGetter(box -> box.minY),
                    Codec.DOUBLE.fieldOf("min_z").forGetter(box -> box.minZ),
                    Codec.DOUBLE.fieldOf("max_x").forGetter(box -> box.maxX),
                    Codec.DOUBLE.fieldOf("max_y").forGetter(box -> box.maxY),
                    Codec.DOUBLE.fieldOf("max_z").forGetter(box -> box.maxZ)
            ).apply(instance, (minX, minY, minZ, maxX, maxY, maxZ) -> new AABB(minX, minY, minZ, maxX, maxY, maxZ))
    );

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<StructureInstance> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("id").forGetter(StructureInstance::instanceId),
                    ResourceLocation.CODEC.fieldOf("blueprint_id").forGetter(StructureInstance::blueprintId),
                    ResourceLocation.CODEC.fieldOf("template_id").forGetter(StructureInstance::variantTemplateId),
                    ResourceLocation.CODEC.fieldOf("dimension_id").forGetter(StructureInstance::dimensionId),
                    BlockPos.CODEC.fieldOf("anchor").forGetter(StructureInstance::anchorPos),
                    AABB_CODEC.fieldOf("bounds").forGetter(StructureInstance::bounds),
                    Codec.LONG.fieldOf("placed_at").forGetter(StructureInstance::placedAt)
            ).apply(instance, StructureInstance::new)
    );

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
