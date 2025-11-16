package net.shiroha233.roadweaver.generation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.planning.RoadPlanningService;

import java.util.List;

/**
 * 初始道路生成管理器：在服务器启动后，阻塞直到初始规划范围内的道路生成完成，并提供进度统计。
 */
public final class InitialGenManager {
    private InitialGenManager() {}

    private static volatile boolean active;
    private static volatile int total;
    private static volatile int done;
    private static volatile int planned;
    private static volatile int generating;
    private static volatile int failed;

    public static boolean isActive() { return active; }
    public static int getTotal() { return total; }
    public static int getDone() { return done; }
    public static int getPlanned() { return planned; }
    public static int getGenerating() { return generating; }
    public static int getFailed() { return failed; }

    /**
     * 在服务器启动时调用：执行初始规划并计算总任务数。
     */
    public static void begin(ServerLevel level) {
        if (level == null || !Level.OVERWORLD.equals(level.dimension())) return;
        // 清零状态
        active = true;
        total = 0;
        done = 0;
        planned = 0;
        generating = 0;
        failed = 0;

        // 确保生成线程池已初始化
        RoadGenerationService.onServerStarted();

        // 首开世界：尝试放置出生点小屋（幂等）
        net.shiroha233.roadweaver.structures.spawn.SpawnCabinService.ensurePlaced(level);

        // 进行初始规划：写入结构连接（PLANNED）
        RoadPlanningService.initialPlan(level);

        // 统计总数
        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> conns = provider.getStructureConnections(level);
        total = (conns == null) ? 0 : conns.size();
        // 初始化一次完成度
        update(level);
    }

    /**
     * 循环推进生成并阻塞直到全部完成或总数为0。
     * 注意：在服务器启动线程中调用，期间不会触发常规 tick，因此这里主动调用 tick 推进队列与执行。
     */
    public static void blockUntilDone(ServerLevel level) {
        if (!active) return;
        // 采用同步方式逐条生成，确保在世界生成前完成（避免依赖线程池和玩家列表）
        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> list = provider.getStructureConnections(level);
        if (list != null) {
            for (Records.StructureConnection c : new java.util.ArrayList<>(list)) {
                if (c.status() == Records.ConnectionStatus.PLANNED) {
                    RoadGenerationService.generateInline(level, c);
                    update(level);
                }
            }
        }
        active = false;
    }

    /**
     * 读取世界数据统计完成数量。
     */
    public static void update(ServerLevel level) {
        WorldDataProvider provider = WorldDataProvider.getInstance();
        List<Records.StructureConnection> conns = provider.getStructureConnections(level);
        if (conns == null) { total = 0; planned = 0; generating = 0; done = 0; failed = 0; return; }
        int p = 0, g = 0, c = 0, f = 0;
        for (Records.StructureConnection sc : conns) {
            Records.ConnectionStatus s = sc.status();
            if (s == Records.ConnectionStatus.PLANNED) p++;
            else if (s == Records.ConnectionStatus.GENERATING) g++;
            else if (s == Records.ConnectionStatus.COMPLETED) c++;
            else if (s == Records.ConnectionStatus.FAILED) f++;
        }
        total = conns.size();
        planned = p;
        generating = g;
        done = c;
        failed = f;
    }
}
