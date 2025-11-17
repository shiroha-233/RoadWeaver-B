package net.shiroha233.roadweaver.mixin.fabric;

import net.shiroha233.roadweaver.client.fabric.ConfigScreenFactoryImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 用于在创建世界界面添加 RoadWeaver 配置按钮
 */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class CreateWorldScreenMixin extends GridLayoutTab {

    protected CreateWorldScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addConfigButton(CallbackInfo ci) {
        Button configButton = Button.builder(
                Component.translatable("gui.roadweaver.config_button"),
                button -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.screen instanceof CreateWorldScreen screen) {
                        mc.setScreen(ConfigScreenFactoryImpl.createConfigScreen((Screen) screen));
                    }
                })
                .width(210)
                .build();
        // 行索引说明（参考原版 GameTab 构造函数）：
        // 0: 世界名称输入
        // 1: 游戏模式
        // 2: 难度
        // 3: 允许作弊
        // 4: 实验性特性按钮（仅快照版本存在）
        // 因此：稳定版放在第 4 行（紧跟允许作弊），快照版放在第 5 行（紧跟实验性特性）。
        int row = net.minecraft.SharedConstants.getCurrentVersion().isStable() ? 4 : 5;
        this.layout.addChild(configButton, row, 0, this.layout.newCellSettings().alignHorizontallyCenter());
    }
}
