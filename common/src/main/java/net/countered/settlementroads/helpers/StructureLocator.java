package net.countered.settlementroads.helpers;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerLevel;

public class StructureLocator {
    @ExpectPlatform
    public static void locateConfiguredStructure(ServerLevel serverWorld, int locateCount, boolean locateAtPlayer) {
        throw new AssertionError();
    }
}
