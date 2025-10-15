package net.countered.settlementroads.helpers;

import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 通用结构定位实现（Common）。
 * 平台桥接类直接委托到此处，避免重复逻辑。
 */
public final class StructureLocatorImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    private StructureLocatorImpl() {}

    public static void locateConfiguredStructure(ServerLevel level, int locateCount, boolean locateAtPlayer) {
        if (locateCount <= 0) {
            return;
        }

        IModConfig config = ConfigProvider.get();
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();

        Records.StructureLocationData locationData = dataProvider.getStructureLocations(level);
        if (locationData == null) {
            locationData = new Records.StructureLocationData(new ArrayList<>());
        }

        List<BlockPos> knownLocations = new ArrayList<>(locationData.structureLocations());
        List<Records.StructureInfo> structureInfos = new ArrayList<>(locationData.structureInfos());
        Set<BlockPos> newlyFound = new HashSet<>();

        Optional<HolderSet<Structure>> targetStructures = resolveStructureTargets(level, config.structuresToLocate());
        if (targetStructures.isEmpty()) {
            LOGGER.warn("RoadWeaver: 无法解析结构目标列表，跳过定位。");
            return;
        }

        List<BlockPos> centers = collectSearchCenters(level, locateAtPlayer);
        int radius = Math.max(config.structureSearchRadius(), 1);
        LOGGER.debug("RoadWeaver: locating up to {} structure(s) - centers={}, radius={}, atPlayer={}", locateCount, centers.size(), radius, locateAtPlayer);

        for (BlockPos center : centers) {
            if (locateCount <= 0) {
                break;
            }

            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource()
                    .getGenerator()
                    .findNearestMapStructure(level, targetStructures.get(), center, radius, true);

            if (result != null) {
                BlockPos structurePos = result.getFirst();
                Holder<Structure> structureHolder = result.getSecond();
                
                if (!containsBlockPos(knownLocations, structurePos)) {
                    knownLocations.add(structurePos);
                    newlyFound.add(structurePos);
                    
                    // 保存结构类型信息
                    String structureId = structureHolder.unwrapKey()
                            .map(key -> key.location().toString())
                            .orElse("unknown");
                    structureInfos.add(new Records.StructureInfo(structurePos, structureId));
                    
                    locateCount--;
                }
            }
        }

        if (!newlyFound.isEmpty()) {
            dataProvider.setStructureLocations(level, new Records.StructureLocationData(knownLocations, structureInfos));
            LOGGER.debug("RoadWeaver: 定位到 {} 个新结构: {}", newlyFound.size(), newlyFound);
        }
    }

    private static Optional<HolderSet<Structure>> resolveStructureTargets(ServerLevel level, String identifiers) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();

        if (identifiers == null || identifiers.isBlank()) {
            return Optional.empty();
        }

        String[] tokens = identifiers.split("[;,\\s]+");
        for (String raw : tokens) {
            if (raw == null) continue;
            String token = raw.trim()
                    .replace("\r", "")
                    .replace("\n", "");
            // 去除首尾引号/反引号，并去掉末尾标点（逗号/分号/中文标点）
            token = token.replaceAll("^[\\\"'`]+|[\\\"'`]+$", "");
            token = token.replaceAll("[,;，；]+$", "");
            // 规范化：去除 BOM、替换全角符号
            if (!token.isEmpty() && token.charAt(0) == '\uFEFF') token = token.substring(1);
            token = token
                    .replace('＃', '#')
                    .replace('“', ' ')
                    .replace('”', ' ')
                    .replace('「', ' ')
                    .replace('」', ' ')
                    .replace('『', ' ')
                    .replace('』', ' ')
                    .replace('《', ' ')
                    .replace('》', ' ')
                    .trim();
            if (token.isBlank()) continue;

            // 若存在 #，不论位置，将其视为标签起始
            int hashIdx = token.indexOf('#');
            if (hashIdx >= 0) {
                String tagToken = token.substring(hashIdx + 1).trim();
                try {
                    ResourceLocation tagId = new ResourceLocation(tagToken);
                    TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
                    registry.getTag(tag).ifPresentOrElse(named -> {
                        for (Holder<Structure> h : named) {
                            holders.add(h);
                        }
                    }, () -> LOGGER.warn("RoadWeaver: structure tag not found: #{}", tagToken));
                } catch (Exception ex) {
                    LOGGER.warn("RoadWeaver: invalid structure tag token skipped: #{} (raw='{}')", tagToken, raw);
                }
            } else {
                try {
                    // 去掉前置非法字符（如意外的引号/符号），保留斜杠
                    String cleaned = token.replaceAll("^[^a-z0-9_.:/\\-]+", "");
                    
                    // 支持通配符匹配（例如：modid:structure_*）
                    if (cleaned.contains("*")) {
                        String pattern = cleaned.replace("*", "");
                        int matchCount = 0;
                        for (var entry : registry.entrySet()) {
                            String structureId = entry.getKey().location().toString();
                            if (structureId.startsWith(pattern)) {
                                registry.getHolder(entry.getKey()).ifPresent(holders::add);
                                matchCount++;
                            }
                        }
                        if (matchCount > 0) {
                            LOGGER.info("RoadWeaver: 通配符 '{}' 匹配到 {} 个结构", cleaned, matchCount);
                        } else {
                            LOGGER.warn("RoadWeaver: 通配符 '{}' 未匹配到任何结构", cleaned);
                        }
                    } else {
                        // 精确匹配
                        ResourceLocation id = new ResourceLocation(cleaned);
                        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
                        registry.getHolder(key).ifPresentOrElse(holders::add,
                                () -> LOGGER.warn("RoadWeaver: structure id not found: {}", cleaned));
                    }
                } catch (Exception ex) {
                    LOGGER.warn("RoadWeaver: invalid structure id token skipped: {} (raw='{}')", token, raw);
                }
            }
        }

        if (holders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(HolderSet.direct(holders));
    }

    private static Optional<HolderSet<Structure>> resolveStructureTargets(ServerLevel level, java.util.List<String> identifiersList) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();

        if (identifiersList == null || identifiersList.isEmpty()) {
            return Optional.empty();
        }

        for (String line : identifiersList) {
            if (line == null) continue;
            String norm = line.replace('\r', ' ').replace('\n', ' ').trim();
            if (norm.isEmpty()) continue;
            // 允许行内继续使用逗号/分号/空白再分割
            String[] tokens = norm.split("[;,\\s]+");
            for (String raw : tokens) {
                if (raw == null) continue;
                String token = raw.trim();
                if (token.isEmpty()) continue;
                // 重用单字符串解析的清洗逻辑
                token = token
                        .replace("\r", "")
                        .replace("\n", "");
                token = token.replaceAll("^[\\\"'`]+|[\\\"'`]+$", "");
                token = token.replaceAll("[,;，；]+$", "");
                if (!token.isEmpty() && token.charAt(0) == '\uFEFF') token = token.substring(1);
                token = token
                        .replace('＃', '#')
                        .replace('“', ' ')
                        .replace('”', ' ')
                        .replace('「', ' ')
                        .replace('」', ' ')
                        .replace('『', ' ')
                        .replace('』', ' ')
                        .replace('《', ' ')
                        .replace('》', ' ')
                        .trim();
                if (token.isEmpty()) continue;

                int hashIdx = token.indexOf('#');
                if (hashIdx >= 0) {
                    String tagToken = token.substring(hashIdx + 1).trim();
                    try {
                        ResourceLocation tagId = new ResourceLocation(tagToken);
                        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
                        registry.getTag(tag).ifPresentOrElse(named -> {
                            for (Holder<Structure> h : named) holders.add(h);
                        }, () -> LOGGER.warn("RoadWeaver: structure tag not found: #{}", tagToken));
                    } catch (Exception ex) {
                        LOGGER.warn("RoadWeaver: invalid structure tag token skipped: #{} (line='{}')", tagToken, line);
                    }
                } else {
                    try {
                        String cleaned = token.replaceAll("^[^a-z0-9_.:/\\-]+", "");
                        
                        // 支持通配符匹配（例如：modid:structure_*）
                        if (cleaned.contains("*")) {
                            String pattern = cleaned.replace("*", "");
                            int matchCount = 0;
                            for (var entry : registry.entrySet()) {
                                String structureId = entry.getKey().location().toString();
                                if (structureId.startsWith(pattern)) {
                                    registry.getHolder(entry.getKey()).ifPresent(holders::add);
                                    matchCount++;
                                }
                            }
                            if (matchCount > 0) {
                                LOGGER.info("RoadWeaver: 通配符 '{}' 匹配到 {} 个结构", cleaned, matchCount);
                            } else {
                                LOGGER.warn("RoadWeaver: 通配符 '{}' 未匹配到任何结构", cleaned);
                            }
                        } else {
                            // 精确匹配
                            ResourceLocation id = new ResourceLocation(cleaned);
                            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
                            registry.getHolder(key).ifPresentOrElse(holders::add,
                                    () -> LOGGER.warn("RoadWeaver: structure id not found: {}", cleaned));
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("RoadWeaver: invalid structure id token skipped: {} (line='{}')", token, line);
                    }
                }
            }
        }

        if (holders.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(HolderSet.direct(holders));
    }

    private static List<BlockPos> collectSearchCenters(ServerLevel level, boolean locateAtPlayer) {
        List<BlockPos> centers = new ArrayList<>();
        if (locateAtPlayer) {
            for (ServerPlayer player : level.players()) {
                centers.add(player.blockPosition());
            }
        }

        BlockPos spawn = level.getSharedSpawnPos();
        if (centers.isEmpty()) {
            centers.add(spawn);
            // 扩展搜索：以出生点为中心，按配置半径的倍数在八个方向取样
            int r = Math.max(ConfigProvider.get().structureSearchRadius(), 1);
            int[] muls = new int[] {3, 6};
            for (int m : muls) {
                int d = r * m;
                centers.add(spawn.offset( d, 0,  0));
                centers.add(spawn.offset(-d, 0,  0));
                centers.add(spawn.offset( 0, 0,  d));
                centers.add(spawn.offset( 0, 0, -d));
                centers.add(spawn.offset( d, 0,  d));
                centers.add(spawn.offset(-d, 0,  d));
                centers.add(spawn.offset( d, 0, -d));
                centers.add(spawn.offset(-d, 0, -d));
            }
        }
        return centers;
    }

    private static boolean containsBlockPos(List<BlockPos> list, BlockPos pos) {
        for (BlockPos existing : list) {
            if (existing.equals(pos)) {
                return true;
            }
        }
        return false;
    }
}
