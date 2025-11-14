package net.shiroha233.roadweaver.util;

import net.shiroha233.roadweaver.runtime.ThreadPoolManager;

import java.util.concurrent.*;
import java.util.function.Supplier;

public final class ComputeService {
    private ComputeService() {}

    public static Executor executor() {
        return ThreadPoolManager.computeExecutor();
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ThreadPoolManager.computeExecutor());
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ThreadPoolManager.computeExecutor());
    }

    public static void shutdownNow() {
        // Delegate to central manager to ensure epoch rollover and full pool shutdown
        ThreadPoolManager.onServerStopping();
    }
}
