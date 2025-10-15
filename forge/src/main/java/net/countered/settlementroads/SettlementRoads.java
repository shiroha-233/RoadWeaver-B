package net.countered.settlementroads;

import net.countered.settlementroads.client.gui.ClothConfigScreen;
import net.countered.settlementroads.config.forge.ForgeJsonConfig;
import net.countered.settlementroads.events.ModEventHandler;
import net.countered.settlementroads.features.config.forge.ForgeRoadFeatureRegistry;
import net.countered.settlementroads.datagen.SettlementRoadsDataGenerator;
import net.countered.settlementroads.network.RoadWeaverNetworkManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SettlementRoads.MOD_ID)
public class SettlementRoads {

	public static final String MOD_ID = "roadweaver";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public SettlementRoads() {
		LOGGER.info("Initializing RoadWeaver (Forge)...");
		
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		
		// 加载 JSON 配置（与 Fabric 一致，写入 config/roadweaver.json）
		ForgeJsonConfig.load();
		
		// 注册配置屏幕（Forge 模组菜单集成）
		ModLoadingContext.get().registerExtensionPoint(
			net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
				(client, parent) -> ClothConfigScreen.createConfigScreen(parent)
			)
		);
		
		// 使用 Forge 原生的 DeferredRegister 注册特性
		// 避免 Architectury DeferredRegister 的事件总线注册时序问题
		ForgeRoadFeatureRegistry.register(modEventBus);
		
		// 注册通用设置事件
		modEventBus.addListener(this::commonSetup);
		// 注册数据生成事件（确保 runData 时 provider 被加入）
		modEventBus.addListener(SettlementRoadsDataGenerator::gatherData);
		
		// 注册事件处理器（使用 Architectury 事件的 common 实现）
		ModEventHandler.register();
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		// 注册网络包
		event.enqueueWork(() -> {
			RoadWeaverNetworkManager.registerPackets();
			LOGGER.info("Network packets registered");
		});
		LOGGER.info("RoadWeaver common setup completed");
	}
	
	public static Logger getLogger() {
		return LOGGER;
	}
}
