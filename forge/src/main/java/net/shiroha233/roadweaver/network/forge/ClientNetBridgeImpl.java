package net.shiroha233.roadweaver.network.forge;

public final class ClientNetBridgeImpl {
    private ClientNetBridgeImpl() {}

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        MapNetworkForge.requestSnapshot(minX, minZ, maxX, maxZ);
    }

    public static void requestTeleport(int x, int y, int z) {
        MapNetworkForge.requestTeleport(x, y, z);
    }

    public static void requestManualConnect(int ax, int az, int bx, int bz) {
        MapNetworkForge.requestManualConnect(ax, az, bx, bz);
    }
}
