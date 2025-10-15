package net.countered.settlementroads.client.gui;

import net.countered.settlementroads.helpers.Records;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * 地图渲染器 - 负责绘制道路、连接线、结构等元素（Fabric）
 */
public class MapRenderer {

    // LOD级别枚举
    public enum LODLevel {
        HIGH,
        MEDIUM,
        LOW,
        MINIMAL
    }

    // 道路LOD级别
    public enum RoadLODLevel {
        FINEST, EIGHTH, SIXTEENTH, THIRTY_SECOND, SIXTY_FOURTH,
        ONE_TWENTY_EIGHTH, TWO_FIFTY_SIXTH, FIVE_TWELVE, NONE
    }

    private static final int RADIUS = 5;
    private static final int TARGET_GRID_PX = 80;

    private final Map<String, Integer> statusColors;
    private final RoadDebugScreen.ScreenBounds bounds;
    private final StructureColorManager colorManager;

    public MapRenderer(Map<String, Integer> statusColors, RoadDebugScreen.ScreenBounds bounds, StructureColorManager colorManager) {
        this.statusColors = statusColors;
        this.bounds = bounds;
        this.colorManager = colorManager;
    }

    public LODLevel getLODLevel(double zoom) {
        if (zoom > 3.0) return LODLevel.HIGH;
        if (zoom > 1.0) return LODLevel.MEDIUM;
        if (zoom > 0.3) return LODLevel.LOW;
        return LODLevel.MINIMAL;
    }

    public RoadLODLevel getRoadLODLevel(double baseScale, double zoom) {
        double blocksPerPixel = 1.0 / (baseScale * zoom);
        double blocksPerGrid = blocksPerPixel * TARGET_GRID_PX;

        if (blocksPerGrid < 300) return RoadLODLevel.FINEST;
        if (blocksPerGrid < 800) return RoadLODLevel.EIGHTH;
        if (blocksPerGrid < 1600) return RoadLODLevel.SIXTEENTH;
        if (blocksPerGrid < 3200) return RoadLODLevel.THIRTY_SECOND;
        if (blocksPerGrid < 6400) return RoadLODLevel.SIXTY_FOURTH;
        if (blocksPerGrid < 12800) return RoadLODLevel.ONE_TWENTY_EIGHTH;
        if (blocksPerGrid < 25600) return RoadLODLevel.TWO_FIFTY_SIXTH;
        if (blocksPerGrid < 50000) return RoadLODLevel.FIVE_TWELVE;
        return RoadLODLevel.NONE;
    }

    public void drawRoadPaths(GuiGraphics ctx, List<Records.RoadData> roads,
                              LODLevel lod, double baseScale, double zoom,
                              WorldToScreenConverter converter) {
        if (roads == null || roads.isEmpty() || lod == LODLevel.MINIMAL) return;

        RoadLODLevel roadLOD = getRoadLODLevel(baseScale, zoom);
        if (roadLOD == RoadLODLevel.NONE) return;

        int roadColor = (statusColors.get("road") & 0x00FFFFFF) | 0x80000000;

        for (Records.RoadData roadData : roads) {
            List<Records.RoadSegmentPlacement> segments = roadData.roadSegmentList();
            if (segments == null || segments.size() < 2) continue;

            drawRoadPathWithLOD(ctx, segments, roadColor, roadLOD, converter);
        }
    }

    private void drawRoadPathWithLOD(GuiGraphics ctx, List<Records.RoadSegmentPlacement> segments,
                                     int color, RoadLODLevel roadLOD, WorldToScreenConverter converter) {
        int step = switch (roadLOD) {
            case FINEST -> 1;
            case EIGHTH -> 8;
            case SIXTEENTH -> 16;
            case THIRTY_SECOND -> 32;
            case SIXTY_FOURTH -> 64;
            case ONE_TWENTY_EIGHTH -> 128;
            case TWO_FIFTY_SIXTH -> 256;
            case FIVE_TWELVE -> 512;
            case NONE -> Integer.MAX_VALUE;
        };

        if (step >= segments.size()) return;

        RoadDebugScreen.ScreenPos prevPos = null;

        for (int i = 0; i < segments.size(); i += step) {
            BlockPos pos = segments.get(i).middlePos();
            RoadDebugScreen.ScreenPos currentPos = converter.worldToScreen(pos.getX(), pos.getZ());

            if (prevPos != null && i > 0) {
                if (bounds.isLineInBounds(prevPos.x(), prevPos.y(), currentPos.x(), currentPos.y())) {
                    RenderUtils.drawLine(ctx, prevPos.x(), prevPos.y(), currentPos.x(), currentPos.y(), color);
                }
            }
            prevPos = currentPos;
        }
    }

    public void drawConnections(GuiGraphics ctx, List<Records.StructureConnection> connections,
                                List<Records.RoadData> roads, LODLevel lod,
                                WorldToScreenConverter converter) {
        if (connections == null || connections.isEmpty() || lod == LODLevel.MINIMAL) return;

        for (Records.StructureConnection connection : connections) {
            if (connection.status() == Records.ConnectionStatus.COMPLETED) continue;

            RoadDebugScreen.ScreenPos fromPos = converter.worldToScreen(
                    connection.from().getX(), connection.from().getZ());
            RoadDebugScreen.ScreenPos toPos = converter.worldToScreen(
                    connection.to().getX(), connection.to().getZ());

            if (bounds.isLineInBounds(fromPos.x(), fromPos.y(), toPos.x(), toPos.y())) {
                int color = getConnectionColor(connection);
                RenderUtils.drawDashedLine(ctx, fromPos.x(), fromPos.y(), toPos.x(), toPos.y(), color);
            }
        }
    }

    private int getConnectionColor(Records.StructureConnection connection) {
        return switch (connection.status()) {
            case PLANNED -> statusColors.get("planned");
            case GENERATING -> statusColors.get("generating");
            case FAILED -> statusColors.get("failed");
            default -> statusColors.get("completed");
        };
    }

    public void drawStructures(GuiGraphics ctx, List<net.countered.settlementroads.helpers.Records.StructureInfo> structureInfos,
                               BlockPos hoveredStructure, BlockPos manualFirst, LODLevel lod,
                               WorldToScreenConverter converter) {
        if (structureInfos == null || structureInfos.isEmpty()) return;

        int adaptiveRadius = getAdaptiveNodeRadius(lod, 3.0);

        for (net.countered.settlementroads.helpers.Records.StructureInfo info : structureInfos) {
            BlockPos structure = info.pos();
            String structureId = info.structureId();
            int structureColor = colorManager.getColorForStructure(structureId);
            
            RoadDebugScreen.ScreenPos pos = converter.worldToScreen(structure.getX(), structure.getZ());

            if (!bounds.isInBounds(pos.x(), pos.y(), adaptiveRadius + 6)) continue;

            boolean isHovered = structure.equals(hoveredStructure);
            boolean isManualSelected = structure.equals(manualFirst);
            int radius = isHovered ? adaptiveRadius + 2 : adaptiveRadius;

            switch (lod) {
                case HIGH -> {
                    // 手动选中的结构：金色脉冲光晕
                    if (isManualSelected) {
                        long time = System.currentTimeMillis();
                        float pulse = (float) (Math.sin(time / 200.0) * 0.5 + 0.5);
                        int pulseAlpha = (int) (0x60 + pulse * 0x40);
                        int glowColor = (pulseAlpha << 24) | 0xFFD700;
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius + 4, glowColor);
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius + 2, 0x80FFD700);
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius, 0xFFFFD700);
                        RenderUtils.drawCircleOutline(ctx, pos.x(), pos.y(), radius, 0xFFFFAA00);
                        
                        // 绘制十字标记
                        int crossSize = radius + 3;
                        ctx.fill(pos.x() - crossSize, pos.y() - 1, pos.x() + crossSize, pos.y() + 1, 0xFFFFD700);
                        ctx.fill(pos.x() - 1, pos.y() - crossSize, pos.x() + 1, pos.y() + crossSize, 0xFFFFD700);
                    } else {
                        // 使用结构类型的颜色
                        int glowColor = (structureColor & 0x00FFFFFF) | 0x40000000;
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius + 1, glowColor);
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius, structureColor);
                        int outlineColor = darkenColor(structureColor);
                        RenderUtils.drawCircleOutline(ctx, pos.x(), pos.y(), radius, outlineColor);

                        int highlightSize = Math.max(1, radius / 3);
                        ctx.fill(pos.x() - highlightSize, pos.y() - highlightSize,
                                pos.x() + highlightSize + 1, pos.y() + highlightSize + 1, 0x80FFFFFF);
                    }
                }
                case MEDIUM -> {
                    if (isManualSelected) {
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius + 1, 0x80FFD700);
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius, 0xFFFFD700);
                        RenderUtils.drawCircleOutline(ctx, pos.x(), pos.y(), radius, 0xFFFFAA00);
                    } else {
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius, structureColor);
                        RenderUtils.drawCircleOutline(ctx, pos.x(), pos.y(), radius, darkenColor(structureColor));
                    }
                    if (radius >= 3) {
                        ctx.fill(pos.x() - 1, pos.y() - 1, pos.x() + 1, pos.y() + 1, 0x60FFFFFF);
                    }
                }
                case LOW -> {
                    RenderUtils.fillCircle(ctx, pos.x(), pos.y(), radius, structureColor);
                    if (radius >= 4) {
                        RenderUtils.drawCircleOutline(ctx, pos.x(), pos.y(), radius, darkenColor(structureColor));
                    }
                }
                case MINIMAL -> {
                    if (adaptiveRadius >= 2) {
                        ctx.fill(pos.x() - 1, pos.y() - 1, pos.x() + 2, pos.y() + 2, structureColor);
                    } else {
                        RenderUtils.fillCircle(ctx, pos.x(), pos.y(), adaptiveRadius, structureColor);
                    }
                }
            }
        }
    }
    
    private int darkenColor(int color) {
        int r = ((color >> 16) & 0xFF) * 2 / 3;
        int g = ((color >> 8) & 0xFF) * 2 / 3;
        int b = (color & 0xFF) * 2 / 3;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public void drawPlayerMarker(GuiGraphics ctx, LODLevel lod, double zoom,
                                 WorldToScreenConverter converter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        RoadDebugScreen.ScreenPos p = converter.worldToScreen(px, pz);

        int playerRadius = Math.max(3, getAdaptiveNodeRadius(lod, zoom) + 1);

        if (!bounds.isInBounds(p.x(), p.y(), playerRadius + 10)) return;

        final int fill = 0xFFE74C3C;
        final int glow = 0x40E74C3C;
        final int outline = 0xFF932D1F;

        float yaw = mc.player.getYRot();
        // Minecraft yaw: 0=South(+Z), 90=West(-X), 180=North(-Z), 270=East(+X)
        // Screen angle: 0=Right(+X), 90=Down(+Y), 180=Left(-X), 270=Up(-Y)
        // Map: North(-Z) is up, so we need: yaw 0 -> 90°, yaw 90 -> 180°, yaw 180 -> 270°, yaw 270 -> 0°
        double angle = Math.toRadians(yaw + 90);

        int arrowLength = playerRadius + Math.max(3, (int)(4 * Math.min(zoom / 3.0, 1.5)));

        double tx = p.x() + Math.cos(angle) * arrowLength;
        double ty = p.y() + Math.sin(angle) * arrowLength;

        switch (lod) {
            case HIGH -> {
                RenderUtils.fillCircle(ctx, p.x(), p.y(), playerRadius + 1, glow);
                RenderUtils.fillCircle(ctx, p.x(), p.y(), playerRadius, fill);
                RenderUtils.drawCircleOutline(ctx, p.x(), p.y(), playerRadius, outline);
                int highlightSize = Math.max(1, playerRadius / 4);
                ctx.fill(p.x() - highlightSize, p.y() - highlightSize,
                        p.x() + highlightSize + 1, p.y() + highlightSize + 1, 0xAAFFFFFF);

                RenderUtils.drawSmoothLine(ctx, p.x(), p.y(), tx, ty, 0xFFFFFFFF);
            }
            case MEDIUM, LOW -> {
                RenderUtils.fillCircle(ctx, p.x(), p.y(), playerRadius, fill);
                RenderUtils.drawCircleOutline(ctx, p.x(), p.y(), playerRadius, outline);
                RenderUtils.drawSmoothLine(ctx, p.x(), p.y(), tx, ty, 0xFFFFFFFF);
            }
            case MINIMAL -> {
                RenderUtils.fillCircle(ctx, p.x(), p.y(), Math.max(2, playerRadius), fill);
                int shortArrow = playerRadius + 2;
                double stx = p.x() + Math.cos(angle) * shortArrow;
                double sty = p.y() + Math.sin(angle) * shortArrow;
                RenderUtils.drawSmoothLine(ctx, p.x(), p.y(), stx, sty, 0xFFFFFFFF);
            }
        }
    }

    private int getAdaptiveNodeRadius(LODLevel lod, double zoom) {
        double baseRadius = RADIUS;
        double zoomFactor = Math.max(0.3, Math.min(1.2, 1.0 + Math.log10(zoom) * 0.15));
        double scaledRadius = baseRadius * zoomFactor;

        double lodMultiplier = switch (lod) {
            case HIGH -> 0.9;
            case MEDIUM -> 1.0;
            case LOW -> 0.8;
            case MINIMAL -> 0.6;
        };

        return Math.max(2, (int) Math.round(scaledRadius * lodMultiplier));
    }

    public interface WorldToScreenConverter {
        RoadDebugScreen.ScreenPos worldToScreen(double worldX, double worldZ);
    }
}
