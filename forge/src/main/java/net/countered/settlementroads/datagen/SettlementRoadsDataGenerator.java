package net.countered.settlementroads.datagen;

import net.countered.settlementroads.SettlementRoads;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge 数据生成器
 * 
 * 注意：configured_feature 和 placed_feature 已在 Common 模块中通过 JSON 定义
 * Forge 只需要 biome_modifier 来注入这些特性到生物群系
 * biome_modifier 文件位于: forge/src/main/resources/data/roadweaver/forge/biome_modifier/road_feature.json
 */
@Mod.EventBusSubscriber(modid = SettlementRoads.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SettlementRoadsDataGenerator {
    
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // Forge 版本不需要代码生成 configured/placed features
        // 这些已经在 Common 模块的 JSON 文件中定义
        // biome_modifier 会自动从 resources 目录加载
        
        SettlementRoads.getLogger().info("RoadWeaver data generation - using JSON-defined features from Common module");
    }
}
