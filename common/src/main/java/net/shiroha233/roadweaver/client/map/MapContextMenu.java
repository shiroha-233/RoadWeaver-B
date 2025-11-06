package net.shiroha233.roadweaver.client.map;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

final class MapContextMenu {
    private MapContextMenu() {}

    static int[] computeMenuBounds(Font font,
                                   Component label,
                                   int menuX, int menuY,
                                   int screenW, int screenH,
                                   int padX, int padY, int itemH, int minW) {
        int textW = font.width(label);
        int w = Math.max(minW, textW + padX * 2);
        int h = padY * 2 + itemH;
        int x = menuX + 12;
        int y = menuY - 12;
        if (x + w > screenW) x = screenW - w - 4;
        if (y + h > screenH) y = screenH - h - 4;
        if (x < 4) x = 4;
        if (y < 4) y = 4;
        return new int[]{x, y, w, h};
    }

    static int getMenuHoverIndex(int mouseX, int mouseY,
                                 int[] bounds,
                                 int padY, int itemH,
                                 int count) {
        int x = bounds[0], y = bounds[1], w = bounds[2], h = bounds[3];
        if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) return -1;
        int innerTop = y + padY;
        if (mouseY < innerTop) return -1;
        int rel = mouseY - innerTop;
        int idx = rel / itemH;
        if (idx < 0 || idx >= count) return -1;
        return idx;
    }

    static void renderContextMenu(GuiGraphics g,
                                  Font font,
                                  Component label,
                                  int mouseX, int mouseY,
                                  int[] bounds,
                                  int hover,
                                  int MENU_BG, int MENU_BORDER, int MENU_HOVER, int MENU_TEXT,
                                  int screenW, int screenH) {
        int x = bounds[0], y = bounds[1], w = bounds[2], h = bounds[3];
        int shadow = 0x80101010;
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, shadow);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, MENU_BORDER);
        g.fill(x, y, x + w, y + h, MENU_BG);
        int itemTop = y + 4;
        if (hover == 0) g.fill(x + 1, itemTop, x + w - 1, itemTop + 14, MENU_HOVER);
        g.fill(x + 1, itemTop + 14, x + w - 1, itemTop + 15, MENU_BORDER & 0x40FFFFFF);
        int ty = itemTop + (14 - font.lineHeight) / 2;
        g.drawString(font, label, x + 6, ty, MENU_TEXT, false);
        int baseY = itemTop + 14 / 2;
        int tipX = x - 6;
        int tipY = baseY;
        int bx1 = x - 1, by1 = baseY - 4;
        int bx2 = x - 1, by2 = baseY + 4;
        RenderUtils.fillTriangle(g, tipX, tipY, bx1, by1, bx2, by2, MENU_BORDER, 0, 0, screenW, screenH);
        RenderUtils.fillTriangle(g, tipX + 1, tipY, bx1, by1 + 1, bx2, by2 - 1, MENU_BG, 0, 0, screenW, screenH);
    }
}
