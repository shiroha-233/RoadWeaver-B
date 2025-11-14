package net.shiroha233.roadweaver.structures.interop;

import net.minecraft.server.level.ServerLevel;
import net.shiroha233.roadweaver.structures.api.StructureBlueprint;
import net.shiroha233.roadweaver.structures.model.StructureInstance;

public final class RoadInteropBridge {
    private RoadInteropBridge() {}

    public static void publishConnectors(ServerLevel level, StructureInstance inst, StructureBlueprint bp) {
        // 占位：把 bp.connectors() 转为道路端点，写入 WorldDataProvider 并触发增量规划
    }
}
