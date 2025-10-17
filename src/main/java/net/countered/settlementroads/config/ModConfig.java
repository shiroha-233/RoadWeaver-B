package net.countered.settlementroads.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class ModConfig extends MidnightConfig {

    @Entry(category = "structures")
    public static int maxLocatingCount = 100;

    @Entry(category = "structures")
    public static String structureToLocate = "#minecraft:village";

    @Entry(category = "structures", min = 100, max = 1000)
    public static int maxChunksForLocating = 300;

    @Entry(category = "pre-generation")
    public static int initialLocatingCount = 7;

    @Entry(category = "pre-generation", min = 1, max = 10)
    public static int maxConcurrentRoadGeneration = 3;

    @Entry(category = "roads")
    public static int averagingRadius = 1;

    @Entry(category = "roads", min = 3, max = 10)
    public static int maxHeightDifference = 5;

    @Entry(category = "roads", min = 2, max = 10)
    public static int maxTerrainStability = 4;

    @Entry(category = "roads", min = 10, max = 200)
    public static int segmentStartOffset = 60;

    @Entry(category = "roads", min = 10, max = 200)
    public static int segmentEndOffset = 60;

    @Entry(category = "roads", min = 1, max = 10)
    public static int maxClearBlocksAbove = 3;

    @Entry(category = "decorations", min = 10, max = 200)
    public static int decorationStartOffset = 65;

    public static String mainConfigFile = "config/settlement-roads/settlement-roads.json";
    public static String roadTypesConfigFile = "config/settlement-roads/road_types.json";
    public static String decorationsConfigFile = "config/settlement-roads/decorations.json";
    public static String biomeCostsConfigFile = "config/settlement-roads/biome_costs.json";
    public static String pathfindingConfigFile = "config/settlement-roads/pathfinding.json";
    
    public static boolean enableConfigValidation = true;
    public static boolean fallbackToDefaults = true;

    @Entry(category = "global")
    public static int heightCacheLimit = 1000;

    @Entry(category = "global")
    public static int maxConcurrentPathfinding = 5;

    @Entry(category = "roads")
    public static String replaceableBlocksConfigFile = "config/settlement-roads/replaceable_blocks.json";

}