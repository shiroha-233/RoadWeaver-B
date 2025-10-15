package net.countered.settlementroads.helpers.forge;

import com.mojang.datafixers.util.Pair;
import net.countered.settlementroads.config.ConfigProvider;
import net.countered.settlementroads.config.IModConfig;
import net.countered.settlementroads.helpers.Records;
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

import java.util.List;
import java.util.Optional;

/**
 * Architectury @ExpectPlatform 实现类（Forge）。
 * 位置必须为：net.countered.settlementroads.helpers.forge.StructureLocatorImpl
 */
public final class StructureLocatorImpl {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    public static void locateConfiguredStructure(ServerLevel serverWorld, int locateCount, boolean locateAtPlayer) {
        // 委托到 common 实现以复用"多结构同时搜寻"逻辑
        net.countered.settlementroads.helpers.StructureLocatorImpl.locateConfiguredStructure(serverWorld, locateCount, locateAtPlayer);
    }

    private static void executeLocateStructure(BlockPos locatePos, ServerLevel serverWorld, String structureId) {
        IModConfig config = ConfigProvider.get();
        Registry<Structure> registry = serverWorld.registryAccess().registryOrThrow(Registries.STRUCTURE);
        
        HolderSet<Structure> structures;
        
        // 判断是标签还是单个结构
        if (structureId.startsWith("#")) {
            // 标签格式: #minecraft:village
            String tagId = structureId.substring(1);
            ResourceLocation tagLocation = new ResourceLocation(tagId);
            TagKey<Structure> tagKey = TagKey.create(Registries.STRUCTURE, tagLocation);
            
            Optional<HolderSet.Named<Structure>> tagOpt = registry.getTag(tagKey);
            if (tagOpt.isEmpty()) {
                LOGGER.warn("Structure tag not found: {}", structureId);
                return;
            }
            structures = tagOpt.get();
        } else {
            // 单个结构: minecraft:village_plains
            ResourceLocation structureLocation = new ResourceLocation(structureId);
            ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, structureLocation);
            
            Optional<Holder.Reference<Structure>> holderOpt = registry.getHolder(structureKey);
            if (holderOpt.isEmpty()) {
                LOGGER.warn("Structure not found: {}", structureId);
                return;
            }
            structures = HolderSet.direct(holderOpt.get());
        }
        
        // 搜寻结构 (skipKnownStructures=true 避免重复搜寻)
        Pair<BlockPos, Holder<Structure>> pair = serverWorld.getChunkSource()
                .getGenerator()
                .findNearestMapStructure(serverWorld, structures, locatePos, config.structureSearchRadius(), true);
        
        if (pair == null) {
            LOGGER.debug("❌ Structure not found for: {} (search radius: {})", structureId, config.structureSearchRadius());
            return;
        }
        
        BlockPos structureLocation = pair.getFirst();
        LOGGER.info("✅ Structure found at {} for {}", structureLocation, structureId);
        
        // 使用便捷方法添加结构
        WorldDataProvider dataProvider = WorldDataProvider.getInstance();
        int beforeCount = dataProvider.getStructureLocations(serverWorld).structureLocations().size();
        dataProvider.addStructureLocation(serverWorld, structureLocation);
        int afterCount = dataProvider.getStructureLocations(serverWorld).structureLocations().size();
        
        if (afterCount > beforeCount) {
            LOGGER.info("📍 Added new structure at {}, total: {}", structureLocation, afterCount);
        } else {
            LOGGER.debug("Structure already exists at {}", structureLocation);
        }
    }
}
