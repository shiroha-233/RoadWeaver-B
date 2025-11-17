package net.shiroha233.roadweaver.persistence.sharded;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.shiroha233.roadweaver.helpers.Records;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 磁盘分片持久化 + 惰性加载 + LRU 缓存 的道路数据存储。
 * 分片尺寸：每片 32x32 区块（512x512 方块）。文件：r.<rx>.<rz>.nbt
 */
public final class RoadShardStorage {
    private RoadShardStorage() {}

    private static final int SHARD_SIZE_CHUNKS = 32;
    private static final int MAX_CACHE_SHARDS = 128;

    private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;

    private static final java.util.concurrent.ConcurrentHashMap<String, LinkedHashMap<Long, Shard>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static String dimKey(ServerLevel level) {
        ResourceLocation rl = level.dimension().location();
        return rl.getNamespace() + "/" + rl.getPath();
    }

    private static String cacheKey(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        String worldId = worldRoot == null ? "unknown" : worldRoot.toAbsolutePath().normalize().toString();
        return worldId + "|" + dimKey(level);
    }


    private static Path basePath(ServerLevel level) {
        Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
        return worldRoot.resolve("data/roadweaver/roads").resolve(dimKey(level));
    }

    private static long shardKey(int rx, int rz) {
        return (((long) rx) << 32) ^ (rz & 0xffffffffL);
    }

    private static int floorDiv(int a, int b) {
        int r = a / b;
        if ((a ^ b) < 0 && (r * b != a)) r--;
        return r;
    }

    private static int blockToRegion(int block) {
        int chunk = floorDiv(block, 16);
        return floorDiv(chunk, SHARD_SIZE_CHUNKS);
    }

    private static Path shardPath(ServerLevel level, int rx, int rz) throws IOException {
        Path dir = basePath(level);
        Files.createDirectories(dir);
        return dir.resolve("r." + rx + "." + rz + ".nbt");
    }

    private static LinkedHashMap<Long, Shard> cacheForDim(ServerLevel level) {
        String dk = cacheKey(level);
        return CACHE.computeIfAbsent(dk, k -> new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Shard> eldest) {
                if (size() > MAX_CACHE_SHARDS) {
                    Shard s = eldest.getValue();
                    if (s != null) {
                        try { saveShard(level, s); } catch (IOException ignored) {}
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private static Shard loadShard(ServerLevel level, int rx, int rz) throws IOException {
        LinkedHashMap<Long, Shard> cache = cacheForDim(level);
        long sk = shardKey(rx, rz);
        synchronized (cache) {
            Shard s = cache.get(sk);
            if (s != null) return s;
            Path p = shardPath(level, rx, rz);
            List<Records.RoadData> roads = new ArrayList<>();
            if (Files.exists(p)) {
                CompoundTag tag = net.minecraft.nbt.NbtIo.readCompressed(p.toFile());
                if (tag != null && tag.contains("roads")) {
                    Tag list = tag.get("roads");
                    DataResult<List<Records.RoadData>> res = Codec.list(Records.RoadData.CODEC).parse(new Dynamic<>(OPS, list));
                    res.result().ifPresent(roads::addAll);
                }
            }
            s = new Shard(rx, rz, roads);
            cache.put(sk, s);
            return s;
        }
    }

    private static void saveShard(ServerLevel level, Shard s) throws IOException {
        List<Records.RoadData> snapshot;
        synchronized (s) {
            if (!s.dirty) {
                return;
            }
            // 在锁内复制一次，避免在写入磁盘时被其他线程修改
            snapshot = new ArrayList<>(s.roads);
            s.dirty = false;
        }
        CompoundTag tag = new CompoundTag();
        Codec.list(Records.RoadData.CODEC).encodeStart(OPS, snapshot)
                .result()
                .ifPresent(nbt -> tag.put("roads", nbt));
        Path p = shardPath(level, s.rx, s.rz);
        net.minecraft.nbt.NbtIo.writeCompressed(tag, p.toFile());
    }

    public static void flushAll(ServerLevel level) {
        LinkedHashMap<Long, Shard> cache = cacheForDim(level);
        synchronized (cache) {
            for (Shard s : cache.values()) {
                try { saveShard(level, s); } catch (IOException ignored) {}
            }
        }
    }

    public static void clearAll(ServerLevel level) {
        LinkedHashMap<Long, Shard> cache = cacheForDim(level);
        synchronized (cache) { cache.clear(); }
    }

    public static void addRoad(ServerLevel level, Records.RoadData rd) {
        if (rd == null || rd.roadSegmentList() == null || rd.roadSegmentList().isEmpty()) return;
        int minx = Integer.MAX_VALUE, minz = Integer.MAX_VALUE, maxx = Integer.MIN_VALUE, maxz = Integer.MIN_VALUE;
        for (Records.RoadSegmentPlacement seg : rd.roadSegmentList()) {
            BlockPos p = seg.middlePos();
            int x = p.getX();
            int z = p.getZ();
            if (x < minx) minx = x;
            if (z < minz) minz = z;
            if (x > maxx) maxx = x;
            if (z > maxz) maxz = z;
        }
        int rx0 = blockToRegion(minx);
        int rz0 = blockToRegion(minz);
        int rx1 = blockToRegion(maxx);
        int rz1 = blockToRegion(maxz);
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int rz = rz0; rz <= rz1; rz++) {
                try {
                    Shard s = loadShard(level, rx, rz);
                    long id = fingerprint(rd);
                    synchronized (s) {
                        if (s.ids.add(id)) {
                            s.roads.add(rd);
                            s.dirty = true;
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    private static long fingerprint(Records.RoadData rd) {
        if (rd == null || rd.roadSegmentList() == null || rd.roadSegmentList().isEmpty()) return 0L;
        BlockPos a = rd.roadSegmentList().get(0).middlePos();
        BlockPos b = rd.roadSegmentList().get(rd.roadSegmentList().size() - 1).middlePos();
        long ka = (((long)a.getX()) << 32) ^ (a.getZ() & 0xffffffffL);
        long kb = (((long)b.getX()) << 32) ^ (b.getZ() & 0xffffffffL);
        long lo = Math.min(ka, kb), hi = Math.max(ka, kb);
        long f = (hi << 1) ^ lo;
        f ^= ((long) rd.width() & 0xffffffffL);
        f ^= ((long) rd.roadType() & 0xffffffffL) << 33;
        return f;
    }

    public static List<Records.RoadData> queryRect(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        int rx0 = blockToRegion(minBlockX);
        int rz0 = blockToRegion(minBlockZ);
        int rx1 = blockToRegion(maxBlockX);
        int rz1 = blockToRegion(maxBlockZ);
        List<Records.RoadData> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int rx = rx0; rx <= rx1; rx++) {
            for (int rz = rz0; rz <= rz1; rz++) {
                try {
                    Shard s = loadShard(level, rx, rz);
                    List<Records.RoadData> snapshot;
                    synchronized (s) {
                        // 这里只在锁内复制列表，实际判定与放置在锁外完成，减少锁持有时间
                        snapshot = new ArrayList<>(s.roads);
                    }
                    for (Records.RoadData rd : snapshot) {
                        if (intersects(rd, minBlockX, minBlockZ, maxBlockX, maxBlockZ)) {
                            long id = fingerprint(rd);
                            if (seen.add(id)) out.add(rd);
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
        return out;
    }

    private static boolean intersects(Records.RoadData rd, int minx, int minz, int maxx, int maxz) {
        if (rd == null || rd.roadSegmentList() == null || rd.roadSegmentList().isEmpty()) return false;
        int rminx = Integer.MAX_VALUE, rminz = Integer.MAX_VALUE, rmaxx = Integer.MIN_VALUE, rmaxz = Integer.MIN_VALUE;
        for (Records.RoadSegmentPlacement seg : rd.roadSegmentList()) {
            BlockPos p = seg.middlePos();
            int x = p.getX(), z = p.getZ();
            if (x < rminx) rminx = x;
            if (z < rminz) rminz = z;
            if (x > rmaxx) rmaxx = x;
            if (z > rmaxz) rmaxz = z;
        }
        return !(rmaxx < minx || rminx > maxx || rmaxz < minz || rminz > maxz);
    }

    private static final class Shard {
        final int rx, rz;
        // roads / ids 由 RoadShardStorage 使用 synchronized(this) 保护，避免并发修改异常
        final List<Records.RoadData> roads;
        final Set<Long> ids = new HashSet<>();
        boolean dirty;
        Shard(int rx, int rz, List<Records.RoadData> roads) {
            this.rx = rx; this.rz = rz; this.roads = new ArrayList<>(roads != null ? roads : List.of());
            for (Records.RoadData rd : this.roads) ids.add(fingerprint(rd));
            this.dirty = false;
        }
    }
}
