package net.shiroha233.roadweaver.search;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.core.Registry;

import net.shiroha233.roadweaver.helpers.Records;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构预测点验证服务：
 * 利用 vanilla 的 StructureCheck + ChunkScanAccess，对基于种子/噪声预测出的结构点做轻量验证，
 * 尽量剔除“伪结构点”，只保留真实存在结构起点的区块。
 *
 * 注意：
 * - 仅在后台线程（ComputeService / 规划线程等）调用，避免在主线程上进行磁盘 IO。
 * - 若底层环境不支持 chunkScanner 或出现异常，将直接返回原预测列表，不影响主流程。
 */
public final class StructureVerificationService {
    private StructureVerificationService() {}

    /**
     * 对一批预测结构点进行验证，返回“通过验证”的子集。
     *
     * 调用约定：只能在后台线程调用（例如 ComputeService 的线程池中），不要在主线程直接调用，
     * 否则 scanChunk 的 join 可能造成卡顿。
     */
    public static List<Records.StructureInfo> verifyPredictedStructures(ServerLevel level,
                                                                        List<Records.StructureInfo> predicted) {
        if (predicted == null || predicted.isEmpty()) {
            return List.of();
        }

        var source = level.getChunkSource();
        if (!(source instanceof ServerChunkCache)) {
            // 无法访问 ServerChunkCache（理论上不该发生），保持原样返回
            return new ArrayList<>(predicted);
        }
        ServerChunkCache chunkCache = (ServerChunkCache) source;
        var server = level.getServer();
        if (server == null) {
            return new ArrayList<>(predicted);
        }

        ChunkScanAccess scanAccess;
        try {
            scanAccess = chunkCache.chunkScanner();
        } catch (Throwable t) {
            // 某些环境若缺失该接口，直接退化为“不做验证”
            return new ArrayList<>(predicted);
        }

        var registryAccess = level.registryAccess();
        ChunkGenerator generator = chunkCache.getGenerator();
        RandomState randomState = chunkCache.randomState();
        BiomeSource biomeSource = generator.getBiomeSource();
        long seed = level.getSeed();

        // Vanilla 的结构校验器：内部通过 ChunkScanAccess 做 NBT 流式扫描，几乎不增加内存占用
        StructureCheck checker = new StructureCheck(
                scanAccess,
                registryAccess,
                server.getStructureManager(),
                level.dimension(),
                generator,
                randomState,
                level,
                biomeSource,
                seed,
                server.getFixerUpper()
        );

        Registry<Structure> structureRegistry = registryAccess.registryOrThrow(Registries.STRUCTURE);

        ArrayList<Records.StructureInfo> result = new ArrayList<>();

        for (Records.StructureInfo info : predicted) {
            String idStr = info.structureId();
            if (idStr == null || idStr.isEmpty()) {
                // 没有类型信息的预测点无法精确比对，此处选择保留，避免误删
                result.add(info);
                continue;
            }

            ResourceLocation rl = ResourceLocation.tryParse(idStr);
            if (rl == null) {
                result.add(info);
                continue;
            }

            Structure structure = structureRegistry.get(rl);
            if (structure == null) {
                // 不在当前维度结构注册表中，多半是配置或版本差异，出于安全考虑保留
                result.add(info);
                continue;
            }

            // 按预测位置所在区块做校验：StructurePredictor 也是基于相同的 placement 逻辑计算 locatePos，
            // 因此这里直接用 pos 对应的 ChunkPos 即可。
            ChunkPos chunkPos = new ChunkPos(info.pos().getX() >> 4, info.pos().getZ() >> 4);

            StructureCheckResult checkResult;
            try {
                checkResult = checker.checkStart(chunkPos, structure, false);
            } catch (Throwable t) {
                // 任何异常都不影响主流程：保守起见保留该点
                result.add(info);
                continue;
            }

            if (checkResult == StructureCheckResult.START_PRESENT) {
                // NBT 中确认存在结构起点：保留
                result.add(info);
            } else if (checkResult == StructureCheckResult.CHUNK_LOAD_NEEDED) {
                // 需要真正加载区块才能确认（例如老版本数据），为了避免误删，这里选择保留
                result.add(info);
            } else {
                // START_NOT_PRESENT：判定为“伪结构点”，丢弃
            }
        }

        return result;
    }
}
