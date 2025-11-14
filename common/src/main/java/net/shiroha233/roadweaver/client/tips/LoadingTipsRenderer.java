package net.shiroha233.roadweaver.client.tips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 世界加载界面 Tips 渲染器
 * 职责：
 * - 管理提示文本列表
 * - 控制切换时间间隔
 * - 在右下角绘制当前提示文本
 */
public final class LoadingTipsRenderer {

    /** 每条提示停留时间（毫秒） */
    private static final long INTERVAL_MILLIS = 3000L; // 3 秒切换一次

    /** 提示文本列表，使用可本地化的翻译键 */
    private static final List<Component> TIPS = List.of(
            Component.translatable("tip.roadweaver.loading.1"),
            Component.translatable("tip.roadweaver.loading.2"),
            Component.translatable("tip.roadweaver.loading.3"),
            Component.translatable("tip.roadweaver.loading.4"),
            Component.translatable("tip.roadweaver.loading.5")
    );

    /** 当前显示的提示索引 */
    private static int currentIndex = 0;
    /** 上次切换提示的时间戳（毫秒） */
    private static long lastSwitchTimeMillis = 0L;

    private LoadingTipsRenderer() {
    }

    /**
     * 在世界加载界面右下角渲染一条提示文本。
     * 该方法假定在客户端渲染线程中被调用。
     */
    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (TIPS.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (lastSwitchTimeMillis == 0L) {
            // 首次调用时初始化时间戳，避免立即跳过第一条
            lastSwitchTimeMillis = now;
        }
        if (now - lastSwitchTimeMillis >= INTERVAL_MILLIS) {
            lastSwitchTimeMillis = now;
            currentIndex = (currentIndex + 1) % TIPS.size();
        }

        Component tip = TIPS.get(currentIndex);

        var font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // 右下角位置：预留少量边距
        int marginX = 6;
        int marginY = 6;
        int x = sw - font.width(tip) - marginX;
        int y = sh - font.lineHeight - marginY;

        graphics.drawString(font, tip, x, y, 0xFFFFFF, false);
    }

    /**
     * 重置内部状态，可在需要时调用（例如以后想在界面关闭时复位）。
     */
    public static void reset() {
        currentIndex = 0;
        lastSwitchTimeMillis = 0L;
    }
}
