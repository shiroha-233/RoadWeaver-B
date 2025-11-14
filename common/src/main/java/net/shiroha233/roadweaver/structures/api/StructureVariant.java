package net.shiroha233.roadweaver.structures.api;

import net.minecraft.resources.ResourceLocation;

public final class StructureVariant {
    private final ResourceLocation templateId;
    private final int weight;
    private final boolean allowMirror;

    public StructureVariant(ResourceLocation templateId, int weight, boolean allowMirror) {
        this.templateId = templateId;
        this.weight = Math.max(1, weight);
        this.allowMirror = allowMirror;
    }

    public ResourceLocation templateId() { return templateId; }
    public int weight() { return weight; }
    public boolean allowMirror() { return allowMirror; }
}
