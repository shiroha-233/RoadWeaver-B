package net.countered.settlementroads.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

/**
 * Terrain cost calculator
 * Calculates costs based on terrain characteristics
 */
public class TerrainCostCalculator implements BiomeCostCalculator {
    
    // LOGGER removed as it was not being used
    
    private final double elevationCostMultiplier;
    private final double stabilityCostMultiplier;
    private final double seaLevelCost;
    private final int seaLevel;
    private final int maxHeightDifference;
    private final int maxTerrainStability;
    
    public TerrainCostCalculator(double elevationCostMultiplier, 
                               double stabilityCostMultiplier, 
                               double seaLevelCost,
                               int seaLevel,
                               int maxHeightDifference,
                               int maxTerrainStability) {
        this.elevationCostMultiplier = elevationCostMultiplier;
        this.stabilityCostMultiplier = stabilityCostMultiplier;
        this.seaLevelCost = seaLevelCost;
        this.seaLevel = seaLevel;
        this.maxHeightDifference = maxHeightDifference;
        this.maxTerrainStability = maxTerrainStability;
    }
    
    @Override
    public double calculateCost(RegistryEntry<Biome> biome, BlockPos position, ServerWorld world) {
        double totalCost = 0.0;
        
        int y = position.getY();
        double elevationCost = calculateElevationCost(y, position, world);
        totalCost += elevationCost * elevationCostMultiplier;
        
        double stabilityCost = calculateTerrainStabilityCost(position, world);
        totalCost += stabilityCost * stabilityCostMultiplier;
        if (y == seaLevel) {
            totalCost += seaLevelCost;
        }
        
        return totalCost;
    }
    
    /**
     * Calculate elevation cost
     * @param y Y coordinate
     * @param position Position
     * @param world World
     * @return Elevation cost
     */
    private double calculateElevationCost(int y, BlockPos position, ServerWorld world) {
        int surfaceY = world.getChunkManager()
            .getChunkGenerator()
            .getHeightInGround(position.getX(), position.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, world.getChunkManager().getNoiseConfig());
        
        int heightDifference = Math.abs(y - surfaceY);
        if (heightDifference > maxHeightDifference) {
            return Double.MAX_VALUE;
        }
        
        return heightDifference;
    }
    
    /**
     * Calculate terrain stability cost
     * @param position Position
     * @param world World
     * @return Terrain stability cost
     */
    private double calculateTerrainStabilityCost(BlockPos position, ServerWorld world) {
        int y = position.getY();
        double stabilityCost = 0.0;
        
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos testPos = position.offset(direction);
            int testY = world.getChunkManager()
                .getChunkGenerator()
                .getHeightInGround(testPos.getX(), testPos.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, world.getChunkManager().getNoiseConfig());
            
            int elevation = Math.abs(y - testY);
            stabilityCost += elevation;
            if (stabilityCost > maxTerrainStability) {
                return Double.MAX_VALUE;
            }
        }
        
        return stabilityCost;
    }
    
    @Override
    public String getCalculatorId() {
        return "terrain_cost_calculator";
    }
    
    @Override
    public int getPriority() {
        return 3; // Higher priority, executes after biome calculators
    }
    
    /**
     * Get elevation cost multiplier
     * @return Elevation cost multiplier
     */
    public double getElevationCostMultiplier() {
        return elevationCostMultiplier;
    }
    
    /**
     * Get stability cost multiplier
     * @return Stability cost multiplier
     */
    public double getStabilityCostMultiplier() {
        return stabilityCostMultiplier;
    }
    
    /**
     * Get sea level cost
     * @return Sea level cost
     */
    public double getSeaLevelCost() {
        return seaLevelCost;
    }
    
    /**
     * Get sea level height
     * @return Sea level height
     */
    public int getSeaLevel() {
        return seaLevel;
    }
    
    /**
     * Get maximum height difference
     * @return Maximum height difference
     */
    public int getMaxHeightDifference() {
        return maxHeightDifference;
    }
    
    /**
     * Get maximum terrain stability
     * @return Maximum terrain stability
     */
    public int getMaxTerrainStability() {
        return maxTerrainStability;
    }
}
