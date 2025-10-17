package net.countered.settlementroads.client.gui;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Road network debug screen
 * Features: Display structure nodes, road connections, support pan/zoom, click to teleport
 */
public class RoadDebugScreen extends Screen {

    private static final int RADIUS = 5;
    private static final int PADDING = 20;
    private static final int TARGET_GRID_PX = 80;

    private final List<BlockPos> structures;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;

    private final Map<BlockPos, ScreenPos> screenPositions = new HashMap<>();
    private final Map<String, Integer> statusColors = Map.of(
            "structure", 0xFF27AE60,   // Green - Structure
            "planned", 0xFFF2C94C,     // Yellow - Planned
            "generating", 0xFFE67E22,  // Orange - Generating
            "completed", 0xFF27AE60,   // Green - Completed (not displayed)
            "failed", 0xFFE74C3C,      // Red - Generation failed
            "road", 0xFF3498DB         // Blue - Road
    );

    private boolean dragging = false;
    private boolean firstLayout = true;
    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double baseScale = 1.0;
    private int minX, maxX, minZ, maxZ;
    
    // Progress update timer
    private long lastProgressUpdate = 0;
    private static final long PROGRESS_UPDATE_INTERVAL = 1000; // Update once per second

    public RoadDebugScreen(List<BlockPos> structures, 
                          List<Records.StructureConnection> connections,
                          List<Records.RoadData> roads) {
        super(Text.translatable("gui.settlementroads.debug_map.title"));
        // Create immutable copies to avoid concurrent modification exceptions
        this.structures = structures != null ? new ArrayList<>(structures) : new ArrayList<>();
        this.connections = connections != null ? new ArrayList<>(connections) : new ArrayList<>();
        this.roads = roads != null ? new ArrayList<>(roads) : new ArrayList<>();

        if (!this.structures.isEmpty()) {
            minX = this.structures.stream().mapToInt(BlockPos::getX).min().orElse(0);
            maxX = this.structures.stream().mapToInt(BlockPos::getX).max().orElse(0);
            minZ = this.structures.stream().mapToInt(BlockPos::getZ).min().orElse(0);
            maxZ = this.structures.stream().mapToInt(BlockPos::getZ).max().orElse(0);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Update progress once per second
        updateProgressIfNeeded();
        
        computeLayout();

        // Draw main background panel - dark semi-transparent
        drawPanel(ctx, PADDING, PADDING, width - PADDING, height - PADDING, 0xE0101010, 0xFF2C2C2C);

        // Draw grid
        drawGrid(ctx);

        // Draw road paths
        drawRoadPaths(ctx);

        // Draw connection lines (completed ones are not displayed because they have actual roads)
        for (Records.StructureConnection conn : connections) {
            // Skip completed connections
            if (conn.status() == Records.ConnectionStatus.COMPLETED) {
                continue;
            }
            
            ScreenPos a = screenPositions.get(conn.from());
            ScreenPos b = screenPositions.get(conn.to());
            if (a == null || b == null) continue;
            
            // Select color based on status - use more vibrant colors
            int color = switch (conn.status()) {
                case PLANNED -> 0xFFFFD700; // Gold
                case GENERATING -> 0xFFFF8C00; // Dark orange
                case COMPLETED -> statusColors.get("completed");
                case FAILED -> 0xFFFF4444; // Bright red
            };
            
            drawLine(ctx, a.x, a.y, b.x, b.y, color);
            
            // Show progress for generating connections
            if (conn.status() == Records.ConnectionStatus.GENERATING) {
                drawGeneratingProgress(ctx, a, b, conn);
            }
        }

        // Draw structure nodes - larger and more visible
        BlockPos hovered = null;
        for (BlockPos pos : structures) {
            ScreenPos p = screenPositions.get(pos);
            if (p == null) continue;
            
            // Outer glow effect
            fillCircle(ctx, p.x, p.y, RADIUS + 2, 0x40FFFFFF);
            // Main body
            fillCircle(ctx, p.x, p.y, RADIUS, 0xFF2ECC71);
            // Highlight
            fillCircle(ctx, p.x - 1, p.y - 1, 2, 0x8CFFFFFF);
            // Border
            drawCircleOutline(ctx, p.x, p.y, RADIUS, 0xFF1E8449);

            if (dist2(p.x, p.y, mouseX, mouseY) <= (RADIUS + 2) * (RADIUS + 2)) {
                hovered = pos;
            }
        }

        // Draw player position
        drawPlayerMarker(ctx);

        // Draw UI elements
        drawTitle(ctx);
        drawStatsPanel(ctx);
        drawLegendPanel(ctx);
        drawScalePanel(ctx);

        // Show hover tooltip - placed last to ensure it's on top
        if (hovered != null) {
            drawTooltip(ctx, hovered, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawRoadPaths(DrawContext ctx) {
        if (roads == null || roads.isEmpty()) return;

        int roadColor = statusColors.get("road");
        
        for (Records.RoadData roadData : roads) {
            List<Records.RoadSegmentPlacement> segments = roadData.roadSegmentList();
            if (segments == null || segments.size() < 2) continue;

            // Draw road paths (connecting center points)
            for (int i = 0; i < segments.size() - 1; i++) {
                BlockPos pos1 = segments.get(i).middlePos();
                BlockPos pos2 = segments.get(i + 1).middlePos();
                
                ScreenPos p1 = worldToScreen(pos1.getX(), pos1.getZ());
                ScreenPos p2 = worldToScreen(pos2.getX(), pos2.getZ());
                
                // Use semi-transparent blue to draw roads
                drawLine(ctx, p1.x, p1.y, p2.x, p2.y, (roadColor & 0x00FFFFFF) | 0x80000000);
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Don't pause game
    }

    @Override
    protected void applyBlur(float delta) {
        // Disable blur effect
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Click node to teleport
        BlockPos clicked = findClickedStructure(mouseX, mouseY);
        if (clicked != null) {
            teleportTo(clicked);
            return true;
        }
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            offsetX += deltaX;
            offsetY += deltaY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double old = zoom;
        zoom = vertical > 0 ? zoom * 1.1 : zoom / 1.1;
        zoom = Math.max(0.1, Math.min(10.0, zoom)); // Limit zoom range
        
        offsetX = (offsetX - mouseX + PADDING) * (zoom / old) + mouseX - PADDING;
        offsetY = (offsetY - mouseY + PADDING) * (zoom / old) + mouseY - PADDING;
        return true;
    }

    // Draw title bar
    private void drawTitle(DrawContext ctx) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        Text title = Text.translatable("gui.settlementroads.debug_map.title");
        int tw = font.getWidth(title);
        int x = (width - tw) / 2;
        int y = PADDING + 8;
        
        // Title background panel
        drawPanel(ctx, x - 10, y - 5, x + tw + 10, y + 14, 0xC0000000, 0xFF4A90E2);
        
        // Draw title text - with shadow
        ctx.drawText(font, title, x, y, 0xFFFFFFFF, true);
    }

    // Draw statistics panel - top right corner
    private void drawStatsPanel(DrawContext ctx) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        
        // Count connections by status
        int planned = 0;
        int generating = 0;
        int completed = 0;
        int failed = 0;
        for (Records.StructureConnection conn : connections) {
            switch (conn.status()) {
                case PLANNED -> planned++;
                case GENERATING -> generating++;
                case COMPLETED -> completed++;
                case FAILED -> failed++;
            }
        }
        
        // Prepare display text
        String[] labels = {
            "Structures: " + structures.size(),
            "Planned: " + planned,
            "Generating: " + generating,
            "Completed: " + completed,
            "Failed: " + failed,
            "Roads: " + roads.size(),
            "Zoom: " + String.format("%.1fx", zoom)
        };
        
        int[] colors = {
            0xFFFFFFFF,
            0xFFFFD700, // Gold
            0xFFFF8C00, // Dark orange
            0xFF2ECC71, // Green
            0xFFFF4444, // Red
            0xFF3498DB, // Blue
            0xFFBDC3C7  // Gray
        };
        
        // Calculate panel size
        int maxWidth = 0;
        for (String label : labels) {
            maxWidth = Math.max(maxWidth, font.getWidth(label));
        }
        
        int panelWidth = maxWidth + 20;
        int panelHeight = labels.length * 14 + 10;
        int x = width - PADDING - panelWidth - 5;
        int y = PADDING + 30;
        
        // Draw panel background
        drawPanel(ctx, x, y, x + panelWidth, y + panelHeight, 0xD0000000, 0xFF34495E);
        
        // Draw text
        int textY = y + 5;
        for (int i = 0; i < labels.length; i++) {
            // Icon indicator
            ctx.fill(x + 5, textY + 2, x + 10, textY + 7, colors[i]);
            ctx.drawBorder(x + 5, textY + 2, 5, 5, 0x80FFFFFF);
            // Text
            ctx.drawText(font, labels[i], x + 13, textY, colors[i], true);
            textY += 14;
        }
    }

    private void drawGrid(DrawContext ctx) {
        int w = width - PADDING * 2;
        int h = height - PADDING * 2;

        double worldX0 = minX + (-offsetX) / (baseScale * zoom);
        double worldZ0 = minZ + (-offsetY) / (baseScale * zoom);
        double worldX1 = minX + (w - offsetX) / (baseScale * zoom);
        double worldZ1 = minZ + (h - offsetY) / (baseScale * zoom);

        int spacing = computeGridSpacing();

        int startWX = (int) Math.floor(worldX0 / spacing) * spacing;
        int startWZ = (int) Math.floor(worldZ0 / spacing) * spacing;

        // Draw vertical lines
        for (int x = startWX; x <= worldX1; x += spacing) {
            int sx = PADDING + (int) ((x - worldX0) * baseScale * zoom);
            fillV(ctx, sx, PADDING, PADDING + h, 0x40444444);
            drawSmallLabel(ctx, String.valueOf(x), sx + 2, PADDING + 2);
        }

        // Draw horizontal lines
        for (int z = startWZ; z <= worldZ1; z += spacing) {
            int sz = PADDING + (int) ((z - worldZ0) * baseScale * zoom);
            fillH(ctx, PADDING, PADDING + w, sz, 0x40444444);
            drawSmallLabel(ctx, String.valueOf(z), PADDING + 2, sz + 2);
        }
    }

    // Draw scale panel - bottom right corner
    private void drawScalePanel(DrawContext ctx) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int spacing = computeGridSpacing();
        int lengthPx = (int) (spacing * baseScale * zoom);
        
        String text = spacing + " blocks";
        int textWidth = font.getWidth(text);
        int panelWidth = Math.max(lengthPx + 20, textWidth + 20);
        int panelHeight = 35;
        
        int x = width - PADDING - panelWidth - 5;
        int y = height - PADDING - panelHeight - 5;
        
        // Draw panel background
        drawPanel(ctx, x, y, x + panelWidth, y + panelHeight, 0xD0000000, 0xFF34495E);
        
        // Draw scale
        int scaleX = x + (panelWidth - lengthPx) / 2;
        int scaleY = y + panelHeight - 10;
        
        // Scale line
        fillH(ctx, scaleX, scaleX + lengthPx, scaleY, 0xFFFFFFFF);
        fillV(ctx, scaleX, scaleY - 4, scaleY + 4, 0xFFFFFFFF);
        fillV(ctx, scaleX + lengthPx, scaleY - 4, scaleY + 4, 0xFFFFFFFF);
        
        // Text
        ctx.drawText(font, text, x + (panelWidth - textWidth) / 2, y + 8, 0xFFFFFFFF, true);
    }

    // Draw legend panel - top left corner
    private void drawLegendPanel(DrawContext ctx) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        
        String[] labels = {
            "Structures",
            "Planned",
            "Generating",
            "Failed",
            "Roads"
        };
        
        int[] colors = {
            0xFF2ECC71, // Green
            0xFFFFD700, // Gold
            0xFFFF8C00, // Dark orange
            0xFFFF4444, // Red
            0xFF3498DB  // Blue
        };
        
        // Calculate panel size
        int maxWidth = 0;
        for (String label : labels) {
            maxWidth = Math.max(maxWidth, font.getWidth(label));
        }
        
        int panelWidth = maxWidth + 30;
        int panelHeight = labels.length * 16 + 10;
        int x = PADDING + 5;
        int y = PADDING + 30;
        
        // Draw panel background
        drawPanel(ctx, x, y, x + panelWidth, y + panelHeight, 0xD0000000, 0xFF34495E);
        
        // Draw legend items
        int itemY = y + 5;
        for (int i = 0; i < labels.length; i++) {
            // Color indicator - circle
            fillCircle(ctx, x + 10, itemY + 4, 4, colors[i]);
            drawCircleOutline(ctx, x + 10, itemY + 4, 4, 0x80FFFFFF);
            
            // Text
            ctx.drawText(font, labels[i], x + 20, itemY, 0xFFFFFFFF, true);
            itemY += 16;
        }
    }

    private void computeLayout() {
        if (structures.isEmpty()) return;
        
        int w = Math.max(1, width - PADDING * 2);
        int h = Math.max(1, height - PADDING * 2);

        double scaleX = (double) w / Math.max(1, maxX - minX);
        double scaleZ = (double) h / Math.max(1, maxZ - minZ);
        baseScale = Math.min(scaleX, scaleZ) * 0.9; // Leave some margin

        if (firstLayout) {
            double graphW = (maxX - minX) * baseScale * zoom;
            double graphH = (maxZ - minZ) * baseScale * zoom;
            offsetX = (w - graphW) / 2.0;
            offsetY = (h - graphH) / 2.0;
            firstLayout = false;
        }

        screenPositions.clear();
        for (BlockPos pos : structures) {
            double sx = (pos.getX() - minX) * baseScale * zoom + offsetX;
            double sy = (pos.getZ() - minZ) * baseScale * zoom + offsetY;
            screenPositions.put(pos, new ScreenPos(PADDING + (int) sx, PADDING + (int) sy));
        }
    }

    private int computeGridSpacing() {
        double unitsPerPixel = 1.0 / (baseScale * zoom);
        double raw = TARGET_GRID_PX * unitsPerPixel;
        double pow10 = Math.pow(10, Math.floor(Math.log10(raw)));
        
        for (int n : new int[]{1, 2, 5}) {
            double candidate = n * pow10;
            if (candidate >= raw) return (int) candidate;
        }
        return (int) (10 * pow10);
    }

    private BlockPos findClickedStructure(double mouseX, double mouseY) {
        for (BlockPos pos : structures) {
            ScreenPos p = screenPositions.get(pos);
            if (p != null && dist2(p.x, p.y, mouseX, mouseY) <= RADIUS * RADIUS) {
                return pos;
            }
        }
        return null;
    }

    private void teleportTo(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mc.getServer() != null) {
            // Single player: execute teleport on server thread
            mc.getServer().execute(() -> {
                ServerPlayerEntity sp = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
                if (sp != null) {
                    sp.requestTeleport(pos.getX() + 0.5, 128.0, pos.getZ() + 0.5);
                }
            });
        }
    }

    private ScreenPos worldToScreen(double wx, double wz) {
        int sx = PADDING + (int) ((wx - minX) * baseScale * zoom + offsetX);
        int sy = PADDING + (int) ((wz - minZ) * baseScale * zoom + offsetY);
        return new ScreenPos(sx, sy);
    }

    private void drawGeneratingProgress(DrawContext ctx, ScreenPos a, ScreenPos b, Records.StructureConnection conn) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        
        // Calculate midpoint of connection line
        int midX = (a.x + b.x) / 2;
        int midY = (a.y + b.y) / 2;
        
        // Get real road generation progress
        double progress = getRealRoadGenerationProgress(conn);
        
        // Draw progress bar background
        int barWidth = 60;
        int barHeight = 8;
        int barX = midX - barWidth / 2;
        int barY = midY - barHeight / 2;
        
        // Background
        ctx.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);
        ctx.drawBorder(barX, barY, barWidth, barHeight, 0xFFFFFFFF);
        
        // Progress bar
        int progressWidth = (int) (barWidth * progress);
        if (progressWidth > 0) {
            ctx.fill(barX + 1, barY + 1, barX + progressWidth, barY + barHeight - 1, 0xFF00FF00);
        }
        
        // Progress text
        String progressText = String.format("%.0f%%", progress * 100);
        int textWidth = font.getWidth(progressText);
        int textX = midX - textWidth / 2;
        int textY = midY + barHeight + 2;
        
        // Text background
        ctx.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, 0x80000000);
        ctx.drawText(font, progressText, textX, textY, 0xFFFFFFFF, true);
    }
    
    /**
     * Get road generation progress
     * Now uses real progress information
     */
    private double getRealRoadGenerationProgress(Records.StructureConnection conn) {
        // Check if there's already corresponding road data (indicates generation complete)
        if (roads != null && !roads.isEmpty()) {
            for (Records.RoadData roadData : roads) {
                if (isRoadConnectingStructures(roadData, conn.from(), conn.to())) {
                    // If corresponding road found, generation is complete
                    return 1.0;
                }
            }
        }
        
        // Use real progress information from connection
        return conn.progress();
    }
    
    /**
     * Check if road connects the specified two structures
     */
    private boolean isRoadConnectingStructures(Records.RoadData roadData, BlockPos from, BlockPos to) {
        List<Records.RoadSegmentPlacement> segments = roadData.roadSegmentList();
        if (segments == null || segments.isEmpty()) {
            return false;
        }
        
        BlockPos firstPos = segments.get(0).middlePos();
        BlockPos lastPos = segments.get(segments.size() - 1).middlePos();
        
        // Check if road start and end points are close to target structures
        double threshold = 32.0; // Consider connected within 32 blocks
        
        boolean connectsFrom = firstPos.getSquaredDistance(from) <= threshold * threshold || 
                              lastPos.getSquaredDistance(from) <= threshold * threshold;
        boolean connectsTo = firstPos.getSquaredDistance(to) <= threshold * threshold || 
                            lastPos.getSquaredDistance(to) <= threshold * threshold;
        
        return connectsFrom && connectsTo;
    }

    private void drawPlayerMarker(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || structures.isEmpty()) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        ScreenPos p = worldToScreen(px, pz);

        // Red player marker
        final int r = RADIUS + 2;
        final int fill = 0xFFE74C3C;
        final int outline = 0xFF000000;

        fillCircle(ctx, p.x, p.y, r, fill);
        drawCircleOutline(ctx, p.x, p.y, r, outline);

        // Direction arrow
        float yaw = mc.player.getYaw();
        double angle = Math.toRadians(yaw) + Math.PI / 2.0;
        int tx = p.x + (int) Math.round(Math.cos(angle) * (r + 4));
        int ty = p.y + (int) Math.round(Math.sin(angle) * (r + 4));
        drawLine(ctx, p.x, p.y, tx, ty, 0xFFFFFFFF);
    }

    private static double dist2(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private void drawSmallLabel(DrawContext ctx, String s, int x, int y) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(font, Text.literal(s), x, y, 0xFFFFFFFF, true);
    }

    // Draw styled panel
    private void drawPanel(DrawContext ctx, int x1, int y1, int x2, int y2, int bgColor, int borderColor) {
        // Background
        ctx.fill(x1, y1, x2, y2, bgColor);
        // Border
        ctx.drawBorder(x1, y1, x2 - x1, y2 - y1, borderColor);
        // Inner highlight
        ctx.drawHorizontalLine(x1 + 1, x2 - 2, y1 + 1, 0x40FFFFFF);
        ctx.drawVerticalLine(x1 + 1, y1 + 1, y2 - 2, 0x40FFFFFF);
    }

    // Draw styled tooltip
    private void drawTooltip(DrawContext ctx, BlockPos pos, int mouseX, int mouseY) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        
        String[] lines = {
            "Position: " + pos.getX() + ", " + pos.getZ(),
            "Height: Y " + pos.getY(),
            "Click to teleport"
        };
        
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.getWidth(line));
        }
        
        int tooltipWidth = maxWidth + 12;
        int tooltipHeight = lines.length * 11 + 6;
        
        // Adjust position to avoid going off screen
        int tx = mouseX + 10;
        int ty = mouseY + 10;
        if (tx + tooltipWidth > width - 5) tx = mouseX - tooltipWidth - 10;
        if (ty + tooltipHeight > height - 5) ty = mouseY - tooltipHeight - 10;
        
        // Draw tooltip background
        drawPanel(ctx, tx, ty, tx + tooltipWidth, ty + tooltipHeight, 0xF0000000, 0xFF4A90E2);
        
        // Draw text
        int textY = ty + 3;
        for (String line : lines) {
            ctx.drawText(font, line, tx + 6, textY, 0xFFFFFFFF, false);
            textY += 11;
        }
    }

    // ========== Drawing Primitives ==========

    private static void fillH(DrawContext ctx, int x0, int x1, int y, int argb) {
        if (x1 < x0) {
            int t = x0;
            x0 = x1;
            x1 = t;
        }
        ctx.fill(x0, y, x1, y + 1, argb);
    }

    private static void fillV(DrawContext ctx, int x, int y0, int y1, int argb) {
        if (y1 < y0) {
            int t = y0;
            y0 = y1;
            y1 = t;
        }
        ctx.fill(x, y0, x + 1, y1, argb);
    }

    private static void drawLine(DrawContext ctx, int x0, int y0, int x1, int y1, int argb) {
        // Bresenham algorithm
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0;
        int y = y0;
        
        while (true) {
            ctx.fill(x, y, x + 1, y + 1, argb);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int span = (int) Math.round(Math.sqrt(r * r - dy * dy));
            ctx.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, argb);
        }
    }

    private static void drawCircleOutline(DrawContext ctx, int cx, int cy, int r, int argb) {
        int x = r;
        int y = 0;
        int err = 0;
        
        while (x >= y) {
            plot8(ctx, cx, cy, x, y, argb);
            y++;
            if (err <= 0) {
                err += 2 * y + 1;
            }
            if (err > 0) {
                x--;
                err -= 2 * x + 1;
            }
        }
    }

    private static void plot8(DrawContext ctx, int cx, int cy, int x, int y, int argb) {
        ctx.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, argb);
        ctx.fill(cx + y, cy + x, cx + y + 1, cy + x + 1, argb);
        ctx.fill(cx - y, cy + x, cx - y + 1, cy + x + 1, argb);
        ctx.fill(cx - x, cy + y, cx - x + 1, cy + y + 1, argb);
        ctx.fill(cx - x, cy - y, cx - x + 1, cy - y + 1, argb);
        ctx.fill(cx - y, cy - x, cx - y + 1, cy - x + 1, argb);
        ctx.fill(cx + y, cy - x, cx + y + 1, cy - x + 1, argb);
        ctx.fill(cx + x, cy - y, cx + x + 1, cy - y + 1, argb);
    }

    private record ScreenPos(int x, int y) {}
    
    /**
     * Check if progress update is needed
     * Update all generating connection progress every second
     */
    private void updateProgressIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL) {
            lastProgressUpdate = currentTime;
            
            // Update progress for all generating connections
            for (Records.StructureConnection conn : connections) {
                if (conn.status() == Records.ConnectionStatus.GENERATING) {
                    // Get latest progress information from server
                    updateConnectionProgressFromServer(conn);
                }
            }
        }
    }
    
    /**
     * Update connection progress from server
     * @param conn Connection to update
     */
    private void updateConnectionProgressFromServer(Records.StructureConnection conn) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getServer() == null) return;
        
        // Get server world data attachment to access latest progress
        ServerWorld serverWorld = mc.getServer().getWorld(World.OVERWORLD);
        if (serverWorld == null) return;
        
        // Get latest connections from server
        List<Records.StructureConnection> serverConnections = serverWorld.getAttachedOrCreate(
            WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new);
        
        // Find matching connection on server
        for (Records.StructureConnection serverConn : serverConnections) {
            if (isSameConnection(serverConn, conn)) {
                // Update local connection with server progress
                updateLocalConnectionProgress(conn, serverConn);
                break;
            }
        }
    }
    
    /**
     * Check if two connections are the same
     * @param conn1 First connection
     * @param conn2 Second connection
     * @return True if same connection
     */
    private boolean isSameConnection(Records.StructureConnection conn1, Records.StructureConnection conn2) {
        return (conn1.from().equals(conn2.from()) && conn1.to().equals(conn2.to())) ||
               (conn1.from().equals(conn2.to()) && conn1.to().equals(conn2.from()));
    }
    
    /**
     * Update local connection progress with server data
     * @param localConn Local connection to update
     * @param serverConn Server connection with latest data
     */
    private void updateLocalConnectionProgress(Records.StructureConnection localConn, Records.StructureConnection serverConn) {
        // Update the connection in the local list
        for (int i = 0; i < connections.size(); i++) {
            Records.StructureConnection conn = connections.get(i);
            if (isSameConnection(conn, localConn)) {
                connections.set(i, serverConn);
                break;
            }
        }
    }
}
