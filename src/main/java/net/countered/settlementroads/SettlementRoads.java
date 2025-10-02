package net.countered.settlementroads;

import net.countered.settlementroads.config.ModConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.RoadFeatureRegistry;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
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
		WorldDataAttachment.registerWorldDataAttachment();
		ModConfig.init(MOD_ID, ModConfig.class);
		RoadFeatureRegistry.registerFeatures();
		ModEventHandler.register();
	}
}