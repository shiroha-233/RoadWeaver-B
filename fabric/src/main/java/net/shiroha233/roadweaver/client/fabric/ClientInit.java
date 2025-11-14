package net.shiroha233.roadweaver.client.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.shiroha233.roadweaver.client.map.RoadMapScreen;
import net.shiroha233.roadweaver.client.map.MapSnapshotCache;
import net.shiroha233.roadweaver.network.fabric.MapNetworkFabric;
import org.lwjgl.glfw.GLFW;

public class ClientInit implements ClientModInitializer {
    public static KeyMapping OPEN_MAP;

    @Override
    public void onInitializeClient() {
        MapNetworkFabric.registerClientReceivers();
        
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MapSnapshotCache.clearNow());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MapSnapshotCache.clearNow());

        OPEN_MAP = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.roadweaver.open_map",
                GLFW.GLFW_KEY_H,
                "key.categories.roadweaver"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.screen instanceof RoadMapScreen) return;
            while (OPEN_MAP.consumeClick()) {
                client.setScreen(new RoadMapScreen());
            }
        });
    }
}
