package net.countered.settlementroads.client.gui;

import net.countered.settlementroads.helpers.Records;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * UI 渲染器（Fabric） - 标题、统计、图例、提示
 */
public class UIRenderer {

    private static final int PADDING = 20;
    private final Map<String, Integer> statusColors;
    private final StructureColorManager colorManager;

    public UIRenderer(Map<String, Integer> statusColors, StructureColorManager colorManager) {
        this.statusColors = statusColors;
        this.colorManager = colorManager;
    }

    public void drawTitle(GuiGraphics ctx, int width) {
        Font font = Minecraft.getInstance().font;
        String title = net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.title").getString();
        int tw = font.width(title);
        int x = (width - tw) / 2;
        int y = PADDING + 8;

        RenderUtils.drawPanel(ctx, x - 10, y - 5, x + tw + 10, y + 14, 0xC0000000, 0xFF4A90E2);
        ctx.drawString(font, title, x, y, 0xFFFFFFFF, true);
    }

    public void drawStatsPanel(GuiGraphics ctx, int width,
                               List<Records.StructureInfo> structureInfos,
                               List<Records.StructureConnection> connections,
                               List<Records.RoadData> roads,
                               double zoom,
                               double baseScale) {
        Font font = Minecraft.getInstance().font;

        int planned = 0, generating = 0, completed = 0, failed = 0;
        for (Records.StructureConnection conn : connections) {
            switch (conn.status()) {
                case PLANNED -> planned++;
                case GENERATING -> generating++;
                case COMPLETED -> completed++;
                case FAILED -> failed++;
            }
        }

        int validRoads = 0;
        for (Records.RoadData road : roads) {
            if (road.roadSegmentList() != null && road.roadSegmentList().size() >= 2) {
                validRoads++;
            }
        }

        String[] labels = {
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.structures", structureInfos.size()).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.planned", planned).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.generating", generating).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.completed", completed).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.failed", failed).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.roads", roads.size()).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.valid", validRoads).getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.zoom", String.format("%.1f", zoom)).getString()
        };

        int[] colors = {
                statusColors.get("structure"),
                statusColors.get("planned"),
                statusColors.get("generating"),
                statusColors.get("completed"),
                statusColors.get("failed"),
                statusColors.get("road"),
                0xFF00FF00,
                0xFFFFFFFF
        };

        int maxWidth = 0;
        for (String label : labels) {
            maxWidth = Math.max(maxWidth, font.width(label));
        }

        int panelX = width - maxWidth - PADDING - 20;
        int panelY = PADDING + 40;
        int panelW = maxWidth + 16;
        int panelH = labels.length * 12 + 8;

        RenderUtils.drawPanel(ctx, panelX, panelY, panelX + panelW, panelY + panelH, 0xE0000000, 0xFF2C3E50);

        for (int i = 0; i < labels.length; i++) {
            int textY = panelY + 8 + i * 12;
            ctx.drawString(font, labels[i], panelX + 8, textY, colors[i], false);
        }
    }

    public void drawLegendPanel(GuiGraphics ctx, int height, List<Records.StructureInfo> visibleStructureInfos) {
        Font font = Minecraft.getInstance().font;

        // 只显示当前可见范围内的结构类型
        Map<String, Integer> visibleStructureColors = new java.util.LinkedHashMap<>();
        for (Records.StructureInfo info : visibleStructureInfos) {
            String structureId = info.structureId();
            if (!visibleStructureColors.containsKey(structureId)) {
                visibleStructureColors.put(structureId, colorManager.getColorForStructure(structureId));
            }
        }
        
        // 固定图例项
        String[] fixedLabels = {
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.legend.planned").getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.legend.generating").getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.legend.failed").getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.legend.roads").getString(),
                net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.legend.player").getString()
        };
        int[] fixedColors = {
                statusColors.get("planned"),
                statusColors.get("generating"),
                statusColors.get("failed"),
                statusColors.get("road"),
                0xFFE74C3C
        };

        int maxWidth = 0;
        for (String label : fixedLabels) {
            maxWidth = Math.max(maxWidth, font.width(label));
        }
        for (String structureId : visibleStructureColors.keySet()) {
            String displayName = colorManager.getDisplayName(structureId);
            maxWidth = Math.max(maxWidth, font.width(displayName));
        }

        int totalItems = fixedLabels.length + visibleStructureColors.size();
        int panelX = PADDING;
        int panelY = height - totalItems * 12 - PADDING - 16;
        int panelW = maxWidth + 32;
        int panelH = totalItems * 12 + 8;

        RenderUtils.drawPanel(ctx, panelX, panelY, panelX + panelW, panelY + panelH, 0xE0000000, 0xFF34495E);

        int currentY = panelY + 8;
        
        // 绘制可见结构类型图例
        for (Map.Entry<String, Integer> entry : visibleStructureColors.entrySet()) {
            String displayName = colorManager.getDisplayName(entry.getKey());
            int color = entry.getValue();
            
            RenderUtils.fillCircle(ctx, panelX + 12, currentY + 4, 3, color);
            ctx.drawString(font, displayName, panelX + 24, currentY, 0xFFFFFFFF, false);
            currentY += 12;
        }
        
        // 绘制固定图例
        for (int i = 0; i < fixedLabels.length; i++) {
            RenderUtils.fillCircle(ctx, panelX + 12, currentY + 4, 3, fixedColors[i]);
            ctx.drawString(font, fixedLabels[i], panelX + 24, currentY, 0xFFFFFFFF, false);
            currentY += 12;
        }
    }

    public void drawTooltip(GuiGraphics ctx, BlockPos structure, String structureId, int mouseX, int mouseY, int width) {
        Font font = Minecraft.getInstance().font;
        
        String displayName = colorManager.getDisplayName(structureId);
        String posText = net.minecraft.network.chat.Component.translatable("gui.roadweaver.debug_map.position").getString() + ": " + structure.getX() + ", " + structure.getZ();
        
        int nameWidth = font.width(displayName);
        int posWidth = font.width(posText);
        int tooltipWidth = Math.max(nameWidth, posWidth) + 8;
        int tooltipHeight = 28;

        int x = mouseX + 10;
        int y = mouseY - 35;

        if (x + tooltipWidth > width) x = mouseX - tooltipWidth - 10;
        if (y < 0) y = mouseY + 10;

        int structureColor = colorManager.getColorForStructure(structureId);
        RenderUtils.drawPanel(ctx, x, y, x + tooltipWidth, y + tooltipHeight, 0xF0000000, structureColor);
        
        ctx.drawString(font, displayName, x + 4, y + 5, 0xFFFFFFFF, true);
        ctx.drawString(font, posText, x + 4, y + 16, 0xFFAAAAAA, false);
    }
}
