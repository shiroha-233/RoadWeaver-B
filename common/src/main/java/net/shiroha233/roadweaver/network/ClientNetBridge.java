package net.shiroha233.roadweaver.network;

import dev.architectury.injectables.annotations.ExpectPlatform;

public final class ClientNetBridge {
    private ClientNetBridge() {}

    @ExpectPlatform
    public static void requestSnapshot(int minX, int minZ, int maxX, int maxZ) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void requestTeleport(int x, int y, int z) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void requestManualConnect(int ax, int az, int bx, int bz) {
        throw new AssertionError();
    }
}
