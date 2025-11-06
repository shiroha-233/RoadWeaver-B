package net.shiroha233.roadweaver.util;

import java.util.concurrent.*;
import java.util.function.Supplier;

public final class ComputeService {
    private ComputeService() {}

    private static volatile ExecutorService EXEC = null;

    private static synchronized ExecutorService ensure() {
        if (EXEC == null || EXEC.isShutdown() || EXEC.isTerminated()) {
            int threads;
            try {
                int cores = Runtime.getRuntime().availableProcessors();
                threads = Math.max(1, cores - 1);
            } catch (Throwable t) {
                threads = 1;
            }
            EXEC = Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "RW-Compute-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });
        }
        return EXEC;
    }

    public static Executor executor() {
        return ensure();
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ensure());
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ensure());
    }

    public static void shutdownNow() {
        ExecutorService e = EXEC;
        if (e != null) {
            try { e.shutdownNow(); } catch (Throwable ignored) {}
            EXEC = null;
        }
    }
}
