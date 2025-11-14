package net.shiroha233.roadweaver.structures.index;

import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.structures.StructureSystem;
import net.shiroha233.roadweaver.structures.model.StructureInstance;

import java.util.List;

/**
 * 从世界持久化数据恢复结构索引的工具类（Common）。
 *
 * 职责：
 * - 读取 WorldDataProvider 中的结构实例列表；
 * - 将这些实例注册到当前维度的 StructureSystem.index(level) 中；
 * - 不负责持久化，仅负责“内存索引重建”。
 */
public final class StructureIndexRestorer {
    private StructureIndexRestorer() {}

    public static void restore(ServerLevel level) {
        if (level == null) return;
        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<StructureInstance> list = provider.getStructureInstances(level);
        if (list == null || list.isEmpty()) return;
        StructureIndex index = StructureSystem.index(level);
        for (StructureInstance inst : list) {
            if (inst == null) continue;
            if (inst.bounds() == null) continue;
            index.add(inst);
        }
    }
}
