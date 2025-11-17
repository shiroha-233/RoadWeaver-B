package net.shiroha233.roadweaver.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.shiroha233.roadweaver.config.PresetService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MaterialPresetEditorScreen extends Screen {
    private static final int MAX_MATERIALS = 16;
    private static final int LEFT_COLS = 4;
    private static final int LEFT_ROWS = 4;
    // 右侧网格的最大列数和最大行数（实际可见列行数会根据窗口尺寸自适应）
    private static final int RIGHT_COLS = 25;
    private static final int RIGHT_ROWS = 20;
    private static final int SLOT_SIZE = 18;

    private final Screen parent;
    private EditBox searchBox;
    private EditBox presetNameBox;
    private final List<String> materialIds = new ArrayList<>();
    private final List<Block> allBlocks = new ArrayList<>();
    private final List<Block> filteredBlocks = new ArrayList<>();
    private int blockScrollOffset = 0;
    // 右侧可见行数和列数，随窗口尺寸动态调整
    private int visibleRightRows = RIGHT_ROWS;
    private int visibleRightCols = RIGHT_COLS;

    private final List<UiPreset> presets = new ArrayList<>();
    private final List<String> originalPresetIds = new ArrayList<>();
    private int activePresetIndex = 0;
    private final List<Button> presetButtons = new ArrayList<>();
    private boolean loadedFromConfig = false;

    private static class UiPreset {
        String id;
        String name;
        List<String> materials = new ArrayList<>();
        int weight;
    }

    public MaterialPresetEditorScreen(Screen parent) {
        super(Component.translatable("gui.roadweaver.preset_editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        if (!loadedFromConfig) {
            loadPresetsFromJson();
            loadedFromConfig = true;
        }

        if (allBlocks.isEmpty()) {
            buildCandidateBlocksFromCreativeTabs();
        }

        int centerX = this.width / 2;
        int top = 40;
        int gridTop = 70;

        // 根据窗口高度动态计算右侧方块网格的可见行数
        int actionY = this.height - 60;
        int availableHeight = Math.max(0, actionY - gridTop);
        this.visibleRightRows = Math.max(3, Math.min(RIGHT_ROWS, availableHeight / SLOT_SIZE));
        if (this.visibleRightRows <= 0) {
            this.visibleRightRows = 1;
        }

        // 根据窗口宽度动态计算右侧方块网格的可见列数
        int rightAreaX = centerX - 10;
        int marginRight = 20;
        int availableWidth = Math.max(1, this.width - marginRight - rightAreaX);
        int colsByWidth = Math.max(1, availableWidth / SLOT_SIZE);
        this.visibleRightCols = Math.min(RIGHT_COLS, colsByWidth);

        // 搜索框（居中，保持原版风格）
        this.searchBox = new EditBox(this.font, centerX - 80, top, 160, 20,
                Component.translatable("gui.roadweaver.preset_editor.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        // 左侧预设名称 + 预设列表
        int presetWidth = 120;
        int presetsLeftX = centerX - 260;

        this.presetNameBox = new EditBox(this.font, presetsLeftX, top, presetWidth, 20,
                Component.translatable("gui.roadweaver.preset_editor.name"));
        this.presetNameBox.setMaxLength(32);
        this.presetNameBox.setValue(getActivePresetName());
        this.addRenderableWidget(this.presetNameBox);

        presetButtons.clear();
        for (int i = 0; i < presets.size(); i++) {
            int idx = i;
            UiPreset p = presets.get(i);
            Button btn = Button.builder(Component.literal(p.name), b -> selectPreset(idx))
                    .bounds(presetsLeftX, gridTop + i * 22, presetWidth, 20)
                    .build();
            presetButtons.add(btn);
            this.addRenderableWidget(btn);
        }
        updatePresetButtonStates();

        // 预设管理按钮（新建 / 重命名 / 删除），放在左下角一排
        int smallWidth = 50;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.roadweaver.preset_editor.new"), b -> onNewPreset())
                .bounds(presetsLeftX, actionY, smallWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.roadweaver.preset_editor.rename"), b -> onRenamePreset())
                .bounds(presetsLeftX + smallWidth + 4, actionY, smallWidth + 10, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.roadweaver.preset_editor.delete"), b -> onDeletePreset())
                .bounds(presetsLeftX + smallWidth * 2 + 4 * 2 + 10, actionY, smallWidth + 10, 20)
                .build());

        // 保存 / 取消（保持原有位置）
        this.addRenderableWidget(Button.builder(Component.translatable("gui.roadweaver.common.save"), btn -> onSave())
                .bounds(centerX - 80, this.height - 30, 70, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.roadweaver.common.cancel"), btn -> onCancel())
                .bounds(centerX + 10, this.height - 30, 70, 20)
                .build());

        rebuildFilteredList();
    }

    private void onSearchChanged(String text) {
        blockScrollOffset = 0;
        rebuildFilteredList();
    }

    private void rebuildFilteredList() {
        filteredBlocks.clear();
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
        for (Block b : allBlocks) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (id == null) continue;
            if (q.isEmpty() || id.toString().toLowerCase().contains(q)) {
                filteredBlocks.add(b);
            }
        }
    }

    private void loadPresetsFromJson() {
        presets.clear();
        originalPresetIds.clear();

        PresetService.reload();
        List<PresetService.PresetDef> defs = PresetService.getAllPresets();
        for (PresetService.PresetDef def : defs) {
            UiPreset p = new UiPreset();
            p.id = def.id();
            p.name = def.name();
            p.materials = new ArrayList<>(def.materials());
            p.weight = def.weight();
            presets.add(p);
            originalPresetIds.add(p.id);
        }

        if (presets.isEmpty()) {
            UiPreset p = new UiPreset();
            p.id = "custom_1";
            p.name = "Custom 1";
            p.materials = new ArrayList<>();
            p.weight = 1;
            presets.add(p);
        }

        if (activePresetIndex < 0 || activePresetIndex >= presets.size()) {
            activePresetIndex = 0;
        }

        materialIds.clear();
        materialIds.addAll(presets.get(activePresetIndex).materials);
        if (materialIds.size() > MAX_MATERIALS) {
            materialIds.subList(MAX_MATERIALS, materialIds.size()).clear();
        }
    }

    private String getActivePresetName() {
        if (presets.isEmpty()) return "";
        int idx = Math.max(0, Math.min(activePresetIndex, presets.size() - 1));
        return presets.get(idx).name;
    }

    // 将当前界面中的名称与材质列表同步回活动预设，仅更新内存，不写磁盘
    private void syncActivePresetFromUi() {
        if (presets.isEmpty()) {
            return;
        }
        int idx = Math.max(0, Math.min(activePresetIndex, presets.size() - 1));
        UiPreset active = presets.get(idx);

        String nameFromBox = presetNameBox != null ? presetNameBox.getValue().trim() : active.name;
        if (nameFromBox == null || nameFromBox.isBlank()) {
            nameFromBox = active.name;
        }
        active.name = nameFromBox;

        // 缓存当前材质列表到预设（限制最大数量）
        List<String> copy = new ArrayList<>(materialIds);
        if (copy.size() > MAX_MATERIALS) {
            copy = new ArrayList<>(copy.subList(0, MAX_MATERIALS));
        }
        active.materials = copy;
    }

    private String generateNewPresetId() {
        int idx = 1;
        while (true) {
            String candidate = "custom_" + idx;
            boolean exists = false;
            for (UiPreset p : presets) {
                if (candidate.equals(p.id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return candidate;
            }
            idx++;
        }
    }

    private void selectPreset(int index) {
        if (index < 0 || index >= presets.size()) return;
        // 切换前先把当前 UI 的改动写回到活动预设（仅内存缓存）
        syncActivePresetFromUi();
        activePresetIndex = index;
        materialIds.clear();
        materialIds.addAll(presets.get(activePresetIndex).materials);
        if (materialIds.size() > MAX_MATERIALS) {
            materialIds.subList(MAX_MATERIALS, materialIds.size()).clear();
        }
        if (presetNameBox != null) {
            presetNameBox.setValue(getActivePresetName());
        }
        updatePresetButtonStates();
    }

    private void updatePresetButtonStates() {
        for (int i = 0; i < presetButtons.size(); i++) {
            Button btn = presetButtons.get(i);
            if (btn == null) continue;
            boolean isActive = i == activePresetIndex;
            btn.active = !isActive;
            if (i < presets.size()) {
                btn.setMessage(Component.literal(presets.get(i).name));
            }
        }
    }

    private void onNewPreset() {
        // 新建前先缓存当前活动预设的改动
        syncActivePresetFromUi();
        String baseName = Component.translatable("gui.roadweaver.preset_editor.default_name").getString();
        if (baseName == null || baseName.isBlank()) {
            baseName = "Preset";
        }
        String name = baseName + " " + (presets.size() + 1);
        String id = generateNewPresetId();
        UiPreset p = new UiPreset();
        p.id = id;
        p.name = name;
        p.materials = new ArrayList<>();
        p.weight = 1;
        presets.add(p);
        activePresetIndex = presets.size() - 1;
        materialIds.clear();
        if (presetNameBox != null) {
            presetNameBox.setValue(name);
        }
        // 重新构建左侧按钮布局
        this.init();
    }

    private void onRenamePreset() {
        if (presets.isEmpty()) return;
        if (presetNameBox == null) return;
        String name = presetNameBox.getValue().trim();
        if (name.isEmpty()) return;
        presets.get(activePresetIndex).name = name;
        updatePresetButtonStates();
    }

    private void onDeletePreset() {
        if (presets.isEmpty()) return;
        presets.remove(activePresetIndex);
        if (presets.isEmpty()) {
            UiPreset p = new UiPreset();
            p.id = "custom_1";
            p.name = "Custom 1";
            p.materials = new ArrayList<>();
            p.weight = 1;
            presets.add(p);
            activePresetIndex = 0;
            materialIds.clear();
        } else {
            if (activePresetIndex >= presets.size()) {
                activePresetIndex = presets.size() - 1;
            }
            materialIds.clear();
            materialIds.addAll(presets.get(activePresetIndex).materials);
            if (materialIds.size() > MAX_MATERIALS) {
                materialIds.subList(MAX_MATERIALS, materialIds.size()).clear();
            }
        }
        if (presetNameBox != null) {
            presetNameBox.setValue(getActivePresetName());
        }
        this.init();
    }

    private void buildCandidateBlocksFromCreativeTabs() {
        allBlocks.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.player.level() == null) {
            for (Block b : BuiltInRegistries.BLOCK) {
                if (b == Blocks.AIR) continue;
                allBlocks.add(b);
            }
            return;
        }

        FeatureFlagSet features = mc.player.connection.enabledFeatures();
        boolean hasPermissions = mc.player.canUseGameMasterBlocks();
        HolderLookup.Provider registries = mc.player.level().registryAccess();
        CreativeModeTabs.tryRebuildTabContents(features, hasPermissions, registries);

        Set<Block> unique = new LinkedHashSet<>();
        addBlocksFromTab(unique, "building_blocks");
        addBlocksFromTab(unique, "natural_blocks");

        if (unique.isEmpty()) {
            for (Block b : BuiltInRegistries.BLOCK) {
                if (b == Blocks.AIR) continue;
                unique.add(b);
            }
        }

        allBlocks.addAll(unique);
    }

    private void addBlocksFromTab(Set<Block> out, String tabId) {
        ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(tabId));
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
        if (tab == null) {
            return;
        }
        for (ItemStack stack : tab.getDisplayItems()) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            Block block = blockItem.getBlock();
            if (block == Blocks.AIR) {
                continue;
            }
            out.add(block);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        super.render(g, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int leftAreaX = centerX - 140;
        int rightAreaX = centerX - 10;
        int gridTop = 70;

        renderMaterialsGrid(g, leftAreaX, gridTop, mouseX, mouseY);
        renderBlocksGrid(g, rightAreaX, gridTop, mouseX, mouseY);
    }

    private void renderMaterialsGrid(GuiGraphics g, int startX, int startY, int mouseX, int mouseY) {
        int index = 0;
        for (int row = 0; row < LEFT_ROWS; row++) {
            for (int col = 0; col < LEFT_COLS; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
                if (index < materialIds.size()) {
                    Block b = blockFromId(materialIds.get(index));
                    if (b != null && b != Blocks.AIR) {
                        ItemStack stack = new ItemStack(b);
                        g.renderFakeItem(stack, x + 1, y + 1);
                    }
                }
                index++;
            }
        }
    }

    private void renderBlocksGrid(GuiGraphics g, int startX, int startY, int mouseX, int mouseY) {
        int rows = visibleRightRows;
        int cols = visibleRightCols;
        int maxOffset = Math.max(0, (filteredBlocks.size() + cols - 1) / cols - rows);
        if (blockScrollOffset > maxOffset) blockScrollOffset = maxOffset;
        if (blockScrollOffset < 0) blockScrollOffset = 0;

        int rowOffset = blockScrollOffset;
        int indexBase = rowOffset * cols;
        int index = indexBase;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x80000000);
                if (index < filteredBlocks.size()) {
                    Block b = filteredBlocks.get(index);
                    ItemStack stack = new ItemStack(b);
                    g.renderFakeItem(stack, x + 1, y + 1);
                }
                index++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int centerX = this.width / 2;
        int leftAreaX = centerX - 140;
        int rightAreaX = centerX - 10;
        int gridTop = 70;

        if (handleClickMaterials(mouseX, mouseY, leftAreaX, gridTop, button)) return true;
        if (handleClickBlocks(mouseX, mouseY, rightAreaX, gridTop, button)) return true;

        return false;
    }

    private boolean handleClickMaterials(double mouseX, double mouseY, int startX, int startY, int button) {
        int index = 0;
        for (int row = 0; row < LEFT_ROWS; row++) {
            for (int col = 0; col < LEFT_COLS; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    if (button == 0 && index < materialIds.size()) {
                        materialIds.remove(index);
                        return true;
                    }
                    return true;
                }
                index++;
            }
        }
        return false;
    }

    private boolean handleClickBlocks(double mouseX, double mouseY, int startX, int startY, int button) {
        int rowOffset = blockScrollOffset;
        int cols = visibleRightCols;
        int indexBase = rowOffset * cols;
        int index = indexBase;
        for (int row = 0; row < visibleRightRows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    if (index < filteredBlocks.size() && button == 0) {
                        if (materialIds.size() < MAX_MATERIALS) {
                            Block b = filteredBlocks.get(index);
                            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
                            if (id != null) {
                                materialIds.add(id.toString());
                            }
                        }
                        return true;
                    }
                    return true;
                }
                index++;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int centerX = this.width / 2;
        int rightAreaX = centerX - 10;
        int gridTop = 70;
        int widthPx = visibleRightCols * SLOT_SIZE;
        int heightPx = visibleRightRows * SLOT_SIZE;
        int x0 = rightAreaX;
        int y0 = gridTop;
        if (mouseX >= x0 && mouseX < x0 + widthPx && mouseY >= y0 && mouseY < y0 + heightPx) {
            if (delta > 0) blockScrollOffset--;
            if (delta < 0) blockScrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private Block blockFromId(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            return BuiltInRegistries.BLOCK.get(rl);
        } catch (Exception e) {
            return Blocks.AIR;
        }
    }

    private void onSave() {
        // 将当前编辑的列表写回活动预设（仅内存）
        if (!presets.isEmpty()) {
            int idx = Math.max(0, Math.min(activePresetIndex, presets.size() - 1));
            UiPreset active = presets.get(idx);
            String name = presetNameBox != null ? presetNameBox.getValue().trim() : active.name;
            if (name == null || name.isBlank()) {
                name = active.name;
            }
            active.name = name;
            active.materials = new ArrayList<>(materialIds);
        }

        // 计算需要删除的旧预设（原来存在，现在已经不在 UI 列表中）
        java.util.Set<String> currentIds = new java.util.LinkedHashSet<>();
        for (UiPreset p : presets) {
            if (p.id != null && !p.id.isBlank()) {
                currentIds.add(p.id);
            }
        }
        for (String id : originalPresetIds) {
            if (!currentIds.contains(id)) {
                PresetService.deletePresetFile(id);
            }
        }

        // 保存或更新所有当前预设到 JSON 文件
        for (UiPreset p : presets) {
            if (p.id == null || p.id.isBlank()) continue;
            PresetService.saveOrUpdatePresetFile(p.id, p.name, p.materials, p.weight <= 0 ? 1 : p.weight);
        }
        PresetService.reload();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void onCancel() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
