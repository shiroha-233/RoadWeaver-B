package net.shiroha233.roadweaver.structures.api;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public final class SpawnRule {
    private final Set<ResourceLocation> dimensionAllow;
    private final Set<ResourceLocation> biomeAllowTags;
    private final Set<ResourceLocation> biomeDenyTags;
    private final int spacing;
    private final int separation;
    private final int minY;
    private final int maxY;
    private final int maxSlope;
    private final int avoidRadius;

    public SpawnRule(Set<ResourceLocation> dimensionAllow, Set<ResourceLocation> biomeAllowTags, Set<ResourceLocation> biomeDenyTags,
                     int spacing, int separation, int minY, int maxY, int maxSlope, int avoidRadius) {
        this.dimensionAllow = dimensionAllow;
        this.biomeAllowTags = biomeAllowTags;
        this.biomeDenyTags = biomeDenyTags;
        this.spacing = Math.max(1, spacing);
        this.separation = Math.max(0, separation);
        this.minY = minY;
        this.maxY = maxY;
        this.maxSlope = Math.max(0, maxSlope);
        this.avoidRadius = Math.max(0, avoidRadius);
    }

    public Set<ResourceLocation> dimensionAllow() { return dimensionAllow; }
    public Set<ResourceLocation> biomeAllowTags() { return biomeAllowTags; }
    public Set<ResourceLocation> biomeDenyTags() { return biomeDenyTags; }
    public int spacing() { return spacing; }
    public int separation() { return separation; }
    public int minY() { return minY; }
    public int maxY() { return maxY; }
    public int maxSlope() { return maxSlope; }
    public int avoidRadius() { return avoidRadius; }
}
