package net.countered.settlementroads.client;

import net.countered.settlementroads.client.gui.ClothConfigScreen;
import net.countered.settlementroads.client.gui.RoadDebugScreen;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.network.DebugDataPacket;
import net.countered.settlementroads.network.PacketHandler;
import net.countered.settlementroads.network.RoadWeaverNetworkManager;
import net.countered.settlementroads.persistence.forge.WorldDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "roadweaver", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SettlementRoadsClient {

    private static KeyMapping debugMapKey;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册按键绑定 (默认 H 键)
        debugMapKey = new KeyMapping(
                "key.roadweaver.debug_map",
                GLFW.GLFW_KEY_H,
                "category.roadweaver"
        );
        event.register(debugMapKey);
    }
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 配置屏幕已在主类中通过 ModLoadingContext.registerExtensionPoint 注册
        // 这里不需要额外操作
    }

    @Mod.EventBusSubscriber(modid = "roadweaver", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            
            Minecraft client = Minecraft.getInstance();
            
            while (debugMapKey.consumeClick()) {
                handleDebugMapKey(client);
            }
        }
    }

    private static void handleDebugMapKey(Minecraft client) {
        // 如果已经打开调试屏幕，关闭它
        if (client.screen instanceof RoadDebugScreen) {
            client.execute(() -> client.setScreen(null));
            PacketHandler.clearCachedDebugData(); // 清除缓存
            return;
        }

        // 权限检查：单人游戏或多人游戏管理员
        if (client.getSingleplayerServer() != null) {
            // 单人游戏 - 直接访问数据
            ServerLevel world = client.getSingleplayerServer().overworld();
            if (world == null) return;

            // 获取数据并在渲染线程打开界面
            try {
                Records.StructureLocationData structureData = WorldDataHelper.getStructureLocations(world);
                List<Records.StructureConnection> connections = WorldDataHelper.getConnectedStructures(world);
                List<Records.RoadData> roads = WorldDataHelper.getRoadDataList(world);

                List<Records.StructureInfo> structureInfos = structureData != null ?
                    new ArrayList<>(structureData.structureInfos()) : new ArrayList<>();

                client.execute(() -> client.setScreen(new RoadDebugScreen(structureInfos, connections, roads)));
            } catch (Exception e) {
                // 如果获取数据失败，打开空屏幕（仍然在渲染线程调度）
                client.execute(() -> client.setScreen(new RoadDebugScreen(new ArrayList<>(), new ArrayList<>(), new ArrayList<>())));
            }
        } else if (client.player != null && client.getConnection() != null) {
            // 多人游戏：检查是否有管理员权限
            if (!client.player.hasPermissions(2)) {
                // 没有权限，显示提示消息
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("roadweaver.debug_map.no_permission"),
                            true
                        );
                    }
                });
                return;
            }
            
            // 有管理员权限，通过网络包请求数据
            RoadWeaverNetworkManager.requestDebugData();
            
            // 显示加载提示
            client.execute(() -> {
                if (client.player != null) {
                    client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("roadweaver.debug_map.loading"),
                        true
                    );
                }
            });
            
            // 等待数据返回后打开界面
            new Thread(() -> {
                try {
                    // 最多等待5秒
                    for (int i = 0; i < 50; i++) {
                        Thread.sleep(100);
                        DebugDataPacket cachedData = PacketHandler.getCachedDebugData();
                        if (cachedData != null) {
                            // 数据到达，在主线程打开界面
                            client.execute(() -> {
                                client.setScreen(new RoadDebugScreen(
                                    cachedData.getStructureInfos(),
                                    cachedData.getConnections(),
                                    cachedData.getRoads()
                                ));
                            });
                            return;
                        }
                    }
                    // 超时，显示错误
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("roadweaver.debug_map.timeout"),
                                false
                            );
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
