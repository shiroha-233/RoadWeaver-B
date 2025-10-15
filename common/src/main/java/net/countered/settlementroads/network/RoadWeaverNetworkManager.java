package net.countered.settlementroads.network;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 网络管理器 - 处理客户端和服务器之间的数据包通信
 */
public class RoadWeaverNetworkManager {
    
    // 数据包ID
    public static final ResourceLocation REQUEST_DEBUG_DATA = new ResourceLocation("roadweaver", "request_debug_data");
    public static final ResourceLocation SEND_DEBUG_DATA = new ResourceLocation("roadweaver", "send_debug_data");
    
    /**
     * 注册网络处理器（在Common模块中定义，由平台实现调用）
     */
    public static void registerPackets() {
        // 服务器端接收：客户端请求调试数据
        dev.architectury.networking.NetworkManager.registerReceiver(
            dev.architectury.networking.NetworkManager.Side.C2S,
            REQUEST_DEBUG_DATA,
            (buf, context) -> {
                context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        if (player.hasPermissions(2)) {
                            // 管理员权限，发送数据
                            PacketHandler.handleDebugDataRequest(player);
                        }
                    }
                });
            }
        );
        
        // 客户端接收：服务器发送调试数据（仅在客户端环境注册）
        if (Platform.getEnvironment() == Env.CLIENT) {
            dev.architectury.networking.NetworkManager.registerReceiver(
                dev.architectury.networking.NetworkManager.Side.S2C,
                SEND_DEBUG_DATA,
                (buf, context) -> {
                    DebugDataPacket packet = DebugDataPacket.decode(buf);
                    context.queue(() -> {
                        PacketHandler.handleDebugDataResponse(packet);
                    });
                }
            );
        }
    }
    
    /**
     * 客户端请求调试数据
     */
    public static void requestDebugData() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        dev.architectury.networking.NetworkManager.sendToServer(REQUEST_DEBUG_DATA, buf);
    }
    
    /**
     * 服务器发送调试数据给客户端
     */
    public static void sendDebugData(ServerPlayer player, DebugDataPacket packet) {
        dev.architectury.networking.NetworkManager.sendToPlayer(player, SEND_DEBUG_DATA, packet.encode());
    }
}
