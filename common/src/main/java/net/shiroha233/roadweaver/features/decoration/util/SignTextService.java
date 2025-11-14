package net.shiroha233.roadweaver.features.decoration.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public final class SignTextService {
    private SignTextService() {}

    public static void writeDistanceSign(WorldGenLevel level, BlockPos pos, String text) {
        net.minecraft.world.level.Level l = level.getLevel();
        if (!(l instanceof net.minecraft.server.level.ServerLevel sLevel)) return;
        sLevel.getServer().execute(() -> {
            BlockEntity be = sLevel.getBlockEntity(pos);
            if (be instanceof HangingSignBlockEntity sign) {
                SignText front = sign.getText(true);
                front = front.setMessage(0, Component.translatable("gui.roadweaver.sign.next_location"));
                front = front.setMessage(1, Component.literal(text + " m"));
                front = front.setMessage(2, Component.literal(""));
                front = front.setMessage(3, Component.literal(""));
                sign.setText(front, true);

                SignText back = sign.getText(false);
                back = back.setMessage(0, Component.literal("----------"));
                back = back.setMessage(1, Component.translatable("gui.roadweaver.sign.welcome"));
                back = back.setMessage(2, Component.translatable("gui.roadweaver.sign.traveller"));
                back = back.setMessage(3, Component.literal("----------"));
                sign.setText(back, false);

                sign.setChanged();
            }
        });
    }
}
