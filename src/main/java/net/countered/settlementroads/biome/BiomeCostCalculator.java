package net.countered.settlementroads.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

/**
 * Biome cost calculator interface
 * Defines basic methods for biome cost calculation
 */
public interface BiomeCostCalculator {
    
    /**
     * Calculate biome cost
     * @param biome Biome
     * @param position Position
     * @param world World
     * @return Cost value
     */
    double calculateCost(RegistryEntry<Biome> biome, BlockPos position, ServerWorld world);
    
    /**
     * Get calculator ID
     * @return Calculator unique identifier
     */
    String getCalculatorId();
    
    /**
     * Get calculator priority
     * Higher values execute first
     * @return Priority
     */
    int getPriority();
}
