package net.countered.settlementroads.persistence.fabric;

import net.countered.settlementroads.persistence.WorldDataProvider;

/**
 * Architectury @ExpectPlatform 实现类（Fabric）。
 * 位置必须为：net.countered.settlementroads.persistence.fabric.WorldDataProviderImpl
 */
public final class WorldDataProviderImpl {
    private static final WorldDataProvider INSTANCE = new FabricWorldDataProvider();

    public static WorldDataProvider getInstance() {
        return INSTANCE;
    }
}
