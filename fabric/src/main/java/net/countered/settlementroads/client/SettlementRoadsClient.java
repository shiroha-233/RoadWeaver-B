package net.countered.settlementroads.client;

import net.countered.settlementroads.client.gui.RoadDebugScreen;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.network.DebugDataPacket;
import net.countered.settlementroads.network.PacketHandler;
import net.countered.settlementroads.network.RoadWeaverNetworkManager;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SettlementRoadsClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static KeyMapping debugMapKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing RoadWeaver Client (Fabric)...");

        // 注册按键：默认 H 打开调试地图
        debugMapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.roadweaver.debug_map",
                GLFW.GLFW_KEY_H,
                "category.roadweaver"
        ));

        // 客户端每 tick 轮询按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debugMapKey.consumeClick()) {
                handleDebugMapKey(client);
            }
        });
    }

    private static void handleDebugMapKey(Minecraft client) {
        if (client == null) return;

        // 如果已打开则关闭
        if (client.screen instanceof RoadDebugScreen) {
            client.setScreen(null);
            PacketHandler.clearCachedDebugData(); // 清除缓存
            return;
        }

        // 权限检查：单人游戏或多人游戏管理员
        if (client.getSingleplayerServer() != null) {
            // 单人游戏 - 直接访问数据
            ServerLevel world = client.getSingleplayerServer().overworld();
            if (world == null) return;

            try {
                Records.StructureLocationData data = WorldDataProvider.getInstance().getStructureLocations(world);
                List<Records.StructureConnection> connections = WorldDataProvider.getInstance().getStructureConnections(world);
                List<Records.RoadData> roads = WorldDataProvider.getInstance().getRoadDataList(world);

                List<Records.StructureInfo> structureInfos = data != null ? new ArrayList<>(data.structureInfos()) : new ArrayList<>();
                client.setScreen(new RoadDebugScreen(structureInfos, connections, roads));
            } catch (Exception e) {
                client.setScreen(new RoadDebugScreen(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            }
        } else if (client.player != null && client.getConnection() != null) {
            // 多人游戏：检查是否有管理员权限
            if (!client.player.hasPermissions(2)) {
                // 没有权限，显示提示消息
                client.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("roadweaver.debug_map.no_permission"),
                    true
                );
                return;
            }
            
            // 有管理员权限，通过网络包请求数据
            RoadWeaverNetworkManager.requestDebugData();
            
            // 显示加载提示
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("roadweaver.debug_map.loading"),
                true
            );
            
            // 等待数据返回后打开界面（在PacketHandler中处理）
            // 使用延迟任务检查数据是否到达
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
