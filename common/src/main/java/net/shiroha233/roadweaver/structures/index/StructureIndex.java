package net.shiroha233.roadweaver.structures.index;

import net.shiroha233.roadweaver.structures.model.StructureInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import java.util.Collection;

public interface StructureIndex {
    void add(StructureInstance inst);
    void remove(StructureInstance inst);
    Collection<StructureInstance> query(AABB box);
    boolean existsNear(BlockPos pos, int radius);
    void clear();
}
