package net.countered.settlementroads.mixin.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.countered.settlementroads.chunk.ChunkRoadStateManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraftforge.event.ForgeEventFactory;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.List;

/**
 * 拦截区块发送给玩家的最终决策点（Forge）
 * 在 playerLoadedChunk 方法中拦截，确保等待道路生成的区块不会被发送
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    @Final
    private ThreadedLevelLightEngine lightEngine;

    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    /**
     * 拦截 playerLoadedChunk 方法
     * 这是区块实际发送给玩家的最终步骤
     */
    @Inject(
            method = "playerLoadedChunk",
            at = @At("HEAD"),
            cancellable = true
    )
    private void roadweaver$onPlayerLoadedChunk(
            ServerPlayer serverPlayer,
            MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject,
            LevelChunk levelChunk,
            CallbackInfo ci
    ) {
        ChunkPos pos = levelChunk.getPos();

        // 检查是否需要等待道路生成
        if (ChunkRoadStateManager.isChunkPendingRoad(this.level, pos)) {
            // 注册回调：道路生成完成后重新触发发送
            ChunkRoadStateManager.registerReleaseCallback(this.level, pos, () -> {
                // 道路完成后，在主线程重新尝试发送
                this.level.getServer().execute(() -> {
                    try {
                        // 标记区块需要更新，触发重新发送
                        if (mutableObject.getValue() == null) {
                            mutableObject.setValue(new ClientboundLevelChunkWithLightPacket(levelChunk, this.lightEngine, (BitSet)null, (BitSet)null));
                        }

                        serverPlayer.trackChunk(levelChunk.getPos(), (Packet)mutableObject.getValue());
                        DebugPackets.sendPoiPacketsForChunk(this.level, levelChunk.getPos());
                        List<Entity> list = Lists.newArrayList();
                        List<Entity> list1 = Lists.newArrayList();
                        ObjectIterator var6 = this.entityMap.values().iterator();

                        while(var6.hasNext()) {
                            ChunkMap.TrackedEntity chunkmap$trackedentity = (ChunkMap.TrackedEntity)var6.next();
                            Entity entity = chunkmap$trackedentity.entity;
                            if (entity != serverPlayer && entity.chunkPosition().equals(levelChunk.getPos())) {
                                chunkmap$trackedentity.updatePlayer(serverPlayer);
                                if (entity instanceof Mob && ((Mob)entity).getLeashHolder() != null) {
                                    list.add(entity);
                                }

                                if (!entity.getPassengers().isEmpty()) {
                                    list1.add(entity);
                                }
                            }
                        }

                        if (!list.isEmpty()) {
                            for(Entity entity1 : list) {
                                serverPlayer.connection.send(new ClientboundSetEntityLinkPacket(entity1, ((Mob)entity1).getLeashHolder()));
                            }
                        }

                        if (!list1.isEmpty()) {
                            for(Entity entity2 : list1) {
                                serverPlayer.connection.send(new ClientboundSetPassengersPacket(entity2));
                            }
                        }

                        ForgeEventFactory.fireChunkWatch(serverPlayer, levelChunk, this.level);
                    } catch (Exception e) {
                        // 忽略异常
                    }
                });
            });

            // 取消本次发送
            ci.cancel();
        }
    }
}