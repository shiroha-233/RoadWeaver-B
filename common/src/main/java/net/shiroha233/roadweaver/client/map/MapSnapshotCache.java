package net.shiroha233.roadweaver.client.map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public final class MapSnapshotCache {
    private static volatile MapSnapshot SNAPSHOT;
    private static final AtomicInteger CLEAR_SEQ = new AtomicInteger();

    private MapSnapshotCache() {}

    public static MapSnapshot peek() {
        return SNAPSHOT;
    }

    public static void put(MapSnapshot s) {
        SNAPSHOT = s;
    }
    public static void scheduleClear(long delayMs) {
        int token = CLEAR_SEQ.incrementAndGet();
        long d = Math.max(0L, delayMs);
        Executor delayed = CompletableFuture.delayedExecutor(d, TimeUnit.MILLISECONDS);
        CompletableFuture.runAsync(() -> {
            if (CLEAR_SEQ.get() == token) {
                SNAPSHOT = null;
            }
        }, delayed);
    }
    public static void cancelClear() {
        CLEAR_SEQ.incrementAndGet();
    }
    
    public static void clearNow() {
        CLEAR_SEQ.incrementAndGet();
        SNAPSHOT = null;
    }
}
