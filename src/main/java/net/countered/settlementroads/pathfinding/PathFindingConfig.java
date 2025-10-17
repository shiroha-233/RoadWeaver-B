package net.countered.settlementroads.pathfinding;

import java.util.Map;
import java.util.HashMap;

/**
 * Pathfinding configuration
 * Contains all configuration parameters for pathfinding algorithms
 */
public class PathFindingConfig {
    
    private int neighborDistance;
    private double diagonalStepCost;
    private double straightStepCost;
    private final Map<String, Double> costMultipliers;
    private int maxSteps;
    private double heuristicWeight;
    
    public PathFindingConfig() {
        this.neighborDistance = 4;
        this.diagonalStepCost = 1.5;
        this.straightStepCost = 1.0;
        this.costMultipliers = new HashMap<>();
        this.maxSteps = 10000;
        this.heuristicWeight = 1.0;
        
        costMultipliers.put("elevation", 40.0);
        costMultipliers.put("biome", 8.0);
        costMultipliers.put("seaLevel", 8.0);
        costMultipliers.put("terrainStability", 16.0);
    }
    
    public PathFindingConfig(int neighborDistance, double diagonalStepCost, 
                           double straightStepCost, Map<String, Double> costMultipliers, 
                           int maxSteps, double heuristicWeight) {
        this.neighborDistance = neighborDistance;
        this.diagonalStepCost = diagonalStepCost;
        this.straightStepCost = straightStepCost;
        this.costMultipliers = new HashMap<>(costMultipliers);
        this.maxSteps = maxSteps;
        this.heuristicWeight = heuristicWeight;
    }
    
    /**
     * 获取邻居距离
     */
    public int getNeighborDistance() {
        return neighborDistance;
    }
    
    /**
     * 获取对角线步长成本
     */
    public double getDiagonalStepCost() {
        return diagonalStepCost;
    }
    
    /**
     * 获取直线步长成本
     */
    public double getStraightStepCost() {
        return straightStepCost;
    }
    
    /**
     * 获取成本乘数映射
     */
    public Map<String, Double> getCostMultipliers() {
        return costMultipliers;
    }
    
    /**
     * 获取最大步数
     */
    public int getMaxSteps() {
        return maxSteps;
    }
    
    /**
     * 获取启发式权重
     */
    public double getHeuristicWeight() {
        return heuristicWeight;
    }
    
    /**
     * 获取特定成本乘数
     * @param key 成本类型
     * @return 成本乘数
     */
    public double getCostMultiplier(String key) {
        return costMultipliers.getOrDefault(key, 1.0);
    }
    
    /**
     * 设置成本乘数
     * @param key 成本类型
     * @param value 成本乘数
     */
    public void setCostMultiplier(String key, double value) {
        costMultipliers.put(key, value);
    }
    
    /**
     * 设置邻居距离
     */
    public void setNeighborDistance(int neighborDistance) {
        this.neighborDistance = neighborDistance;
    }
    
    /**
     * 设置对角线步长成本
     */
    public void setDiagonalStepCost(double diagonalStepCost) {
        this.diagonalStepCost = diagonalStepCost;
    }
    
    /**
     * 设置直线步长成本
     */
    public void setStraightStepCost(double straightStepCost) {
        this.straightStepCost = straightStepCost;
    }
    
    /**
     * 设置最大步数
     */
    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }
    
    /**
     * 设置启发式权重
     */
    public void setHeuristicWeight(double heuristicWeight) {
        this.heuristicWeight = heuristicWeight;
    }
}
