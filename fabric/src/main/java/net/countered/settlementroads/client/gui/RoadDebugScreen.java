package net.countered.settlementroads.client.gui;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.helpers.StructureConnector;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/**
 * 道路网络调试屏幕（Fabric）
 * - 显示结构/连接/道路
 * - 支持拖拽/缩放/点击传送
 * - 手动连接模式：选择两处结构创建 PLANNED 连接，写入世界数据并入队生成
 */
public class RoadDebugScreen extends Screen {

    private static final int PADDING = 20;

    private final List<Records.StructureInfo> structureInfos;
    private final List<Records.StructureConnection> connections;
    private final List<Records.RoadData> roads;
    private final StructureColorManager colorManager;

    private final Map<String, Integer> statusColors = Map.of(
            "structure", 0xFF27AE60,
            "planned", 0xFFF2C94C,
            "generating", 0xFFE67E22,
            "completed", 0xFF27AE60,
            "failed", 0xFFE74C3C,
            "road", 0xFF3498DB
    );

    private boolean dragging = false;
    private boolean firstLayout = true;
    private boolean layoutDirty = true;
    private double zoom = 3.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double baseScale = 1.0;
    private int minX, maxX, minZ, maxZ;

    private int lastWidth = 0;
    private int lastHeight = 0;
    private double lastZoom = 1.0;
    private double lastOffsetX = 0;
    private double lastOffsetY = 0;

    private BlockPos hoveredStructure = null;
    private String hoveredStructureId = null;

    // 手动连接模式
    private boolean manualMode = false;
    private BlockPos manualFirst = null;
    private String toastMessage = null;
    private long toastExpireMs = 0;
    
    // 拖动检测
    private double mouseDownX = 0;
    private double mouseDownY = 0;
    private boolean hasDragged = false;
    
    // 传送确认
    private BlockPos pendingTeleport = null;
    private long teleportConfirmExpireMs = 0;

    // 渲染组件
    private final MapRenderer mapRenderer;
    private final GridRenderer gridRenderer;
    private final UIRenderer uiRenderer;
    private final ScreenBounds bounds;

    // 按钮
    private Button manualButton;
    private Button configButton;
    private Button refreshButton;

    public RoadDebugScreen(List<Records.StructureInfo> structureInfos,
                           List<Records.StructureConnection> connections,
                           List<Records.RoadData> roads) {
        super(Component.translatable("gui.roadweaver.debug_map.title"));
        this.structureInfos = structureInfos != null ? new ArrayList<>(structureInfos) : new ArrayList<>();
        this.connections = connections != null ? new ArrayList<>(connections) : new ArrayList<>();
        this.roads = roads != null ? new ArrayList<>(roads) : new ArrayList<>();
        this.colorManager = new StructureColorManager();

        if (!this.structureInfos.isEmpty()) {
            minX = this.structureInfos.stream().mapToInt(info -> info.pos().getX()).min().orElse(0);
            maxX = this.structureInfos.stream().mapToInt(info -> info.pos().getX()).max().orElse(0);
            minZ = this.structureInfos.stream().mapToInt(info -> info.pos().getZ()).min().orElse(0);
            maxZ = this.structureInfos.stream().mapToInt(info -> info.pos().getZ()).max().orElse(0);
        }

        this.bounds = new ScreenBounds();
        this.mapRenderer = new MapRenderer(statusColors, bounds, colorManager);
        this.gridRenderer = new GridRenderer();
        this.uiRenderer = new UIRenderer(statusColors, colorManager);
    }

    @Override
    protected void init() {
        super.init();
        
        // 右上角：配置按钮
        int configButtonW = 50;
        int configButtonH = 16;
        int configButtonX = this.width - configButtonW - 8;
        int configButtonY = 8;
        this.configButton = Button.builder(
                Component.translatable("gui.roadweaver.config"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(ClothConfigScreen.createConfigScreen(this));
                    }
                })
                .bounds(configButtonX, configButtonY, configButtonW, configButtonH)
                .build();
        this.addRenderableWidget(this.configButton);
        
        // 右上角：刷新按钮（配置按钮左侧）
        int refreshButtonW = 50;
        int refreshButtonH = 16;
        int refreshButtonX = configButtonX - refreshButtonW - 4;
        int refreshButtonY = 8;
        this.refreshButton = Button.builder(
                Component.translatable("gui.roadweaver.debug_map.refresh"),
                button -> refreshData())
                .bounds(refreshButtonX, refreshButtonY, refreshButtonW, refreshButtonH)
                .build();
        this.addRenderableWidget(this.refreshButton);
        
        // 左下角：手动连接模式开关
        int buttonW = 110;
        int buttonH = 16;
        int buttonX = 8;
        int buttonY = this.height - buttonH - 8;
        this.manualButton = Button.builder(getManualModeLabel(), b -> toggleManualMode())
                .bounds(buttonX, buttonY, buttonW, buttonH)
                .build();
        this.addRenderableWidget(this.manualButton);
    }

    private Component getManualModeLabel() {
        Component state = Component.translatable(manualMode ? "gui.roadweaver.common.on" : "gui.roadweaver.common.off");
        return Component.translatable("gui.roadweaver.debug_map.manual_mode", state);
    }

    private void toggleManualMode() {
        manualMode = !manualMode;
        manualFirst = null;
        if (manualButton != null) manualButton.setMessage(getManualModeLabel());
        String msg = Component.translatable(manualMode ? "toast.roadweaver.manual_mode_on" : "toast.roadweaver.manual_mode_off").getString();
        toast(msg, 2000);
    }
    
    private void refreshData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSingleplayerServer() == null) return;
        
        ServerLevel world = mc.getSingleplayerServer().overworld();
        if (world == null) return;
        
        try {
            // 重新加载数据
            Records.StructureLocationData data = WorldDataProvider.getInstance().getStructureLocations(world);
            List<Records.StructureConnection> newConnections = WorldDataProvider.getInstance().getStructureConnections(world);
            List<Records.RoadData> newRoads = WorldDataProvider.getInstance().getRoadDataList(world);
            
            // 清理失败的连接
            int failedCount = 0;
            if (newConnections != null) {
                List<Records.StructureConnection> filteredConnections = new ArrayList<>();
                for (Records.StructureConnection conn : newConnections) {
                    if (conn.status() == Records.ConnectionStatus.FAILED) {
                        failedCount++;
                    } else {
                        filteredConnections.add(conn);
                    }
                }
                
                // 如果有失败的连接被移除，更新到存档
                if (failedCount > 0) {
                    WorldDataProvider.getInstance().setStructureConnections(world, filteredConnections);
                    newConnections = filteredConnections;
                }
            }
            
            // 更新列表
            this.structureInfos.clear();
            if (data != null && data.structureInfos() != null) {
                this.structureInfos.addAll(data.structureInfos());
            }
            
            this.connections.clear();
            if (newConnections != null) {
                this.connections.addAll(newConnections);
            }
            
            this.roads.clear();
            if (newRoads != null) {
                this.roads.addAll(newRoads);
            }
            
            // 重新计算边界
            if (!this.structureInfos.isEmpty()) {
                minX = this.structureInfos.stream().mapToInt(info -> info.pos().getX()).min().orElse(0);
                maxX = this.structureInfos.stream().mapToInt(info -> info.pos().getX()).max().orElse(0);
                minZ = this.structureInfos.stream().mapToInt(info -> info.pos().getZ()).min().orElse(0);
                maxZ = this.structureInfos.stream().mapToInt(info -> info.pos().getZ()).max().orElse(0);
            }
            
            // 标记需要重新布局
            layoutDirty = true;
            
            // 显示提示
            String message = Component.translatable("gui.roadweaver.debug_map.refreshed").getString();
            if (failedCount > 0) {
                message += " (" + Component.translatable("gui.roadweaver.debug_map.removed_failed", failedCount).getString() + ")";
            }
            toast(message, 2000);
        } catch (Exception e) {
            toast("Refresh failed: " + e.getMessage(), 2000);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // 不画默认背景
    }

    private void computeLayout() {
        if (structureInfos.isEmpty()) {
            baseScale = 1.0;
            return;
        }
        int w = width - PADDING * 2;
        int h = height - PADDING * 2;
        if (w <= 0 || h <= 0) return;

        int worldW = Math.max(1, maxX - minX);
        int worldH = Math.max(1, maxZ - minZ);
        double scaleX = (double) w / worldW;
        double scaleY = (double) h / worldH;
        baseScale = Math.min(scaleX, scaleY) * 0.8;

        if (firstLayout) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                double playerX = mc.player.getX();
                double playerZ = mc.player.getZ();
                double playerScreenX = (playerX - minX) * baseScale * zoom;
                double playerScreenZ = (playerZ - minZ) * baseScale * zoom;
                offsetX = w / 2.0 - playerScreenX;
                offsetY = h / 2.0 - playerScreenZ;
            } else {
                offsetX = (w - worldW * baseScale * zoom) / 2;
                offsetY = (h - worldH * baseScale * zoom) / 2;
            }
            firstLayout = false;
        }
        layoutDirty = false;
    }

    private void updateUIBounds() {
        bounds.update(PADDING, width - PADDING, PADDING, height - PADDING);
    }

    private ScreenPos worldToScreen(double worldX, double worldZ) {
        int x = PADDING + (int) ((worldX - minX) * baseScale * zoom + offsetX);
        int y = PADDING + (int) ((worldZ - minZ) * baseScale * zoom + offsetY);
        return new ScreenPos(x, y);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (layoutDirty || lastWidth != width || lastHeight != height ||
                lastZoom != zoom || lastOffsetX != offsetX || lastOffsetY != offsetY) {
            computeLayout();
            updateUIBounds();
            lastWidth = width;
            lastHeight = height;
            lastZoom = zoom;
            lastOffsetX = offsetX;
            lastOffsetY = offsetY;
            layoutDirty = false;
        }

        // 背景
        ctx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);

        MapRenderer.LODLevel lod = mapRenderer.getLODLevel(zoom);
        MapRenderer.WorldToScreenConverter converter = this::worldToScreen;

        if (lod != MapRenderer.LODLevel.MINIMAL) {
            gridRenderer.drawGrid(ctx, lod, width, height, PADDING,
                    baseScale, zoom, offsetX, offsetY, minX, minZ, bounds);
        }

        mapRenderer.drawRoadPaths(ctx, roads, lod, baseScale, zoom, converter);
        mapRenderer.drawConnections(ctx, connections, roads, lod, converter);
        mapRenderer.drawStructures(ctx, structureInfos, hoveredStructure, manualFirst, lod, converter);
        mapRenderer.drawPlayerMarker(ctx, lod, zoom, converter);

        // 过滤可见结构
        List<Records.StructureInfo> visibleStructures = getVisibleStructures(converter);

        // UI 面板
        uiRenderer.drawTitle(ctx, width);
        uiRenderer.drawStatsPanel(ctx, width, structureInfos, connections, roads, zoom, baseScale);
        uiRenderer.drawLegendPanel(ctx, height, visibleStructures);

        // 渲染默认控件（按钮）
        ctx.pose().pushPose();
        ctx.pose().translate(0, 0, 100);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.pose().popPose();

        if (hoveredStructure != null && hoveredStructureId != null) {
            uiRenderer.drawTooltip(ctx, hoveredStructure, hoveredStructureId, mouseX, mouseY, width);
        }

        updateHoveredStructure(mouseX, mouseY);

        // Toast
        if (toastMessage != null && System.currentTimeMillis() < toastExpireMs) {
            drawToast(ctx, toastMessage);
        }
    }

    private void drawToast(GuiGraphics ctx, String message) {
        var font = Minecraft.getInstance().font;
        int tw = font.width(message);
        int x = 10;
        int y = 10;
        RenderUtils.drawPanel(ctx, x - 4, y - 4, x + tw + 6, y + 12, 0xC0000000, 0xFF666666);
        ctx.drawString(font, message, x, y, 0xFFFFFFFF, false);
    }

    private void toast(String msg, long durationMs) {
        this.toastMessage = msg;
        this.toastExpireMs = System.currentTimeMillis() + durationMs;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 检查是否按下了调试地图按键（默认 H 键）
        if (this.minecraft != null && this.minecraft.options.keyMappings != null) {
            for (var keyMapping : this.minecraft.options.keyMappings) {
                if (keyMapping.getName().equals("key.roadweaver.debug_map") && 
                    keyMapping.matches(keyCode, scanCode)) {
                    this.onClose();
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (dragging && button == 0) {
            // 检测是否真的在拖动（移动超过3像素）
            double totalDrag = Math.abs(mouseX - mouseDownX) + Math.abs(mouseY - mouseDownY);
            if (totalDrag > 3) {
                hasDragged = true;
            }
            offsetX += dragX;
            offsetY += dragY;
            layoutDirty = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double old = zoom;
        zoom = delta > 0 ? zoom * 1.1 : zoom / 1.1;
        zoom = Math.max(0.1, Math.min(10.0, zoom));
        offsetX = (offsetX - mouseX + PADDING) * (zoom / old) + mouseX - PADDING;
        offsetY = (offsetY - mouseY + PADDING) * (zoom / old) + mouseY - PADDING;
        layoutDirty = true;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        // 记录鼠标按下位置
        mouseDownX = mouseX;
        mouseDownY = mouseY;
        hasDragged = false;
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) return true;
        if (button == 0 && dragging) {
            dragging = false;
            
            // 只有在没有拖动时才处理点击
            if (!hasDragged) {
                Records.StructureInfo clickedInfo = findClickedStructure(mouseX, mouseY);
                if (clickedInfo != null) {
                    BlockPos clicked = clickedInfo.pos();
                    if (manualMode) {
                        handleManualClick(clicked);
                    } else {
                        // 传送确认逻辑
                        if (pendingTeleport != null && pendingTeleport.equals(clicked) && 
                            System.currentTimeMillis() < teleportConfirmExpireMs) {
                            // 确认传送
                            teleportTo(clicked);
                            pendingTeleport = null;
                        } else {
                            // 第一次点击：请求确认
                            pendingTeleport = clicked;
                            teleportConfirmExpireMs = System.currentTimeMillis() + 3000;
                            String confirm = Component.translatable("toast.roadweaver.teleport_confirm", 
                                clicked.getX(), clicked.getZ()).getString();
                            toast(confirm, 3000);
                        }
                    }
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    private void updateHoveredStructure(int mouseX, int mouseY) {
        Records.StructureInfo info = findClickedStructure(mouseX, mouseY);
        if (info != null) {
            hoveredStructure = info.pos();
            hoveredStructureId = info.structureId();
        } else {
            hoveredStructure = null;
            hoveredStructureId = null;
        }
    }

    private Records.StructureInfo findClickedStructure(double mouseX, double mouseY) {
        for (Records.StructureInfo info : structureInfos) {
            BlockPos structure = info.pos();
            ScreenPos pos = worldToScreen(structure.getX(), structure.getZ());
            double dx = pos.x - mouseX;
            double dy = pos.y - mouseY;
            if (Math.sqrt(dx * dx + dy * dy) <= 7) {
                return info;
            }
        }
        return null;
    }

    private void handleManualClick(BlockPos clicked) {
        if (manualFirst == null) {
            manualFirst = clicked;
            String pick = Component.translatable("toast.roadweaver.manual_pick_start", clicked.getX(), clicked.getZ()).getString();
            toast(pick, 2000);
        } else if (manualFirst.equals(clicked)) {
            // 再次点击同一个结构：取消选中
            manualFirst = null;
            String cancel = Component.translatable("toast.roadweaver.manual_cancelled").getString();
            toast(cancel, 2000);
        } else {
            BlockPos first = manualFirst;
            manualFirst = null;
            createManualConnection(first, clicked);
        }
    }

    private void createManualConnection(BlockPos from, BlockPos to) {
        Minecraft client = Minecraft.getInstance();
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            toast(Component.translatable("toast.roadweaver.manual_multiplayer_not_supported").getString(), 2500);
            return;
        }

        Records.StructureConnection newConn = new Records.StructureConnection(from, to, Records.ConnectionStatus.PLANNED, true);

        // 服务器线程执行：写入世界数据并入队
        server.execute(() -> {
            ServerLevel world = server.overworld();
            WorldDataProvider provider = WorldDataProvider.getInstance();
            List<Records.StructureConnection> list = new ArrayList<>(
                    Optional.ofNullable(provider.getStructureConnections(world)).orElseGet(ArrayList::new)
            );
            
            // 移除已存在的失败连接（如果有）
            list.removeIf(conn -> 
                ((conn.from().equals(from) && conn.to().equals(to)) || 
                 (conn.from().equals(to) && conn.to().equals(from))) &&
                conn.status() == Records.ConnectionStatus.FAILED
            );
            
            // 添加新的计划连接
            list.add(newConn);
            provider.setStructureConnections(world, list);
            StructureConnector.getQueueForWorld(world).add(newConn);
        });

        // 立即在客户端侧可视化：移除失败连接，添加新连接
        this.connections.removeIf(conn -> 
            ((conn.from().equals(from) && conn.to().equals(to)) || 
             (conn.from().equals(to) && conn.to().equals(from))) &&
            conn.status() == Records.ConnectionStatus.FAILED
        );
        this.connections.add(newConn);
        toast(Component.translatable("toast.roadweaver.manual_created").getString(), 2000);
    }

    private void teleportTo(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        var server = mc.getSingleplayerServer();
        String command = "/tp " + mc.player.getName().getString() + " " + pos.getX() + " ~ " + pos.getZ();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // --- 辅助类型 ---
    private List<Records.StructureInfo> getVisibleStructures(MapRenderer.WorldToScreenConverter converter) {
        List<Records.StructureInfo> visible = new ArrayList<>();
        for (Records.StructureInfo info : structureInfos) {
            BlockPos pos = info.pos();
            ScreenPos screenPos = converter.worldToScreen(pos.getX(), pos.getZ());
            
            // 检查是否在屏幕范围内（带一些边距）
            if (screenPos.x >= -50 && screenPos.x <= width + 50 &&
                screenPos.y >= -50 && screenPos.y <= height + 50) {
                visible.add(info);
            }
        }
        return visible;
    }

    public record ScreenPos(int x, int y) {}

    public static class ScreenBounds {
        private int left, right, top, bottom;
        public void update(int left, int right, int top, int bottom) {
            this.left = left; this.right = right; this.top = top; this.bottom = bottom;
        }
        public boolean isInBounds(int x, int y, int margin) {
            return x >= left - margin && x <= right + margin && y >= top - margin && y <= bottom + margin;
        }
        public boolean isLineInBounds(int x1, int y1, int x2, int y2) {
            if ((x1 < left && x2 < left) || (x1 > right && x2 > right) || (y1 < top && y2 < top) || (y1 > bottom && y2 > bottom)) return false;
            return true;
        }
        public int left() { return left; }
        public int right() { return right; }
        public int top() { return top; }
        public int bottom() { return bottom; }
    }
}
