package net.countered.settlementroads;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.config.RoadWeaverConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.RoadFeatureRegistry;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettlementRoads implements ModInitializer {

	public static final String MOD_ID = "settlement-roads";

	private static final Logger LOGGER = LoggerFactory.getLogger(SettlementRoads.MOD_ID);

	// -5233360391469774945

	// Fix:
	// Clean snow from roads

	// OPTIONAL
	// Location lag reducing (async locator?)/ structure essentials / place instant roads?
	// Bridges
	// Tunnels

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Settlement Roads...");
		
		ModConfig.init(MOD_ID, ModConfig.class);
		LOGGER.info("MidnightConfig initialized");
		
		moveMainConfigToSubfolder();
		LOGGER.info("Initializing RoadWeaver configuration system...");
		try {
			new RoadWeaverConfig();
			LOGGER.info("RoadWeaver configuration system initialized successfully");
		} catch (Exception e) {
			LOGGER.error("Failed to initialize RoadWeaver configuration system", e);
		}
		
		WorldDataAttachment.registerWorldDataAttachment();
		RoadFeatureRegistry.registerFeatures();
		ModEventHandler.register();
		
		LOGGER.info("Settlement Roads initialization completed");
	}
	
	/**
	 * Move main config file to settlement-roads subfolder
	 * to maintain configuration structure consistency
	 */
	private void moveMainConfigToSubfolder() {
		try {
			Path sourcePath = Paths.get("config/settlement-roads.json");
			Path targetPath = Paths.get("config/settlement-roads/settlement-roads.json");
			
			if (Files.exists(sourcePath)) {
				Files.createDirectories(targetPath.getParent());
				if (!Files.exists(targetPath)) {
					Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
					LOGGER.info("Moved main config file to settlement-roads subfolder");
				} else {
					Files.deleteIfExists(sourcePath);
					LOGGER.info("Main config file already exists in settlement-roads subfolder");
				}
			} else {
				LOGGER.info("Main config file not found, will be created in settlement-roads subfolder");
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to move main config file to subfolder: {}", e.getMessage());
		}
	}
}