package net.countered.settlementroads.config;

import java.util.List;

public interface IModConfig {
    // Structures
    List<String> structuresToLocate();
    int structureSearchRadius();

    // Pre-generation
    int initialLocatingCount();
    int maxConcurrentRoadGeneration();
    int structureSearchTriggerDistance();

    // Roads
    int averagingRadius();
    boolean allowArtificial();
    boolean allowNatural();
    boolean placeWaypoints();
    boolean placeRoadFences();
    boolean placeSwings();
    boolean placeBenches();
    boolean placeGloriettes();
    int structureDistanceFromRoad();
    int maxHeightDifference();
    int maxTerrainStability();

    // 手动连接时更激进的阈值
    int manualMaxHeightDifference();
    int manualMaxTerrainStability();
    boolean manualIgnoreWater();
}
