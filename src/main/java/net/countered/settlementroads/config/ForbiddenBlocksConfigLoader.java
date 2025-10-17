package net.countered.settlementroads.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Replaceable blocks configuration loader
 * Responsible for loading and managing blocks that can be replaced by roads
 */
public class ForbiddenBlocksConfigLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ForbiddenBlocksConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Set<Block> replaceableBlocks = new HashSet<>();
    private static Set<String> replaceableBlockTags = new HashSet<>();
    
    /**
     * Load replaceable blocks configuration
     * @param configFile Configuration file path
     * @return Whether loading was successful
     */
    public static boolean loadConfig(Path configFile) {
        try {
            if (!Files.exists(configFile)) {
                LOGGER.info("Replaceable blocks config not found, creating default: {}", configFile);
                return createDefaultConfig(configFile);
            }
            
            String content = Files.readString(configFile);
            JsonObject config = GSON.fromJson(content, JsonObject.class);
            
            if (!config.has("replaceableBlocks")) {
                LOGGER.error("Invalid replaceable blocks config: missing 'replaceableBlocks' section");
                return false;
            }
            
            JsonObject replaceableBlocksSection = config.getAsJsonObject("replaceableBlocks");
            
            // Load replaceable blocks
            replaceableBlocks.clear();
            if (replaceableBlocksSection.has("blocks")) {
                var blocksArray = replaceableBlocksSection.getAsJsonArray("blocks");
                for (var element : blocksArray) {
                    String blockId = element.getAsString();
                    Block block = Registries.BLOCK.get(Identifier.tryParse(blockId));
                    if (block != null) {
                        replaceableBlocks.add(block);
                        LOGGER.debug("Added replaceable block: {}", blockId);
                    } else {
                        LOGGER.warn("Unknown block ID: {}", blockId);
                    }
                }
            }
            
            // Load replaceable block tags
            replaceableBlockTags.clear();
            if (replaceableBlocksSection.has("blockTags")) {
                var tagsArray = replaceableBlocksSection.getAsJsonArray("blockTags");
                for (var element : tagsArray) {
                    String tagId = element.getAsString();
                    replaceableBlockTags.add(tagId);
                    LOGGER.debug("Added replaceable block tag: {}", tagId);
                }
            }
            
            LOGGER.info("Loaded replaceable blocks config: {} blocks, {} tags", 
                       replaceableBlocks.size(), replaceableBlockTags.size());
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to load replaceable blocks config: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create default configuration file
     * @param configFile Configuration file path
     * @return Whether creation was successful
     */
    public static boolean createDefaultConfig(Path configFile) {
        try {
            JsonObject root = new JsonObject();
            JsonObject replaceableBlocks = new JsonObject();
            
            // Replaceable blocks list (blocks allowed to be replaced by default)
            var blocksArray = new com.google.gson.JsonArray();
            blocksArray.add("minecraft:grass_block");
            blocksArray.add("minecraft:dirt");
            blocksArray.add("minecraft:stone");
            blocksArray.add("minecraft:sand");
            blocksArray.add("minecraft:gravel");
            replaceableBlocks.add("blocks", blocksArray);
            
            // Replaceable block tags list (tags allowed to be replaced by default)
            var tagsArray = new com.google.gson.JsonArray();
            tagsArray.add("minecraft:leaves");
            tagsArray.add("minecraft:logs");
            tagsArray.add("minecraft:wooden_fences");
            tagsArray.add("minecraft:planks");
            tagsArray.add("minecraft:flowers");
            tagsArray.add("minecraft:small_flowers");
            replaceableBlocks.add("blockTags", tagsArray);
            
            root.add("replaceableBlocks", replaceableBlocks);
            
            // Ensure directory exists
            Files.createDirectories(configFile.getParent());
            
            // Write to file
            Files.writeString(configFile, GSON.toJson(root));
            
            LOGGER.info("Created default replaceable blocks config: {}", configFile);
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to create default replaceable blocks config: {}", configFile, e);
            return false;
        }
    }
    
    /**
     * Check if block can be replaced
     * @param block Block to check
     * @return True if replaceable, false otherwise
     */
    public static boolean isBlockReplaceable(Block block) {
        return replaceableBlocks.contains(block);
    }
    
    /**
     * Check if block tag can be replaced
     * @param tagId Block tag ID
     * @return True if replaceable, false otherwise
     */
    public static boolean isBlockTagReplaceable(String tagId) {
        return replaceableBlockTags.contains(tagId);
    }
    
    /**
     * Get set of replaceable blocks
     * @return Set of replaceable blocks
     */
    public static Set<Block> getReplaceableBlocks() {
        return new HashSet<>(replaceableBlocks);
    }
    
    /**
     * Get set of replaceable block tags
     * @return Set of replaceable block tags
     */
    public static Set<String> getReplaceableBlockTags() {
        return new HashSet<>(replaceableBlockTags);
    }
}
