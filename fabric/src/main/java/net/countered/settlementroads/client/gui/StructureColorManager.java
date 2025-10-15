package net.countered.settlementroads.client.gui;

import java.util.*;

/**
 * 结构颜色管理器 - 为每种结构类型分配唯一颜色
 */
public class StructureColorManager {
    
    private final Map<String, Integer> structureColors = new HashMap<>();
    private final List<Integer> colorPalette = new ArrayList<>();
    private int nextColorIndex = 0;
    
    public StructureColorManager() {
        initializeColorPalette();
    }
    
    /**
     * 初始化颜色调色板 - 使用高对比度、易区分的颜色
     */
    private void initializeColorPalette() {
        // 明亮、高饱和度的颜色，确保在地图上清晰可见
        colorPalette.add(0xFF27AE60); // 绿色（原结构颜色）
        colorPalette.add(0xFF3498DB); // 蓝色
        colorPalette.add(0xFFE74C3C); // 红色
        colorPalette.add(0xFFF39C12); // 橙色
        colorPalette.add(0xFF9B59B6); // 紫色
        colorPalette.add(0xFF1ABC9C); // 青色
        colorPalette.add(0xFFE91E63); // 粉色
        colorPalette.add(0xFFFFEB3B); // 黄色
        colorPalette.add(0xFF00BCD4); // 浅蓝
        colorPalette.add(0xFFFF5722); // 深橙
        colorPalette.add(0xFF8BC34A); // 浅绿
        colorPalette.add(0xFFFF9800); // 琥珀色
        colorPalette.add(0xFF673AB7); // 深紫
        colorPalette.add(0xFF009688); // 蓝绿
        colorPalette.add(0xFFCDDC39); // 柠檬绿
        colorPalette.add(0xFFFF4081); // 粉红
        colorPalette.add(0xFF00E676); // 荧光绿
        colorPalette.add(0xFF2196F3); // 天蓝
        colorPalette.add(0xFFFF6F00); // 橘色
        colorPalette.add(0xFF7C4DFF); // 靛蓝
        colorPalette.add(0xFF00BFA5); // 水绿
        colorPalette.add(0xFFFFC107); // 金色
        colorPalette.add(0xFFD500F9); // 洋红
        colorPalette.add(0xFF64DD17); // 草绿
        colorPalette.add(0xFF00B8D4); // 深青
        colorPalette.add(0xFFFF3D00); // 朱红
        colorPalette.add(0xFF651FFF); // 紫罗兰
        colorPalette.add(0xFF1DE9B6); // 薄荷绿
        colorPalette.add(0xFFFFD600); // 亮黄
        colorPalette.add(0xFFFF1744); // 玫瑰红
    }
    
    /**
     * 获取结构类型的颜色
     */
    public int getColorForStructure(String structureId) {
        if (structureId == null || structureId.equals("unknown")) {
            return 0xFF808080; // 灰色表示未知
        }
        
        return structureColors.computeIfAbsent(structureId, id -> {
            int color = colorPalette.get(nextColorIndex % colorPalette.size());
            nextColorIndex++;
            return color;
        });
    }
    
    /**
     * 获取所有已分配颜色的结构类型
     */
    public Map<String, Integer> getAllStructureColors() {
        return new HashMap<>(structureColors);
    }
    
    /**
     * 获取结构类型的简短显示名称
     */
    public String getDisplayName(String structureId) {
        if (structureId == null || structureId.equals("unknown")) {
            return "未知结构";
        }
        
        // 移除命名空间前缀（minecraft:, modid:）
        String name = structureId;
        int colonIndex = name.indexOf(':');
        if (colonIndex >= 0 && colonIndex < name.length() - 1) {
            name = name.substring(colonIndex + 1);
        }
        
        // 将下划线和斜杠替换为空格，并首字母大写
        name = name.replace('_', ' ').replace('/', ' ');
        
        // 限制长度
        if (name.length() > 25) {
            name = name.substring(0, 22) + "...";
        }
        
        return name;
    }
    
    /**
     * 生成基于字符串哈希的颜色（备用方案）
     */
    private int generateColorFromHash(String str) {
        int hash = str.hashCode();
        
        // 确保颜色明亮且饱和
        int r = 128 + ((hash & 0xFF0000) >> 16) / 2;
        int g = 128 + ((hash & 0x00FF00) >> 8) / 2;
        int b = 128 + (hash & 0x0000FF) / 2;
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    /**
     * 清除所有颜色分配
     */
    public void clear() {
        structureColors.clear();
        nextColorIndex = 0;
    }
    
    /**
     * 获取已分配的结构类型数量
     */
    public int getStructureTypeCount() {
        return structureColors.size();
    }
}
