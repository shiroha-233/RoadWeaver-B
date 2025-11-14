package net.shiroha233.roadweaver.structures.pipeline;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.shiroha233.roadweaver.structures.api.BlendProfile;
import net.shiroha233.roadweaver.structures.api.StructureBlueprint;
import net.shiroha233.roadweaver.structures.blend.BlendPlan;
import net.shiroha233.roadweaver.structures.blend.TerrainBlender;
import net.shiroha233.roadweaver.structures.model.StructureInstance;

import java.util.Optional;
import java.util.UUID;

public final class StructurePlacer {
    private StructurePlacer() {}

    public static StructureInstance place(ServerLevel level, StructureBlueprint bp, ResourceLocation variantTemplateId,
                                          BlockPos anchor, Rotation rotation, Mirror mirror, AABB preBounds, BlendProfile blend) {
        // 允许模板 ID 既可为 "roadweaver:starting_cabin"，也可能误传 "roadweaver:structures/starting_cabin"
        ResourceLocation id = normalizeTemplateId(variantTemplateId);
        StructureTemplateManager mgr = level.getServer().getStructureManager();
        Optional<StructureTemplate> opt = mgr.get(id);
        if (opt.isEmpty()) {
            // fallback: 再尝试原 ID
            opt = mgr.get(variantTemplateId);
        }
        if (opt.isEmpty()) {
            // 获取失败时，返回占位实例
            AABB b = (preBounds != null) ? preBounds : new AABB(anchor).inflate(8, 5, 8);
            return new StructureInstance(UUID.randomUUID(), bp.id(), id, level.dimension().location(), anchor, b, level.getGameTime());
        }
        StructureTemplate tpl = opt.get();

        // 根据旋转计算放置尺寸
        Vec3i raw = tpl.getSize();
        Vec3i size = rotatedSize(raw, rotation);
        BlockPos min = anchor;
        BlockPos max = anchor.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        AABB bounds = new AABB(min, max);

        // 放置前做一次简易地形融合（平台整平+环形软化）
        if (blend != null) {
            // 仅按 footprint 的水平范围进行整平
            AABB horiz = new AABB(min.getX(), level.getMinBuildHeight(), min.getZ(), max.getX(), level.getMaxBuildHeight(), max.getZ());
            BlendPlan plan = TerrainBlender.plan(level, horiz, blend);
            TerrainBlender.apply(level, plan);
        }

        // 托盘状地基：在模板投影平面 y-1 铺一层板，并沿四周向下填充围墙到地表
        buildTrayFoundation(level, min, max);

        // 放置模板
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror);
        tpl.placeInWorld(level, anchor, anchor, settings, level.getRandom(), 2);

        return new StructureInstance(UUID.randomUUID(), bp.id(), id, level.dimension().location(), anchor, bounds, level.getGameTime());
    }

    private static ResourceLocation normalizeTemplateId(ResourceLocation in) {
        String path = in.getPath();
        if (path.startsWith("structures/")) {
            return new ResourceLocation(in.getNamespace(), path.substring("structures/".length()));
        }
        return in;
    }

    private static Vec3i rotatedSize(Vec3i s, Rotation r) {
        return switch (r) {
            case NONE, CLOCKWISE_180 -> s;
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new Vec3i(s.getZ(), s.getY(), s.getX());
        };
    }

    private static void buildTrayFoundation(ServerLevel level, BlockPos min, BlockPos max) {
        int yTop = min.getY();          // 托盘顶层对齐结构地基（表层：草）
        int yMid = yTop - 1;            // 中间层（泥土）
        int yBot = yTop - 2;            // 底层（泥土）

        // 托盘扩展：比结构 footprint 在 X/Z 各方向多 3 格
        int expand = 3;
        int cx0 = min.getX() - expand;
        int cz0 = min.getZ() - expand;
        int cx1 = max.getX() + expand;
        int cz1 = max.getZ() + expand;
        int chamfer = 3; // 角倒角尺寸（与扩展一致）

        // 原始结构 footprint：完全不在 footprint 内铺设，避免与自带地基重叠
        int fx0 = min.getX();
        int fz0 = min.getZ();
        int fx1 = max.getX();
        int fz1 = max.getZ();

        // 铺设三层：顶层草方块、下面两层泥土
        for (int x = cx0; x <= cx1; x++) {
            for (int z = cz0; z <= cz1; z++) {
                if (!insideChamfered(x, z, cx0, cz0, cx1, cz1, chamfer)) continue;
                // 若在 footprint 内部则跳过，托盘只在结构外延伸
                boolean insideFootprint = (x >= fx0 && x <= fx1 && z >= fz0 && z <= fz1);
                if (insideFootprint) continue;

                // 顶层草
                level.setBlock(new BlockPos(x, yTop, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                // 中间/底层泥土
                level.setBlock(new BlockPos(x, yMid, z), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, yBot, z), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
            }
        }

        // 围裙：仅对托盘边界格子，从 yBot-1 向下填充到地表（与原地形联结）
        for (int x = cx0; x <= cx1; x++) {
            for (int z = cz0; z <= cz1; z++) {
                if (!insideChamfered(x, z, cx0, cz0, cx1, cz1, chamfer)) continue;
                boolean insideFootprint = (x >= fx0 && x <= fx1 && z >= fz0 && z <= fz1);
                if (insideFootprint) continue;

                boolean edge = false;
                if (!insideChamfered(x - 1, z, cx0, cz0, cx1, cz1, chamfer)) edge = true;
                else if (!insideChamfered(x + 1, z, cx0, cz0, cx1, cz1, chamfer)) edge = true;
                else if (!insideChamfered(x, z - 1, cx0, cz0, cx1, cz1, chamfer)) edge = true;
                else if (!insideChamfered(x, z + 1, cx0, cz0, cx1, cz1, chamfer)) edge = true;
                if (edge) {
                    fillDownToSurface(level, new BlockPos(x, yBot - 1, z));
                }
            }
        }
    }

    // 倒角规则：在四角以曼哈顿距离裁去一个等腰直角三角形区，形成斜切角
    private static boolean insideChamfered(int x, int z, int minX, int minZ, int maxX, int maxZ, int c) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) return false;
        // 左上角
        if ((x - minX) + (z - minZ) < c) return false;
        // 右上角
        if ((maxX - x) + (z - minZ) < c) return false;
        // 左下角
        if ((x - minX) + (maxZ - z) < c) return false;
        // 右下角
        if ((maxX - x) + (maxZ - z) < c) return false;
        return true;
    }

    private static void fillDownToSurface(ServerLevel level, BlockPos start) {
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start.getX(), start.getZ());
        for (int y = start.getY(); y >= Math.max(level.getMinBuildHeight(), top); y--) {
            level.setBlock(new BlockPos(start.getX(), y, start.getZ()), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
        }
    }
}
