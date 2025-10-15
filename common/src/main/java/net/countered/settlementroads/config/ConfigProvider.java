package net.countered.settlementroads.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class ConfigProvider {
    @ExpectPlatform
    public static IModConfig get() {
        throw new AssertionError();
    }
}
