package net.countered.settlementroads.config.fabric;

import net.countered.settlementroads.config.IModConfig;

/**
 * Architectury @ExpectPlatform 实现类（Fabric）。
 * 位置必须为：net.countered.settlementroads.config.fabric.ConfigProviderImpl
 */
public final class ConfigProviderImpl {
    public static IModConfig get() {
        return new FabricModConfigAdapter();
    }
}
