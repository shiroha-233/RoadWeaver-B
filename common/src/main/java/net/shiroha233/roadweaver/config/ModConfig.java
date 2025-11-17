package net.shiroha233.roadweaver.config;

import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
    public enum PlanningAlgorithm {
        KNN,
        DELAUNAY,
        RNG
    }
    private boolean villagePredictionEnabled;
    private int predictRadiusChunks;
    private boolean biomePrefilter;
    private List<String> structureWhitelist;
    private List<String> structureBlacklist;

    // 路网规划配置
    private int initialPlanRadiusChunks;      // 新建世界后以出生点为中心的初始规划半径（区块）
    private boolean dynamicPlanEnabled;       // 是否启用基于玩家的动态增量规划
    private int dynamicPlanRadiusChunks;      // 玩家为中心的动态规划半径（区块）
    private int dynamicPlanStrideChunks;      // 动态规划触发步进（区块），用于判定玩家移动到新网格时触发
    private PlanningAlgorithm planningAlgorithm; // 路网连边算法

    // 道路生成配置
    private boolean allowArtificial;
    private boolean allowNatural;
    private boolean placeWaypoints;
    private int averagingRadius;
    private int generationThreads;
    private int maxConcurrentGenerations;
    private int aStarStep;                 // A* 采样步长（方块）
    private int aStarMaxSteps;             // A* 寻路最大步数上限
    private int causewayMaxDepth;
    private int maxSlopeStepPerTwoSegments;
    private boolean slopeLimitEnabled = true;     // 是否启用基于 maxSlopeStepPerTwoSegments 的限坡平滑
    private boolean useBidirectionalAStar; // 是否启用双向 A* 寻路

    private int roadWidth;         
    private int lampInterval;      
    private int roadClearHeight;
    private boolean tunnelEnabled;
    private int tunnelClearHeight;
    private boolean removeWholeTreeOnPath;
    private int treeRemovalMaxRadius;
    private int treeRemovalMaxHeight;
    private int treeRemovalMaxBlocks;
    private int treeLeavesConfirm;

    // 桥梁配置
    private boolean bridgeEnabled;
    private int bridgeDeckClearance;
    private boolean bridgeRailingEnabled;
    private int bridgePierInterval;
    private int bridgePierWidth;
    private int bridgePierMaxHeight;
    private boolean bridgeKeepLamps;
    private int bridgeRampSegments;

    public ModConfig() {
        this.villagePredictionEnabled = true;
        this.predictRadiusChunks = 1024;
        this.biomePrefilter = true;
        this.structureWhitelist = new ArrayList<>();
        this.structureBlacklist = new ArrayList<>();
        this.structureWhitelist.add("#minecraft:village");

        // 默认规划参数：初始64区块；动态规划开启，半径256区块
        this.initialPlanRadiusChunks = 64;
        this.dynamicPlanEnabled = true;
        this.dynamicPlanRadiusChunks = 256;
        this.dynamicPlanStrideChunks = Math.max(8, Math.min(64, this.dynamicPlanRadiusChunks / 2));
        this.planningAlgorithm = PlanningAlgorithm.RNG;

        // 道路生成默认参数
        this.allowArtificial = true;
        this.allowNatural = true;
        this.placeWaypoints = false;
        this.averagingRadius = 8;
        
        this.generationThreads = Math.max(2, Math.min(3, Runtime.getRuntime().availableProcessors()));
        this.maxConcurrentGenerations = Math.max(1, Math.min(3, this.generationThreads));
        this.aStarStep = 16;
        this.aStarMaxSteps = 10000;
        this.causewayMaxDepth = 1;
        this.maxSlopeStepPerTwoSegments = 1;
        this.slopeLimitEnabled = true;
        this.useBidirectionalAStar = true;

        // 新增默认值
        this.roadWidth = 3;    
        this.lampInterval = 32; 
        this.roadClearHeight = 4;
        this.tunnelEnabled = false;
        this.tunnelClearHeight = 5;
        this.removeWholeTreeOnPath = true;
        this.treeRemovalMaxRadius = 6;
        this.treeRemovalMaxHeight = 24;
        this.treeRemovalMaxBlocks = 2048;
        this.treeLeavesConfirm = 6;

        // 桥梁默认值
        this.bridgeEnabled = true;
        this.bridgeDeckClearance = 2;
        this.bridgeRailingEnabled = true;
        this.bridgePierInterval = 6;
        this.bridgePierWidth = 1;
        this.bridgePierMaxHeight = 20;
        this.bridgeKeepLamps = true;
        this.bridgeRampSegments = 4;
    }

    public boolean villagePredictionEnabled() {
        return villagePredictionEnabled;
    }

    public void setVillagePredictionEnabled(boolean villagePredictionEnabled) {
        this.villagePredictionEnabled = villagePredictionEnabled;
    }

    public int predictRadiusChunks() {
        return predictRadiusChunks;
    }

    public void setPredictRadiusChunks(int predictRadiusChunks) {
        this.predictRadiusChunks = predictRadiusChunks;
    }

    public boolean biomePrefilter() {
        return biomePrefilter;
    }

    public void setBiomePrefilter(boolean biomePrefilter) {
        this.biomePrefilter = biomePrefilter;
    }

    

    public List<String> structureWhitelist() {
        return structureWhitelist;
    }

    public void setStructureWhitelist(List<String> structureWhitelist) {
        this.structureWhitelist = structureWhitelist == null ? new ArrayList<>() : new ArrayList<>(structureWhitelist);
    }

    public List<String> structureBlacklist() {
        return structureBlacklist;
    }

    public void setStructureBlacklist(List<String> structureBlacklist) {
        this.structureBlacklist = structureBlacklist == null ? new ArrayList<>() : new ArrayList<>(structureBlacklist);
    }

    

    // 载入后修复缺省值与兼容项
    public void sanitize() {
        if (structureWhitelist == null) structureWhitelist = new ArrayList<>();
        if (structureBlacklist == null) structureBlacklist = new ArrayList<>();

        

        if (predictRadiusChunks <= 0) predictRadiusChunks = 1024;
        if (initialPlanRadiusChunks <= 0) initialPlanRadiusChunks = 64;
        if (dynamicPlanRadiusChunks <= 0) dynamicPlanRadiusChunks = 256;
        if (dynamicPlanStrideChunks <= 0) dynamicPlanStrideChunks = Math.max(8, Math.min(64, Math.max(1, dynamicPlanRadiusChunks) / 2));
        if (dynamicPlanStrideChunks > dynamicPlanRadiusChunks) dynamicPlanStrideChunks = dynamicPlanRadiusChunks;
        if (dynamicPlanStrideChunks > 256) dynamicPlanStrideChunks = 256;
        if (planningAlgorithm == null) planningAlgorithm = PlanningAlgorithm.RNG;

        // 道路生成安全边界
        if (averagingRadius < 0) averagingRadius = 0;
        if (generationThreads < 1) generationThreads = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        if (generationThreads > 64) generationThreads = 64;
        if (maxConcurrentGenerations < 1) maxConcurrentGenerations = generationThreads;
        int maxCap = Math.max(1, generationThreads * 2);
        if (maxConcurrentGenerations > maxCap) maxConcurrentGenerations = maxCap;
        if (aStarStep < 4) aStarStep = 16;            // 步数下限
        if (aStarStep > 128) aStarStep = 128;         // 步数上限
        if (aStarMaxSteps < 3000) aStarMaxSteps = 3000;  // 最小步数下限
        if (aStarMaxSteps > 100000) aStarMaxSteps = 100000; // 最大步数上限
        if (causewayMaxDepth < 0) causewayMaxDepth = 0;//最小填充深度
        if (causewayMaxDepth > 12) causewayMaxDepth = 12;//最大填充深度
        if (maxSlopeStepPerTwoSegments < 0) maxSlopeStepPerTwoSegments = 0;//最小斜坡步数
        if (maxSlopeStepPerTwoSegments > 8) maxSlopeStepPerTwoSegments = 8;//最大斜坡步数

        // 新增字段校验
        if (roadWidth < 0) roadWidth = 0;            // 0=自动
        if (roadWidth > 15) roadWidth = 15;          // 宽度上限合理限制
        if (lampInterval < 1) lampInterval = 59;     // 保底
        if (lampInterval > 2048) lampInterval = 2048;
        if (roadClearHeight < 1) roadClearHeight = 4;
        if (roadClearHeight > 16) roadClearHeight = 16;
        if (tunnelClearHeight < 2) tunnelClearHeight = 2;
        if (tunnelClearHeight > 16) tunnelClearHeight = 16;
        if (treeRemovalMaxRadius < 2) treeRemovalMaxRadius = 2;
        if (treeRemovalMaxRadius > 12) treeRemovalMaxRadius = 12;
        if (treeRemovalMaxHeight < 8) treeRemovalMaxHeight = 8;
        if (treeRemovalMaxHeight > 64) treeRemovalMaxHeight = 64;
        if (treeRemovalMaxBlocks < 64) treeRemovalMaxBlocks = 64;
        if (treeRemovalMaxBlocks > 8192) treeRemovalMaxBlocks = 8192;
        if (treeLeavesConfirm < 0) treeLeavesConfirm = 0;
        if (treeLeavesConfirm > 128) treeLeavesConfirm = 128;

        // 桥梁字段校验
        if (bridgeDeckClearance < 1) bridgeDeckClearance = 1;
        if (bridgeDeckClearance > 8) bridgeDeckClearance = 8;
        if (bridgePierInterval < 3) bridgePierInterval = 3;
        if (bridgePierInterval > 32) bridgePierInterval = 32;
        if (bridgePierWidth < 1) bridgePierWidth = 1;
        if (bridgePierWidth > 3) bridgePierWidth = 3;
        if (bridgePierMaxHeight < 6) bridgePierMaxHeight = 6;
        if (bridgePierMaxHeight > 64) bridgePierMaxHeight = 64;
        if (bridgeRampSegments < 0) bridgeRampSegments = 0;
        if (bridgeRampSegments > 12) bridgeRampSegments = 12;
    }

    // 初始规划半径
    public int initialPlanRadiusChunks() { return initialPlanRadiusChunks; }
    public void setInitialPlanRadiusChunks(int v) { this.initialPlanRadiusChunks = v; }

    // 动态规划开关
    public boolean dynamicPlanEnabled() { return dynamicPlanEnabled; }
    public void setDynamicPlanEnabled(boolean v) { this.dynamicPlanEnabled = v; }

    // 动态规划半径
    public int dynamicPlanRadiusChunks() { return dynamicPlanRadiusChunks; }
    public void setDynamicPlanRadiusChunks(int v) { this.dynamicPlanRadiusChunks = v; }

    // 动态规划触发步进
    public int dynamicPlanStrideChunks() { return dynamicPlanStrideChunks; }
    public void setDynamicPlanStrideChunks(int v) { this.dynamicPlanStrideChunks = v; }

    public boolean allowArtificial() { return allowArtificial; }
    public void setAllowArtificial(boolean v) { this.allowArtificial = v; }

    public boolean allowNatural() { return allowNatural; }
    public void setAllowNatural(boolean v) { this.allowNatural = v; }


    public boolean placeWaypoints() { return placeWaypoints; }
    public void setPlaceWaypoints(boolean v) { this.placeWaypoints = v; }

    public int averagingRadius() { return averagingRadius; }
    public void setAveragingRadius(int v) { this.averagingRadius = v; }

    

    public int generationThreads() { return generationThreads; }
    public void setGenerationThreads(int v) { this.generationThreads = v; }

    public int maxConcurrentGenerations() { return maxConcurrentGenerations; }
    public void setMaxConcurrentGenerations(int v) { this.maxConcurrentGenerations = v; }

    // A* 采样步长
    public int aStarStep() { return aStarStep; }
    public void setAStarStep(int v) { this.aStarStep = v; }

    // A* 最大步数
    public int aStarMaxSteps() { return aStarMaxSteps; }
    public void setAStarMaxSteps(int v) { this.aStarMaxSteps = v; }

    public int causewayMaxDepth() { return causewayMaxDepth; }
    public void setCausewayMaxDepth(int v) { this.causewayMaxDepth = v; }

    public int maxSlopeStepPerTwoSegments() { return maxSlopeStepPerTwoSegments; }
    public void setMaxSlopeStepPerTwoSegments(int v) { this.maxSlopeStepPerTwoSegments = v; }

    public boolean slopeLimitEnabled() { return slopeLimitEnabled; }
    public void setSlopeLimitEnabled(boolean v) { this.slopeLimitEnabled = v; }

    public boolean useBidirectionalAStar() { return useBidirectionalAStar; }
    public void setUseBidirectionalAStar(boolean v) { this.useBidirectionalAStar = v; }

    // 新增：道路宽度（0=自动）
    public int roadWidth() { return roadWidth; }
    public void setRoadWidth(int v) { this.roadWidth = v; }

    // 新增：路灯间隔（段）
    public int lampInterval() { return lampInterval; }
    public void setLampInterval(int v) { this.lampInterval = v; }

    public int roadClearHeight() { return roadClearHeight; }
    public void setRoadClearHeight(int v) { this.roadClearHeight = v; }

    

    // 路网连边算法
    public PlanningAlgorithm planningAlgorithm() { return planningAlgorithm; }
    public void setPlanningAlgorithm(PlanningAlgorithm v) { this.planningAlgorithm = v; }

    public boolean tunnelEnabled() { return tunnelEnabled; }
    public void setTunnelEnabled(boolean v) { this.tunnelEnabled = v; }

    public int tunnelClearHeight() { return tunnelClearHeight; }
    public void setTunnelClearHeight(int v) { this.tunnelClearHeight = v; }

    public boolean removeWholeTreeOnPath() { return removeWholeTreeOnPath; }
    public void setRemoveWholeTreeOnPath(boolean v) { this.removeWholeTreeOnPath = v; }
    public int treeRemovalMaxRadius() { return treeRemovalMaxRadius; }
    public void setTreeRemovalMaxRadius(int v) { this.treeRemovalMaxRadius = v; }
    public int treeRemovalMaxHeight() { return treeRemovalMaxHeight; }
    public void setTreeRemovalMaxHeight(int v) { this.treeRemovalMaxHeight = v; }
    public int treeRemovalMaxBlocks() { return treeRemovalMaxBlocks; }
    public void setTreeRemovalMaxBlocks(int v) { this.treeRemovalMaxBlocks = v; }
    public int treeLeavesConfirm() { return treeLeavesConfirm; }
    public void setTreeLeavesConfirm(int v) { this.treeLeavesConfirm = v; }

    // 桥梁配置存取
    public boolean bridgeEnabled() { return bridgeEnabled; }
    public void setBridgeEnabled(boolean v) { this.bridgeEnabled = v; }

    public int bridgeDeckClearance() { return bridgeDeckClearance; }
    public void setBridgeDeckClearance(int v) { this.bridgeDeckClearance = v; }

    public boolean bridgeRailingEnabled() { return bridgeRailingEnabled; }
    public void setBridgeRailingEnabled(boolean v) { this.bridgeRailingEnabled = v; }

    public int bridgePierInterval() { return bridgePierInterval; }
    public void setBridgePierInterval(int v) { this.bridgePierInterval = v; }

    public int bridgePierWidth() { return bridgePierWidth; }
    public void setBridgePierWidth(int v) { this.bridgePierWidth = v; }

    public int bridgePierMaxHeight() { return bridgePierMaxHeight; }
    public void setBridgePierMaxHeight(int v) { this.bridgePierMaxHeight = v; }

    public boolean bridgeKeepLamps() { return bridgeKeepLamps; }
    public void setBridgeKeepLamps(boolean v) { this.bridgeKeepLamps = v; }

    public int bridgeRampSegments() { return bridgeRampSegments; }
    public void setBridgeRampSegments(int v) { this.bridgeRampSegments = v; }
}
