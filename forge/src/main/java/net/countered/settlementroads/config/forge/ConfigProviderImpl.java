package net.countered.settlementroads.config.forge;

import net.countered.settlementroads.config.IModConfig;

public class ConfigProviderImpl {
    public static IModConfig get() {
        return new ForgeModConfigAdapter();
    }
}
