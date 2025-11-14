package net.shiroha233.roadweaver.network.fabric;

public final class ClientNetBridgeImpl {
    private ClientNetBridgeImpl() {}

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        MapNetworkFabric.requestSnapshot(minX, minZ, maxX, maxZ);
    }

    public static void requestTeleport(int x, int y, int z) {
        MapNetworkFabric.requestTeleport(x, y, z);
    }

    public static void requestManualConnect(int ax, int az, int bx, int bz) {
        MapNetworkFabric.requestManualConnect(ax, az, bx, bz);
    }
}
