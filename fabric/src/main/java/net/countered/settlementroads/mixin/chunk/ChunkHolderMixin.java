package net.countered.settlementroads.mixin.chunk;

import net.countered.settlementroads.chunk.ChunkRoadStateManager;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截区块发送给玩家的流程
 * 如果区块还在等待道路生成，则延迟发送
 */
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkHolderMixin.class);
    
    @Shadow
    @Final
    private ChunkPos pos;
    
    /**
     * 拦截区块更新广播到玩家
     * 这是区块数据发送给玩家的关键点
     */
    @Inject(
        method = "broadcastChanges",
        at = @At("HEAD"),
        cancellable = true
    )
    private void roadweaver$onBroadcastChanges(LevelChunk chunk, CallbackInfo ci) {
//        LOGGER.info("调试用");
        if (chunk.getLevel() instanceof ServerLevel serverLevel) {
            // 检查是否需要等待道路生成
            if (ChunkRoadStateManager.isChunkPendingRoad(serverLevel, this.pos)) {
                // 注册回调：道路生成完成后再广播
                ChunkRoadStateManager.registerReleaseCallback(serverLevel, this.pos, () -> {
                    // 道路完成后，标记区块需要重新广播
                    serverLevel.getServer().execute(() -> {
                        try {
                            // 强制触发区块更新
                            LevelChunk levelChunk = serverLevel.getChunk(this.pos.x, this.pos.z);
                            if (levelChunk != null) {
                                // 标记所有section需要更新
                                for (int i = levelChunk.getMinSection(); i < levelChunk.getMaxSection(); i++) {
                                    serverLevel.getChunkSource().blockChanged(this.pos.getBlockAt(8, i * 16, 8));
                                }
                            }
                        } catch (Exception e) {
                            // 忽略异常，避免影响游戏运行
                        }
                    });
                });
                
                // 取消本次广播，等待道路生成完成
                ci.cancel();
            }
        }
    }
}

