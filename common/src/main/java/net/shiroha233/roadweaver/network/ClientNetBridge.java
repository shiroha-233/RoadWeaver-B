package net.shiroha233.roadweaver.network;

public final class ClientNetBridge {
    private ClientNetBridge() {}

    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.network.fabric.MapNetworkFabric");
            c.getMethod("requestSnapshot", int.class, int.class, int.class, int.class)
                    .invoke(null, minX, minZ, maxX, maxZ);
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.network.forge.MapNetworkForge");
            c.getMethod("requestSnapshot", int.class, int.class, int.class, int.class)
                    .invoke(null, minX, minZ, maxX, maxZ);
        } catch (Throwable ignored) {}
    }

    public static void requestTeleport(int x, int y, int z) {
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.network.fabric.MapNetworkFabric");
            c.getMethod("requestTeleport", int.class, int.class, int.class)
                    .invoke(null, x, y, z);
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.network.forge.MapNetworkForge");
            c.getMethod("requestTeleport", int.class, int.class, int.class)
                    .invoke(null, x, y, z);
        } catch (Throwable ignored) {}
    }

    public static void requestManualConnect(int ax, int az, int bx, int bz) {
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.network.fabric.MapNetworkFabric");
            c.getMethod("requestManualConnect", int.class, int.class, int.class, int.class)
                    .invoke(null, ax, az, bx, bz);
            return;
        } catch (Throwable ignored) {}
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.network.forge.MapNetworkForge");
            c.getMethod("requestManualConnect", int.class, int.class, int.class, int.class)
                    .invoke(null, ax, az, bx, bz);
        } catch (Throwable ignored) {}
    }
}
