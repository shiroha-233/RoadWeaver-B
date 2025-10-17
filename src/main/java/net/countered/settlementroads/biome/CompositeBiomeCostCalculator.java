package net.countered.settlementroads.biome;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Composite biome cost calculator
 * Combines results from multiple cost calculators
 */
public class CompositeBiomeCostCalculator {
    
    private final List<BiomeCostCalculator> calculators;
    
    public CompositeBiomeCostCalculator() {
        this.calculators = new ArrayList<>();
    }
    
    /**
     * Add cost calculator
     * @param calculator Cost calculator
     */
    public void addCalculator(BiomeCostCalculator calculator) {
        calculators.add(calculator);
        // Sort by priority
        calculators.sort(Comparator.comparingInt(BiomeCostCalculator::getPriority));
    }
    
    /**
     * Remove cost calculator
     * @param calculator Cost calculator
     * @return Whether removal was successful
     */
    public boolean removeCalculator(BiomeCostCalculator calculator) {
        return calculators.remove(calculator);
    }
    
    /**
     * Calculate total cost
     * @param biome Biome
     * @param position Position
     * @param world World
     * @return Total cost
     */
    public double calculateTotalCost(RegistryEntry<Biome> biome, BlockPos position, ServerWorld world) {
        double totalCost = 0.0;
        
        for (BiomeCostCalculator calculator : calculators) {
            try {
                double cost = calculator.calculateCost(biome, position, world);
                totalCost += cost;
            } catch (Exception e) {
                // Ignore individual calculator errors, continue with other calculators
            }
        }
        
        return totalCost;
    }
    
    /**
     * Get all calculators
     * @return Calculator list
     */
    public List<BiomeCostCalculator> getCalculators() {
        return new ArrayList<>(calculators);
    }
    
    /**
     * Clear all calculators
     */
    public void clear() {
        calculators.clear();
    }
}
