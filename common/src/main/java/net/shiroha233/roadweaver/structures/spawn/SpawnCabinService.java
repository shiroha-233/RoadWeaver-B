package net.shiroha233.roadweaver.structures.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.structures.StructureSystem;
import net.shiroha233.roadweaver.structures.api.BlendProfile;
import net.shiroha233.roadweaver.structures.api.StructureBlueprint;
import net.shiroha233.roadweaver.structures.api.StructureConnector;
import net.shiroha233.roadweaver.structures.api.StructureVariant;
import net.shiroha233.roadweaver.structures.api.SpawnRule;
import net.shiroha233.roadweaver.structures.model.StructureInstance;
import net.shiroha233.roadweaver.structures.pipeline.StructurePlacer;

import java.util.List;

public final class SpawnCabinService {
    private SpawnCabinService() {}

    private static final ResourceLocation BLUEPRINT_ID = new ResourceLocation("roadweaver", "spawn_cabin");
    private static final ResourceLocation TEMPLATE_ID = new ResourceLocation("roadweaver", "structures/starting_cabin");

    public static boolean ensurePlaced(ServerLevel level) {
        if (level == null) return false;
        // 幂等：若索引附近已存在实例则直接返回
        BlockPos spawn = level.getSharedSpawnPos();
        if (StructureSystem.index(level).existsNear(spawn, 64)) {
            return false;
        }
        // 若世界数据已有结构位置记录，则认为已首开完成
        var provider = WorldDataProvider.getInstance();
        var locs = provider.getStructureLocations(level);
        if (locs != null && locs.structureLocations() != null && !locs.structureLocations().isEmpty()) {
            return false;
        }

        // 注册一个最小蓝图（仅用于落地参数聚合）
        StructureBlueprint bp = StructureBlueprints.spawnCabin(BLUEPRINT_ID, TEMPLATE_ID);
        StructureSystem.registerBlueprint(bp);

        // 计算近地表锚点与预估包围盒
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        BlockPos anchor = new BlockPos(spawn.getX(), y, spawn.getZ());
        // 粗略包围盒（16x10x16），后续可由模板真实尺寸替换
        AABB bounds = new AABB(anchor).inflate(8, 5, 8);

        BlendProfile blend = BlendProfile.platformDefault();
        // 放置模板（内部目前返回实例，占位 AABB）
        StructureInstance inst = StructurePlacer.place(level, bp, TEMPLATE_ID, anchor, Rotation.NONE, Mirror.NONE, bounds, blend);

        // 写入索引与世界数据（记录锚点与实例）
        StructureSystem.index(level).add(inst);
        provider.addStructureLocation(level, anchor);
        provider.addStructureInstance(level, inst);
        return true;
    }

    static final class StructureBlueprints {
        static StructureBlueprint spawnCabin(ResourceLocation id, ResourceLocation templateId) {
            List<StructureVariant> variants = java.util.Collections.singletonList(new StructureVariant(templateId, 1, true));
            List<StructureConnector> connectors = java.util.Collections.emptyList();
            net.minecraft.core.Vec3i sizeHint = new net.minecraft.core.Vec3i(16, 10, 16);
            SpawnRule rule = new SpawnRule(
                    java.util.Collections.emptySet(),
                    java.util.Collections.emptySet(),
                    java.util.Collections.emptySet(),
                    32, 8, 0, 255, 20, 32
            );
            return new StructureBlueprint(id, variants, connectors, sizeHint, BlendProfile.platformDefault(), rule);
        }
    }
}
