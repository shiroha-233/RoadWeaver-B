package net.countered.settlementroads.mixin;

import net.countered.settlementroads.client.gui.ClothConfigScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add RoadWeaver config button to the Create World screen
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {
    
    protected CreateWorldScreenMixin(Component title) {
        super(title);
    }
    
    @Inject(method = "init", at = @At("RETURN"))
    private void addConfigButton(CallbackInfo ci) {
        // 在左下角添加配置按钮
        int buttonWidth = 120;
        int buttonHeight = 20;
        int x = 10; // 左边距
        int y = this.height - 52; // 底部边距，避开"创建新的世界"按钮
        
        Button configButton = Button.builder(
                Component.translatable("gui.roadweaver.config_button"),
                button -> {
                    // 打开配置界面
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(ClothConfigScreen.createConfigScreen(this));
                    }
                })
                .bounds(x, y, buttonWidth, buttonHeight)
                .build();
        
        // 直接调用继承的 addRenderableWidget 方法
        this.addRenderableWidget(configButton);
    }
}
