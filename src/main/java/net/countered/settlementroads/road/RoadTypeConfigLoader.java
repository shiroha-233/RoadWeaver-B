package net.countered.settlementroads.road;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Road type configuration loader
 * Responsible for loading road type configurations from JSON config files
 */
public class RoadTypeConfigLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadWeaver-RoadTypeConfigLoader");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Load road types from config file
     * @param configFile Config file path
     * @param registry Road type registry
     * @return Number of road types loaded
     */
    public static int loadRoadTypes(Path configFile, RoadTypeRegistry registry) {
        if (!Files.exists(configFile)) {
            LOGGER.warn("Road types config file not found: {}", configFile);
            return 0;
        }
        
        try {
            String jsonContent = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
            
            if (!root.has("roadTypes")) {
                LOGGER.warn("No 'roadTypes' section found in config file: {}", configFile);
                return 0;
            }
            
            JsonObject roadTypesSection = root.getAsJsonObject("roadTypes");
            int loadedCount = 0;
            
            for (String roadTypeId : roadTypesSection.keySet()) {
                try {
                    JsonObject roadTypeConfig = roadTypesSection.getAsJsonObject(roadTypeId);
                    RoadTypeConfig config = parseRoadTypeConfig(roadTypeId, roadTypeConfig);
                    
                    if (config != null && registry.registerRoadType(config)) {
                        loadedCount++;
                        LOGGER.info("Loaded road type: {}", roadTypeId);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load road type {}: {}", roadTypeId, e.getMessage(), e);
                }
            }
            
            LOGGER.info("Loaded {} road types from {}", loadedCount, configFile);
            return loadedCount;
            
        } catch (IOException e) {
            LOGGER.error("Failed to read road types config file: {}", configFile, e);
            return 0;
        }
    }
    
    /**
     * Parse single road type configuration
     * @param id Road type ID
     * @param configJson Configuration JSON object
     * @return Road type configuration
     */
    private static RoadTypeConfig parseRoadTypeConfig(String id, JsonObject configJson) {
        try {
            double generationWeight = configJson.has("generationWeight") ? 
                configJson.get("generationWeight").getAsDouble() : 1.0;
            boolean enabled = !configJson.has("enabled") || 
                configJson.get("enabled").getAsBoolean();
            
            List<List<BlockState>> materials = parseMaterials(configJson);
            if (materials.isEmpty()) {
                LOGGER.warn("No materials found for road type: {}", id);
                return null;
            }
            Map<String, Object> properties = parseProperties(configJson);
            
            return new DefaultRoadTypeConfig(id, materials, 
                                          generationWeight, enabled, properties);
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse road type config for {}: {}", id, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析材料配置
     * @param configJson 配置JSON对象
     * @return 材料列表
     */
    private static List<List<BlockState>> parseMaterials(JsonObject configJson) {
        List<List<BlockState>> materials = new ArrayList<>();
        
        if (!configJson.has("materials")) {
            LOGGER.warn("No materials section found in road type config");
            return materials;
        }
        
        try {
            var materialsArray = configJson.getAsJsonArray("materials");
            for (var materialGroup : materialsArray) {
                List<BlockState> materialList = new ArrayList<>();
                var materialGroupArray = materialGroup.getAsJsonArray();
                
                for (var material : materialGroupArray) {
                    String blockId = material.getAsString();
                    BlockState blockState = parseBlockState(blockId);
                    if (blockState != null) {
                        materialList.add(blockState);
                    }
                }
                
                if (!materialList.isEmpty()) {
                    materials.add(materialList);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse materials: {}", e.getMessage(), e);
        }
        
        return materials;
    }
    
    /**
     * 解析方块状态
     * @param blockId 方块ID字符串
     * @return 方块状态
     */
    private static BlockState parseBlockState(String blockId) {
        try {
            Identifier identifier = Identifier.tryParse(blockId);
            if (identifier == null) {
                LOGGER.warn("Invalid block identifier: {}", blockId);
                return null;
            }
            
            Block block = Registries.BLOCK.get(identifier);
            if (block == null) {
                LOGGER.warn("Block not found: {}", blockId);
                return null;
            }
            
            return block.getDefaultState();
        } catch (Exception e) {
            LOGGER.error("Failed to parse block state for {}: {}", blockId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析扩展属性
     * @param configJson 配置JSON对象
     * @return 属性映射
     */
    private static Map<String, Object> parseProperties(JsonObject configJson) {
        Map<String, Object> properties = new HashMap<>();
        
        if (configJson.has("properties")) {
            try {
                JsonObject propertiesJson = configJson.getAsJsonObject("properties");
                for (String key : propertiesJson.keySet()) {
                    var value = propertiesJson.get(key);
                    if (value.isJsonPrimitive()) {
                        var primitive = value.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            properties.put(key, primitive.getAsString());
                        } else if (primitive.isNumber()) {
                            properties.put(key, primitive.getAsNumber());
                        } else if (primitive.isBoolean()) {
                            properties.put(key, primitive.getAsBoolean());
                        }
                    } else if (value.isJsonArray()) {
                        List<Object> list = new ArrayList<>();
                        var array = value.getAsJsonArray();
                        for (var element : array) {
                            if (element.isJsonPrimitive()) {
                                var primitive = element.getAsJsonPrimitive();
                                if (primitive.isString()) {
                                    list.add(primitive.getAsString());
                                } else if (primitive.isNumber()) {
                                    list.add(primitive.getAsNumber());
                                } else if (primitive.isBoolean()) {
                                    list.add(primitive.getAsBoolean());
                                }
                            }
                        }
                        properties.put(key, list);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse properties: {}", e.getMessage(), e);
            }
        }
        
        return properties;
    }
    
    /**
     * 从JSON对象加载道路类型
     * @param roadTypesJson 道路类型JSON对象
     * @param registry 道路类型注册表
     * @return 加载的道路类型数量
     */
    public static int loadRoadTypesFromJson(com.google.gson.JsonObject roadTypesJson, RoadTypeRegistry registry) {
        int loadedCount = 0;
        
        for (String roadTypeId : roadTypesJson.keySet()) {
            try {
                com.google.gson.JsonObject roadTypeConfig = roadTypesJson.getAsJsonObject(roadTypeId);
                RoadTypeConfig config = parseRoadTypeConfig(roadTypeId, roadTypeConfig);
                
                if (config != null && registry.registerRoadType(config)) {
                    loadedCount++;
                    LOGGER.info("Loaded road type: {}", roadTypeId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load road type {}: {}", roadTypeId, e.getMessage(), e);
            }
        }
        
        LOGGER.info("Loaded {} road types from JSON", loadedCount);
        return loadedCount;
    }
    
    /**
     * 创建默认配置文件
     * @param configFile 配置文件路径
     * @return 是否创建成功
     */
    public static boolean createDefaultConfig(Path configFile) {
        try {
            JsonObject root = new JsonObject();
            JsonObject roadTypes = new JsonObject();
            
            // 默认道路配置 - 使用当前配置文件的值
            JsonObject defaultRoad = new JsonObject();
            defaultRoad.addProperty("generationWeight", 1.0);
            defaultRoad.addProperty("enabled", true);
            
            var defaultMaterials = new com.google.gson.JsonArray();
            var materialGroup = new com.google.gson.JsonArray();
            materialGroup.add("minecraft:iron_block");
            materialGroup.add("minecraft:gold_block");
            materialGroup.add("minecraft:diamond_block");
            materialGroup.add("minecraft:emerald_block");
            materialGroup.add("minecraft:lapis_block");
            materialGroup.add("minecraft:redstone_block");
            materialGroup.add("minecraft:netherite_block");
            defaultMaterials.add(materialGroup);
            
            defaultRoad.add("materials", defaultMaterials);
            
            JsonObject defaultProps = new JsonObject();
            defaultProps.addProperty("averagingRadius", 1);
            var widths = new com.google.gson.JsonArray();
            widths.add(3);
            defaultProps.add("widths", widths);
            var preferredBiomes = new com.google.gson.JsonArray();
            defaultProps.add("preferredBiomes", preferredBiomes);
            // 装饰相关属性
            defaultProps.addProperty("allowsLampposts", true);
            defaultProps.addProperty("allowsFenceWaypoints", false);
            defaultRoad.add("properties", defaultProps);
            
            roadTypes.add("default", defaultRoad);
            
            // 沙漠道路配置
            JsonObject sandRoad = new JsonObject();
            sandRoad.addProperty("generationWeight", 1.0);
            sandRoad.addProperty("enabled", true);
            
            var sandMaterials = new com.google.gson.JsonArray();
            var sandGroup = new com.google.gson.JsonArray();
            sandGroup.add("minecraft:chiseled_sandstone");
            sandGroup.add("minecraft:cut_sandstone");
            sandGroup.add("minecraft:sandstone");
            sandGroup.add("minecraft:orange_terracotta");
            sandMaterials.add(sandGroup);
            
            sandRoad.add("materials", sandMaterials);
            
            JsonObject sandProps = new JsonObject();
            sandProps.addProperty("averagingRadius", 1);
            var sandWidths = new com.google.gson.JsonArray();
            sandWidths.add(3);
            sandWidths.add(5);
            sandWidths.add(7);
            sandProps.add("widths", sandWidths);
            var sandBiomes = new com.google.gson.JsonArray();
            sandBiomes.add("minecraft:desert");
            sandBiomes.add("minecraft:desert_hills");
            sandBiomes.add("minecraft:badlands");
            sandBiomes.add("minecraft:eroded_badlands");
            sandBiomes.add("minecraft:wooded_badlands");
            sandProps.add("preferredBiomes", sandBiomes);
            // 装饰相关属性
            sandProps.addProperty("allowsLampposts", false);
            sandProps.addProperty("allowsFenceWaypoints", true);
            sandRoad.add("properties", sandProps);
            
            roadTypes.add("sand", sandRoad);
            
            root.add("roadTypes", roadTypes);
            
            // 确保目录存在
            Files.createDirectories(configFile.getParent());
            
            // 写入文件
            Files.writeString(configFile, GSON.toJson(root));
            
            LOGGER.info("Created default road types config: {}", configFile);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create default config: {}", configFile, e);
            return false;
        }
    }
}
