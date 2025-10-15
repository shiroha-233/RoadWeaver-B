package net.countered.settlementroads.config.forge;

import net.countered.settlementroads.config.IModConfig;

import java.util.List;

public class ForgeModConfigAdapter implements IModConfig {
    
    @Override
    public List<String> structuresToLocate() {
        return ForgeJsonConfig.getStructuresToLocate();
    }

    @Override
    public int structureSearchRadius() {
        return ForgeJsonConfig.getStructureSearchRadius();
    }

    @Override
    public int initialLocatingCount() {
        return ForgeJsonConfig.getInitialLocatingCount();
    }

    @Override
    public int maxConcurrentRoadGeneration() {
        return ForgeJsonConfig.getMaxConcurrentRoadGeneration();
    }

    @Override
    public int structureSearchTriggerDistance() {
        return ForgeJsonConfig.getStructureSearchTriggerDistance();
    }

    @Override
    public int averagingRadius() {
        return ForgeJsonConfig.getAveragingRadius();
    }

    @Override
    public boolean allowArtificial() {
        return ForgeJsonConfig.getAllowArtificial();
    }

    @Override
    public boolean allowNatural() {
        return ForgeJsonConfig.getAllowNatural();
    }

    @Override
    public boolean placeWaypoints() {
        return ForgeJsonConfig.getPlaceWaypoints();
    }

    @Override
    public boolean placeRoadFences() {
        return ForgeJsonConfig.getPlaceRoadFences();
    }

    @Override
    public boolean placeSwings() {
        return ForgeJsonConfig.getPlaceSwings();
    }

    @Override
    public boolean placeBenches() {
        return ForgeJsonConfig.getPlaceBenches();
    }

    @Override
    public boolean placeGloriettes() {
        return ForgeJsonConfig.getPlaceGloriettes();
    }

    @Override
    public int structureDistanceFromRoad() {
        return ForgeJsonConfig.getStructureDistanceFromRoad();
    }

    @Override
    public int maxHeightDifference() {
        return ForgeJsonConfig.getMaxHeightDifference();
    }

    @Override
    public int maxTerrainStability() {
        return ForgeJsonConfig.getMaxTerrainStability();
    }

    @Override
    public int manualMaxHeightDifference() {
        return ForgeJsonConfig.getManualMaxHeightDifference();
    }

    @Override
    public int manualMaxTerrainStability() {
        return ForgeJsonConfig.getManualMaxTerrainStability();
    }

    @Override
    public boolean manualIgnoreWater() {
        return ForgeJsonConfig.getManualIgnoreWater();
    }
}
