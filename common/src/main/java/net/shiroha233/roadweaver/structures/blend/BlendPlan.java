package net.shiroha233.roadweaver.structures.blend;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Map;

public final class BlendPlan {
    public static final BlendPlan EMPTY = new BlendPlan(new AABB(0,0,0,0,0,0), 0, 0, 0, false, java.util.Collections.emptyMap());

    private final AABB area;
    private final int targetY;
    private final int ringInner;
    private final int ringOuter;
    private final boolean fellTree;
    // 计划阶段计算好的列目标高度表（x,z -> desiredY）
    private final Map<Long, Integer> desiredHeights;

    public BlendPlan(AABB area, int targetY, int ringInner, int ringOuter, boolean fellTree, Map<Long, Integer> desiredHeights) {
        this.area = area;
        this.targetY = targetY;
        this.ringInner = ringInner;
        this.ringOuter = ringOuter;
        this.fellTree = fellTree;
        this.desiredHeights = (desiredHeights == null) ? Collections.emptyMap() : desiredHeights;
    }

    public AABB area() { return area; }
    public int targetY() { return targetY; }
    public int ringInner() { return ringInner; }
    public int ringOuter() { return ringOuter; }
    public boolean fellTree() { return fellTree; }
    public Map<Long, Integer> desiredHeights() { return desiredHeights; }

    public static long key2D(int x, int z) { return (((long)x) << 32) ^ (z & 0xffffffffL); }
    public static BlockPos from2DKey(long k, int y) { return new BlockPos((int)(k>>32), y, (int)k); }
}
