package net.countered.settlementroads.client.gui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.countered.settlementroads.config.forge.ForgeJsonConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigScreen {
    
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.roadweaver.title"))
                .setSavingRunnable(ForgeJsonConfig::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // 结构配置分类
        ConfigCategory structures = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.structures"));
        
        structures.addEntry(entryBuilder.startStrList(
                Component.translatable("config.roadweaver.structureToLocate"),
                new java.util.ArrayList<>(ForgeJsonConfig.getStructuresToLocate()))
                .setTooltip(Component.translatable("config.roadweaver.structureToLocate.tooltip"))
                .setExpanded(true)
                .setSaveConsumer(ForgeJsonConfig::setStructuresToLocate)
                .build());
        
        structures.addEntry(entryBuilder.startIntSlider(
                Component.translatable("config.roadweaver.structureSearchRadius"),
                ForgeJsonConfig.getStructureSearchRadius(),
                50, 200)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.roadweaver.structureSearchRadius.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setStructureSearchRadius)
                .build());
        
        // 预生成配置分类
        ConfigCategory preGeneration = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.pregeneration"));
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.initialLocatingCount"),
                ForgeJsonConfig.getInitialLocatingCount())
                .setDefaultValue(7)
                .setMin(1)
                .setMax(50)
                .setTooltip(Component.translatable("config.roadweaver.initialLocatingCount.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setInitialLocatingCount)
                .build());
        
        preGeneration.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxConcurrentRoadGeneration"),
                ForgeJsonConfig.getMaxConcurrentRoadGeneration())
                .setDefaultValue(3)
                .setMin(1)
                .setMax(256)
                .setTooltip(Component.translatable("config.roadweaver.maxConcurrentRoadGeneration.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setMaxConcurrentRoadGeneration)
                .build());
        
        preGeneration.addEntry(entryBuilder.startIntSlider(
                Component.translatable("config.roadweaver.structureSearchTriggerDistance"),
                ForgeJsonConfig.getStructureSearchTriggerDistance(),
                150, 1500)
                .setDefaultValue(500)
                .setTooltip(Component.translatable("config.roadweaver.structureSearchTriggerDistance.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setStructureSearchTriggerDistance)
                .build());
        
        // 道路配置分类
        ConfigCategory roads = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.roads"));
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.averagingRadius"),
                ForgeJsonConfig.getAveragingRadius())
                .setDefaultValue(1)
                .setMin(0)
                .setMax(5)
                .setTooltip(Component.translatable("config.roadweaver.averagingRadius.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setAveragingRadius)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowArtificial"),
                ForgeJsonConfig.getAllowArtificial())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowArtificial.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setAllowArtificial)
                .build());
        
        roads.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.allowNatural"),
                ForgeJsonConfig.getAllowNatural())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.allowNatural.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setAllowNatural)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.structureDistanceFromRoad"),
                ForgeJsonConfig.getStructureDistanceFromRoad())
                .setDefaultValue(4)
                .setMin(3)
                .setMax(8)
                .setTooltip(Component.translatable("config.roadweaver.structureDistanceFromRoad.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setStructureDistanceFromRoad)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxHeightDifference"),
                ForgeJsonConfig.getMaxHeightDifference())
                .setDefaultValue(5)
                .setMin(3)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxHeightDifference.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setMaxHeightDifference)
                .build());
        
        roads.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.maxTerrainStability"),
                ForgeJsonConfig.getMaxTerrainStability())
                .setDefaultValue(4)
                .setMin(2)
                .setMax(10)
                .setTooltip(Component.translatable("config.roadweaver.maxTerrainStability.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setMaxTerrainStability)
                .build());
        
        // 装饰配置分类
        ConfigCategory decorations = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.decorations"));
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeWaypoints"),
                ForgeJsonConfig.getPlaceWaypoints())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.roadweaver.placeWaypoints.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setPlaceWaypoints)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeRoadFences"),
                ForgeJsonConfig.getPlaceRoadFences())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeRoadFences.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setPlaceRoadFences)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeSwings"),
                ForgeJsonConfig.getPlaceSwings())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeSwings.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setPlaceSwings)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeBenches"),
                ForgeJsonConfig.getPlaceBenches())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeBenches.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setPlaceBenches)
                .build());
        
        decorations.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.placeGloriettes"),
                ForgeJsonConfig.getPlaceGloriettes())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.roadweaver.placeGloriettes.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setPlaceGloriettes)
                .build());
        
        // 手动模式配置分类
        ConfigCategory manual = builder.getOrCreateCategory(
                Component.translatable("config.roadweaver.category.manual"));
        
        manual.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.manualMaxHeightDifference"),
                ForgeJsonConfig.getManualMaxHeightDifference())
                .setDefaultValue(8)
                .setMin(3)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.manualMaxHeightDifference.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setManualMaxHeightDifference)
                .build());
        
        manual.addEntry(entryBuilder.startIntField(
                Component.translatable("config.roadweaver.manualMaxTerrainStability"),
                ForgeJsonConfig.getManualMaxTerrainStability())
                .setDefaultValue(8)
                .setMin(2)
                .setMax(20)
                .setTooltip(Component.translatable("config.roadweaver.manualMaxTerrainStability.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setManualMaxTerrainStability)
                .build());
        
        manual.addEntry(entryBuilder.startBooleanToggle(
                Component.translatable("config.roadweaver.manualIgnoreWater"),
                ForgeJsonConfig.getManualIgnoreWater())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.roadweaver.manualIgnoreWater.tooltip"))
                .setSaveConsumer(ForgeJsonConfig::setManualIgnoreWater)
                .build());
        
        return builder.build();
    }
}
