package net.shiroha233.roadweaver.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.shiroha233.roadweaver.network.ClientNetBridge;
import java.util.concurrent.CompletableFuture;
import net.shiroha233.roadweaver.util.ComputeService;
import net.minecraft.core.BlockPos;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
public class RoadMapScreen extends Screen {
    private static final ResourceLocation MAP_TEXTURE = new ResourceLocation("roadweaver", "textures/gui/map.png");
    private static final int TEX_W = 1536;
    private static final int TEX_H = 1024;

    private static final int COLOR_TEXT = 0xFF5E3D1E; 
    private static final int COLOR_STRUCT = 0xFF5E3D1E;
    private static final int COLOR_PLANNED = 0xFF4CAF50;
    private static final int COLOR_GENERATING = 0xFF000000;
    private static final int COLOR_COMPLETED = 0xFF000000;
    private static final int COLOR_FAILED = 0xE0E05B50;
    private static final int COLOR_GRID = 0x30999999;
    private static final int GRID_TARGET_PX = 32;
    private static final Component MENU_TELEPORT = Component.translatable("gui.roadweaver.map.menu.teleport");
    private static final Component BTN_CONFIG = Component.translatable("gui.roadweaver.config_button");
    private static final Component BTN_MANUAL = Component.translatable("gui.roadweaver.map.manual_connect");
    private static final int MENU_BG = 0xF0101010;
    private static final int MENU_BORDER = 0xFFFFFFFF;
    private static final int MENU_HOVER = 0x40FFFFFF;
    private static final int MENU_TEXT = 0xFFFFFFFF;
    private static final int MENU_MIN_W = 0;
    private static final int MENU_ITEM_H = 14;
    private static final int MENU_PAD_X = 6;
    private static final int MENU_PAD_Y = 4;

    private MapSnapshot snapshot = MapSnapshot.empty();

    private int mapX, mapY, mapW, mapH;
    private static final int OUTER_PAD = 36;
    private static final int INNER_PAD = 25;
    private final MapView view = new MapView();

    private boolean dragging;
    private int dragButton;
    private double lastMouseX, lastMouseY;
    private boolean debounceZoomPending;
    private long debounceZoomDeadlineMs;

    private boolean showContextMenu;
    private int menuX, menuY;
    private BlockPos menuTarget;
    private boolean manualMode;
    private BlockPos selectedA;
    
    // 请求序列号，用于防止旧请求覆盖新数据
    private final AtomicInteger requestSeq = new AtomicInteger(0);

    public RoadMapScreen() {
        super(Component.translatable("gui.roadweaver.map.title"));
    }

    @Override
    protected void init() {
        super.init();
        MapSnapshotCache.cancelClear();
        MapSnapshot cached = MapSnapshotCache.peek();
        if (cached != null) {
            this.snapshot = cached;
        }
        computeMapRect();
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        view.resetFromSnapshot(snapshot);
        Minecraft mc = this.minecraft;
        if (mc != null && mc.player != null) {
            view.calibrateInitialToPlayer(mc, contentW, contentH, GRID_TARGET_PX);
        }
        requestCurrentView();
    }

    private void computeMapRect() {
        int availW = this.width - OUTER_PAD * 2;
        int availH = this.height - OUTER_PAD * 2;
        float ratio = (float) TEX_W / TEX_H;
        int w = availW;
        int h = Math.round(w / ratio);
        if (h > availH) {
            h = availH;
            w = Math.round(h * ratio);
        }
        mapW = w;
        mapH = h;
        mapX = (this.width - w) / 2;
        mapY = (this.height - h) / 2;
    }

    private boolean insideMap(double x, double y) {
        return x >= mapX + INNER_PAD && x <= mapX + mapW - INNER_PAD && y >= mapY + INNER_PAD && y <= mapY + mapH - INNER_PAD;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        g.blit(MAP_TEXTURE, mapX, mapY, mapW, mapH, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        int titleY = mapY - 8;
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, Math.max(6, titleY), COLOR_TEXT);

        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        view.lockAspect(contentW, contentH);

        int left = mapX + INNER_PAD;
        int top = mapY + INNER_PAD;
        int right = mapX + mapW - INNER_PAD;
        int bottom = mapY + mapH - INNER_PAD;
        g.enableScissor(left, top, right, bottom);
        MapRenderers.renderGrid(g, this.font, mapX, mapY, mapW, mapH, INNER_PAD,
                view.getMinX(), view.getMaxX(), view.getMinZ(), view.getMaxZ(), COLOR_GRID, GRID_TARGET_PX, COLOR_TEXT);

        int thickness = computeThickness();
        java.util.List<net.shiroha233.roadweaver.helpers.Records.StructureConnection> connForLines = new java.util.ArrayList<>(snapshot.connections());
        // 当存在详细道路几何时，已完成道路用多段折线表示，这里可以隐藏 COMPLETED 连接线；
        // 当没有详细道路几何时，保留 COMPLETED 连接，让其以简单直线表示已连接道路。
        boolean hasDetailedRoads = !snapshot.roadPolylines().isEmpty();
        if (hasDetailedRoads) {
            connForLines.removeIf(c -> c.status() == net.shiroha233.roadweaver.helpers.Records.ConnectionStatus.COMPLETED);
        }
        MapRenderers.renderConnections(
                g,
                connForLines,
                (x1, z1, x2, z2) -> view.segmentInViewWorld(x1, z1, x2, z2),
                v -> view.toScreenX(v, mapX, INNER_PAD, contentW),
                v -> view.toScreenY(v, mapY, INNER_PAD, contentH),
                thickness,
                COLOR_PLANNED, COLOR_GENERATING, COLOR_COMPLETED, COLOR_FAILED,
                left, top, right, bottom
        );

        int lodStep = GridRenderer.computeGridStep(mapX, mapY, mapW, mapH,
                INNER_PAD,
                view.getMinX(), view.getMaxX(), view.getMinZ(), view.getMaxZ(),
                GRID_TARGET_PX);
        MapRenderers.renderRoadPolylines(
                g,
                snapshot.roadPolylines(),
                (x1, z1, x2, z2) -> view.segmentInViewWorld(x1, z1, x2, z2),
                v -> view.toScreenX(v, mapX, INNER_PAD, contentW),
                v -> view.toScreenY(v, mapY, INNER_PAD, contentH),
                thickness,
                COLOR_COMPLETED,
                left, top, right, bottom,
                lodStep
        );

        
        MapRenderers.renderStructures(
                g,
                snapshot.structures(),
                v -> view.toScreenX(v, mapX, INNER_PAD, contentW),
                v -> view.toScreenY(v, mapY, INNER_PAD, contentH),
                (x, z) -> view.isInViewWorld(x, z),
                computePointSize(),
                COLOR_STRUCT,
                left, top, right, bottom
        );
        if (manualMode && selectedA != null && view.isInViewWorld(selectedA.getX(), selectedA.getZ())) {
            int sxA = view.toScreenX(selectedA.getX(), mapX, INNER_PAD, contentW);
            int syA = view.toScreenY(selectedA.getZ(), mapY, INNER_PAD, contentH);
            int selSize = computePointSize() * 2 + 4;
            // 选中起点的高亮显示
            RenderUtils.drawPoint(g, sxA, syA, selSize, 0xFFFF3B30, left, top, right, bottom);

            // 手动连接预览：从起点到鼠标的一条虚线（尽量吸附到最近结构点）
            if (insideMap(mouseX, mouseY)) {
                int sxB;
                int syB;
                BlockPos hover = findNearestStructure(mouseX, mouseY);
                if (hover != null && view.isInViewWorld(hover.getX(), hover.getZ())) {
                    sxB = view.toScreenX(hover.getX(), mapX, INNER_PAD, contentW);
                    syB = view.toScreenY(hover.getZ(), mapY, INNER_PAD, contentH);
                } else {
                    sxB = (int) Math.round(mouseX);
                    syB = (int) Math.round(mouseY);
                }
                int previewThickness = computeThickness();
                RenderUtils.drawThickDashedLine(
                        g,
                        sxA, syA, sxB, syB,
                        0xCCFF3B30,
                        previewThickness,
                        8, 6,
                        left, top, right, bottom
                );
            }
        }
        if (!showContextMenu) {
            MapInteraction.renderHoverHighlight(g, snapshot, view, mapX, mapY, mapW, mapH, INNER_PAD, mouseX, mouseY);
        }
        renderPlayer(g);
        g.disableScissor();
        int legendRight = mapX + mapW - INNER_PAD;
        int legendStartY = mapY + INNER_PAD + 8;
        int gap = 8;
        MapRenderers.renderLegend(
                g, this.font,
                legendRight, legendStartY, gap,
                COLOR_TEXT, COLOR_STRUCT, COLOR_PLANNED, COLOR_GENERATING, COLOR_COMPLETED, COLOR_FAILED,
                snapshot.structuresCount(), snapshot.plannedCount(), snapshot.generatingCount(), snapshot.completedCount(), snapshot.failedCount()
        );
        renderConfigButton(g, mouseX, mouseY);
        renderManualButton(g, mouseX, mouseY);
        if (!showContextMenu) {
            MapInteraction.renderHoverTooltip(g, this.font, snapshot, view, mapX, mapY, mapW, mapH, INNER_PAD, mouseX, mouseY);
        }

        if (debounceZoomPending && System.currentTimeMillis() >= debounceZoomDeadlineMs) {
            debounceZoomPending = false;
            requestCurrentView();
        }

        if (showContextMenu && menuTarget != null) {
            int[] bounds = MapContextMenu.computeMenuBounds(
                    this.font, MENU_TELEPORT,
                    menuX, menuY,
                    this.width, this.height,
                    MENU_PAD_X, MENU_PAD_Y, MENU_ITEM_H, MENU_MIN_W
            );
            int hover = MapContextMenu.getMenuHoverIndex(mouseX, mouseY, bounds, MENU_PAD_Y, MENU_ITEM_H, 1);
            MapContextMenu.renderContextMenu(
                    g, this.font, MENU_TELEPORT,
                    mouseX, mouseY,
                    bounds, hover,
                    MENU_BG, MENU_BORDER, MENU_HOVER, MENU_TEXT,
                    this.width, this.height
            );
        }

        super.render(g, mouseX, mouseY, partialTick);
    }


    private int computeThickness() {
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        double ppb = Math.min(view.pxPerBlockX(contentW), view.pxPerBlockZ(contentH));
        int t = (int)Math.round(ppb);
        if (t < 1) t = 1;
        if (t > 4) t = 4;
        return t;
    }

    private int computePointSize() { return 2 + computeThickness(); }

    


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        MapSnapshotCache.scheduleClear(1000);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = this.minecraft;
        if (mc != null) {
            for (KeyMapping mapping : mc.options.keyMappings) {
                if ("key.roadweaver.open_map".equals(mapping.getName()) && mapping.matches(keyCode, scanCode)) {
                    while (mapping.consumeClick()) {
                        // 清空残留点击，避免下一帧重新打开地图
                    }
                    this.onClose();
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 更新地图快照数据。
     * 注意：多人模式下由网络包调用（网络包有序性保证正确性）；
     * 单人模式下由异步任务调用（已在回调中做序列号检查）。
     */
    public void setSnapshot(MapSnapshot snapshot) {
        if (snapshot != null) {
            this.snapshot = snapshot;
            MapSnapshotCache.put(snapshot);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!insideMap(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, delta);
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        double factor = delta > 0 ? 0.9 : 1.1;
        double cx = view.screenToWorldX(mouseX, mapX, INNER_PAD, contentW);
        double cz = view.screenToWorldZ(mouseY, mapY, INNER_PAD, contentH);
        view.applyZoomAround(cx, cz, factor, contentW, contentH, GRID_TARGET_PX);
        debounceZoomPending = true;
        debounceZoomDeadlineMs = System.currentTimeMillis() + 500;
        showContextMenu = false;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && insideConfigButton((int)mouseX, (int)mouseY)) {
            openConfig();
            return true;
        }
        if (button == 0 && insideManualButton((int)mouseX, (int)mouseY)) {
            manualMode = !manualMode;
            if (!manualMode) selectedA = null;
            showContextMenu = false;
            return true;
        }
        if (showContextMenu) {
            int[] bounds = MapContextMenu.computeMenuBounds(
                    this.font, MENU_TELEPORT,
                    menuX, menuY,
                    this.width, this.height,
                    MENU_PAD_X, MENU_PAD_Y, MENU_ITEM_H, MENU_MIN_W
            );
            int bx = bounds[0], by = bounds[1], bw = bounds[2], bh = bounds[3];
            boolean inside = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
            if (inside && button == 0) {
                int idx = MapContextMenu.getMenuHoverIndex((int)mouseX, (int)mouseY, bounds, MENU_PAD_Y, MENU_ITEM_H, 1);
                if (idx == 0) {
                    onTeleportSelected();
                    showContextMenu = false;
                    return true;
                }
            } else {
                showContextMenu = false;
                // fallthrough to other handling if needed
            }
        }

        if (manualMode && insideMap(mouseX, mouseY) && button == 0) {
            BlockPos best = findNearestStructure(mouseX, mouseY);
            if (best != null) {
                if (selectedA == null) {
                    selectedA = best;
                } else if (selectedA.equals(best)) {
                    selectedA = null;
                } else {
                    ClientNetBridge.requestManualConnect(selectedA.getX(), selectedA.getZ(), best.getX(), best.getZ());
                    selectedA = null;
                    requestCurrentView();
                }
                showContextMenu = false;
                return true;
            }
        }

        if (insideMap(mouseX, mouseY) && button == 0) {
            dragging = true;
            dragButton = button;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            debounceZoomPending = false;
            showContextMenu = false;
            return true;
        }
        if (insideMap(mouseX, mouseY) && button == 1) {
            BlockPos best = findNearestStructure(mouseX, mouseY);
            if (best != null) {
                menuTarget = best;
                menuX = (int) mouseX;
                menuY = (int) mouseY;
                showContextMenu = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == dragButton) {
            int contentW = mapW - INNER_PAD * 2;
            int contentH = mapH - INNER_PAD * 2;
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            view.panByScreenDelta(dx, dy, contentW, contentH);
            lastMouseX = mouseX; lastMouseY = mouseY;
            view.lockAspect(contentW, contentH);
            showContextMenu = false;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == dragButton) {
            dragging = false;
            view.clampZoom(mapW - INNER_PAD * 2, mapH - INNER_PAD * 2, GRID_TARGET_PX);
            debounceZoomPending = false;
            requestCurrentView();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    

    private void renderPlayer(GuiGraphics g) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        double wx = this.minecraft.player.getX();
        double wz = this.minecraft.player.getZ();
        int sx = view.toScreenX((int)Math.round(wx), mapX, INNER_PAD, mapW - INNER_PAD * 2);
        int sy = view.toScreenY((int)Math.round(wz), mapY, INNER_PAD, mapH - INNER_PAD * 2);
        if (!insideMap(sx, sy)) return;
        float yaw = this.minecraft.player.getYRot();
        int left = mapX + INNER_PAD, right = mapX + mapW - INNER_PAD;
        int top = mapY + INNER_PAD, bottom = mapY + mapH - INNER_PAD;
        MapRenderers.drawPlayerArrow(
                g,
                sx, sy,
                yaw,
                10, 6, 4,
                0xFF000000,
                left, top, right, bottom,
                view.pxPerBlockX(mapW - INNER_PAD * 2),
                view.pxPerBlockZ(mapH - INNER_PAD * 2)
        );
    }

    private void requestCurrentView() {
        int minX = (int)Math.floor(Math.min(view.getMinX(), view.getMaxX()));
        int maxX = (int)Math.ceil(Math.max(view.getMinX(), view.getMaxX()));
        int minZ = (int)Math.floor(Math.min(view.getMinZ(), view.getMaxZ()));
        int maxZ = (int)Math.ceil(Math.max(view.getMinZ(), view.getMaxZ()));

        // 适度扩展边界，减少边缘拖拽时的频繁请求
        int pad = 32;
        minX -= pad; maxX += pad; minZ -= pad; maxZ += pad;
        final int fMinX = minX;
        final int fMaxX = maxX;
        final int fMinZ = minZ;
        final int fMaxZ = maxZ;

        Minecraft mc = this.minecraft;
        if (mc == null) return;
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level != null) {
                int cx = 0;
                int cz = 0;
                if (mc.player != null) {
                    cx = (int) Math.round(mc.player.getX());
                    cz = (int) Math.round(mc.player.getZ());
                }
                int radiusChunks;
                try {
                    net.shiroha233.roadweaver.config.ModConfig cfg = net.shiroha233.roadweaver.config.ConfigService.get();
                    radiusChunks = (cfg.dynamicPlanEnabled() ? cfg.dynamicPlanRadiusChunks() : cfg.initialPlanRadiusChunks());
                } catch (Throwable t) {
                    radiusChunks = 256;
                }
                int radiusBlocks = Math.max(1, radiusChunks) * 16;
                final int fcx = cx, fcz = cz;
                // 递增序列号，只有最新请求的结果才会被应用
                final int currentSeq = requestSeq.incrementAndGet();
                CompletableFuture
                    .supplyAsync(() -> MapDataCollector.build(level, fMinX, fMinZ, fMaxX, fMaxZ, fcx, fcz, radiusBlocks), ComputeService.executor())
                    .thenAccept(snap -> mc.execute(() -> {
                        // 只有当前序列号仍是最新时才更新（防止旧任务覆盖新数据）
                        if (requestSeq.get() == currentSeq) {
                            setSnapshot(snap);
                        }
                    }));
            }
        } else {
            // 多人模式：递增序列号（网络包本身有序，但标记最新请求）
            requestSeq.incrementAndGet();
            ClientNetBridge.requestSnapshot(minX, minZ, maxX, maxZ);
        }

    }

    private BlockPos findNearestStructure(double mouseX, double mouseY) {
        int contentW = mapW - INNER_PAD * 2;
        int contentH = mapH - INNER_PAD * 2;
        int bestDist = Integer.MAX_VALUE;
        BlockPos best = null;
        for (BlockPos p : snapshot.structures()) {
            if (!view.isInViewWorld(p.getX(), p.getZ())) continue;
            int x = view.toScreenX(p.getX(), mapX, INNER_PAD, contentW);
            int y = view.toScreenY(p.getZ(), mapY, INNER_PAD, contentH);
            int dx = (int)Math.abs(x - mouseX);
            int dy = (int)Math.abs(y - mouseY);
            int d2 = dx * dx + dy * dy;
            if (d2 < bestDist) { bestDist = d2; best = p; }
        }
        if (best != null && bestDist <= 64) return best;
        return null;
    }

    

    private void onTeleportSelected() {
        if (menuTarget == null) return;
        ClientNetBridge.requestTeleport(menuTarget.getX(), menuTarget.getY(), menuTarget.getZ());
    }

    private int[] computeConfigBtnBounds() {
        int x = mapX + INNER_PAD + 4;
        int y = mapY + INNER_PAD + 4;
        int w = this.font.width(BTN_CONFIG) + 6;
        int h = this.font.lineHeight + 4;
        return new int[]{x, y, w, h};
    }

    private boolean insideConfigButton(int mx, int my) {
        int[] b = computeConfigBtnBounds();
        int x = b[0], y = b[1], w = b[2], h = b[3];
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void renderConfigButton(GuiGraphics g, int mouseX, int mouseY) {
        int[] b = computeConfigBtnBounds();
        int x = b[0], y = b[1], h = b[3];
        int ty = y + (h - this.font.lineHeight) / 2;
        g.drawString(this.font, BTN_CONFIG, x + 3, ty, COLOR_TEXT, false);
        if (insideConfigButton(mouseX, mouseY)) {
            int textW = this.font.width(BTN_CONFIG);
            int uy = ty + this.font.lineHeight + 1;
            int underline = (COLOR_TEXT & 0x00FFFFFF) | 0x60000000;
            g.fill(x + 2, uy, x + 2 + textW + 2, uy + 1, underline);
        }
    }

    private Component manualLabel() {
        Component onoff = manualMode ? Component.translatable("gui.roadweaver.common.on") : Component.translatable("gui.roadweaver.common.off");
        return Component.empty().append(BTN_MANUAL).append(": ").append(onoff);
    }

    private int[] computeManualBtnBounds() {
        Component lbl = manualLabel();
        int w = this.font.width(lbl) + 6;
        int h = this.font.lineHeight + 4;
        int x = mapX + INNER_PAD + 4;
        int y = mapY + mapH - INNER_PAD - 4 - h;
        return new int[]{x, y, w, h};
    }

    private boolean insideManualButton(int mx, int my) {
        int[] b = computeManualBtnBounds();
        int x = b[0], y = b[1], w = b[2], h = b[3];
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void renderManualButton(GuiGraphics g, int mouseX, int mouseY) {
        int[] b = computeManualBtnBounds();
        int x = b[0], y = b[1], h = b[3];
        int ty = y + (h - this.font.lineHeight) / 2;
        Component lbl = manualLabel();
        g.drawString(this.font, lbl, x + 3, ty, COLOR_TEXT, false);
        if (insideManualButton(mouseX, mouseY)) {
            int textW = this.font.width(lbl);
            int uy = ty + this.font.lineHeight + 1;
            int underline = (COLOR_TEXT & 0x00FFFFFF) | 0x60000000;
            g.fill(x + 2, uy, x + 2 + textW + 2, uy + 1, underline);
        }
    }

    private void openConfig() {
        if (this.minecraft == null) return;
        Screen next = null;
        try {
            Class<?> c = Class.forName("net.shiroha233.roadweaver.client.fabric.ConfigScreenFactoryImpl");
            next = (Screen) c.getMethod("createConfigScreen", Screen.class).invoke(null, this);
        } catch (Throwable ignored) {}
        if (next == null) {
            try {
                Class<?> c = Class.forName("net.shiroha233.roadweaver.client.forge.ConfigScreenFactoryImpl");
                next = (Screen) c.getMethod("createConfigScreen", Screen.class).invoke(null, this);
            } catch (Throwable ignored) {}
        }
        if (next != null) this.minecraft.setScreen(next);
    }
}
